package fun.applecube.core.util;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/** Player lookup that respects vanish: hidden players are invisible to those who can't see them. */
public final class Players {

    public static final String SEE_VANISHED = "applecore.vanish.see";
    public static final String SEE_VANISHED_ESSENTIALS = "essentials.vanish.see";

    private Players() {
    }

    public static @Nullable Player find(CommandSender viewer, String name) {
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null && canSee(viewer, exact)) {
            return exact;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        Player best = null;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!canSee(viewer, player) || !player.getName().toLowerCase(Locale.ROOT).startsWith(lower)) {
                continue;
            }
            if (best == null || player.getName().length() < best.getName().length()) {
                best = player;
            }
        }
        return best;
    }

    public static boolean canSee(CommandSender viewer, Player target) {
        return !(viewer instanceof Player player) || player.equals(target) || player.canSee(target)
                || player.hasPermission(SEE_VANISHED) || player.hasPermission(SEE_VANISHED_ESSENTIALS);
    }

    public static List<String> names(CommandSender viewer) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> canSee(viewer, player))
                .map(Player::getName)
                .toList();
    }
}
