package fun.applecube.core.module.warps;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.gui.Click;
import fun.applecube.core.gui.Menu;
import fun.applecube.core.item.ItemBuilder;
import fun.applecube.core.item.ItemSpec;
import fun.applecube.core.message.Messages;
import fun.applecube.core.permission.PermissionService;
import fun.applecube.core.text.Ph;
import fun.applecube.core.text.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The warp window: "server" and "players" tabs plus "my warps", 28 warps per page. With the resource pack the
 * window background, tabs and cells come from the pack ({@code \uEB98}/{@code \uEB99}); without it the same
 * slots get vanilla items and glass decoration.
 */
final class WarpMenu extends Menu {

    enum Mode { SERVER, PLAYERS, MINE }

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault());

    private final WarpsModule module;
    private final Mode mode;
    private final int page;
    private final boolean quiet;

    WarpMenu(AppleCorePlugin plugin, WarpsModule module, Player viewer, Mode mode, int page, boolean quiet) {
        super(plugin, viewer);
        this.module = module;
        this.mode = mode;
        this.page = page;
        this.quiet = quiet;
    }

    static void open(AppleCorePlugin plugin, WarpsModule module, Player viewer, Mode mode) {
        new WarpMenu(plugin, module, viewer, mode, 0, false).open();
    }

    private void switchTo(Mode target, int targetPage) {
        new WarpMenu(plugin, module, viewer, target, targetPage, true).open();
    }

    @Override
    protected Component title() {
        return plugin.messages().get(viewer, "menus.warps.title-" + mode.name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void onOpen() {
        plugin.pack().play(viewer, quiet ? "click" : "menu-open");
    }

    // ---- items -------------------------------------------------------------------------------------

    private Messages messages() {
        return plugin.messages();
    }

    /** A configured button with name/lore from {@code menus.warps.<key>}. */
    ItemStack button(String key, TagResolver... placeholders) {
        return button(plugin, module, viewer, key, placeholders);
    }

    static ItemStack button(AppleCorePlugin plugin, WarpsModule module, Player viewer, String key,
                            TagResolver... placeholders) {
        ItemStack base = plugin.items().build(module.menu().item(key), viewer);
        return ItemBuilder.of(base)
                .name(plugin.messages().get(viewer, "menus.warps." + key + ".name", placeholders))
                .lore(plugin.messages().lines(viewer, "menus.warps." + key + ".lore", placeholders))
                .hideDetails()
                .build();
    }

    private List<Warp> visibleWarps() {
        return switch (mode) {
            case SERVER -> module.store().serverWarps().stream().filter(w -> module.canUse(viewer, w)
                    || plugin.permissions().has(viewer, "applecore.serverwarp")).toList();
            case PLAYERS -> module.store().playerWarps(null);
            case MINE -> module.store().playerWarps(viewer.getUniqueId());
        };
    }

    static String ownerName(Warp warp) {
        if (warp.owner() != null) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(warp.owner());
            if (owner.getName() != null) {
                return owner.getName();
            }
        }
        return warp.ownerName() == null ? "?" : warp.ownerName();
    }

    static TagResolver warpPlaceholders(AppleCorePlugin plugin, Player viewer, Warp warp) {
        Messages m = plugin.messages();
        return TagResolver.resolver(
                WarpsModule.ph(warp),
                Ph.of("owner", ownerName(warp)),
                Ph.of("visits", warp.visits()),
                Ph.of("likes", warp.likes()),
                Ph.of("date", DATE.format(Instant.ofEpochMilli(warp.created()))),
                Ph.of("world", warp.location().world()),
                Ph.of("x", warp.location().blockX()),
                Ph.of("y", warp.location().blockY()),
                Ph.of("z", warp.location().blockZ()),
                Ph.of("state", m.get(viewer, warp.closed() ? "menus.warps.state-closed" : "menus.warps.state-open")),
                Ph.of("like-action", m.get(viewer, warp.likedBy(viewer.getUniqueId()) ? "menus.warps.unlike" : "menus.warps.like")));
    }

    /** Lore where a line consisting of {@code <description>} expands to the warp's description lines. */
    static List<Component> lore(AppleCorePlugin plugin, Player viewer, String key, Warp warp, TagResolver placeholders) {
        Messages m = plugin.messages();
        List<Component> out = new ArrayList<>();
        for (String line : m.rawList(key)) {
            if (line.trim().equals("<description>")) {
                for (String text : warp.description()) {
                    out.add(Text.noItalic(warp.isServer()
                            ? m.parse(viewer, "<text>" + text, placeholders)
                            : m.parse(viewer, "<text><line>", Ph.of("line", Text.userFormatted(text)))));
                }
                continue;
            }
            out.add(Text.noItalic(m.parse(viewer, line, placeholders)));
        }
        return out;
    }

    static ItemStack icon(AppleCorePlugin plugin, WarpsModule module, Player viewer, Warp warp) {
        ItemSpec spec = warp.icon() != null ? warp.icon()
                : module.menu().item(warp.isServer() ? "server-default" : "player-default");
        if ("owner-head".equalsIgnoreCase(spec.item()) && warp.owner() != null) {
            return ItemBuilder.of(Material.PLAYER_HEAD).owner(Bukkit.getOfflinePlayer(warp.owner())).build();
        }
        if ("owner-head".equalsIgnoreCase(spec.item())) {
            return new ItemStack(Material.PLAYER_HEAD);
        }
        return plugin.items().build(spec, viewer);
    }

    private ItemStack warpItem(Warp warp) {
        String kind = warp.isServer() ? "server-warp" : warp.isOwner(viewer.getUniqueId()) ? "own-warp" : "player-warp";
        TagResolver placeholders = warpPlaceholders(plugin, viewer, warp);
        List<Component> lore = lore(plugin, viewer, "menus.warps." + kind + ".lore", warp, placeholders);
        if (warp.isServer() && module.canManageServer(viewer)) {
            lore = new ArrayList<>(lore);
            lore.addAll(lore(plugin, viewer, "menus.warps.server-warp.staff-lore", warp, placeholders));
        }
        return ItemBuilder.of(icon(plugin, module, viewer, warp))
                .amount(1)
                .name(messages().get(viewer, "menus.warps." + kind + ".name", placeholders))
                .lore(lore)
                .hideDetails()
                .build();
    }

    // ---- layout ------------------------------------------------------------------------------------

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
        ItemStack serverTab = ItemBuilder.of(button("tab-server")).glow(!pack && mode == Mode.SERVER).build();
        for (int slot : cfg.tabServer) {
            set(slot, serverTab, click -> {
                if (mode != Mode.SERVER) {
                    switchTo(Mode.SERVER, 0);
                }
            });
        }
        ItemStack playersTab = ItemBuilder.of(button("tab-players")).glow(!pack && mode != Mode.SERVER).build();
        for (int slot : cfg.tabPlayers) {
            set(slot, playersTab, click -> {
                if (mode != Mode.PLAYERS) {
                    switchTo(Mode.PLAYERS, 0);
                }
            });
        }
        set(cfg.info, button(mode == Mode.SERVER ? "info-server" : "info-players"));

        List<Warp> warps = visibleWarps();
        int perPage = Math.max(1, cfg.warpSlots.length);
        int pages = Math.max(1, (warps.size() + perPage - 1) / perPage);
        int current = Math.min(page, pages - 1);
        for (int i = 0; i < perPage; i++) {
            int index = current * perPage + i;
            if (index >= warps.size()) {
                break;
            }
            Warp warp = warps.get(index);
            set(cfg.warpSlots[i], warpItem(warp), click -> onWarpClick(warp, click));
        }
        if (warps.isEmpty()) {
            set(cfg.warpSlots[Math.min(cfg.warpSlots.length - 1, 10)], button(mode == Mode.MINE ? "empty-mine" : "empty"));
        }

        set(cfg.back, button("back"), click -> {
            viewer.closeInventory();
            String command = plugin.getConfig().getString("warps.back-command", "menu");
            if (command != null && !command.isBlank()) {
                viewer.performCommand(command);
            }
        });
        set(cfg.close, button("close"), click -> viewer.closeInventory());
        if (current > 0) {
            set(cfg.previous, button("previous"), click -> switchTo(mode, current - 1));
        }
        if (current < pages - 1) {
            set(cfg.next, button("next"), click -> switchTo(mode, current + 1));
        }
        set(cfg.page, ItemBuilder.of(button("page", Ph.of("page", current + 1), Ph.of("pages", pages)))
                .amount(Math.min(64, current + 1)).build());

        if (mode != Mode.SERVER) {
            int limit = module.limit(viewer);
            TagResolver counts = TagResolver.resolver(
                    Ph.of("count", module.store().countOwned(viewer.getUniqueId())),
                    Ph.of("limit", limit == PermissionService.UNLIMITED ? messages().get(viewer, "common.unlimited")
                            : Component.text(limit)));
            set(cfg.myWarps, button(mode == Mode.MINE ? "all-warps" : "my-warps", counts),
                    click -> switchTo(mode == Mode.MINE ? Mode.PLAYERS : Mode.MINE, 0));
            set(cfg.create, button("create", counts), click -> {
                viewer.closeInventory();
                plugin.pack().play(viewer, "click");
                viewer.sendMessage(messages().get(viewer, "menus.warps.create-hint")
                        .clickEvent(ClickEvent.suggestCommand("/setwarp ")));
            });
        }
    }

    // ---- clicks ------------------------------------------------------------------------------------

    private void onWarpClick(Warp warp, Click click) {
        boolean own = warp.isOwner(viewer.getUniqueId());
        boolean manage = own || (warp.isServer() && module.canManageServer(viewer));
        if (manage) {
            if (click.shift() && click.right()) {
                if (module.confirmDelete(viewer, warp)) {
                    module.store().remove(warp);
                    messages().send(viewer, "warps.deleted", WarpsModule.ph(warp));
                    plugin.pack().play(viewer, "success");
                    refresh();
                } else {
                    messages().send(viewer, "warps.delete-confirm", WarpsModule.ph(warp));
                    plugin.pack().play(viewer, "error");
                }
                return;
            }
            if (own && click.shift() && click.left()) {
                warp.closed(!warp.closed());
                module.store().save();
                messages().send(viewer, warp.closed() ? "warps.now-closed" : "warps.now-open", WarpsModule.ph(warp));
                plugin.pack().play(viewer, "click");
                refresh();
                return;
            }
            if (click.right() && !click.shift()) {
                new IconPickerMenu(plugin, module, viewer, warp, 0).open();
                return;
            }
        } else if (!warp.isServer() && click.right()) {
            boolean liked = warp.toggleLike(viewer.getUniqueId());
            module.store().save();
            messages().send(viewer, liked ? "warps.liked" : "warps.unliked", WarpsModule.ph(warp));
            plugin.pack().play(viewer, liked ? "success" : "click");
            refresh();
            return;
        }
        if (click.left()) {
            module.teleport(viewer, warp);
        }
    }
}
