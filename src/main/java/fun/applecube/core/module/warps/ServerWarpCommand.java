package fun.applecube.core.module.warps;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.command.AbstractCommand;
import fun.applecube.core.command.CommandContext;
import fun.applecube.core.command.CommandException;
import fun.applecube.core.item.ItemSpec;
import fun.applecube.core.storage.StoredLocation;
import fun.applecube.core.text.Ph;
import fun.applecube.core.text.Text;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /serverwarp: server warps for staff. Permission applecore.serverwarp, which also accepts EssentialsX's
 * essentials.setwarp (in EssentialsX that node creates the shared warps). Titles and descriptions are trusted
 * MiniMessage (or &amp;-codes).
 */
final class ServerWarpCommand extends AbstractCommand {

    private static final List<String> ACTIONS = List.of("set", "delete", "rename", "title", "icon", "desc", "order", "perm", "list");

    private final WarpsModule module;

    ServerWarpCommand(AppleCorePlugin plugin, WarpsModule module) {
        super(plugin, "serverwarp", "swarp");
        this.module = module;
    }

    @Override
    public void execute(CommandContext ctx) throws CommandException {
        if (!ctx.has(0)) {
            throw ctx.usage();
        }
        switch (ctx.arg(0).toLowerCase(Locale.ROOT)) {
            case "set", "create" -> set(ctx);
            case "delete", "del", "remove" -> delete(ctx);
            case "rename" -> rename(ctx);
            case "title" -> title(ctx);
            case "icon" -> icon(ctx);
            case "desc" -> desc(ctx);
            case "order" -> order(ctx);
            case "perm", "permission" -> perm(ctx);
            case "list" -> list(ctx);
            default -> throw ctx.usage();
        }
    }

    private Warp serverWarp(CommandContext ctx) throws CommandException {
        Warp warp = WarpCommands.existing(module, ctx, 1);
        if (!warp.isServer()) {
            throw new CommandException("warps.server.not-server", WarpsModule.ph(warp), Ph.of("owner", WarpMenu.ownerName(warp)));
        }
        return warp;
    }

    private void set(CommandContext ctx) throws CommandException {
        Player self = ctx.player();
        if (!ctx.has(1)) {
            throw ctx.usage();
        }
        String name = ctx.arg(1);
        if (!WarpCommands.namePattern(plugin).matcher(name).matches()) {
            throw new CommandException("warps.invalid-name", Ph.of("warp", name));
        }
        Warp existing = module.store().get(name);
        if (existing != null) {
            if (!existing.isServer()) {
                throw new CommandException("warps.name-taken", Ph.of("warp", name));
            }
            existing.location(StoredLocation.of(self.getLocation()));
            module.store().save();
            ctx.reply("warps.server.moved", WarpsModule.ph(existing));
            return;
        }
        Warp warp = new Warp(name, Warp.Type.SERVER, StoredLocation.of(self.getLocation()), null, null);
        warp.order(module.store().serverWarps().stream().mapToInt(Warp::order).max().orElse(0) + 1);
        module.store().add(warp);
        ctx.reply("warps.server.created", WarpsModule.ph(warp));
        plugin.pack().play(self, "success");
    }

    private void delete(CommandContext ctx) throws CommandException {
        Warp warp = serverWarp(ctx);
        module.store().remove(warp);
        ctx.reply("warps.deleted", WarpsModule.ph(warp));
    }

    private void rename(CommandContext ctx) throws CommandException {
        Warp warp = serverWarp(ctx);
        if (!ctx.has(2)) {
            throw ctx.usage();
        }
        String newName = ctx.arg(2);
        if (!WarpCommands.namePattern(plugin).matcher(newName).matches()) {
            throw new CommandException("warps.invalid-name", Ph.of("warp", newName));
        }
        Warp other = module.store().get(newName);
        if (other != null && other != warp) {
            throw new CommandException("warps.name-taken", Ph.of("warp", newName));
        }
        String old = warp.name();
        module.store().rename(warp, newName);
        ctx.reply("warps.server.renamed", Ph.of("old", old), Ph.of("warp", warp.name()));
    }

