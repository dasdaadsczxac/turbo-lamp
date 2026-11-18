package fun.applecube.core.gui;

import fun.applecube.core.AppleCorePlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Base chest menu. Subclasses describe the content in {@link #render()} with {@link #set}; all clicks
 * inside an open menu are cancelled by {@link MenuListener}, so items can never be taken out.
 */
public abstract class Menu implements InventoryHolder {

    protected final AppleCorePlugin plugin;
    protected final Player viewer;
    private final Map<Integer, ClickHandler> handlers = new HashMap<>();
    private Inventory inventory;

    protected Menu(AppleCorePlugin plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    protected abstract Component title();

    protected abstract int rows();

    /** Fills the inventory; called on open and on {@link #refresh()}. */
    protected abstract void render();

    /** Called once after the inventory is shown (sounds, etc.). */
    protected void onOpen() {
    }

    protected void onClose(InventoryCloseEvent event) {
    }

    public void open() {
        inventory = Bukkit.createInventory(this, rows() * 9, title());
        refresh();
        viewer.openInventory(inventory);
        onOpen();
    }

    public void refresh() {
        handlers.clear();
        inventory.clear();
        render();
    }

    protected void set(int slot, ItemStack item) {
        set(slot, item, null);
    }

    protected void set(int slot, ItemStack item, ClickHandler handler) {
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        inventory.setItem(slot, item);
        if (handler != null) {
            handlers.put(slot, handler);
        } else {
            handlers.remove(slot);
        }
    }

    void handleClick(InventoryClickEvent event) {
        ClickHandler handler = handlers.get(event.getRawSlot());
        if (handler != null) {
            handler.onClick(new Click(event));
        }
    }

    void handleClose(InventoryCloseEvent event) {
        onClose(event);
    }

    public Player viewer() {
        return viewer;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @FunctionalInterface
    public interface ClickHandler {
        void onClick(Click click);
    }
}
