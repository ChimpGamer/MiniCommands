package org.sbst.minicommands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

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
        var locale = Locale.ENGLISH;
        var premium = true;

        if (source instanceof Player) {
            Optional<ServerConnection> potentialServer = ((Player) source).getCurrentServer();
            if (potentialServer.isPresent()) {
                server = potentialServer.get().getServerInfo().getName();
            }

            var playerLocale = ((Player) source).getEffectiveLocale();
            if (playerLocale != null) locale = playerLocale.stripExtensions();

            premium = ((Player) source).isOnlineMode();
        }

        if (!command.messages.containsKey(server)) {
            // no server specific messages here, just get the default set
            server = "default";
        }

        Map<String, Map<String, List<String>>> serverMessages = command.messages.get(server);

        List<String> messages = null;

        while (true) {
            var finalLocale = locale;

            if (!serverMessages.containsKey(finalLocale.getLanguage())) {
                if (!locale.getLanguage().equals(Locale.ENGLISH.getLanguage())) {
                    locale = Locale.ENGLISH;
                    continue;
                }

                break;
            }

            var messageSet = serverMessages.get(finalLocale.getLanguage());

            if (!messageSet.containsKey("cracked")) premium = true;
            messages = messageSet.get(premium ? "premium" : "cracked");

            break;
        }

        if (messages == null) {
            source.sendMessage(
                    Component.text("Failed to get message (no base translation specified for English?)", NamedTextColor.RED));
            return;
        }

        for (var message: messages) {
            var username = source instanceof Player ? ((Player) source).getUsername() : "Console";

            var component = miniMessage.deserialize(message,
                    Placeholder.component("name", Component.text(username)));

            source.sendMessage(component);
        }
    }
}
