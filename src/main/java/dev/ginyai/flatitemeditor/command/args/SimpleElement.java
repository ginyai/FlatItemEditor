package dev.ginyai.flatitemeditor.command.args;

import dev.ginyai.flatitemeditor.FlatItemEditorPlugin;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

@NonnullByDefault
public class SimpleElement<T> extends ParsingElement<T> {
    private Supplier<?> supplier;
    private Method method;
    private List<Function<CommandContext, ?>> args;

    protected SimpleElement(@Nullable Text key, Supplier<?> supplier, Method method, List<Function<CommandContext, ?>> args) {
        super(key);
        this.supplier = supplier;
        this.method = method;
        this.args = args;
    }

    @Override
    protected T parseValue(CommandSource source, CommandContext context, CommandArgs args) throws ArgumentParseException {
        try {
            //noinspection unchecked
            return (T) method.invoke(supplier.get(), this.args.stream().map(f -> f.apply(context)).toArray());
        } catch (IllegalAccessException | InvocationTargetException e) {
            FlatItemEditorPlugin.logger().error("Failed to invoke." , e);
            throw args.createError(Text.of("Exception."));
        }
    }

}
