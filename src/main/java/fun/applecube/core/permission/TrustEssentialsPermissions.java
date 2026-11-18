package fun.applecube.core.permission;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * The third permission set: TrustEssentials nodes ({@code trustessentials.<command>}, {@code .other} for other
 * players, {@code .cooldown.bypass}), so LuckPerms groups made for TrustEssentials keep working. Only commands that
 * TrustEssentials has are mapped; the table was taken from its executors. Reference: docs/PERMISSIONS.md.
 */
public final class TrustEssentialsPermissions {

    /** AppleCore command node → TrustEssentials command node. */
    private static final Map<String, String> COMMAND = Map.ofEntries(
            Map.entry("admin", "admin"));

    private TrustEssentialsPermissions() {
    }

    /** The TrustEssentials equivalent of an AppleCore node, or null if there is none. */
    public static @Nullable String map(String node) {
        if (!node.startsWith("applecore.")) {
            return null;
        }
        String rest = node.substring("applecore.".length());
        int dot = rest.indexOf('.');
        String head = dot < 0 ? rest : rest.substring(0, dot);
        String command = COMMAND.get(head);
        if (command == null) {
            return null;
        }
        String tail = dot < 0 ? "" : rest.substring(dot);
        if (tail.equals(".others")) {
            tail = ".other";
        }
        return "trustessentials." + command + tail;
    }
}
