package dev.ginyai.flatitemeditor.command;

import dev.ginyai.flatitemeditor.command.args.ArgItemInHand;
import dev.ginyai.flatitemeditor.command.args.ArgKey;
import dev.ginyai.flatitemeditor.command.args.ArgValue;
import dev.ginyai.flatitemeditor.util.Warp;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

public class EditItemCommand {
    private static CommandSpec spec;

    static {
        spec = CommandSpec.builder().arguments(
                new ArgItemInHand(Text.of("item")),
                new ArgKey(Text.of("key"), context -> context.getOne("item")),
                new ArgValue(Text.of("value"), context -> context.getOne("key"), context -> context.getOne("item"))
        ).executor((src, args) -> {
            if (!(src instanceof Player)) {
                throw new CommandException(Text.of("Player Only"));
            }
            Key key = args.<Key>getOne("key").get();
            Object value = args.getOne("value").get();
            if (value instanceof Warp) {
                value = ((Warp) value).get();
            }
            ItemStack itemStack = args.<ItemStack>getOne("item").get();
            DataTransactionResult result = itemStack.offer(key, value);
            //todo: handle result
            ((Player) src).setItemInHand(HandTypes.MAIN_HAND, itemStack);
            return CommandResult.success();
        }).build();
    }

    public static CommandCallable getCallable() {
        return spec;
    };


}
