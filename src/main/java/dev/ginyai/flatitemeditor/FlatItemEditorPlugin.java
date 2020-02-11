package dev.ginyai.flatitemeditor;

import com.google.inject.Inject;
import dev.ginyai.flatitemeditor.command.EditItemCommand;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = FlatItemEditorPlugin.PLUGIN_ID, name = FlatItemEditorPlugin.NAME, version = FlatItemEditorPlugin.VERSION)
public class FlatItemEditorPlugin {
    public static final String PLUGIN_ID = "flatitemeditor";
    public static final String NAME = "FlatItemEditor";
    public static final String VERSION = "@version@";

    private static FlatItemEditorPlugin instance;

    public static FlatItemEditorPlugin getInstance() {
        return instance;
    }

    public static Logger logger() {
        return instance.logger;
    }

    @Inject
    private Logger logger;

    public FlatItemEditorPlugin() {
        instance = this;
    }

    @Listener
    public void onPostInit(GamePostInitializationEvent event) {

    }

    @Listener
    public void onStartingServer(GameStartingServerEvent event) {
        Sponge.getCommandManager().register(this, EditItemCommand.getCallable(), "flatitemeditor", "itemeditor", "fie");
    }
}
