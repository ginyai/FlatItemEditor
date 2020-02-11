package dev.ginyai.flatitemeditor;

import com.google.common.reflect.TypeToken;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.*;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.util.TypeTokens;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.spongepowered.api.util.SpongeApiTranslationHelper.t;

@NonnullByDefault
public class ArgFactory {
    private static Map<TypeToken, Function<Text, CommandElement>> map = new HashMap<>();

    static {
        //add basic types
        map.put(TypeTokens.BOOLEAN_TOKEN, GenericArguments::bool);
        map.put(TypeTokens.STRING_TOKEN, GenericArguments::string);
        map.put(TypeTokens.VECTOR_3D_TOKEN, GenericArguments::vector3d);
        map.put(TypeTokens.INTEGER_TOKEN, GenericArguments::integer);
        map.put(TypeTokens.LONG_TOKEN, GenericArguments::longNum);
        map.put(TypeTokens.DOUBLE_TOKEN, GenericArguments::doubleNum);
        map.put(TypeTokens.FLOAT_TOKEN, getFloatArgument());
        map.put(TypeToken.of(BigDecimal.class), GenericArguments::bigDecimal);
        map.put(TypeToken.of(BigInteger.class), GenericArguments::bigInteger);
        map.put(TypeTokens.UUID_TOKEN, GenericArguments::uuid);
        map.put(TypeTokens.TEXT_TOKEN, key -> GenericArguments.text(key, TextSerializers.FORMATTING_CODE, false));
    }

    public static CommandElement getArg(String key, TypeToken typeToken) {
        if (map.containsKey(typeToken)) {
            return map.get(typeToken).apply(Text.of(key));
        }
        Type type = typeToken.getType();
        if (type instanceof Class) {
            Class typeClass = (Class) type;
            Function<Text, CommandElement> function = findOf(typeClass);
            if (function != null) {
                map.put(typeToken, function);
                return function.apply(Text.of(key));
            }
            if (CatalogType.class.isAssignableFrom(typeClass)) {
                function = t -> GenericArguments.onlyOne(GenericArguments.catalogedElement(t, typeClass));
                map.put(typeToken, function);
                return function.apply(Text.of(key));
            }
            function = findBuilder(typeClass);
            if (function != null) {
                map.put(typeToken, function);
                return function.apply(Text.of(key));
            }
            throw new UnsupportedOperationException(typeToken.toString());
        } else {
            throw new UnsupportedOperationException(typeToken.toString());
        }
    }

    public static <T> Tuple<CommandElement, Function<CommandContext, Optional<T>>> getter(TypeToken<T> typeToken) {
        UUID uuid = UUID.randomUUID();
        CommandElement commandElement = getArg(uuid.toString(), typeToken);
        return new Tuple<>(commandElement, commandContext -> commandContext.getOne(uuid.toString()));
    }

    @Nullable
    private static Function<Text, CommandElement> findBuilder (Class<?> clazz) {
        for (Class<?> child: clazz.getClasses()) {
            if (child.getSimpleName().equals("Builder")) {
                throw new UnsupportedOperationException("Builder");
            }
        }
        return null;
    }

    @Nullable
    private static Function<Text, CommandElement> findOf(Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            if ("of".equals(method.getName()) && Modifier.isStatic(method.getModifiers()) && method.getReturnType().equals(clazz)) {
                Class<?>[] pTypes = method.getParameterTypes();
                List<Tuple<CommandElement, Function<CommandContext, Optional<?>>>> args = Arrays.stream(pTypes).map(TypeToken::of).map(t -> Utils.<Tuple<CommandElement, Function<CommandContext, Optional<?>>>>cast(ArgFactory.getter(t))).collect(Collectors.toList());
                return text -> GenericArguments.seq(Stream.concat(args.stream().map(Tuple::getFirst), Stream.of(new SimpleElement(text, method, args.stream().map(Tuple::getSecond).collect(Collectors.toList())))).toArray(CommandElement[]::new));
            }
        }
        return null;
    }

    private static class SimpleElement extends CommandElement {
        private Method method;
        private List<Function<CommandContext, Optional<?>>> args;

        protected SimpleElement(@Nullable Text key, Method method, List<Function<CommandContext, Optional<?>>> args) {
            super(key);
            this.method = method;
            this.args = args;
        }

        @Override
        public void parse(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
            try {
                Object val = method.invoke(null, this.args.stream().map(f -> f.apply(context)).map(Optional::get).toArray());
                String key = getUntranslatedKey();
                if (key != null && val != null) {
                    if (val instanceof Iterable<?>) {
                        for (Object ent : ((Iterable<?>) val)) {
                            context.putArg(key, ent);
                        }
                    } else {
                        context.putArg(key, val);
                    }
                }
            } catch (Throwable throwable) {
                FlatItemEditorPlugin.logger().error("Error", throwable);
                throw args.createError(Text.of("Error"));
            }
        }

        @Nullable
        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            return null;
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            return Collections.emptyList();
        }

        @Override
        public Text getUsage(CommandSource src) {
            return Text.EMPTY;
        }
    }

    private static Function<Text, CommandElement> getFloatArgument() {
        try {
            for (Class<?> clazz : GenericArguments.class.getDeclaredClasses()) {
                if (clazz.getSimpleName().equals("NumericElement")) {
                    Constructor<? extends CommandElement> constructor = (Constructor<? extends CommandElement>) clazz.getDeclaredConstructor(Text.class, Function.class, BiFunction.class, Function.class);
                    constructor.setAccessible(true);
                    Function<String, Float> parseFunc = Float::parseFloat;
                    Function<String, Text> errorSupplier = input -> t("Expected an float, but input '%s' was not", input);
                    constructor.newInstance(Text.EMPTY, parseFunc, null, errorSupplier);
                    return text -> {
                        try {
                            return constructor.newInstance(text, parseFunc, null, errorSupplier);
                        } catch (Exception e) {
                            if (e instanceof RuntimeException) {
                                throw (RuntimeException) e;
                            } else {
                                throw new RuntimeException(e);
                            }
                        }
                    };
                }
            }
            throw new ClassNotFoundException("Not find");
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }
}
