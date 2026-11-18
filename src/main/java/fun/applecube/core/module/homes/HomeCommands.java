package fun.applecube.core.module.homes;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.command.AbstractCommand;
import fun.applecube.core.command.CommandContext;
import fun.applecube.core.command.CommandException;
import fun.applecube.core.message.Messages;
import fun.applecube.core.permission.PermissionService;
import fun.applecube.core.storage.StoredLocation;
import fun.applecube.core.text.Ph;
import fun.applecube.core.user.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** /home (homes), /sethome, /delhome, /playerhome. Other players' homes use {@code player:home}. */
final class HomeCommands {

    static final Pattern NAME = Pattern.compile("[\\p{L}\\p{N}_\\-]{1,24}");

    private HomeCommands() {
    }

    /** Runs {@code action} with the user of a known (possibly offline) player. */
    static void withUser(AppleCorePlugin plugin, CommandContext ctx, OfflinePlayer owner, Consumer<User> action) {
        plugin.users().load(owner).thenAccept(user -> {
            if (user == null) {
                ctx.reply("errors.player-never-joined", Ph.of("player", String.valueOf(owner.getName())));
            } else {
                action.accept(user);
            }
        });
    }

    static Component limitText(Messages messages, CommandSender viewer, int limit) {
        return limit == PermissionService.UNLIMITED ? messages.get(viewer, "common.unlimited") : Component.text(limit);
    }

    /** Clickable home list: each name teleports there, hover shows the coordinates. */
    static void sendList(AppleCorePlugin plugin, CommandSender viewer, User owner, boolean own, int limit) {
        Messages messages = plugin.messages();
        if (owner.homes().isEmpty()) {
            messages.send(viewer, own ? "commands.home.none" : "commands.home.none-other", Ph.of("player", owner.name()));
            return;
        }
        List<Component> chips = new ArrayList<>();
        for (Map.Entry<String, StoredLocation> entry : owner.homes().entrySet()) {
            StoredLocation loc = entry.getValue();
            String command = own ? "/home " + entry.getKey() : "/home " + owner.name() + ":" + entry.getKey();
            chips.add(messages.get(viewer, "commands.home.entry", Ph.of("home", entry.getKey()))
                    .clickEvent(ClickEvent.runCommand(command))
                    .hoverEvent(HoverEvent.showText(messages.get(viewer, "commands.home.entry-hover",
                            Ph.of("home", entry.getKey()), Ph.of("world", loc.world()),
                            Ph.of("x", loc.blockX()), Ph.of("y", loc.blockY()), Ph.of("z", loc.blockZ())))));
        }
        Component list = Component.join(JoinConfiguration.separator(messages.get(viewer, "commands.home.separator")), chips);
        messages.send(viewer, own ? "commands.home.list" : "commands.home.list-other",
                Ph.of("player", owner.name()), Ph.of("count", owner.homes().size()),
                Ph.of("limit", limitText(messages, viewer, limit)), Ph.of("homes", list));
    }

    static void teleport(AppleCorePlugin plugin, Player player, String home, StoredLocation stored) {
        Location to = stored.toLocation();
        if (to == null) {
            plugin.messages().send(player, "errors.world-not-found", Ph.of("world", stored.world()));
            return;
        }
        plugin.teleports().teleport(player, to,
                () -> plugin.messages().send(player, "commands.home.teleported", Ph.of("home", home)));
    }

    static final class Home extends AbstractCommand {

        Home(AppleCorePlugin plugin) {
            super(plugin, "home");
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player self = ctx.player();
            if (ctx.has(0) && ctx.arg(0).contains(":")) {
                ctx.require(ctx.othersPermission());
                String[] parts = ctx.arg(0).split(":", 2);
                OfflinePlayer owner = ctx.knownPlayer(parts[0]);
                withUser(plugin, ctx, owner, user -> {
                    StoredLocation home = parts[1].isEmpty() ? null : user.home(parts[1]);
                    if (home == null) {
                        sendList(plugin, self, user, false, -1);
                    } else {
                        teleport(plugin, self, parts[1].toLowerCase(Locale.ROOT), home);
                    }
                });
                return;
            }
            User user = plugin.users().user(self);
            if (!ctx.has(0)) {
                if (user.homes().size() == 1) {
                    Map.Entry<String, StoredLocation> only = user.homes().entrySet().iterator().next();
                    teleport(plugin, self, only.getKey(), only.getValue());
                } else {
                    sendList(plugin, self, user, true, plugin.permissions().homeLimit(self));
                }
                return;
            }
            StoredLocation home = user.home(ctx.arg(0));
            if (home == null) {
                ctx.replyCmd("not-found", Ph.of("home", ctx.arg(0)));
                if (!user.homes().isEmpty()) {
                    sendList(plugin, self, user, true, plugin.permissions().homeLimit(self));
                }
                return;
            }
            teleport(plugin, self, ctx.arg(0).toLowerCase(Locale.ROOT), home);
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            if (ctx.size() != 1 || !ctx.isPlayer()) {
                return List.of();
            }
            return new ArrayList<>(plugin.users().user((Player) ctx.sender()).homes().keySet());
        }
    }

