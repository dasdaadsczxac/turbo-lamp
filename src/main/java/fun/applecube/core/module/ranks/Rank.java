package fun.applecube.core.module.ranks;

import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * A donation rank from ranks.yml. {@code nodes} is the full preset: own permissions, everything inherited and the
 * generated limit nodes (applecore.homes.limit.N, applecore.playerwarps.limit.N, applecore.teleport.delay.N).
 */
public record Rank(String id, String group, String name, @Nullable String badge, double price, boolean hint,
                   double killerMoney, Set<String> nodes) {

    /** True if the preset grants the node directly or through a {@code prefix.*} entry. */
    public boolean grants(String node) {
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

    /** The {@code <rank>} text: the badge glyph for pack users, the coloured name otherwise. */
    public String display() {
        return badge == null || badge.isEmpty() ? name : "<pack><white>" + badge + "</white></pack><plain>" + name + "</plain>";
    }
}
