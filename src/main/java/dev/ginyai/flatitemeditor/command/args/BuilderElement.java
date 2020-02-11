package dev.ginyai.flatitemeditor.command.args;

import com.google.common.collect.ImmutableSet;
import dev.ginyai.flatitemeditor.FlatItemEditorPlugin;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.*;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.ResettableBuilder;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@NonnullByDefault
public class BuilderElement<T extends DataSerializable, U extends ResettableBuilder<T, U>> extends CommandElement {
    private static Set<String> bypass = ImmutableSet.of("reset", "from");

    public static <T extends DataSerializable, U extends ResettableBuilder<T, U>> CommandElement get(Text text, Class<T> dataClass, Class<U> builderClass) {
        Method buildMethod = null;
        List<Method> methods = new ArrayList<>();
        for (Method method : builderClass.getMethods()) {
            if (bypass.contains(method.getName())) {
                continue;
            }
            if (method.getName().equals("build") && method.getParameters().length == 0 && method.getReturnType().equals(dataClass)) {
                buildMethod = method;
                continue;
            }
            if (method.getReturnType().equals(builderClass)) {
                methods.add(method);
            }
        }
        if (buildMethod == null) {
            throw new IllegalStateException("Unable to find build method for " + builderClass);
        }
        Map<String, Integer> renameMap = new HashMap<>();
        Map<String, BiFunction<Supplier<?>, Text, CommandElement>> map = new LinkedHashMap<>();
        for (Method method : methods) {
            BiFunction<Supplier<?>, Text, CommandElement> biFunction;
            try {
                biFunction = ArgFactory.map(method);
            } catch (Exception e) {
                FlatItemEditorPlugin.logger().warn("Failed to prepare command arg for method " + builderClass.getSimpleName() + "." + method.toString(), e);
                continue;
            }
            String name = method.getName();
            if (renameMap.containsKey(name)) {
                int i = renameMap.get(name);
                renameMap.put(name, i + 1);
                name = name + i;
            } else {
                renameMap.put(name, 1);
            }
            map.put(name, biFunction);
        }
        return new BuilderElement<>(text, dataClass, builderClass, buildMethod, map);
    }

    private Class<T> dataClass;
    private Class<U> builderClass;
    private Method buildMethod;
    private Map<String, BiFunction<Supplier<?>, Text, CommandElement>> map;

    protected BuilderElement(@Nullable Text key, Class<T> dataClass, Class<U> builderClass, Method buildMethod, Map<String, BiFunction<Supplier<?>, Text, CommandElement>> map) {
        super(key);
        this.dataClass = dataClass;
        this.builderClass = builderClass;
        this.buildMethod = buildMethod;
        this.map = map;
    }

    private CommandElement getFlags(U builder) {
        CommandFlags.Builder flagBuilder = GenericArguments.flags();
        map.forEach((s, biFunction) -> {
            flagBuilder.valueFlag(biFunction.apply(()->builder, null), s);
        });
        return flagBuilder.buildWith(GenericArguments.none());
    }

    @Override
    public void parse(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
        U builder = Sponge.getRegistry().createBuilder(builderClass);
        CommandElement flags = getFlags(builder);
        flags.parse(source, args, context);
        try {
            T t = dataClass.cast(buildMethod.invoke(builder));
            if (getKey() != null) {
                context.putArg(getKey(), t);
            }
        } catch (Exception e) {
            FlatItemEditorPlugin.logger().error("Failed to parse builder element.", e);
            throw args.createError(Text.of("Exception: " + e.toString()));
        }
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        return null;
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        U builder = Sponge.getRegistry().createBuilder(builderClass);
        CommandElement flags = getFlags(builder);
        return flags.complete(src, args, context);
    }
}
