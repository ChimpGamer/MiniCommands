package org.sbst.minicommands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class MiniCommand implements RawCommand {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String alias = invocation.alias();

        if (!Plugin.commandMap.containsKey(alias)) {
            source.sendMessage(
                    Component.text(String.format("Unknown message %s", alias), NamedTextColor.RED));
            return;
        }

        var command = Plugin.commandMap.get(alias);
        var server = "default";
        var locale = new Locale("en");
        var premium = true;

        if (source instanceof Player) {
            Optional<ServerConnection> potentialServer = ((Player) source).getCurrentServer();
            if (potentialServer.isPresent()) {
                server = potentialServer.get().getServerInfo().getName();
            }

            var playerLocale = ((Player) source).getEffectiveLocale();
            if (playerLocale != null) locale = playerLocale;

            premium = ((Player) source).isOnlineMode();
        }

        if (!command.messages.containsKey(server)) {
            // no server specific messages here, just get the default set
            server = "default";
        }

        Map<Locale, Map<String, List<String>>> serverMessages = command.messages.get(server);

        final var finalLocale = locale;

        List<String> messages = null;
        do {
            var potentialMessageSetLocale = serverMessages.keySet().stream().filter(key -> key.getLanguage().equals(finalLocale.getLanguage())).findFirst();
            if (potentialMessageSetLocale.isEmpty()) {
                locale = new Locale("en");
                continue;
            }

            var messageSet = serverMessages.get(potentialMessageSetLocale.get());

            if (!messageSet.containsKey("cracked")) premium = true;
            messages = messageSet.get(premium ? "premium" : "cracked");

            break;
        } while (!locale.getLanguage().equals(new Locale("en").getLanguage()));

        if (messages == null) {
            source.sendMessage(
                    Component.text("Failed to get message (no base translation specified for English?)", NamedTextColor.RED));
            return;
        }

        for (var message: messages) {
            source.sendMessage(miniMessage.deserialize(message));
        }
    }
}
