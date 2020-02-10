package dev.ginyai.flatitemeditor;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.plugin.meta.util.NonnullByDefault;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@NonnullByDefault
public class ArgItemInHand extends CommandElement {
    protected ArgItemInHand(@Nullable Text key) {
        super(key);
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        if (!(source instanceof Player)) {
            throw  args.createError(Text.of("Player Only"));
        }
        ItemStack itemStack = ((Player) source).getItemInHand(HandTypes.MAIN_HAND).orElse(ItemStack.empty());
        if (itemStack.isEmpty()) {
            throw  args.createError(Text.of("No item in hand."));
        }
        return itemStack;
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
