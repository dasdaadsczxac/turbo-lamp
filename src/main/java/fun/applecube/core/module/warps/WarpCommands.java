package fun.applecube.core.module.warps;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.command.AbstractCommand;
import fun.applecube.core.command.CommandContext;
import fun.applecube.core.command.CommandException;
import fun.applecube.core.permission.PermissionService;
import fun.applecube.core.storage.StoredLocation;
import fun.applecube.core.text.Ph;
import fun.applecube.core.text.Text;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** /warp, /playerwarps, /setwarp, /delwarp, /warpinfo, /warpicon, /warpdesc. */
final class WarpCommands {

    private WarpCommands() {
    }

    static Pattern namePattern(AppleCorePlugin plugin) {
        return Pattern.compile(plugin.getConfig().getString("warps.name-pattern", "[\\p{L}\\p{N}_\\-]{2,16}"));
    }

    static Warp existing(WarpsModule module, CommandContext ctx, int index) throws CommandException {
        if (!ctx.has(index)) {
            throw ctx.usage();
        }
        Warp warp = module.store().get(ctx.arg(index));
        if (warp == null) {
            throw new CommandException("warps.not-found", Ph.of("warp", ctx.arg(index)));
        }
        return warp;
    }

    /**
     * The sender's own player warp; another player's warp needs {@code <command>.others}, a server warp needs
     * applecore.serverwarp (EssentialsX: essentials.setwarp).
     */
    static Warp editable(WarpsModule module, CommandContext ctx, int index) throws CommandException {
        Warp warp = existing(module, ctx, index);
        boolean own = ctx.isPlayer() && warp.isOwner(((Player) ctx.sender()).getUniqueId());
        if (warp.isServer()) {
            ctx.require("applecore.serverwarp");
        } else if (!own) {
            ctx.require(ctx.othersPermission());
        }
        return warp;
    }

    static List<String> ownWarpNames(WarpsModule module, CommandContext ctx) {
        if (!ctx.isPlayer()) {
            return List.of();
        }
        return module.store().playerWarps(((Player) ctx.sender()).getUniqueId()).stream().map(Warp::name).toList();
    }

    /** /warp [name] [player]: opens the menu, or teleports to a server or player warp by name. */
    static final class WarpCommand extends AbstractCommand {

        private final WarpsModule module;
        private final WarpMenu.Mode tab;

        WarpCommand(AppleCorePlugin plugin, WarpsModule module, String name, WarpMenu.Mode tab, String... aliases) {
            super(plugin, name, aliases);
            permission("applecore.warp");
            this.module = module;
            this.tab = tab;
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            if (!ctx.has(0)) {
                WarpMenu.Mode mode = tab;
                if (mode == null) {
                    mode = "players".equalsIgnoreCase(plugin.getConfig().getString("warps.default-tab", "server"))
                            ? WarpMenu.Mode.PLAYERS : WarpMenu.Mode.SERVER;
                }
                WarpMenu.open(plugin, module, ctx.player(), mode);
                return;
            }
            Warp warp = existing(module, ctx, 0);
            Player who = ctx.has(1) ? ctx.onlinePlayer(1) : ctx.player();
            if (!ctx.isSelf(who)) {
                ctx.require("applecore.warp.others");
            }
            module.teleport(who, warp);
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            if (ctx.size() == 1 && ctx.isPlayer()) {
                Player self = (Player) ctx.sender();
                List<String> names = new ArrayList<>();
                for (Warp warp : module.store().all()) {
                    if (module.canUse(self, warp)) {
                        names.add(warp.name());
                    }
                }
                return names;
            }
            return ctx.size() == 2 && ctx.hasPermission("applecore.warp.others") ? players(ctx) : List.of();
        }
    }

    /** /setwarp <name>: creates a player warp here, or moves your existing one. */
    static final class SetWarp extends AbstractCommand {

        private final WarpsModule module;

        SetWarp(AppleCorePlugin plugin, WarpsModule module) {
            super(plugin, "setwarp", "createwarp", "addwarp");
            this.module = module;
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player self = ctx.player();
            if (!ctx.has(0)) {
                throw ctx.usage();
            }
            String name = ctx.arg(0);
            if (!namePattern(plugin).matcher(name).matches()) {
                throw new CommandException("warps.invalid-name", Ph.of("warp", name));
            }
            if (plugin.getConfig().getStringList("warps.disabled-worlds").contains(self.getWorld().getName())
                    && !ctx.hasPermission("applecore.setwarp.bypass-world")) {
                throw new CommandException("warps.world-disabled");
            }
            Warp existing = module.store().get(name);
            if (existing != null) {
                if (!existing.isOwner(self.getUniqueId())) {
                    throw new CommandException("warps.name-taken", Ph.of("warp", name));
                }
                existing.location(StoredLocation.of(self.getLocation()));
                module.store().save();
                ctx.reply("warps.moved", WarpsModule.ph(existing));
                plugin.pack().play(self, "success");
                return;
            }
            int limit = module.limit(self);
            if (limit <= 0) {
                throw new CommandException("warps.no-warps");
            }
            long owned = module.store().countOwned(self.getUniqueId());
            if (owned >= limit) {
                throw new CommandException("warps.limit", Ph.of("limit", limit));
            }
            double cost = plugin.getConfig().getDouble("warps.creation-cost", 0);
            if (cost > 0 && !ctx.hasPermission("applecore.setwarp.free")) {
                if (!plugin.hooks().economy().available()) {
                    throw new CommandException("errors.economy-unavailable");
                }
                if (!plugin.hooks().economy().withdraw(self, cost)) {
                    throw new CommandException("errors.not-enough-money", Ph.of("amount", plugin.hooks().economy().format(cost)));
                }
            }
            Warp warp = new Warp(name, Warp.Type.PLAYER, StoredLocation.of(self.getLocation()), self.getUniqueId(), self.getName());
            module.store().add(warp);
            ctx.reply("warps.created", WarpsModule.ph(warp), Ph.of("count", owned + 1),
                    Ph.of("limit", limit == PermissionService.UNLIMITED ? plugin.messages().get(self, "common.unlimited")
                            : net.kyori.adventure.text.Component.text(limit)));
            plugin.pack().play(self, "success");
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? ownWarpNames(module, ctx) : List.of();
        }
    }

