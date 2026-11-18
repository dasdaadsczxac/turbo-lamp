package fun.applecube.core.text;

import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * Placeholder shortcuts for language strings ({@code <player>}, {@code <amount>} …).
 * Values are inserted unparsed, so player-controlled text can never inject MiniMessage tags.
 */
public final class Ph {

    private Ph() {
    }

    public static TagResolver of(String key, Object value) {
        if (value instanceof ComponentLike component) {
            return Placeholder.component(key, component);
        }
        return Placeholder.unparsed(key, String.valueOf(value));
    }

    /** Only for trusted strings (config values) that may contain formatting. */
    public static TagResolver parsed(String key, String value) {
        return Placeholder.parsed(key, value);
    }
}
