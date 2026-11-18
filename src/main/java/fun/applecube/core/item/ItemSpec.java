package fun.applecube.core.item;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Config/JSON description of an item. String form: {@code PAPER}, {@code itemsadder:bordo_menu:warp_pin},
 * {@code nexo:ruby}, {@code oraxen:ruby}, {@code craftengine:ns:id}, {@code hdb:1234},
 * {@code head:<base64>} (also DeluxeMenus' {@code basehead-<base64>}) or {@code model:ns:path}
 * (PAPER with an item_model). Section form adds {@code model}, {@code tooltip-style},
 * {@code custom-model-data}, {@code glow}, {@code hide-tooltip}, {@code amount} and {@code fallback}
 * (used for viewers without the resource pack, or when the custom-item plugin is missing).
 */
public record ItemSpec(String item, @Nullable String model, @Nullable String tooltipStyle,
                       @Nullable Integer customModelData, int amount, boolean glow, boolean hideTooltip,
                       @Nullable String fallback) {

    public static ItemSpec of(String item) {
        return new ItemSpec(item, null, null, null, 1, false, false, null);
    }

    public ItemSpec withFallback(@Nullable String fallback) {
        return new ItemSpec(item, model, tooltipStyle, customModelData, amount, glow, hideTooltip, fallback);
    }

    public ItemSpec withModel(@Nullable String model) {
        return new ItemSpec(item, model, tooltipStyle, customModelData, amount, glow, hideTooltip, fallback);
    }

    /** Parses a string or a section/map; returns null for missing or empty values. */
    public static @Nullable ItemSpec from(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof ConfigurationSection section) {
            return from(section.getValues(false));
        }
        if (value instanceof Map<?, ?> map) {
            Object item = first(map, "item", "material");
            if (item == null) {
                return null;
            }
            Object cmd = first(map, "custom-model-data", "custom_model_data");
            Object amount = map.get("amount");
            return new ItemSpec(String.valueOf(item),
                    string(first(map, "model", "item-model", "item_model")),
                    string(first(map, "tooltip-style", "tooltip_style")),
                    cmd instanceof Number n ? n.intValue() : null,
                    amount instanceof Number n ? Math.max(1, n.intValue()) : 1,
                    Boolean.TRUE.equals(map.get("glow")),
                    Boolean.TRUE.equals(first(map, "hide-tooltip", "hide_tooltip")),
                    string(map.get("fallback")));
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : of(s);
    }

    private static @Nullable Object first(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object v = map.get(key);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static @Nullable String string(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }
}
