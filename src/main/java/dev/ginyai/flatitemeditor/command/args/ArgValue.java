package dev.ginyai.flatitemeditor.command.args;

import dev.ginyai.flatitemeditor.FlatItemEditorPlugin;
import dev.ginyai.flatitemeditor.util.Utils;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.plugin.meta.util.NonnullByDefault;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@NonnullByDefault
public class ArgValue extends CommandElement {
    private Function<CommandContext, Optional<Key<?>>> getKey;
    private Function<CommandContext, Optional<DataHolder>> getDataHolder;

    public ArgValue(@Nullable Text key, Function<CommandContext, Optional<Key<?>>> getKey, Function<CommandContext, Optional<DataHolder>> getDataHolder) {
        super(key);
        this.getKey = getKey;
        this.getDataHolder = getDataHolder;
    }

    @Override
    public void parse(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
        Optional<Key<?>> optionalKey = getKey.apply(context);
        if (!optionalKey.isPresent()) {
            throw args.createError(Text.of("Error"));
        }
        Tuple<CommandElement, Function<CommandContext, ?>> tuple;
        try {
            tuple = getElementFor(optionalKey.get());
        } catch (UnsupportedOperationException e) {
            FlatItemEditorPlugin.logger().error("Unsupported Operation:", e);
            throw args.createError(Text.of("UnsupportedOperation: ", e.toString()));
        }
        CommandContext tempContext = new CommandContext();
        Optional<DataHolder> optionalDataHolder = getDataHolder.apply(context);
        optionalDataHolder.flatMap(dataHolder -> dataHolder.get((Key)optionalKey.get())).ifPresent(o -> tempContext.putArg("origin", o));
        tuple.getFirst().parse(source, args, tempContext);
        Object val = tuple.getSecond().apply(tempContext);
        String key = getUntranslatedKey();
        if (key != null) {
            if (val instanceof Iterable<?>) {
                for (Object ent : ((Iterable<?>) val)) {
                    context.putArg(key, ent);
                }
            } else {
                context.putArg(key, val);
            }
        }
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        return null;
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        Optional<Key<?>> optionalKey = getKey.apply(context);
        if (!optionalKey.isPresent()) {
            return Collections.emptyList();
        }
        Tuple<CommandElement, Function<CommandContext, ?>> tuple = getElementFor(optionalKey.get());
        CommandContext commandContext = new CommandContext();
        return tuple.getFirst().complete(src, args, commandContext);
    }

    public static Tuple<CommandElement, Function<CommandContext, ?>> getElementFor(Key<?> key) {
        return Utils.cast(ArgFactory.getter(key.getElementToken()));
    }


}
