package fun.applecube.core.item;

import fun.applecube.core.text.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

/** Fluent editing of a menu item: name, lore, flags. Text loses the implicit italic automatically. */
public final class ItemBuilder {

    private final ItemStack stack;

    private ItemBuilder(ItemStack stack) {
        this.stack = stack;
    }

    public static ItemBuilder of(ItemStack stack) {
        return new ItemBuilder(stack.clone());
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(new ItemStack(material));
    }

    public ItemBuilder name(Component name) {
        stack.editMeta(meta -> meta.displayName(Text.noItalic(name)));
        return this;
    }

    public ItemBuilder lore(List<Component> lore) {
        stack.editMeta(meta -> meta.lore(lore.stream().map(Text::noItalic).toList()));
        return this;
    }

    public ItemBuilder amount(int amount) {
        stack.setAmount(Math.max(1, Math.min(amount, stack.getMaxStackSize())));
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        if (glow) {
            stack.editMeta(meta -> meta.setEnchantmentGlintOverride(true));
        }
        return this;
    }

    /** Hides vanilla tooltip noise (attributes, enchantments …) so menu lore stays clean. */
    public ItemBuilder hideDetails() {
        stack.editMeta(meta -> meta.addItemFlags(ItemFlag.values()));
        return this;
    }

    public ItemBuilder owner(OfflinePlayer owner) {
        if (stack.getType() == Material.PLAYER_HEAD) {
            stack.editMeta(SkullMeta.class, meta -> meta.setOwningPlayer(owner));
        }
        return this;
    }

    public ItemStack build() {
        return stack;
    }
}
