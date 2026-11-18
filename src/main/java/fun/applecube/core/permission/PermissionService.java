package fun.applecube.core.permission;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.text.Ph;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Permission checks for AppleCore. Every check accepts several node sets:
 * <ul>
 *     <li>AppleCore nodes: {@code applecore.<command>}, {@code applecore.<command>.others}, numeric limits such as
 *     {@code applecore.homes.limit.5} (highest wins) or {@code applecore.teleport.delay.1} (lowest wins);</li>
 *     <li>the exact EssentialsX nodes ({@link EssentialsPermissions}) and TrustEssentials nodes
 *     ({@link TrustEssentialsPermissions}), so a LuckPerms setup made for either keeps working
 *     ({@code permissions.essentials-compat} / {@code permissions.trustessentials-compat} in config.yml).</li>
 * </ul>
 * Donation ranks are LuckPerms groups holding these nodes. Limits and multipliers come only from permissions;
 * AppleCore has no rank system and no limit tables of its own.
 */
public final class PermissionService {

    public static final int UNLIMITED = Integer.MAX_VALUE;

    private final AppleCorePlugin plugin;
    private boolean essentialsCompat = true;
    private boolean trustEssentialsCompat = true;

    public PermissionService(AppleCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        essentialsCompat = plugin.getConfig().getBoolean("permissions.essentials-compat", true);
        trustEssentialsCompat = plugin.getConfig().getBoolean("permissions.trustessentials-compat", true);
    }

    /**
     * True when the AppleCore node or its EssentialsX or TrustEssentials equivalent is granted. An AppleCore node
     * explicitly set to false (e.g. negated in LuckPerms) always denies.
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
        return false;
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

    /**
     * Tails of the granted nodes starting with {@code prefix}: the player's own and inherited LuckPerms nodes, plus
     * nodes declared in plugin.yml with a default, which LuckPerms grants through hasPermission only.
     */
    private Set<String> tails(Permissible who, String prefix) {
        Set<String> out = new HashSet<>();
        for (PermissionAttachmentInfo info : who.getEffectivePermissions()) {
            if (info.getValue() && info.getPermission().startsWith(prefix)) {
                out.add(info.getPermission().substring(prefix.length()));
            }
        }
        for (Permission declared : Bukkit.getPluginManager().getPermissions()) {
            String name = declared.getName();
            if (name.startsWith(prefix) && who.hasPermission(name)) {
                out.add(name.substring(prefix.length()));
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
     * Home limit, only from permissions: the highest {@code applecore.homes.limit.<n>}, or
     * {@code applecore.homes.unlimited} (EssentialsX {@code essentials.sethome.multiple.unlimited}); 0 without them.
     * plugin.yml gives everyone {@code applecore.homes.limit.1}; LuckPerms groups raise it or negate it.
     */
    public int homeLimit(Player player) {
        if (has(player, "applecore.homes.unlimited")) {
            return UNLIMITED;
        }
        return highest(player, "applecore.homes.limit.", 0);
    }

    private static @Nullable Integer parse(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Tells the sender they lack a permission. */
    public void denied(CommandSender sender, String permission) {
        plugin.messages().send(sender, "errors.no-permission", Ph.of("permission", permission));
        if (sender instanceof Player player) {
            plugin.pack().play(player, "error");
        }
    }
}
