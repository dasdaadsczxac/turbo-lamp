package fun.applecube.core.permission;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * The second permission set: the exact EssentialsX node that grants the same thing as an AppleCore node.
 * EssentialsX checks {@code essentials.<command>} (where {@code /r} uses {@code essentials.msg}) plus
 * sub-nodes such as {@code .others}; AppleCore command names follow EssentialsX wherever EssentialsX has the
 * command, so most nodes map one-to-one and only the exceptions are listed here.
 * Reference: docs/PERMISSIONS.md.
 */
public final class EssentialsPermissions {

    /** AppleCore command node → EssentialsX node prefix, where they differ. */
    private static final Map<String, String> COMMAND = Map.ofEntries(
            Map.entry("reply", "msg"),
            Map.entry("day", "time.set"),
            Map.entry("night", "time.set"),
            Map.entry("sun", "weather"),
            Map.entry("storm", "weather"),
            Map.entry("rain", "weather"),
            Map.entry("playerhome", "home.others"),
            Map.entry("warpinfo", "warp"));

    /** Whole nodes with a different EssentialsX name. */
    private static final Map<String, String> EXACT = Map.ofEntries(
            Map.entry("applecore.admin", "essentials.essentials"),
            Map.entry("applecore.teleport.bypass-delay", "essentials.teleport.timer.bypass"),
            Map.entry("applecore.teleport.bypass-move", "essentials.teleport.timer.move"),
            Map.entry("applecore.cooldown.bypass", "essentials.commandcooldowns.bypass"),
            Map.entry("applecore.homes.unlimited", "essentials.sethome.multiple.unlimited"),
            Map.entry("applecore.tptoggle.bypass", "essentials.tpo"));

    /** AppleCore-only features; EssentialsX nodes (and its wildcards) must not grant these. */
    private static final Set<String> APPLECORE_ONLY = Set.of(
            "rank", "ranks", "group", "killermoney", "playerwarps", "homes", "teleport", "serverwarp", "warpicon", "warpdesc",
            "world-commands");

    private EssentialsPermissions() {
    }

    /** The EssentialsX equivalent of an AppleCore node, or null if there is none. */
    public static @Nullable String map(String node) {
        if (!node.startsWith("applecore.")) {
            return null;
        }
        String exact = EXACT.get(node);
        if (exact != null) {
            return exact;
        }
        String rest = node.substring("applecore.".length());
        if (rest.endsWith(".cooldown.bypass")) {
            return "essentials.commandcooldowns.bypass." + rest.substring(0, rest.length() - ".cooldown.bypass".length());
        }
        int dot = rest.indexOf('.');
        String head = dot < 0 ? rest : rest.substring(0, dot);
        if (APPLECORE_ONLY.contains(head)) {
            return null;
        }
        String tail = dot < 0 ? "" : rest.substring(dot);
        return "essentials." + COMMAND.getOrDefault(head, head) + tail;
    }
}
