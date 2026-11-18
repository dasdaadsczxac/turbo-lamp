package fun.applecube.core.permission;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.text.Ph;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Essentials-style permission helpers. Numeric limits use nodes such as
 * {@code applecore.homes.limit.5} (highest wins) or {@code applecore.teleport.delay.1} (lowest wins);
 * {@code .unlimited} on a limit prefix removes the limit.
 */
public final class PermissionService {

    public static final int UNLIMITED = Integer.MAX_VALUE;

    private final AppleCorePlugin plugin;
    private @Nullable Function<String, Component> rankHint;

    public PermissionService(AppleCorePlugin plugin) {
        this.plugin = plugin;
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
        if (rankHint != null && sender instanceof Player && plugin.getConfig().getBoolean("commands.no-permission-rank-hint", true)) {
            Component rank = rankHint.apply(permission);
            if (rank != null) {
                plugin.messages().send(sender, "errors.no-permission-rank", Ph.of("rank", rank), Ph.of("permission", permission));
                plugin.pack().play((Player) sender, "error");
                return;
            }
        }
        plugin.messages().send(sender, "errors.no-permission", Ph.of("permission", permission));
        if (sender instanceof Player player) {
            plugin.pack().play(player, "error");
        }
    }
}
