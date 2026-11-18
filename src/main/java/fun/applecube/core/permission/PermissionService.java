package fun.applecube.core.permission;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.text.Ph;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Permission checks for AppleCore. Every check accepts several node sets:
 * <ul>
 *     <li>AppleCore nodes: {@code applecore.<command>}, {@code applecore.<command>.others}, numeric limits such as
 *     {@code applecore.homes.limit.5} (highest wins) or {@code applecore.teleport.delay.1} (lowest wins);</li>
 *     <li>the exact EssentialsX nodes ({@link EssentialsPermissions}) and TrustEssentials nodes
 *     ({@link TrustEssentialsPermissions}), so a LuckPerms setup made for either keeps working
 *     ({@code permissions.essentials-compat} / {@code permissions.trustessentials-compat} in config.yml);</li>
 *     <li>the presets of the player's donation ranks (ranks.yml), including their limits.</li>
 * </ul>
 */
public final class PermissionService {

    public static final int UNLIMITED = Integer.MAX_VALUE;

    private final AppleCorePlugin plugin;
    private final Map<String, Integer> sethomeMultiple = new LinkedHashMap<>();
    private boolean essentialsCompat = true;
    private boolean trustEssentialsCompat = true;
    private @Nullable BiFunction<CommandSender, String, Component> rankHint;
    private @Nullable Function<Player, Set<String>> rankNodes;

    public PermissionService(AppleCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        essentialsCompat = plugin.getConfig().getBoolean("permissions.essentials-compat", true);
        trustEssentialsCompat = plugin.getConfig().getBoolean("permissions.trustessentials-compat", true);
        sethomeMultiple.clear();
        ConfigurationSection sets = plugin.getConfig().getConfigurationSection("homes.sethome-multiple");
        if (sets != null) {
            for (String set : sets.getKeys(false)) {
                sethomeMultiple.put(set, sets.getInt(set));
            }
        }
    }

    /**
     * True when the AppleCore node, its EssentialsX or TrustEssentials equivalent, or the player's donation rank
     * grants it. An AppleCore node explicitly set to false (e.g. negated in LuckPerms) always denies.
     */
    public boolean has(Permissible who, String node) {
        if (who.hasPermission(node)) {
            return true;
        }
        if (who.isPermissionSet(node)) {
            return false;
        }
        if (essentialsCompat) {
            String essentials = EssentialsPermissions.map(node);
            if (essentials != null && who.hasPermission(essentials)) {
                return true;
            }
        }
        if (trustEssentialsCompat) {
            for (String trust : TrustEssentialsPermissions.map(node)) {
                if (who.hasPermission(trust)) {
                    return true;
                }
            }
        }
        return grantedByRank(who, node);
    }

    private boolean grantedByRank(Permissible who, String node) {
        Set<String> nodes = rankNodes(who);
        if (nodes.isEmpty()) {
            return false;
        }
        if (nodes.contains(node)) {
            return true;
        }
        for (int dot = node.lastIndexOf('.'); dot > 0; dot = node.lastIndexOf('.', dot - 1)) {
            if (nodes.contains(node.substring(0, dot) + ".*")) {
                return true;
            }
        }
        return false;
    }

    /** Nodes granted by the player's donation ranks (ranks.yml); empty without the ranks module. */
    public Set<String> rankNodes(Permissible who) {
        return rankNodes != null && who instanceof Player player ? rankNodes.apply(player) : Set.of();
    }

    /** The ranks module registers the presets of the player's ranks. */
    public void rankProvider(@Nullable Function<Player, Set<String>> provider) {
        this.rankNodes = provider;
    }

    public boolean essentialsCompat() {
        return essentialsCompat;
    }

