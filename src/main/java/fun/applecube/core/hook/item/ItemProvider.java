package fun.applecube.core.hook.item;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A custom-item plugin (ItemsAdder, Nexo, Oraxen, CraftEngine, HeadDatabase). Item specs in configs
 * use {@code <prefix>:<id>}, e.g. {@code itemsadder:bordo_menu:warp_pin} or {@code nexo:ruby}.
 */
public interface ItemProvider {

    /** Display name, e.g. "ItemsAdder". */
    String name();

    /** Spec prefixes handled by this provider (lower case, without the colon). */
    List<String> prefixes();

    /** Creates a fresh copy of the item, or null if the id is unknown. */
    @Nullable ItemStack create(String id);

    /** The provider id of a stack (without prefix), or null if the stack is not from this provider. */
    @Nullable String idOf(ItemStack stack);
}
