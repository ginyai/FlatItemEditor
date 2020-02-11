package dev.ginyai.flatitemeditor.command.args;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@NonnullByDefault
public abstract class ParsingElement<T> extends CommandElement {
    protected ParsingElement(@Nullable Text key) {
        super(key);
    }

    @Override
    public void parse(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
        T value = parseValue(source, context, args);
        if (getKey() != null) {
            context.putArg(getKey(), value);
        }
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        return null;
    }

    protected abstract T parseValue(CommandSource source, CommandContext context, CommandArgs args) throws ArgumentParseException;

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        return Collections.emptyList();
    }

    @Override
    public Text getUsage(CommandSource src) {
        return Text.EMPTY;
    }
}
