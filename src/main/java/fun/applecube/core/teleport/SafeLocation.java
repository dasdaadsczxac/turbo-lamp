package fun.applecube.core.teleport;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

/** Finds a spot where a player can stand: two passable blocks above solid, harmless ground. */
public final class SafeLocation {

    private static final Set<Material> DANGEROUS = EnumSet.of(
            Material.LAVA, Material.FIRE, Material.SOUL_FIRE, Material.MAGMA_BLOCK, Material.CACTUS,
            Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.SWEET_BERRY_BUSH, Material.POWDER_SNOW,
            Material.WITHER_ROSE, Material.POINTED_DRIPSTONE, Material.END_PORTAL, Material.NETHER_PORTAL);

    private static final int SEARCH_RANGE = 48;

    private SafeLocation() {
    }

    /** Returns the target itself when safe, otherwise the nearest safe spot in the same column, or null. */
    public static @Nullable Location find(Location target) {
        World world = target.getWorld();
        int x = target.getBlockX();
        int y = target.getBlockY();
        int z = target.getBlockZ();
        if (isSafe(world, x, y, z)) {
            return target;
        }
        int min = world.getMinHeight() + 1;
        int max = world.getMaxHeight() - 2;
        for (int dy = 1; dy <= SEARCH_RANGE; dy++) {
            if (y + dy <= max && isSafe(world, x, y + dy, z)) {
                return moved(target, y + dy);
            }
            if (y - dy >= min && isSafe(world, x, y - dy, z)) {
                return moved(target, y - dy);
            }
        }
        return null;
    }

    /** Highest safe spot in the column (used by /top). */
    public static @Nullable Location highest(Location from) {
        World world = from.getWorld();
        Block top = world.getHighestBlockAt(from);
        int y = top.getY() + 1;
        if (y > world.getMaxHeight() - 2 || !isSafe(world, from.getBlockX(), y, from.getBlockZ())) {
            return null;
        }
        return moved(from, y);
    }

    public static boolean isSafe(World world, int x, int y, int z) {
        Block feet = world.getBlockAt(x, y, z);
        Block head = feet.getRelative(BlockFace.UP);
        Block ground = feet.getRelative(BlockFace.DOWN);
        return passable(feet) && passable(head) && solidGround(ground);
    }

    private static boolean passable(Block block) {
        return block.getType() == Material.WATER || block.isPassable() && !DANGEROUS.contains(block.getType());
    }

    private static boolean solidGround(Block block) {
        Material type = block.getType();
        if (DANGEROUS.contains(type)) {
            return false;
        }
        return type.isSolid() || Tag.LEAVES.isTagged(type) || type == Material.WATER;
    }

    private static Location moved(Location base, int y) {
        Location copy = base.clone();
        copy.setX(base.getBlockX() + 0.5);
        copy.setY(y);
        copy.setZ(base.getBlockZ() + 0.5);
        return copy;
    }
}
