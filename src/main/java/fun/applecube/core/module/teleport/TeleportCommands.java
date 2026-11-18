package fun.applecube.core.module.teleport;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.command.AbstractCommand;
import fun.applecube.core.command.CommandContext;
import fun.applecube.core.command.CommandException;
import fun.applecube.core.storage.StoredLocation;
import fun.applecube.core.teleport.SafeLocation;
import fun.applecube.core.text.Ph;
import fun.applecube.core.user.User;
import fun.applecube.core.util.Locations;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Staff and utility teleports: tp, tphere, tpo, tpohere, tppos, tpall, back, top, jump. */
final class TeleportCommands {

    private TeleportCommands() {
    }

    /** Players who turned teleports off can only be reached with {@code applecore.tptoggle.bypass} ({@code essentials.tpo}). */
    static void checkToggle(AppleCorePlugin plugin, CommandContext ctx, Player target) throws CommandException {
        if (!ctx.isSelf(target) && !plugin.users().user(target).teleportRequests()
                && !ctx.hasPermission("applecore.tptoggle.bypass")) {
            throw new CommandException("tpa.disabled", Ph.of("player", target.getName()));
        }
    }

    /** /tp: players, coordinates ({@code ~} relative); selectors and other vanilla syntax go to minecraft:tp. */
    static final class Tp extends AbstractCommand {

        Tp(AppleCorePlugin plugin) {
            super(plugin, "tp", "tele", "tp2p");
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            String[] a = ctx.args();
            if (a.length == 0) {
                throw ctx.usage();
            }
            if (Arrays.stream(a).anyMatch(s -> s.startsWith("@") || s.startsWith("^"))) {
                vanilla(ctx);
                return;
            }
            switch (a.length) {
                case 1 -> {
                    Player self = ctx.player();
                    Player target = ctx.onlinePlayer(0);
                    checkToggle(plugin, ctx, target);
                    plugin.teleports().teleport(self, target.getLocation(), 0,
                            () -> ctx.replyCmd("to-player", Ph.of("player", target.getName())));
                }
                case 2 -> {
                    ctx.require("applecore.tp.others");
                    Player who = ctx.onlinePlayer(0);
                    Player target = ctx.onlinePlayer(1);
                    checkToggle(plugin, ctx, who);
                    checkToggle(plugin, ctx, target);
                    plugin.teleports().teleport(who, target.getLocation(), 0,
                            () -> ctx.replyCmd("other-to-player", Ph.of("player", who.getName()), Ph.of("target", target.getName())));
                }
                case 3, 5 -> {
                    ctx.require("applecore.tp.position");
                    Player self = ctx.player();
                    Location to = position(ctx, self.getLocation(), 0);
                    plugin.teleports().teleport(self, to, 0, () -> ctx.replyCmd("to-position", Locations.placeholders(to)));
                }
                case 4, 6 -> {
                    ctx.require("applecore.tp.others");
                    ctx.require("applecore.tp.position");
                    Player who = ctx.onlinePlayer(0);
                    Location to = position(ctx, who.getLocation(), 1);
                    plugin.teleports().teleport(who, to, 0,
                            () -> ctx.replyCmd("other-to-position", Ph.of("player", who.getName()), Locations.placeholders(to)));
                }
                default -> vanilla(ctx);
            }
        }

        private static Location position(CommandContext ctx, Location base, int from) throws CommandException {
            String[] a = ctx.args();
            try {
                Location to = base.clone();
                to.setX(Locations.coordinate(a[from], base.getX(), true));
                to.setY(Locations.coordinate(a[from + 1], base.getY(), false));
                to.setZ(Locations.coordinate(a[from + 2], base.getZ(), true));
                if (a.length >= from + 5) {
                    to.setYaw((float) Locations.coordinate(a[from + 3], base.getYaw(), false));
                    to.setPitch((float) Locations.coordinate(a[from + 4], base.getPitch(), false));
                }
                return to;
            } catch (NumberFormatException e) {
                throw ctx.usage();
            }
        }

        private void vanilla(CommandContext ctx) {
            Bukkit.dispatchCommand(ctx.sender(), "minecraft:tp " + String.join(" ", ctx.args()));
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() <= 2 ? players(ctx) : List.of();
        }
    }

    /** /tphere and /tpohere: bring a player to you. */
    static final class TpHere extends AbstractCommand {

        private final boolean override;

        TpHere(AppleCorePlugin plugin, boolean override) {
            super(plugin, override ? "tpohere" : "tphere", override ? new String[0] : new String[]{"s"});
            this.override = override;
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player self = ctx.player();
            Player target = ctx.onlinePlayer(0);
            if (ctx.isSelf(target)) {
                throw new CommandException("tpa.self");
            }
            if (!override) {
                checkToggle(plugin, ctx, target);
            }
            plugin.teleports().teleport(target, self.getLocation(), 0, () -> {
                ctx.reply("commands.tphere.done", Ph.of("player", target.getName()));
                plugin.messages().send(target, "commands.tphere.target", Ph.of("player", self.getName()));
            });
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? players(ctx) : List.of();
        }
    }

    /** /tpo: like /tp to a player, ignoring /tptoggle. */
    static final class Tpo extends AbstractCommand {

        Tpo(AppleCorePlugin plugin) {
            super(plugin, "tpo");
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player self = ctx.player();
            Player target = ctx.onlinePlayer(0);
            plugin.teleports().teleport(self, target.getLocation(), 0,
                    () -> ctx.reply("commands.tp.to-player", Ph.of("player", target.getName())));
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? players(ctx) : List.of();
        }
    }

