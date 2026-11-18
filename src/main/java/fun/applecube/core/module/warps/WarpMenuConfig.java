package fun.applecube.core.module.warps;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.config.ConfigFile;
import fun.applecube.core.item.ItemSpec;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Layout and items of the warp menus from menus/warps.yml (defaults follow the bordo pack background). */
final class WarpMenuConfig {

    final int[] warpSlots;
    final int[] tabServer;
    final int[] tabPlayers;
    final int[] decoration;
    final int info;
    final int back;
    final int myWarps;
    final int previous;
    final int page;
    final int next;
    final int create;
    final int close;
    final Map<String, ItemSpec> items = new HashMap<>();
    final List<ItemSpec> palette = new ArrayList<>();

    private WarpMenuConfig(YamlConfiguration yaml) {
        warpSlots = ints(yaml, "slots.warps");
        tabServer = ints(yaml, "slots.tab-server");
        tabPlayers = ints(yaml, "slots.tab-players");
        decoration = ints(yaml, "slots.decoration");
        info = yaml.getInt("slots.info", 4);
        back = yaml.getInt("slots.back", 45);
        myWarps = yaml.getInt("slots.my-warps", 47);
        previous = yaml.getInt("slots.previous", 48);
        page = yaml.getInt("slots.page", 49);
        next = yaml.getInt("slots.next", 50);
        create = yaml.getInt("slots.create", 51);
        close = yaml.getInt("slots.close", 53);
        ConfigurationSection section = yaml.getConfigurationSection("items");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ItemSpec spec = ItemSpec.from(section.get(key));
                if (spec != null) {
                    items.put(key, spec);
                }
            }
        }
        for (Object entry : yaml.getList("icon-palette", List.of())) {
            ItemSpec spec = ItemSpec.from(entry);
            if (spec != null) {
                palette.add(spec);
            }
        }
    }

    static WarpMenuConfig load(AppleCorePlugin plugin) {
        return new WarpMenuConfig(new ConfigFile(plugin, "menus/warps.yml", true).load().yaml());
    }

    ItemSpec item(String key) {
        ItemSpec spec = items.get(key);
        return spec != null ? spec : ItemSpec.of("PAPER");
    }

    private static int[] ints(YamlConfiguration yaml, String path) {
        return yaml.getIntegerList(path).stream().mapToInt(Integer::intValue).toArray();
    }
}
