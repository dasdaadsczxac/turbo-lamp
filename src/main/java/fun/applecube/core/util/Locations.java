package fun.applecube.core.util;

import fun.applecube.core.text.Ph;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;

/** Coordinate placeholders for messages: {@code <world> <x> <y> <z>}. */
public final class Locations {

    private Locations() {
    }

    public static TagResolver placeholders(Location location) {
        return TagResolver.resolver(
                Ph.of("world", location.getWorld() == null ? "?" : location.getWorld().getName()),
                Ph.of("x", location.getBlockX()),
                Ph.of("y", location.getBlockY()),
                Ph.of("z", location.getBlockZ()));
    }

    /**
     * Parses a coordinate; {@code ~} and {@code ~5} are relative to {@code base}. Whole numbers on the X/Z axes
     * are centred on the block, like EssentialsX does.
     */
    public static double coordinate(String raw, double base, boolean centre) {
        if (raw.startsWith("~")) {
            return base + (raw.length() == 1 ? 0 : Double.parseDouble(raw.substring(1).replace(',', '.')));
        }
        String normalized = raw.replace(',', '.');
        double value = Double.parseDouble(normalized);
        return centre && !normalized.contains(".") ? value + 0.5 : value;
    }
}
