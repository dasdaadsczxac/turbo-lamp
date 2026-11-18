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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Permission checks for AppleCore. Every check accepts two node sets:
 * <ul>
 *     <li>AppleCore nodes: {@code applecore.<command>}, {@code applecore.<command>.others}, numeric limits such as
 *     {@code applecore.homes.limit.5} (highest wins) or {@code applecore.teleport.delay.1} (lowest wins);</li>
 *     <li>the exact EssentialsX nodes ({@link EssentialsPermissions}), so a LuckPerms setup made for
 *     EssentialsX keeps working ({@code permissions.essentials-compat} in config.yml).</li>
 * </ul>
 */
public final class PermissionService {

    public static final int UNLIMITED = Integer.MAX_VALUE;

    private final AppleCorePlugin plugin;
    private final Map<String, Integer> sethomeMultiple = new LinkedHashMap<>();
    private boolean essentialsCompat = true;
    private @Nullable Function<String, Component> rankHint;

    public PermissionService(AppleCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        essentialsCompat = plugin.getConfig().getBoolean("permissions.essentials-compat", true);
        sethomeMultiple.clear();
        ConfigurationSection sets = plugin.getConfig().getConfigurationSection("homes.sethome-multiple");
        if (sets != null) {
            for (String set : sets.getKeys(false)) {
                sethomeMultiple.put(set, sets.getInt(set));
            }
        }
    }

    /** True when the AppleCore node or its EssentialsX equivalent is granted. */
    public boolean has(Permissible who, String node) {
        if (who.hasPermission(node)) {
            return true;
        }
        if (!essentialsCompat) {
            return false;
        }
        String essentials = EssentialsPermissions.map(node);
        return essentials != null && who.hasPermission(essentials);
    }

    public boolean essentialsCompat() {
        return essentialsCompat;
    }

    /** Highest {@code <prefix><n>} the player has, {@link #UNLIMITED} for {@code <prefix>unlimited}, else {@code def}. */
    public int highest(Permissible who, String prefix, int def) {
        int best = Integer.MIN_VALUE;
        for (PermissionAttachmentInfo info : who.getEffectivePermissions()) {
            if (!info.getValue() || !info.getPermission().startsWith(prefix)) {
                continue;
            }
            String tail = info.getPermission().substring(prefix.length());
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
        for (PermissionAttachmentInfo info : who.getEffectivePermissions()) {
            if (!info.getValue() || !info.getPermission().startsWith(prefix)) {
                continue;
            }
            Integer value = parse(info.getPermission().substring(prefix.length()));
            if (value != null && value < best) {
                best = value;
                found = true;
            }
        }
        return found ? best : def;
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

    /** Ranks module registers a provider: permission → name of the cheapest rank that grants it. */
    public void rankHint(@Nullable Function<String, Component> provider) {
        this.rankHint = provider;
    }

    /** Tells the sender they lack a permission; mentions the donation rank that unlocks it if known. */
    public void denied(CommandSender sender, String permission) {
        if (rankHint != null && sender instanceof Player player
                && plugin.getConfig().getBoolean("commands.no-permission-rank-hint", true)) {
            Component rank = rankHint.apply(permission);
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
