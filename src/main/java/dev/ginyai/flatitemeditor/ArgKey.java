package dev.ginyai.flatitemeditor;

import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.text.Text;
import org.spongepowered.plugin.meta.util.NonnullByDefault;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@NonnullByDefault
public class ArgKey extends CommandElement {
    private Function<CommandContext, Optional<DataHolder>> getDataHolder;

    protected ArgKey(@Nullable Text key, Function<CommandContext, Optional<DataHolder>> getDataHolder) {
        super(key);
        this.getDataHolder = getDataHolder;
    }

    @Override
    public void parse(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
        Optional<DataHolder> optionalDataHolder = getDataHolder.apply(context);
        if (!optionalDataHolder.isPresent()) {
            throw args.createError(Text.of("Error"));
        }
        DataHolder dataHolder = optionalDataHolder.get();
        Optional<Key> optionalKey = Sponge.getRegistry().getType(Key.class, args.next());
        Key<?> key = optionalKey.orElseThrow(()-> args.createError(Text.of("unable to find key")));
        if (!dataHolder.supports(key)) {
            throw args.createError(Text.of("Not Support"));
        }
        String keyString = getUntranslatedKey();
        if (keyString != null) {
            context.putArg(keyString, key);
        }
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        return null;
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        Optional<DataHolder> optionalDataHolder = getDataHolder.apply(context);
        if (!optionalDataHolder.isPresent()) {
            return Collections.emptyList();
        } else {
            String prefix = args.nextIfPresent().orElse("").toLowerCase(Locale.ROOT);
            DataHolder dataHolder = optionalDataHolder.get();
            return Sponge.getRegistry().getAllOf(Key.class).stream()
                    .filter(dataHolder::supports)
                    .map(CatalogType::getId)
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .collect(Collectors.toList());
        }
    }
}
