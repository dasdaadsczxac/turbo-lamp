package fun.applecube.core.module.warps;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.module.Module;
import fun.applecube.core.permission.PermissionService;
import fun.applecube.core.text.Ph;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GUI warps (stage 4): server warps set by staff with /serverwarp and player warps created with /setwarp, in the
 * APPLECUBE bordo pack menus (or a vanilla layout for players without the pack).
 */
public final class WarpsModule extends Module {

    private final WarpStore store;
    private WarpMenuConfig menu;
    private final Map<UUID, PendingDelete> pendingDelete = new ConcurrentHashMap<>();

    public WarpsModule(AppleCorePlugin plugin) {
        super(plugin);
        this.store = new WarpStore(plugin);
    }

    @Override
    public String id() {
        return "warps";
    }

    @Override
    public void enable() {
        store.load();
        menu = WarpMenuConfig.load(plugin);
        WarpCommands.all(plugin, this).forEach(this::command);
        command(new ServerWarpCommand(plugin, this));
    }

    @Override
    public void reload() {
        menu = WarpMenuConfig.load(plugin);
    }

    public WarpStore store() {
        return store;
    }

    WarpMenuConfig menu() {
        return menu;
    }

    /** The {@code <warp>} placeholder: server warp titles are staff-set MiniMessage, player warp names stay literal. */
    static TagResolver ph(Warp warp) {
        return warp.isServer() ? Ph.parsed("warp", warp.displayName()) : Ph.of("warp", warp.displayName());
    }

    /** Staff who manage server warps: applecore.serverwarp or EssentialsX essentials.setwarp. */
    boolean canManageServer(Permissible who) {
        return plugin.permissions().has(who, "applecore.serverwarp");
    }

    /** Player-warp limit: applecore.playerwarps.limit.<n> (highest wins) / .unlimited, else warps.player-limit. */
    public int limit(Player player) {
        if (plugin.permissions().has(player, "applecore.playerwarps.unlimited")) {
            return PermissionService.UNLIMITED;
        }
        return plugin.permissions().highest(player, "applecore.playerwarps.limit.",
                plugin.getConfig().getInt("warps.player-limit", 1));
    }

    /** Whether the player may use the warp: its own permission, per-warp permissions, closed player warps. */
    public boolean canUse(Player player, Warp warp) {
        if (warp.isServer()) {
            if (warp.permission() != null && !plugin.permissions().has(player, warp.permission())) {
                return false;
            }
            return !plugin.getConfig().getBoolean("warps.per-warp-permission", false)
                    || plugin.permissions().has(player, "applecore.warps." + warp.name());
        }
        return !warp.closed() || warp.isOwner(player.getUniqueId())
                || plugin.permissions().has(player, "applecore.warp.bypass-closed");
    }

    /** Teleports with the usual warm-up; counts a visit when someone other than the owner arrives. */
    public void teleport(Player player, Warp warp) {
        if (!canUse(player, warp)) {
            plugin.messages().send(player, warp.closed() ? "warps.closed" : "warps.no-access", ph(warp));
            plugin.pack().play(player, "error");
            return;
        }
        Location to = warp.location().toLocation();
        if (to == null) {
            plugin.messages().send(player, "errors.world-not-found", Ph.of("world", warp.location().world()));
            plugin.pack().play(player, "error");
            return;
        }
        player.closeInventory();
        plugin.teleports().teleport(player, to, () -> {
            plugin.messages().send(player, "warps.teleported", ph(warp));
            if (!warp.isServer() && !warp.isOwner(player.getUniqueId())) {
                warp.visit();
                store.save();
            }
        });
    }

    /** Two-step deletion from the menu: the first Shift+RMB arms, a second one within 5 s deletes. */
    boolean confirmDelete(Player player, Warp warp) {
        long now = System.currentTimeMillis();
        PendingDelete previous = pendingDelete.get(player.getUniqueId());
        if (previous != null && previous.warp().equals(warp.name()) && now - previous.at() < 5000) {
            pendingDelete.remove(player.getUniqueId());
            return true;
        }
        pendingDelete.put(player.getUniqueId(), new PendingDelete(warp.name(), now));
        return false;
    }

    private record PendingDelete(String warp, long at) {
    }
}
