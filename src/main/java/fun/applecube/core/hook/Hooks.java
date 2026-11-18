package fun.applecube.core.hook;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.hook.item.ItemProviders;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

/** Optional integrations. Every hook degrades gracefully when its plugin is missing. */
public final class Hooks {

    private final AppleCorePlugin plugin;
    private final EconomyService economy;
    private final PapiHook papi;
    private final ItemProviders items;

    public Hooks(AppleCorePlugin plugin) {
        this.plugin = plugin;
        this.economy = new EconomyService(plugin);
        this.papi = new PapiHook(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null);
        this.items = new ItemProviders(plugin);
    }

    public void enable() {
        economy.setup();
        items.detect();
    }

    public EconomyService economy() {
        return economy;
    }

    public PapiHook papi() {
        return papi;
    }

    public ItemProviders items() {
        return items;
    }

    /** Human-readable status lines for {@code /applecore info}. */
    public List<String> status() {
        List<String> out = new ArrayList<>();
        out.add("Vault economy: " + (economy.available() ? economy.providerName() : "-"));
        out.add("PlaceholderAPI: " + (papi.enabled() ? "yes" : "-"));
        out.add("Item providers: " + (items.names().isEmpty() ? "-" : String.join(", ", items.names())));
        return out;
    }

    public AppleCorePlugin plugin() {
        return plugin;
    }
}
