package dev.ginyai.flatitemeditor.command.args;

import com.google.common.reflect.TypeToken;
import dev.ginyai.flatitemeditor.util.Warp;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.*;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

@NonnullByDefault
public class ListElement<T> extends CommandElement {
    public static <T> CommandElement get(Text key, Function<CommandContext, List<T>> function, Class<T> vClass) {
        return new ListElement<>(key, vClass, function);
    }

    private Class<T> vClass;
    private Function<CommandContext, List<T>> function;

    protected ListElement(@Nullable Text key, Class<T> vClass, Function<CommandContext, List<T>> function) {
        super(key);
        this.vClass = vClass;
        this.function = function;
    }

    private CommandElement getTree(CommandContext context) {
        List<T> list = function.apply(context);
        Map<String, CommandElement> choice = new LinkedHashMap<>();
        Tuple<CommandElement, Function<CommandContext, T>> tuple = ArgFactory.getter(TypeToken.of(vClass));
        choice.put("add", GenericArguments.seq(
                GenericArguments.optionalWeak(BasicArguments.rangeInt(Text.of("index"), 0, list.size())),
                tuple.getFirst(),
                new Add<>(getKey(), list, tuple.getSecond())
        ));
        if (!list.isEmpty()) {
            choice.put("set", GenericArguments.seq(
                    BasicArguments.rangeInt(Text.of("index"), 0, list.size() - 1),
                    tuple.getFirst(),
                    new Set<>(getKey(), list, tuple.getSecond())
            ));
        }
        choice.put("del", GenericArguments.seq(
                BasicArguments.rangeInt(Text.of("index"), 0, list.size() - 1),
                new Del<>(getKey(), list)
        ));
        choice.put("clear", new Clear<>(getKey(), list));
        return BasicArguments.tree(choice);
    }

    @Override
    public void parse(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
        getTree(context).parse(source, args, context);
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        return null;
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        return getTree(context).complete(src, args, context);
    }

    @Override
    public Text getUsage(CommandSource src) {
        return Text.EMPTY;
    }

    private static class Add<T> extends ParsingElement<Warp<List<T>>> {

        private List<T> list;
        private Function<CommandContext, T> getter;

        protected Add(@Nullable Text key, List<T> list, Function<CommandContext, T> getter) {
            super(key);
            this.list = list;
            this.getter = getter;
        }

        @Override
        protected Warp<List<T>> parseValue(CommandSource source, CommandContext context, CommandArgs args) throws ArgumentParseException {
            Optional<Integer> optionalIndex = context.getOne("index");
            T t = getter.apply(context);
            if (!optionalIndex.isPresent()) {
                list.add(t);
            } else {
                list.add(optionalIndex.get(), t);
            }
            return new Warp<>(list);
        }
    }

    private static class Set<T> extends ParsingElement<Warp<List<T>>> {

        private List<T> list;
        private Function<CommandContext, T> getter;

        protected Set(@Nullable Text key, List<T> list, Function<CommandContext, T> getter) {
            super(key);
            this.list = list;
            this.getter = getter;
        }

        @Override
        protected Warp<List<T>> parseValue(CommandSource source, CommandContext context, CommandArgs args) throws ArgumentParseException {
            Optional<Integer> optionalIndex = context.getOne("index");
            T t = getter.apply(context);
            //noinspection OptionalGetWithoutIsPresent
            list.set(optionalIndex.get(), t);
            return new Warp<>(list);
        }
    }

    private static class Del<T> extends ParsingElement<Warp<List<T>>> {

        private List<T> list;

        protected Del(@Nullable Text key, List<T> list) {
            super(key);
            this.list = list;
        }

        @Override
        protected Warp<List<T>> parseValue(CommandSource source, CommandContext context, CommandArgs args) throws ArgumentParseException {
            Optional<Integer> optionalIndex = context.getOne("index");
            //noinspection OptionalGetWithoutIsPresent
            list.remove(optionalIndex.get().intValue());
            return new Warp<>(list);
        }
    }

    private static class Clear<T> extends ParsingElement<Warp<List<T>>> {

        private List<T> list;

        protected Clear(@Nullable Text key, List<T> list) {
            super(key);
            this.list = list;
        }

        @Override
        protected Warp<List<T>> parseValue(CommandSource source, CommandContext context, CommandArgs args) throws ArgumentParseException {
            list.clear();
            return new Warp<>(list);
        }
    }
}
