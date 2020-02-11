package dev.ginyai.flatitemeditor.command.args;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.*;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.spongepowered.api.util.SpongeApiTranslationHelper.t;

@NonnullByDefault
public final class BasicArguments {

    public static CommandElement rangeInt(Text key, int min, int max) {
        return new RangeNumber<>(GenericArguments.integer(key), min, max);
    }

    private static class RangeNumber<T extends Number & Comparable<T>> extends CommandElement {
        private CommandElement warped;
        private T min;
        private T max;

        private RangeNumber(CommandElement warped, T min, T max) {
            super(warped.getKey());
            this.warped = warped;
            this.min = min;
            this.max = max;
        }

        @Override
        public void parse(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
            warped.parse(source, args, context);
            T t = context.<T>getOne(getKey()).get();
            if (t.compareTo(min) < 0 || t.compareTo(max) > 0) {
                throw args.createError(Text.of("Expected a number between " , min , " and " , max, ", but input ", t, " was not"));
            }
        }

        @Nullable
        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            return null;
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            return warped.complete(src, args, context);
        }

        @Override
        public Text getUsage(CommandSource src) {
            return warped.getUsage(src);
        }
    }

    public static CommandElement formattingText(Text key) {
        return GenericArguments.text(key, TextSerializers.FORMATTING_CODE, false);
    }

    public static CommandElement floatNum(Text key) {
        return new NumericElement<>(key, Float::parseFloat, null, input -> t("Expected a number, but input '%s' was not", input));
    }

    //Copied form `org.spongepowered.api.command.args.GenericArguments.NumericElement`
    @NonnullByDefault
    private static class NumericElement<T extends Number> extends CommandElement {
        private final Function<String, T> parseFunc;
        @Nullable
        private final BiFunction<String, Integer, T> parseRadixFunction;
        private final Function<String, Text> errorSupplier;

        protected NumericElement(Text key, Function<String, T> parseFunc, @Nullable BiFunction<String, Integer, T> parseRadixFunction,
                                 Function<String, Text> errorSupplier) {
            super(key);
            this.parseFunc = parseFunc;
            this.parseRadixFunction = parseRadixFunction;
            this.errorSupplier = errorSupplier;
        }

        @Override
        public Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            final String input = args.next();
            try {
                if (this.parseRadixFunction != null) {
                    if (input.startsWith("0x")) {
                        return this.parseRadixFunction.apply(input.substring(2), 16);
                    } else if (input.startsWith("0b")) {
                        return this.parseRadixFunction.apply(input.substring(2), 2);
                    }
                }
                return this.parseFunc.apply(input);
            } catch (NumberFormatException ex) {
                throw args.createError(this.errorSupplier.apply(input));
            }
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            return Collections.emptyList();
        }
    }

    public static CommandElement tree(Map<String, CommandElement> map) {
        return new TreeElement(map);
    }

    private static class TreeElement extends CommandElement {

        private Map<String, CommandElement> map;

        private TreeElement(Map<String, CommandElement> map) {
            super(null);
            this.map = map;
        }

        @Override
        public void parse(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
            String key = args.next();
            CommandElement commandElement = map.get(key);
            if (commandElement == null) {
                throw args.createError(Text.of("Argument was not a valid choice."));
            }
            commandElement.parse(source, args, context);
        }

        @Nullable
        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            return null;
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            String key = args.nextIfPresent().orElse("");
            String prefix = key.toLowerCase(Locale.ROOT);
            CommandElement commandElement = map.get(key);
            if (commandElement == null) {
                return map.keySet().stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix)).collect(Collectors.toList());
            } else {
                return commandElement.complete(src, args, context);
            }
        }

        @Override
        public Text getUsage(CommandSource src) {
            return super.getUsage(src);
        }
    }
}