    /** /tppos x y z [world] [player]. */
    static final class TpPos extends AbstractCommand {

        TpPos(AppleCorePlugin plugin) {
            super(plugin, "tppos", "tpcord", "pos");
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            if (ctx.size() < 3) {
                throw ctx.usage();
            }
            Player who = ctx.size() >= 5 ? ctx.onlinePlayer(4) : ctx.player();
            if (!ctx.isSelf(who)) {
                ctx.require(ctx.othersPermission());
            }
            World world = who.getWorld();
            if (ctx.size() >= 4) {
                world = Bukkit.getWorld(ctx.arg(3));
                if (world == null) {
                    throw new CommandException("errors.world-not-found", Ph.of("world", ctx.arg(3)));
                }
            }
            Location base = who.getLocation();
            Location to;
            try {
                to = new Location(world,
                        Locations.coordinate(ctx.arg(0), base.getX(), true),
                        Locations.coordinate(ctx.arg(1), base.getY(), false),
                        Locations.coordinate(ctx.arg(2), base.getZ(), true),
                        base.getYaw(), base.getPitch());
            } catch (NumberFormatException e) {
                throw ctx.usage();
            }
            plugin.teleports().teleport(who, to, 0, () -> {
                if (ctx.isSelf(who)) {
                    ctx.reply("commands.tp.to-position", Locations.placeholders(to));
                } else {
                    ctx.reply("commands.tp.other-to-position", Ph.of("player", who.getName()), Locations.placeholders(to));
                }
            });
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            if (ctx.size() <= 3) {
                return List.of("~");
            }
            if (ctx.size() == 4) {
                return Bukkit.getWorlds().stream().map(World::getName).toList();
            }
            return ctx.size() == 5 ? othersIfAllowed(ctx) : List.of();
        }
    }

    /** /tpall [player]: everyone to you (or to the given player). */
    static final class TpAll extends AbstractCommand {

        TpAll(AppleCorePlugin plugin) {
            super(plugin, "tpall");
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player target = ctx.has(0) ? ctx.onlinePlayer(0) : ctx.player();
            int moved = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.equals(target)) {
                    continue;
                }
                plugin.teleports().teleport(player, target.getLocation(), 0, null);
                plugin.messages().send(player, "commands.tpall.target", Ph.of("player", target.getName()));
                moved++;
            }
            ctx.replyCmd("done", Ph.of("amount", moved), Ph.of("player", target.getName()));
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? players(ctx) : List.of();
        }
    }

    /** /back [player]: previous location (teleport or, with applecore.back.ondeath, death). */
    static final class Back extends AbstractCommand {

        Back(AppleCorePlugin plugin) {
            super(plugin, "back", "return");
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player who = ctx.targetOrSelf(0);
            User user = plugin.users().user(who);
            StoredLocation last = user.lastLocation();
            if (last == null) {
                throw new CommandException(ctx.isSelf(who) ? "commands.back.none" : "commands.back.none-other",
                        Ph.of("player", who.getName()));
            }
            Location to = last.toLocation();
            if (to == null) {
                throw new CommandException("errors.world-not-found", Ph.of("world", last.world()));
            }
            int delay = ctx.isSelf(who) ? plugin.teleports().delaySeconds(who) : 0;
            plugin.teleports().teleport(who, to, delay, () -> {
                plugin.messages().send(who, "commands.back.done");
                if (!ctx.isSelf(who)) {
                    ctx.replyCmd("done-other", Ph.of("player", who.getName()));
                }
            });
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? othersIfAllowed(ctx) : List.of();
        }
    }

    /** /top: the highest safe block above you. */
    static final class Top extends AbstractCommand {

        Top(AppleCorePlugin plugin) {
            super(plugin, "top");
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player self = ctx.player();
            Location to = SafeLocation.highest(self.getLocation());
            if (to == null || to.getBlockY() <= self.getLocation().getBlockY()) {
                throw new CommandException("commands.top.none");
            }
            plugin.teleports().teleport(self, to, 0, () -> ctx.replyCmd("done", Locations.placeholders(to)));
        }
    }

    /** /jump: onto the block you are looking at. */
    static final class Jump extends AbstractCommand {

        Jump(AppleCorePlugin plugin) {
            super(plugin, "jump", "j", "jumpto");
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player self = ctx.player();
            int distance = Math.max(8, plugin.getConfig().getInt("teleport.jump-distance", 160));
            Block block = self.getTargetBlockExact(distance);
            if (block == null) {
                throw new CommandException("commands.jump.no-target");
            }
            Location aim = block.getLocation().add(0.5, 1, 0.5);
            aim.setYaw(self.getLocation().getYaw());
            aim.setPitch(self.getLocation().getPitch());
            Location to = SafeLocation.find(aim);
            if (to == null) {
                throw new CommandException("teleport.unsafe");
            }
            plugin.teleports().teleport(self, to, 0, () -> ctx.replyCmd("done", Locations.placeholders(to)));
        }
    }

    static List<AbstractCommand> all(AppleCorePlugin plugin) {
        List<AbstractCommand> list = new ArrayList<>();
        list.add(new Tp(plugin));
        list.add(new TpHere(plugin, false));
        list.add(new TpHere(plugin, true));
        list.add(new Tpo(plugin));
        list.add(new TpPos(plugin));
        list.add(new TpAll(plugin));
        list.add(new Back(plugin));
        list.add(new Top(plugin));
        list.add(new Jump(plugin));
        return list;
    }
}
