package net.blockhost.minicommands;

import io.github.miniplaceholders.api.MiniPlaceholders;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public class MiniPlaceholdersModule {
    public static TagResolver getTagResolver(Audience audience) {
        return MiniPlaceholders.getAudienceGlobalPlaceholders(audience);
    }
}
