package dev.ginyai.flatitemeditor.command.args;

import com.google.common.reflect.TypeToken;
import dev.ginyai.flatitemeditor.util.Utils;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.command.args.*;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.data.persistence.DataBuilder;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.ResettableBuilder;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.util.TypeTokens;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import javax.annotation.Nullable;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NonnullByDefault
public class ArgFactory {

    private static Map<TypeToken<?>, Function<Text, CommandElement>> map = new HashMap<>();

    static {
        //add basic types
        map.put(TypeTokens.BOOLEAN_TOKEN, GenericArguments::bool);
        map.put(TypeTokens.STRING_TOKEN, GenericArguments::string);
        map.put(TypeTokens.VECTOR_3D_TOKEN, GenericArguments::vector3d);
        map.put(TypeTokens.INTEGER_TOKEN, GenericArguments::integer);
        map.put(TypeTokens.LONG_TOKEN, GenericArguments::longNum);
        map.put(TypeTokens.DOUBLE_TOKEN, GenericArguments::doubleNum);
        map.put(TypeTokens.FLOAT_TOKEN, BasicArguments::floatNum);
        map.put(TypeToken.of(BigDecimal.class), GenericArguments::bigDecimal);
        map.put(TypeToken.of(BigInteger.class), GenericArguments::bigInteger);
        map.put(TypeTokens.UUID_TOKEN, GenericArguments::uuid);
        map.put(TypeTokens.TEXT_TOKEN, BasicArguments::formattingText);
    }

    public static CommandElement getArg(String key, TypeToken<?> typeToken) {
        typeToken = typeToken.wrap();
        if (map.containsKey(typeToken)) {
            return map.get(typeToken).apply(Text.of(key));
        }
        Function<Text, CommandElement> function = prepareFunction(typeToken);
        map.put(typeToken, function);
        return function.apply(Text.of(key));
    }

    private static Set<TypeToken<?>> preparing = Collections.synchronizedSet(new HashSet<>());

    public static Function<Text, CommandElement> prepareFunction(TypeToken<?> typeToken) {
        if (preparing.contains(typeToken)) {
            //loop
            throw new IllegalStateException("Loop");
        }
        preparing.add(typeToken);
        Type type = typeToken.getType();
        if (type instanceof Class) {
            Class typeClass = (Class) type;
            Function<Text, CommandElement> function = findOf(typeClass);
            if (function != null) {
                preparing.remove(typeToken);
                return function;
            }
            if (CatalogType.class.isAssignableFrom(typeClass)) {
                preparing.remove(typeToken);
                return function = t -> GenericArguments.onlyOne(GenericArguments.catalogedElement(t, typeClass));
            }
            if (DataSerializable.class.isAssignableFrom(typeClass)) {
                function = findBuilder(typeClass);
                if (function != null) {
                    preparing.remove(typeToken);
                    return function;
                }
            }
            //todo: findConstructor
            //todo: get from DataHolder then Modify
            preparing.remove(typeToken);
            throw new UnsupportedOperationException(typeToken.toString());
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class) {
                Class<?> rawClass = (Class<?>) rawType;
                if (List.class.isAssignableFrom(rawClass)) {
                    preparing.remove(typeToken);
                    return t -> ListElement.get(t, context -> context.<List>getOne("origin").orElse(new ArrayList<>()), typeToken.resolveType(List.class.getTypeParameters()[0]).getRawType());
                } else if (Set.class.equals(rawType)) {
                    //todo:Set
                } else if (Map.class.equals(rawType)) {
                    //todo:Map
                } else if (Iterator.class.equals(rawClass)) {
                    //todo: List
                }
            }
        } else if (type instanceof GenericArrayType) {
            preparing.remove(typeToken);
            throw new UnsupportedOperationException("GenericArrayType: " + typeToken.toString());
        } else if (type instanceof TypeVariable) {
            preparing.remove(typeToken);
            throw new UnsupportedOperationException("TypeVariable: " + typeToken.toString());
        } else if (type instanceof WildcardType) {
            preparing.remove(typeToken);
            throw new UnsupportedOperationException("WildcardType: " + typeToken.toString());
        } else {
            preparing.remove(typeToken);
            throw new UnsupportedOperationException(typeToken.toString());
        }
        preparing.remove(typeToken);
        throw new UnsupportedOperationException(typeToken.toString());
    }

    public static <T> Tuple<CommandElement, Function<CommandContext, T>> getter(TypeToken<T> typeToken) {
        UUID uuid = UUID.randomUUID();
        CommandElement commandElement = getArg(uuid.toString(), typeToken);
        return new Tuple<>(commandElement, commandContext -> commandContext.<T>getOne(uuid.toString()).orElseThrow(NoSuchElementException::new));
    }

    public static BiFunction<Supplier<?>, Text, CommandElement> map(Method method) {
        Parameter[] parameters = method.getParameters();
        List<Tuple<CommandElement, Function<CommandContext, ?>>> args = Arrays.stream(parameters).map(Parameter::getType).map(TypeToken::of).map(t -> Utils.<Tuple<CommandElement, Function<CommandContext, ?>>>cast(ArgFactory.getter(t))).collect(Collectors.toList());
        return (supplier, text) -> GenericArguments.seq(Stream.concat(args.stream().map(Tuple::getFirst), Stream.of(new SimpleElement<>(text, supplier, method, args.stream().map(Tuple::getSecond).collect(Collectors.toList())))).toArray(CommandElement[]::new));
    }

    @Nullable
    private static Function<Text, CommandElement> findBuilder(Class<? extends DataSerializable> clazz) {
        for (Class<?> child: clazz.getClasses()) {
            if (child.getSimpleName().equals("Builder") && ResettableBuilder.class.isAssignableFrom(child)) {
                TypeToken<?> dateType = TypeToken.of(child).resolveType(ResettableBuilder.class.getTypeParameters()[0]);
                if (clazz.equals(dateType.getType())) {
                    return t -> BuilderElement.get(t, Utils.cast(clazz), Utils.cast(child));
                }
            }
        }
        return null;
    }

    @Nullable
    private static Function<Text, CommandElement> findOf(Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            if ("of".equals(method.getName()) && Modifier.isStatic(method.getModifiers()) && method.getReturnType().equals(clazz)) {
                return t -> map(method).apply(()->null, t);
            }
        }
        return null;
    }

}