    static final class SetHome extends AbstractCommand {

        SetHome(AppleCorePlugin plugin) {
            super(plugin, "sethome", "createhome");
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player self = ctx.player();
            String raw = ctx.has(0) ? ctx.arg(0) : plugin.getConfig().getString("homes.default-name", "home");
            if (raw.contains(":")) {
                ctx.require(ctx.othersPermission());
                String[] parts = raw.split(":", 2);
                String name = validName(parts[1]);
                OfflinePlayer owner = ctx.knownPlayer(parts[0]);
                StoredLocation here = StoredLocation.of(self.getLocation());
                withUser(plugin, ctx, owner, user -> {
                    user.setHome(name, here);
                    plugin.users().save(user);
                    ctx.replyCmd("set-other", Ph.of("player", user.name()), Ph.of("home", name));
                });
                return;
            }
            String name = validName(raw);
            List<String> disabled = plugin.getConfig().getStringList("homes.disabled-worlds");
            if (disabled.contains(self.getWorld().getName()) && !ctx.hasPermission("applecore.sethome.bypass-world")) {
                throw new CommandException("commands.sethome.world-disabled");
            }
            User user = plugin.users().user(self);
            boolean exists = user.home(name) != null;
            int limit = plugin.permissions().homeLimit(self);
            if (!exists && user.homes().size() >= limit) {
                throw new CommandException("commands.sethome.limit", Ph.of("limit", limit));
            }
            user.setHome(name, StoredLocation.of(self.getLocation()));
            plugin.users().save(user);
            ctx.replyCmd(exists ? "updated" : "set", Ph.of("home", name), Ph.of("count", user.homes().size()),
                    Ph.of("limit", limitText(plugin.messages(), self, limit)));
            plugin.pack().play(self, "success");
        }

        private static String validName(String raw) throws CommandException {
            if (!NAME.matcher(raw).matches()) {
                throw new CommandException("commands.sethome.invalid-name", Ph.of("home", raw));
            }
            return raw.toLowerCase(Locale.ROOT);
        }
    }

    static final class DelHome extends AbstractCommand {

        DelHome(AppleCorePlugin plugin) {
            super(plugin, "delhome", "remhome", "rmhome", "removehome", "deletehome");
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            if (!ctx.has(0)) {
                throw ctx.usage();
            }
            if (ctx.arg(0).contains(":")) {
                ctx.require(ctx.othersPermission());
                String[] parts = ctx.arg(0).split(":", 2);
                OfflinePlayer owner = ctx.knownPlayer(parts[0]);
                withUser(plugin, ctx, owner, user -> {
                    if (user.removeHome(parts[1])) {
                        plugin.users().save(user);
                        ctx.replyCmd("deleted-other", Ph.of("player", user.name()), Ph.of("home", parts[1]));
                    } else {
                        ctx.reply("commands.home.not-found", Ph.of("home", parts[1]));
                    }
                });
                return;
            }
            Player self = ctx.player();
            User user = plugin.users().user(self);
            if (!user.removeHome(ctx.arg(0))) {
                throw new CommandException("commands.home.not-found", Ph.of("home", ctx.arg(0)));
            }
            plugin.users().save(user);
            ctx.replyCmd("deleted", Ph.of("home", ctx.arg(0).toLowerCase(Locale.ROOT)));
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            if (ctx.size() != 1 || !ctx.isPlayer()) {
                return List.of();
            }
            return new ArrayList<>(plugin.users().user((Player) ctx.sender()).homes().keySet());
        }
    }

    /** /playerhome player [home]: staff view of another player's homes (TrustEssentials /phome). */
    static final class PlayerHome extends AbstractCommand {

        PlayerHome(AppleCorePlugin plugin) {
            super(plugin, "playerhome", "phome");
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player self = ctx.player();
            OfflinePlayer owner = ctx.knownPlayer(0);
            withUser(plugin, ctx, owner, user -> {
                StoredLocation home = ctx.has(1) ? user.home(ctx.arg(1)) : null;
                if (home == null) {
                    sendList(plugin, self, user, false, -1);
                } else {
                    teleport(plugin, self, ctx.arg(1).toLowerCase(Locale.ROOT), home);
                }
            });
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? players(ctx) : List.of();
        }
    }

    /** /homes [player]: always shows the list (uses the /home permission). */
    static final class Homes extends AbstractCommand {

        Homes(AppleCorePlugin plugin) {
            super(plugin, "homes");
            permission("applecore.home");
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            if (ctx.has(0)) {
                ctx.require("applecore.home.others");
                OfflinePlayer owner = ctx.knownPlayer(0);
                withUser(plugin, ctx, owner, user -> sendList(plugin, ctx.sender(), user, false, -1));
                return;
            }
            Player self = ctx.player();
            sendList(plugin, self, plugin.users().user(self), true, plugin.permissions().homeLimit(self));
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 && ctx.hasPermission("applecore.home.others") ? players(ctx) : List.of();
        }
    }

    static List<AbstractCommand> all(AppleCorePlugin plugin) {
        return List.of(new Home(plugin), new Homes(plugin), new SetHome(plugin), new DelHome(plugin), new PlayerHome(plugin));
    }
}