    /**
     * For opt-in flags such as {@code applecore.near.exclude}: only an explicitly granted node counts. Bukkit grants
     * undeclared nodes (all EssentialsX nodes) to operators, which must not hide every op from /near.
     */
    public boolean hasExplicit(Permissible who, String node) {
        if (who.isPermissionSet(node) && who.hasPermission(node)) {
            return true;
        }
        String essentials = essentialsCompat ? EssentialsPermissions.map(node) : null;
        if (essentials != null && who.isPermissionSet(essentials) && who.hasPermission(essentials)) {
            return true;
        }
        if (trustEssentialsCompat) {
            for (String trust : TrustEssentialsPermissions.map(node)) {
                if (who.isPermissionSet(trust) && who.hasPermission(trust)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Tails of the granted nodes starting with {@code prefix}: effective permissions plus the rank presets. */
    private List<String> tails(Permissible who, String prefix) {
        List<String> out = new ArrayList<>();
        for (PermissionAttachmentInfo info : who.getEffectivePermissions()) {
            if (info.getValue() && info.getPermission().startsWith(prefix)) {
                out.add(info.getPermission().substring(prefix.length()));
            }
        }
        for (String node : rankNodes(who)) {
            if (node.startsWith(prefix)) {
                out.add(node.substring(prefix.length()));
            }
        }
        return out;
    }

    /** Highest {@code <prefix><n>} the player has, {@link #UNLIMITED} for {@code <prefix>unlimited}, else {@code def}. */
    public int highest(Permissible who, String prefix, int def) {
        int best = Integer.MIN_VALUE;
        for (String tail : tails(who, prefix)) {
            if (tail.equals("unlimited") || tail.equals("*")) {
                return UNLIMITED;
            }
            Integer value = parse(tail);
            if (value != null && value > best) {
                best = value;
            }
        }
        return best == Integer.MIN_VALUE ? def : best;
    }

    /** Lowest {@code <prefix><n>} the player has, else {@code def}. */
    public int lowest(Permissible who, String prefix, int def) {
        int best = Integer.MAX_VALUE;
        boolean found = false;
        for (String tail : tails(who, prefix)) {
            Integer value = parse(tail);
            if (value != null && value < best) {
                best = value;
                found = true;
            }
        }
        return found ? best : def;
    }

    /** Highest decimal {@code <prefix><x>} ({@code 1.5}, {@code x1.5}, {@code 1_5}), else {@code def}. */
    public double highestDecimal(Permissible who, String prefix, double def) {
        double best = Double.NaN;
        for (String tail : tails(who, prefix)) {
            String s = tail.toLowerCase(java.util.Locale.ROOT).replace('_', '.');
            if (s.startsWith("x")) {
                s = s.substring(1);
            }
            try {
                double value = Double.parseDouble(s);
                if (Double.isFinite(value) && (Double.isNaN(best) || value > best)) {
                    best = value;
                }
            } catch (NumberFormatException ignored) {
                // not a number
            }
        }
        return Double.isNaN(best) ? def : best;
    }

    /**
     * Home limit: {@code applecore.homes.limit.<n>} / {@code applecore.homes.unlimited}, or EssentialsX's
     * {@code essentials.sethome.multiple[.<set>]} with the sets from {@code homes.sethome-multiple}
     * (same meaning as {@code sethome-multiple} in the EssentialsX config). The larger value wins.
     */
    public int homeLimit(Player player) {
        if (has(player, "applecore.homes.unlimited")) {
            return UNLIMITED;
        }
        int limit = highest(player, "applecore.homes.limit.", plugin.getConfig().getInt("homes.default-limit", 1));
        if (essentialsCompat) {
            int essentials = 0;
            if (player.hasPermission("essentials.sethome.multiple")) {
                essentials = sethomeMultiple.getOrDefault("default", 1);
            }
            for (Map.Entry<String, Integer> set : sethomeMultiple.entrySet()) {
                if (player.hasPermission("essentials.sethome.multiple." + set.getKey())) {
                    essentials = Math.max(essentials, set.getValue());
                }
            }
            limit = Math.max(limit, essentials);
        }
        return limit;
    }

    private static @Nullable Integer parse(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Ranks module registers a provider: (viewer, permission) → the cheapest rank that grants it, or null. */
    public void rankHint(@Nullable BiFunction<CommandSender, String, Component> provider) {
        this.rankHint = provider;
    }

    /** Tells the sender they lack a permission; mentions the donation rank that unlocks it if known. */
    public void denied(CommandSender sender, String permission) {
        if (rankHint != null && sender instanceof Player player
                && plugin.getConfig().getBoolean("commands.no-permission-rank-hint", true)) {
            Component rank = rankHint.apply(sender, permission);
            if (rank != null) {
                plugin.messages().send(sender, "errors.no-permission-rank", Ph.of("rank", rank), Ph.of("permission", permission));
                plugin.pack().play(player, "error");
                return;
            }
        }
        plugin.messages().send(sender, "errors.no-permission", Ph.of("permission", permission));
        if (sender instanceof Player player) {
            plugin.pack().play(player, "error");
        }
    }
}
