package fun.applecube.core.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

/** A click inside a {@link Menu}. */
public record Click(InventoryClickEvent event) {

    public Player player() {
        return (Player) event.getWhoClicked();
    }

    public int slot() {
        return event.getRawSlot();
    }

    public ClickType type() {
        return event.getClick();
    }

    public boolean left() {
        return event.isLeftClick();
    }

    public boolean right() {
        return event.isRightClick();
    }

    public boolean shift() {
        return event.isShiftClick();
    }

    public boolean middle() {
        return event.getClick() == ClickType.MIDDLE;
    }
}