    static final class DelWarp extends AbstractCommand {

        private final WarpsModule module;

        DelWarp(AppleCorePlugin plugin, WarpsModule module) {
            super(plugin, "delwarp", "remwarp", "rmwarp", "deletewarp", "removewarp");
            this.module = module;
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Warp warp = editable(module, ctx, 0);
            module.store().remove(warp);
            ctx.reply("warps.deleted", WarpsModule.ph(warp));
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? ownWarpNames(module, ctx) : List.of();
        }
    }

    static final class WarpInfo extends AbstractCommand {

        private final WarpsModule module;

        WarpInfo(AppleCorePlugin plugin, WarpsModule module) {
            super(plugin, "warpinfo");
            this.module = module;
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Warp warp = existing(module, ctx, 0);
            Player self = ctx.player();
            TagResolver placeholders = WarpMenu.warpPlaceholders(plugin, self, warp);
            String key = warp.isServer() ? "warps.info-server" : "warps.info-player";
            for (var line : WarpMenu.lore(plugin, self, key, warp, placeholders)) {
                self.sendMessage(line);
            }
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? module.store().all().stream().map(Warp::name).toList() : List.of();
        }
    }

    /** /warpicon <warp>: the held item becomes the icon (custom items keep their plugin id); empty hand opens the picker. */
    static final class WarpIcon extends AbstractCommand {

        private final WarpsModule module;

        WarpIcon(AppleCorePlugin plugin, WarpsModule module) {
            super(plugin, "warpicon");
            this.module = module;
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player self = ctx.player();
            Warp warp = editable(module, ctx, 0);
            ItemStack hand = self.getInventory().getItemInMainHand();
            if (hand.getType().isAir()) {
                new IconPickerMenu(plugin, module, self, warp, 0).open();
                return;
            }
            warp.icon(plugin.items().describe(hand));
            module.store().save();
            ctx.reply("warps.icon-set", WarpsModule.ph(warp));
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? ownWarpNames(module, ctx) : List.of();
        }
    }

    /** /warpdesc <warp> [text… use | for new lines]; no text clears it. */
    static final class WarpDesc extends AbstractCommand {

        private final WarpsModule module;

        WarpDesc(AppleCorePlugin plugin, WarpsModule module) {
            super(plugin, "warpdesc");
            this.module = module;
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Warp warp = editable(module, ctx, 0);
            String raw = ctx.joinFrom(1);
            if (raw.isBlank()) {
                warp.description(List.of());
                module.store().save();
                ctx.reply("warps.description-cleared", WarpsModule.ph(warp));
                return;
            }
            if (!ctx.hasPermission("applecore.warpdesc.color")) {
                raw = Text.plain(Text.userFormatted(raw));
            }
            int maxLines = plugin.getConfig().getInt("warps.description.max-lines", 3);
            int maxLength = plugin.getConfig().getInt("warps.description.max-line-length", 40);
            List<String> lines = new ArrayList<>();
            for (String line : raw.split("\\|")) {
                String trimmed = line.strip();
                if (Text.plain(Text.userFormatted(trimmed)).length() > maxLength) {
                    throw new CommandException("warps.description-too-long", Ph.of("max", maxLength));
                }
                lines.add(trimmed);
            }
            if (lines.size() > maxLines) {
                throw new CommandException("warps.description-too-many-lines", Ph.of("max", maxLines));
            }
            warp.description(lines);
            module.store().save();
            ctx.reply("warps.description-set", WarpsModule.ph(warp));
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? ownWarpNames(module, ctx) : List.of();
        }
    }

    static List<AbstractCommand> all(AppleCorePlugin plugin, WarpsModule module) {
        return List.of(
                new WarpCommand(plugin, module, "warp", null, "warps"),
                new WarpCommand(plugin, module, "playerwarps", WarpMenu.Mode.PLAYERS, "pwarps", "pwarp"),
                new SetWarp(plugin, module),
                new DelWarp(plugin, module),
                new WarpInfo(plugin, module),
                new WarpIcon(plugin, module),
                new WarpDesc(plugin, module));
    }
}