    /** Display name in menus and messages; no text resets it to the warp name. */
    private void title(CommandContext ctx) throws CommandException {
        Warp warp = serverWarp(ctx);
        String raw = ctx.joinFrom(2).strip();
        warp.displayName(raw.isEmpty() ? warp.name() : Text.legacyToMiniMessage(raw));
        module.store().save();
        ctx.reply("warps.server.title-set", Ph.of("name", warp.name()), WarpsModule.ph(warp));
    }

    /** Icon from the hand, from an item id (with an optional vanilla fallback for players without the pack), or the picker. */
    private void icon(CommandContext ctx) throws CommandException {
        Warp warp = serverWarp(ctx);
        if (ctx.has(2)) {
            String item = ctx.arg(2);
            if (plugin.items().resolve(item) == null) {
                throw new CommandException("warps.server.unknown-item", Ph.of("item", item));
            }
            String fallback = ctx.has(3) ? ctx.arg(3) : null;
            if (fallback != null && plugin.items().resolve(fallback) == null) {
                throw new CommandException("warps.server.unknown-item", Ph.of("item", fallback));
            }
            warp.icon(ItemSpec.of(item).withFallback(fallback));
        } else {
            Player self = ctx.player();
            ItemStack hand = self.getInventory().getItemInMainHand();
            if (hand.getType().isAir()) {
                new IconPickerMenu(plugin, module, self, warp, 0).open();
                return;
            }
            warp.icon(plugin.items().describe(hand));
        }
        module.store().save();
        ctx.reply("warps.icon-set", WarpsModule.ph(warp));
    }

    /** Description lines separated by |; no text clears it. Formatting is allowed (staff only). */
    private void desc(CommandContext ctx) throws CommandException {
        Warp warp = serverWarp(ctx);
        String raw = ctx.joinFrom(2).strip();
        if (raw.isEmpty()) {
            warp.description(List.of());
            module.store().save();
            ctx.reply("warps.description-cleared", WarpsModule.ph(warp));
            return;
        }
        List<String> lines = new ArrayList<>();
        for (String line : raw.split("\\|")) {
            lines.add(Text.legacyToMiniMessage(line.strip()));
        }
        warp.description(lines);
        module.store().save();
        ctx.reply("warps.description-set", WarpsModule.ph(warp));
    }

    private void order(CommandContext ctx) throws CommandException {
        Warp warp = serverWarp(ctx);
        if (!ctx.has(2)) {
            throw ctx.usage();
        }
        warp.order(ctx.integer(2, -10000, 10000));
        module.store().save();
        ctx.reply("warps.server.order-set", WarpsModule.ph(warp), Ph.of("order", warp.order()));
    }

    private void perm(CommandContext ctx) throws CommandException {
        Warp warp = serverWarp(ctx);
        String node = ctx.has(2) ? ctx.arg(2) : "";
        if (node.isEmpty() || node.equalsIgnoreCase("none") || node.equals("-")) {
            warp.permission(null);
            module.store().save();
            ctx.reply("warps.server.perm-cleared", WarpsModule.ph(warp));
            return;
        }
        warp.permission(node);
        module.store().save();
        ctx.reply("warps.server.perm-set", WarpsModule.ph(warp), Ph.of("permission", node));
    }

    private void list(CommandContext ctx) {
        List<Warp> warps = module.store().serverWarps();
        if (warps.isEmpty()) {
            ctx.reply("warps.server.list-empty");
            return;
        }
        ctx.reply("warps.server.list-header", Ph.of("count", warps.size()));
        for (Warp warp : warps) {
            ctx.reply("warps.server.list-entry", WarpsModule.ph(warp), Ph.of("name", warp.name()),
                    Ph.of("order", warp.order()), Ph.of("world", warp.location().world()),
                    Ph.of("x", warp.location().blockX()), Ph.of("y", warp.location().blockY()),
                    Ph.of("z", warp.location().blockZ()),
                    Ph.of("permission", warp.permission() == null ? "-" : warp.permission()));
        }
    }

    @Override
    public List<String> suggest(CommandContext ctx) {
        if (ctx.size() == 1) {
            return ACTIONS;
        }
        String action = ctx.arg(0).toLowerCase(Locale.ROOT);
        if (ctx.size() == 2 && !action.equals("list")) {
            return module.store().serverWarps().stream().map(Warp::name).toList();
        }
        if (ctx.size() == 3 && action.equals("perm")) {
            return List.of("applecore.warps." + ctx.arg(1).toLowerCase(Locale.ROOT), "none");
        }
        return List.of();
    }
}
