package fun.applecube.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/** Routes clicks to {@link Menu}s and blocks every way of moving items while a menu is open. */
public final class MenuListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder(false);
        if (!(holder instanceof Menu menu)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            menu.handleClick(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof Menu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof Menu menu) {
            menu.handleClose(event);
        }
    }

    /** Closes every AppleCore menu (used on disable/reload so stale menus can't be clicked). */
    public static void closeAll() {
        for (HumanEntity viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.getOpenInventory().getTopInventory().getHolder(false) instanceof Menu) {
                viewer.closeInventory();
            }
        }
    }
}
