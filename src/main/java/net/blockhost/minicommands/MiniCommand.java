package net.blockhost.minicommands;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@com.velocitypowered.api.plugin.Plugin(
        id = "minicommands",
        name = "MiniCommands",
        version = "1.0-SNAPSHOT",
        description = "Simple text based responses.",
        authors = {"lewisakura"},
        dependencies = {
                @Dependency(id = "miniplaceholders", optional = true)
        }
)
public class MiniCommand {
    @Getter
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    @Getter
    private final Map<String, CommandMeta> commandMap = new HashMap<>();

    private final Path config;

    @Getter
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Inject
    public MiniCommand(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;

        this.config = dataDirectory.resolve("config.toml");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent e) {
        if (!Files.exists(dataDirectory)) {
            try {
                Files.createDirectory(dataDirectory);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Could not create data directory, skipping initialization", ex);
                return;
            }
        }

        if (!Files.exists(config)) {
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream("config.toml")) {
                assert stream != null;
                Files.copy(stream, config);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Could not copy config file and no config file is present, skipping initialization", ex);
                return;
            }
        }

        var commandManager = server.getCommandManager();
        var meta = commandManager.metaBuilder("minicommands")
                // remove the first alias
                .plugin(this)
                .build();

        commandManager.register(meta, (SimpleCommand) invocation -> {
            reload();
            invocation.source().sendMessage(miniMessage.deserialize("<green>Reloaded config!"));
        });

        loadConfig();

        registerCommands();
    }

    private void loadConfig() {
        Toml toml;
        try (BufferedReader reader = Files.newBufferedReader(config)) {
            toml = new Toml().read(reader);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Could not read config file", ex);
            return;
        }

        List<Toml> commandList = toml.getTables("commands");

        if (commandList.isEmpty()) {
            logger.log(Level.INFO, "No commands defined");
            return;
        }

        for (Toml command: commandList) {
            Map<String, Map<String, Map<String, List<String>>>> messages = new HashMap<>();

            Map<String, Object> servers = command.getTable("servers").toMap();

            servers.forEach((serverName, translationData) -> {
                @SuppressWarnings("unchecked") // this cast is guaranteed unless they mess up the format somehow...
                var translations = (Map<String, Map<String, List<String>>>) translationData;

                if (!messages.containsKey(serverName)) {
                    messages.put(serverName, new HashMap<>());
                }

                translations.forEach((locale, message) -> messages.get(serverName).put(locale, message));
            });

            this.commandMap.put(command.getString("name"), new CommandMeta(
                    command.getString("name"),
                    messages
            ));

            logger.log(Level.INFO, "Defined command " + command.getString("name"));
        }
    }

    private void unregisterAllCommands() {
        var commandManager = server.getCommandManager();
        for (var command : this.commandMap.keySet()) {
            var commandMeta = commandManager.getCommandMeta(command);
            if (commandMeta == null) {
                logger.warning("Tried to unregister command " + command + " but it is not registered!");
                return;
            }
            server.getCommandManager().unregister(commandMeta);
            this.commandMap.remove(command);
            logger.info("Unregistered command " + command);
        }
    }

    private void registerCommands() {
        var aliases = this.commandMap.keySet().toArray(String[]::new);

        var commandManager = server.getCommandManager();
        var meta = commandManager.metaBuilder(aliases[0])
                // remove the first alias
                .aliases(Arrays.stream(aliases).skip(1).toArray(String[]::new))
                .plugin(this)
                .build();

        commandManager.register(meta, new MiniCommandRegistration(this));

        logger.log(Level.INFO, "Commands registered, good to go");
    }

    public void reload() {
        unregisterAllCommands();
        loadConfig();
        registerCommands();
    }

    // <server -> <locale -> <premium/cracked -> string>>>
    protected record CommandMeta(String name, Map<String, Map<String, Map<String, List<String>>>> messages) {
    }
}
