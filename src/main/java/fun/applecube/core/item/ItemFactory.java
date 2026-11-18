package fun.applecube.core.item;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.hook.item.ItemProvider;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Builds {@link ItemStack}s from {@link ItemSpec}s, honouring the viewer's resource-pack state. */
public final class ItemFactory {

    private final AppleCorePlugin plugin;
    private final Set<String> reported = ConcurrentHashMap.newKeySet();

    public ItemFactory(AppleCorePlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack build(ItemSpec spec, @Nullable Player viewer) {
        boolean pack = plugin.pack().usesPack(viewer);
        boolean fallbackUsed = !pack && spec.fallback() != null;
        ItemStack stack = resolve(fallbackUsed ? spec.fallback() : spec.item());
        if (stack == null && !fallbackUsed && spec.fallback() != null) {
            stack = resolve(spec.fallback());
            fallbackUsed = stack != null;
        }
        if (stack == null) {
            if (reported.add(spec.item())) {
                plugin.getLogger().warning("Unknown item '" + spec.item() + "' (missing plugin or typo); using BARRIER");
            }
            stack = new ItemStack(Material.BARRIER);
        }
        stack.setAmount(Math.max(1, Math.min(spec.amount(), stack.getMaxStackSize())));
        boolean applyPackLooks = pack && !fallbackUsed;
        stack.editMeta(meta -> {
            if (applyPackLooks && spec.model() != null) {
                meta.setItemModel(NamespacedKey.fromString(spec.model()));
            }
            NamespacedKey tooltip = null;
            if (pack) {
                tooltip = spec.tooltipStyle() == null ? plugin.pack().tooltipStyle()
                        : "none".equalsIgnoreCase(spec.tooltipStyle()) ? null
                        : NamespacedKey.fromString(spec.tooltipStyle());
            }
            if (tooltip != null) {
                meta.setTooltipStyle(tooltip);
            }
            if (spec.customModelData() != null) {
                applyCustomModelData(meta, spec.customModelData());
            }
            if (spec.glow()) {
                meta.setEnchantmentGlintOverride(true);
            }
            if (spec.hideTooltip()) {
                meta.setHideTooltip(true);
            }
        });
        return stack;
    }

    @SuppressWarnings("deprecation")
    private static void applyCustomModelData(ItemMeta meta, int value) {
        meta.setCustomModelData(value);
    }

    /** Resolves the base item of a spec (without pack decorations). */
    public @Nullable ItemStack resolve(String base) {
        String lower = base.toLowerCase(Locale.ROOT);
        if (lower.startsWith("head:")) {
            return head(base.substring(5));
        }
        if (lower.startsWith("basehead-")) {
            return head(base.substring(9));
        }
        if (lower.startsWith("model:")) {
            NamespacedKey model = NamespacedKey.fromString(base.substring(6));
            if (model == null) {
                return null;
            }
            ItemStack paper = new ItemStack(Material.PAPER);
            paper.editMeta(meta -> meta.setItemModel(model));
            return paper;
        }
        int colon = base.indexOf(':');
        if (colon > 0) {
            String prefix = lower.substring(0, colon);
            ItemProvider provider = plugin.hooks().items().byPrefix(prefix);
            if (provider != null) {
                return provider.create(base.substring(colon + 1));
            }
            if (!prefix.equals("minecraft")) {
                return null;
            }
        }
        Material material = Material.matchMaterial(base);
        return material == null || !material.isItem() || material.isAir() ? null : new ItemStack(material);
    }

    /**
     * Describes an item a player holds, for storing as an icon: a custom-item id when a provider
     * recognises it, otherwise the material plus its item_model / custom-model-data.
     */
    @SuppressWarnings("deprecation")
    public ItemSpec describe(ItemStack stack) {
        for (ItemProvider provider : plugin.hooks().items().all()) {
            String id = provider.idOf(stack);
            if (id != null && !id.isEmpty()) {
                return ItemSpec.of(provider.prefixes().getFirst() + ":" + id);
            }
        }
        ItemMeta meta = stack.getItemMeta();
        String model = meta != null && meta.hasItemModel() && meta.getItemModel() != null
                ? meta.getItemModel().toString() : null;
        Integer cmd = meta != null && meta.hasCustomModelData() ? meta.getCustomModelData() : null;
        if (meta instanceof SkullMeta skull && skull.getPlayerProfile() != null) {
            for (ProfileProperty property : skull.getPlayerProfile().getProperties()) {
                if (property.getName().equals("textures")) {
                    return ItemSpec.of("head:" + property.getValue());
                }
            }
        }
        return new ItemSpec(stack.getType().getKey().getKey(), model, null, cmd, 1, false, false, null);
    }

    private static ItemStack head(String texture) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        head.editMeta(SkullMeta.class, meta -> {
            PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(texture.getBytes()), "");
            profile.setProperty(new ProfileProperty("textures", texture));
            meta.setPlayerProfile(profile);
        });
        return head;
    }
}
