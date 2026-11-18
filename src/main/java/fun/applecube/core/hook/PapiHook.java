package fun.applecube.core.hook;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;

/** PlaceholderAPI bridge; the PAPI class is only touched when the plugin is installed. */
public final class PapiHook {

    private final boolean enabled;

    public PapiHook(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }

    public String apply(OfflinePlayer player, String text) {
        if (!enabled || text.indexOf('%') < 0) {
            return text;
        }
        return Bridge.apply(player, text);
    }

    private static final class Bridge {
        static String apply(OfflinePlayer player, String text) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }
    }
}
