package fun.applecube.core.module.ranks;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.config.ConfigFile;
import fun.applecube.core.module.Module;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Donation ranks (stage 5): permission presets from ranks.yml. A player has a rank through the LuckPerms group
 * ({@code group.<group>}) or {@code applecore.rank.<id>}; the preset then counts in every AppleCore permission check
 * and limit, so a donation shop only has to put the player into the group.
 */
public final class RanksModule extends Module {

    private final Map<String, Rank> ranks = new LinkedHashMap<>();
    private final List<String> registered = new ArrayList<>();
    private ConfigFile file;

    public RanksModule(AppleCorePlugin plugin) {
        super(plugin);
    }

    @Override
    public String id() {
        return "ranks";
    }

    @Override
    public void enable() {
        file = new ConfigFile(plugin, "ranks.yml", false);
        load();
        command(new RanksCommand(plugin, this));
        plugin.permissions().rankNodes(this::nodes);
        plugin.permissions().rankHint(this::hint);
    }

    @Override
    public void reload() {
        load();
    }

    @Override
    public void disable() {
        plugin.permissions().rankNodes(null);
        plugin.permissions().rankHint(null);
        unregister();
    }

    ConfigFile file() {
        return file;
    }

    private void load() {
        YamlConfiguration yaml = file.load().yaml();
        ConfigurationSection section = yaml.getConfigurationSection("ranks");
        Map<String, ConfigurationSection> raw = new LinkedHashMap<>();
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection rank = section.getConfigurationSection(id);
                if (rank != null) {
                    raw.put(id.toLowerCase(Locale.ROOT), rank);
                }
            }
        }
        ranks.clear();
        Map<String, Rank> built = new LinkedHashMap<>();
        for (String id : raw.keySet()) {
            ranks.put(id, build(id, raw, built, new HashSet<>()));
        }
        unregister();
        for (Rank rank : ranks.values()) {
            Map<String, Boolean> children = new LinkedHashMap<>();
            for (String node : rank.nodes()) {
                children.put(node, true);
            }
            String node = "applecore.rank." + rank.id();
            Bukkit.getPluginManager().addPermission(new Permission(node, "AppleCore rank " + rank.id(), PermissionDefault.FALSE, children));
            registered.add(node);
        }
        plugin.getLogger().info("Ranks: " + String.join(", ", ranks.keySet()));
    }

    private Rank build(String id, Map<String, ConfigurationSection> raw, Map<String, Rank> built, Set<String> visiting) {
        Rank done = built.get(id);
        if (done != null) {
            return done;
        }
        visiting.add(id);
        ConfigurationSection cfg = raw.get(id);
        Set<String> nodes = new LinkedHashSet<>();
        String parent = cfg.getString("inherits");
        if (parent != null && !parent.isBlank()) {
            String parentId = parent.toLowerCase(Locale.ROOT);
            if (!raw.containsKey(parentId)) {
                plugin.getLogger().warning("ranks.yml: " + id + " inherits unknown rank '" + parent + "'");
            } else if (visiting.contains(parentId)) {
                plugin.getLogger().warning("ranks.yml: inheritance loop at " + id);
            } else {
                nodes.addAll(build(parentId, raw, built, visiting).nodes());
            }
        }
        for (String node : cfg.getStringList("permissions")) {
            nodes.add(node.trim().toLowerCase(Locale.ROOT));
        }
        if (cfg.isInt("homes")) {
            nodes.add("applecore.homes.limit." + cfg.getInt("homes"));
        }
        if (cfg.isInt("warps")) {
            nodes.add("applecore.playerwarps.limit." + cfg.getInt("warps"));
        }
        if (cfg.isInt("teleport-delay")) {
            nodes.add("applecore.teleport.delay." + cfg.getInt("teleport-delay"));
        }
        Rank rank = new Rank(id, cfg.getString("group", id), cfg.getString("name", id), cfg.getString("badge"),
                cfg.getDouble("price", 0), cfg.getBoolean("hint", true),
                parseMultiplier(cfg.getString("killermoney", "1")), Set.copyOf(nodes));
        built.put(id, rank);
        return rank;
    }

    /** "x1.5", "1.5", "x2" → 1.5 / 2; anything else → 1. */
    public static double parseMultiplier(@Nullable String raw) {
        if (raw == null) {
            return 1;
        }
        String s = raw.trim().toLowerCase(Locale.ROOT).replace(',', '.');
        if (s.startsWith("x") || s.startsWith("х")) {
            s = s.substring(1);
        }
        try {
            double value = Double.parseDouble(s);
            return value > 0 && Double.isFinite(value) ? value : 1;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void unregister() {
        for (String node : registered) {
            Bukkit.getPluginManager().removePermission(node);
        }
        registered.clear();
    }

    public List<Rank> all() {
        return List.copyOf(ranks.values());
    }

    public @Nullable Rank get(String id) {
        return ranks.get(id.toLowerCase(Locale.ROOT));
    }

    /** Whether the player holds the rank: applecore.rank.<id> or an explicitly set LuckPerms group.<group>. */
    public boolean has(Player player, Rank rank) {
        if (player.hasPermission("applecore.rank." + rank.id())) {
            return true;
        }
        // isPermissionSet: Bukkit grants undeclared nodes such as group.* to operators by default
        String group = "group." + rank.group().toLowerCase(Locale.ROOT);
        return player.isPermissionSet(group) && player.hasPermission(group);
    }

    /** The player's ranks in ranks.yml order. */
    public List<Rank> of(Player player) {
        List<Rank> out = new ArrayList<>();
        for (Rank rank : ranks.values()) {
            if (has(player, rank)) {
                out.add(rank);
            }
        }
        return out;
    }

    /** The last rank in ranks.yml order that the player holds (shown in placeholders), or null. */
    public @Nullable Rank top(Player player) {
        List<Rank> held = of(player);
        return held.isEmpty() ? null : held.getLast();
    }

    private Set<String> nodes(Player player) {
        List<Rank> held = of(player);
        if (held.isEmpty()) {
            return Set.of();
        }
        if (held.size() == 1) {
            return held.getFirst().nodes();
        }
        Set<String> union = new HashSet<>();
        for (Rank rank : held) {
            union.addAll(rank.nodes());
        }
        return union;
    }

    /** Highest KillerMoney multiplier among the player's ranks (1 without a rank). */
    public double killerMoney(Player player) {
        double best = 1;
        for (Rank rank : of(player)) {
            best = Math.max(best, rank.killerMoney());
        }
        return best;
    }

    /** The cheapest rank (with hint: true) whose preset grants the permission, rendered for the viewer. */
    private @Nullable Component hint(CommandSender viewer, String permission) {
        Rank best = null;
        for (Rank rank : ranks.values()) {
            if (rank.hint() && rank.grants(permission) && (best == null || rank.price() < best.price())) {
                best = rank;
            }
        }
        return best == null ? null : plugin.messages().parse(viewer, best.display());
    }
}
