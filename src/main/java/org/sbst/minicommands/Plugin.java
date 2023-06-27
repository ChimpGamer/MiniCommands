package org.sbst.minicommands;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

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
        authors = {"lewisakura"}
)
public class Plugin {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    protected static Map<String, Command> commandMap = new HashMap<>();

    @Inject
    public Plugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent e) {
        Path config = dataDirectory.resolve("config.toml");

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

        Toml toml;
        try (BufferedReader reader = Files.newBufferedReader(config)) {
            toml = new Toml().read(reader);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Could not read config file, skipping initialization", ex);
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

            Plugin.commandMap.put(command.getString("name"), new Command(
                    command.getString("name"),
                    messages
            ));

            logger.log(Level.INFO, "Defined command " + command.getString("name"));
        }

        var aliases = Plugin.commandMap.keySet().toArray(String[]::new);

        var commandManager = server.getCommandManager();
        var meta = commandManager.metaBuilder(aliases[0])
                // remove the first alias
                .aliases(Arrays.stream(aliases).skip(1).toArray(String[]::new))
                .plugin(this)
                .build();

        commandManager.register(meta, new MiniCommand());

        logger.log(Level.INFO, "Commands registered, good to go");
    }

    protected static class Command {
        public String name;
        // <server -> <locale -> <premium/cracked -> string>>>
        public Map<String, Map<String, Map<String, List<String>>>> messages;

        private Command(String name, Map<String, Map<String, Map<String, List<String>>>> messages) {
            this.name = name;
            this.messages = messages;
        }
    }
}
