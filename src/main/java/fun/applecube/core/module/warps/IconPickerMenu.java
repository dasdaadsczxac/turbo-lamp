package fun.applecube.core.module.warps;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.gui.Menu;
import fun.applecube.core.item.ItemBuilder;
import fun.applecube.core.item.ItemSpec;
import fun.applecube.core.text.Ph;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Icon choice for an own player warp: the palette from menus/warps.yml, or "from hand", which stores the held item
 * as an icon (ItemsAdder/Nexo/Oraxen/CraftEngine/HeadDatabase ids, item_model or material).
 */
final class IconPickerMenu extends Menu {

    private final WarpsModule module;
    private final Warp warp;
    private final int page;

    IconPickerMenu(AppleCorePlugin plugin, WarpsModule module, Player viewer, Warp warp, int page) {
        super(plugin, viewer);
        this.module = module;
        this.warp = warp;
        this.page = page;
    }

    @Override
    protected Component title() {
        return plugin.messages().get(viewer, "menus.warps.title-icons", WarpsModule.ph(warp));
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void onOpen() {
        plugin.pack().play(viewer, "click");
    }

    private ItemStack button(String key) {
        return WarpMenu.button(plugin, module, viewer, key);
    }

    @Override
    protected void render() {
        WarpMenuConfig cfg = module.menu();
        boolean pack = plugin.pack().usesPack(viewer);
        if (!pack) {
            ItemStack filler = ItemBuilder.of(plugin.items().build(cfg.item("decoration"), viewer))
                    .name(Component.empty()).hideDetails().build();
            for (int slot : cfg.decoration) {
                set(slot, filler);
            }
        }
        for (int slot : cfg.tabServer) {
            set(slot, button("tab-server"), click -> new WarpMenu(plugin, module, viewer, WarpMenu.Mode.SERVER, 0, true).open());
        }
        for (int slot : cfg.tabPlayers) {
            set(slot, button("tab-players"), click -> new WarpMenu(plugin, module, viewer, WarpMenu.Mode.PLAYERS, 0, true).open());
        }
        set(cfg.info, button("info-icons"));

        List<ItemSpec> palette = cfg.palette;
        int perPage = Math.max(1, cfg.warpSlots.length);
        int pages = Math.max(1, (palette.size() + perPage - 1) / perPage);
        int current = Math.min(page, pages - 1);
        for (int i = 0; i < perPage; i++) {
            int index = current * perPage + i;
            if (index >= palette.size()) {
                break;
            }
            ItemSpec spec = palette.get(index);
            ItemStack icon = "owner-head".equalsIgnoreCase(spec.item())
                    ? ItemBuilder.of(Material.PLAYER_HEAD).owner(viewer).build()
                    : plugin.items().build(spec, viewer);
            boolean selected = spec.equals(warp.icon());
            set(cfg.warpSlots[i], ItemBuilder.of(icon)
                    .name(plugin.messages().get(viewer, selected ? "menus.warps.icon-selected.name" : "menus.warps.icon.name"))
                    .lore(plugin.messages().lines(viewer, "menus.warps.icon.lore"))
                    .glow(selected && !pack)
                    .hideDetails()
                    .build(), click -> choose(spec));
        }

        ItemStack hand = viewer.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            set(cfg.create, button("icon-hand-empty"));
        } else {
            set(cfg.create, ItemBuilder.of(hand).amount(1)
                    .name(plugin.messages().get(viewer, "menus.warps.icon-hand.name"))
                    .lore(plugin.messages().lines(viewer, "menus.warps.icon-hand.lore"))
                    .hideDetails()
                    .build(), click -> choose(plugin.items().describe(viewer.getInventory().getItemInMainHand())));
        }
        set(cfg.back, button(warp.isServer() ? "back-server" : "back-mine"), click -> back());
        set(cfg.close, button("close"), click -> viewer.closeInventory());
        if (current > 0) {
            set(cfg.previous, button("previous"), click -> new IconPickerMenu(plugin, module, viewer, warp, current - 1).open());
        }
        if (current < pages - 1) {
            set(cfg.next, button("next"), click -> new IconPickerMenu(plugin, module, viewer, warp, current + 1).open());
        }
    }

    private void choose(ItemSpec spec) {
        if (spec.item().equalsIgnoreCase("air")) {
            return;
        }
        warp.icon(spec);
        module.store().save();
        plugin.messages().send(viewer, "warps.icon-set", WarpsModule.ph(warp));
        plugin.pack().play(viewer, "success");
        back();
    }

    private void back() {
        new WarpMenu(plugin, module, viewer, warp.isServer() ? WarpMenu.Mode.SERVER : WarpMenu.Mode.MINE, 0, true).open();
    }
}
