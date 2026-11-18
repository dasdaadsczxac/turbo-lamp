package fun.applecube.core.module.player;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.command.AbstractCommand;
import fun.applecube.core.command.CommandContext;
import fun.applecube.core.command.CommandException;
import fun.applecube.core.message.Messages;
import fun.applecube.core.text.Ph;
import fun.applecube.core.text.Text;
import fun.applecube.core.user.User;
import fun.applecube.core.util.Locations;
import fun.applecube.core.util.Players;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** ping, playtime, depth, getpos, compass, near, list, whois, realname. */
final class InfoCommands {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());
    private static final String[] DIRECTIONS = {"south", "southwest", "west", "northwest", "north", "northeast", "east", "southeast"};

    private InfoCommands() {
    }

    static final class Ping extends AbstractCommand {

        Ping(AppleCorePlugin plugin) {
            super(plugin, "ping", "pong");
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player who = ctx.targetOrSelf(0);
            ctx.replyCmd(ctx.isSelf(who) ? "self" : "other", Ph.of("player", who.getName()), Ph.of("ping", who.getPing()));
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? othersIfAllowed(ctx) : List.of();
        }
    }

    static final class Playtime extends AbstractCommand {

        Playtime(AppleCorePlugin plugin) {
            super(plugin, "playtime");
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player who = ctx.targetOrSelf(0);
            long millis = who.getStatistic(Statistic.PLAY_ONE_MINUTE) * 50L;
            ctx.replyCmd(ctx.isSelf(who) ? "self" : "other", Ph.of("player", who.getName()),
                    Ph.of("time", plugin.messages().duration(ctx.sender(), millis)));
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? othersIfAllowed(ctx) : List.of();
        }
    }

    static final class Depth extends AbstractCommand {

        Depth(AppleCorePlugin plugin) {
            super(plugin, "depth", "height");
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player self = ctx.player();
            int depth = self.getLocation().getBlockY() - self.getWorld().getSeaLevel();
            String key = depth > 0 ? "above" : depth < 0 ? "below" : "level";
            ctx.replyCmd(key, Ph.of("blocks", Math.abs(depth)));
        }
    }

    static final class GetPos extends AbstractCommand {

        GetPos(AppleCorePlugin plugin) {
            super(plugin, "getpos", "coords", "position", "whereami", "getlocation", "getloc", "getcord", "curpos");
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player who = ctx.targetOrSelf(0);
            Location loc = who.getLocation();
            ctx.replyCmd(ctx.isSelf(who) ? "self" : "other", Ph.of("player", who.getName()), Locations.placeholders(loc),
                    Ph.of("yaw", Math.round(loc.getYaw())), Ph.of("pitch", Math.round(loc.getPitch())),
                    Ph.of("direction", direction(plugin.messages(), ctx, loc.getYaw())));
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? othersIfAllowed(ctx) : List.of();
        }
    }

    static Component direction(Messages messages, CommandContext ctx, float yaw) {
        int index = Math.floorMod(Math.round(yaw / 45f), 8);
        return messages.get(ctx.sender(), "commands.compass.directions." + DIRECTIONS[index]);
    }

    static final class Compass extends AbstractCommand {

        Compass(AppleCorePlugin plugin) {
            super(plugin, "compass", "direction");
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player self = ctx.player();
            ctx.replyCmd("done", Ph.of("direction", direction(plugin.messages(), ctx, self.getLocation().getYaw())),
                    Ph.of("degrees", Math.floorMod(Math.round(self.getLocation().getYaw()) + 180, 360)));
        }
    }

    static final class Near extends AbstractCommand {

        Near(AppleCorePlugin plugin) {
            super(plugin, "near", "nearby");
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            int max = plugin.getConfig().getInt("near.radius", 200);
            Player who = ctx.has(0) && !isNumber(ctx.arg(0)) ? ctx.targetOrSelf(0) : ctx.player();
            int radius = max;
            int radiusArg = ctx.has(0) && isNumber(ctx.arg(0)) ? 0 : 1;
            if (ctx.has(radiusArg)) {
                radius = ctx.integer(radiusArg, 1, ctx.hasPermission("applecore.near.maxexempt") ? 100_000 : max);
            }
            Location center = who.getLocation();
            double square = (double) radius * radius;
            List<Player> found = new ArrayList<>();
            for (Player other : who.getWorld().getPlayers()) {
                if (other.equals(who) || !Players.canSee(ctx.sender(), other)
                        || plugin.permissions().hasExplicit(other, "applecore.near.exclude")
                        || other.getLocation().distanceSquared(center) > square) {
                    continue;
                }
                found.add(other);
            }
            if (found.isEmpty()) {
                ctx.replyCmd("none", Ph.of("radius", radius));
                return;
            }
            found.sort(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(center)));
            List<Component> entries = new ArrayList<>();
            for (Player other : found) {
                entries.add(plugin.messages().get(ctx.sender(), "commands.near.entry", Ph.of("player", other.getName()),
                        Ph.of("distance", Math.round(other.getLocation().distance(center)))));
            }
            ctx.replyCmd("list", Ph.of("radius", radius), Ph.of("count", found.size()),
                    Ph.of("players", Component.join(JoinConfiguration.separator(
                            plugin.messages().get(ctx.sender(), "commands.near.separator")), entries)));
        }

        private static boolean isNumber(String raw) {
            return raw.chars().allMatch(Character::isDigit);
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? othersIfAllowed(ctx) : List.of();
        }
    }

    static final class ListCommand extends AbstractCommand {

        ListCommand(AppleCorePlugin plugin) {
            super(plugin, "list", "online", "playerlist", "plist", "who", "players");
        }

        @Override
        public void execute(CommandContext ctx) {
            List<Player> visible = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> Players.canSee(ctx.sender(), p))
                    .sorted(Comparator.comparing(p -> p.getName().toLowerCase(Locale.ROOT)))
                    .map(p -> (Player) p)
                    .toList();
            List<Component> names = new ArrayList<>();
            for (Player player : visible) {
                boolean hidden = plugin.users().user(player).vanished();
                names.add(plugin.messages().get(ctx.sender(), hidden ? "commands.list.entry-vanished" : "commands.list.entry",
                        Ph.of("player", player.getName())));
            }
            ctx.replyCmd("done", Ph.of("count", visible.size()), Ph.of("max", Bukkit.getMaxPlayers()),
                    Ph.of("players", Component.join(JoinConfiguration.separator(
                            plugin.messages().get(ctx.sender(), "commands.list.separator")), names)));
        }
    }

    static final class Whois extends AbstractCommand {

        Whois(AppleCorePlugin plugin) {
            super(plugin, "whois");
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player who = ctx.onlinePlayer(0);
            User user = plugin.users().user(who);
            Messages m = plugin.messages();
            Location loc = who.getLocation();
            boolean ip = ctx.hasPermission("applecore.whois.ip") && who.getAddress() != null;
            ctx.replyCmd("done",
                    Ph.of("player", who.getName()),
                    Ph.of("uuid", who.getUniqueId()),
                    Ph.of("nick", user.nickname() == null ? m.get(ctx.sender(), "common.none") : Text.plain(who.displayName())),
                    Ph.of("health", Math.round(who.getHealth())),
                    Ph.of("food", who.getFoodLevel()),
                    Ph.of("gamemode", m.get(ctx.sender(), "commands.gamemode.modes." + who.getGameMode().name().toLowerCase(Locale.ROOT))),
                    Locations.placeholders(loc),
                    Ph.of("op", m.bool(ctx.sender(), who.isOp())),
                    Ph.of("fly", m.bool(ctx.sender(), who.getAllowFlight())),
                    Ph.of("god", m.bool(ctx.sender(), user.god())),
                    Ph.of("vanished", m.bool(ctx.sender(), user.vanished())),
                    Ph.of("ping", who.getPing()),
                    Ph.of("first", user.firstJoin() == 0 ? "?" : DATE.format(Instant.ofEpochMilli(user.firstJoin()))),
                    Ph.of("playtime", m.duration(ctx.sender(), who.getStatistic(Statistic.PLAY_ONE_MINUTE) * 50L)),
                    Ph.of("ip", ip ? who.getAddress().getAddress().getHostAddress() : "—"));
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? players(ctx) : List.of();
        }
    }

    static final class RealName extends AbstractCommand {

        RealName(AppleCorePlugin plugin) {
            super(plugin, "realname", "rn");
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            if (!ctx.has(0)) {
                throw ctx.usage();
            }
            String query = ctx.arg(0).toLowerCase(Locale.ROOT);
            List<Component> matches = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!Players.canSee(ctx.sender(), player)) {
                    continue;
                }
                String nick = Text.plain(player.displayName()).toLowerCase(Locale.ROOT);
                if (nick.contains(query)) {
                    matches.add(plugin.messages().get(ctx.sender(), "commands.realname.entry",
                            Ph.of("nick", player.displayName()), Ph.of("player", player.getName())));
                }
            }
            if (matches.isEmpty()) {
                throw new CommandException("commands.realname.none", Ph.of("input", ctx.arg(0)));
            }
            ctx.sender().sendMessage(Component.join(JoinConfiguration.newlines(), matches));
        }
    }

    static List<AbstractCommand> all(AppleCorePlugin plugin) {
        return List.of(new Ping(plugin), new Playtime(plugin), new Depth(plugin), new GetPos(plugin),
                new Compass(plugin), new Near(plugin), new ListCommand(plugin), new Whois(plugin), new RealName(plugin));
    }
}
