package fun.applecube.core.module.teleport;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.command.AbstractCommand;
import fun.applecube.core.command.CommandContext;
import fun.applecube.core.command.CommandException;
import fun.applecube.core.message.Messages;
import fun.applecube.core.text.Ph;
import fun.applecube.core.user.User;
import fun.applecube.core.util.Players;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Teleport requests: tpa, tpahere, tpaall, tpaccept, tpdeny, tpacancel, tptoggle, tpauto. */
final class RequestCommands {

    private RequestCommands() {
    }

    /** Sends a request and notifies both sides; auto-accepts /tpa when the target enabled /tpauto. */
    static void send(AppleCorePlugin plugin, TeleportRequests requests, Player from, Player to, boolean here) {
        Messages messages = plugin.messages();
        User target = plugin.users().user(to);
        requests.put(from, to, here);
        if (!here && target.teleportAuto()) {
            accept(plugin, requests, to, from.getUniqueId());
            return;
        }
        messages.send(from, "tpa.sent", Ph.of("player", to.getName()),
                Ph.of("time", messages.duration(from, requests.timeoutMillis())));
        if (target.isIgnoring(from.getUniqueId())) {
            return;
        }
        messages.send(to, here ? "tpa.received-here" : "tpa.received", Ph.of("player", from.getName()),
                Ph.of("time", messages.duration(to, requests.timeoutMillis())));
        Component accept = messages.get(to, "tpa.button-accept")
                .clickEvent(ClickEvent.runCommand("/tpaccept " + from.getName()))
                .hoverEvent(HoverEvent.showText(messages.get(to, "tpa.button-accept-hover")));
        Component deny = messages.get(to, "tpa.button-deny")
                .clickEvent(ClickEvent.runCommand("/tpdeny " + from.getName()))
                .hoverEvent(HoverEvent.showText(messages.get(to, "tpa.button-deny-hover")));
        messages.send(to, "tpa.buttons", Ph.of("accept", accept), Ph.of("deny", deny));
        plugin.pack().play(to, "click");
    }

    /** Accepts the newest (or the named) request sent to {@code target}. */
    static void accept(AppleCorePlugin plugin, TeleportRequests requests, Player target, @Nullable UUID from) {
        TeleportRequests.Request request = requests.take(target, from);
        if (request == null) {
            plugin.messages().send(target, "tpa.none");
            return;
        }
        Player requester = Bukkit.getPlayer(request.from());
        if (requester == null) {
            plugin.messages().send(target, "tpa.offline");
            return;
        }
        plugin.messages().send(target, "tpa.accepted", Ph.of("player", requester.getName()));
        plugin.messages().send(requester, "tpa.accepted-by", Ph.of("player", target.getName()));
        if (request.here()) {
            plugin.teleports().teleport(target, () -> requester.isOnline() ? requester.getLocation() : null,
                    plugin.teleports().delaySeconds(target), null);
        } else {
            plugin.teleports().teleport(requester, () -> target.isOnline() ? target.getLocation() : null,
                    plugin.teleports().delaySeconds(requester), null);
        }
    }

    /** Checks shared by /tpa, /tpahere and /tpaall. */
    static void checkTarget(AppleCorePlugin plugin, CommandContext ctx, Player from, Player to) throws CommandException {
        if (from.equals(to)) {
            throw new CommandException("tpa.self");
        }
        if (!plugin.users().user(to).teleportRequests() && !ctx.hasPermission("applecore.tptoggle.bypass")) {
            throw new CommandException("tpa.disabled", Ph.of("player", to.getName()));
        }
        if (!plugin.permissions().has(to, "applecore.tpaccept")) {
            throw new CommandException("tpa.cannot-accept", Ph.of("player", to.getName()));
        }
    }

    static final class Tpa extends AbstractCommand {

        private final TeleportRequests requests;
        private final boolean here;

        Tpa(AppleCorePlugin plugin, TeleportRequests requests, boolean here) {
            super(plugin, here ? "tpahere" : "tpa", here ? new String[0] : new String[]{"call", "tpask"});
            this.requests = requests;
            this.here = here;
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player self = ctx.player();
            Player target = ctx.onlinePlayer(0);
            checkTarget(plugin, ctx, self, target);
            send(plugin, requests, self, target, here);
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? players(ctx) : List.of();
        }
    }

    /** /tpaall: asks everyone online to teleport to you. */
    static final class TpaAll extends AbstractCommand {

        private final TeleportRequests requests;

        TpaAll(AppleCorePlugin plugin, TeleportRequests requests) {
            super(plugin, "tpaall");
            this.requests = requests;
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player self = ctx.player();
            int sent = 0;
            for (Player target : Bukkit.getOnlinePlayers()) {
                try {
                    checkTarget(plugin, ctx, self, target);
                } catch (CommandException skipped) {
                    continue;
                }
                requests.put(self, target, true);
                plugin.messages().send(target, "tpa.received-here", Ph.of("player", self.getName()),
                        Ph.of("time", plugin.messages().duration(target, requests.timeoutMillis())));
                sent++;
            }
            ctx.replyCmd("done", Ph.of("amount", sent));
        }
    }

    static final class TpAccept extends AbstractCommand {

        private final TeleportRequests requests;

        TpAccept(AppleCorePlugin plugin, TeleportRequests requests) {
            super(plugin, "tpaccept", "tpyes", "tpallow");
            this.requests = requests;
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player self = ctx.player();
            accept(plugin, requests, self, ctx.has(0) ? requesterId(ctx) : null);
        }

        private UUID requesterId(CommandContext ctx) throws CommandException {
            Player requester = Players.find(ctx.sender(), ctx.arg(0));
            if (requester == null) {
                throw new CommandException("errors.player-not-found", Ph.of("player", ctx.arg(0)));
            }
            return requester.getUniqueId();
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 && ctx.isPlayer() ? names(requests.requesters((Player) ctx.sender())) : List.of();
        }
    }

    static final class TpDeny extends AbstractCommand {

        private final TeleportRequests requests;

        TpDeny(AppleCorePlugin plugin, TeleportRequests requests) {
            super(plugin, "tpdeny", "tpno", "tpcancel");
            this.requests = requests;
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player self = ctx.player();
            UUID from = null;
            if (ctx.has(0)) {
                Player requester = ctx.onlinePlayer(0);
                from = requester.getUniqueId();
            }
            TeleportRequests.Request request = requests.take(self, from);
            if (request == null) {
                throw new CommandException("tpa.none");
            }
            Player requester = Bukkit.getPlayer(request.from());
            String name = requester != null ? requester.getName() : "?";
            ctx.reply("tpa.denied", Ph.of("player", name));
            if (requester != null) {
                plugin.messages().send(requester, "tpa.denied-by", Ph.of("player", self.getName()));
            }
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 && ctx.isPlayer() ? names(requests.requesters((Player) ctx.sender())) : List.of();
        }
    }

    static final class TpaCancel extends AbstractCommand {

        private final TeleportRequests requests;

        TpaCancel(AppleCorePlugin plugin, TeleportRequests requests) {
            super(plugin, "tpacancel");
            this.requests = requests;
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player self = ctx.player();
            UUID to = ctx.has(0) ? ctx.onlinePlayer(0).getUniqueId() : null;
            List<TeleportRequests.Request> cancelled = requests.cancelOutgoing(self.getUniqueId(), to);
            if (cancelled.isEmpty()) {
                throw new CommandException("tpa.none-outgoing");
            }
            for (TeleportRequests.Request request : cancelled) {
                Player target = Bukkit.getPlayer(request.to());
                if (target != null) {
                    plugin.messages().send(target, "tpa.cancelled-by", Ph.of("player", self.getName()));
                }
            }
            ctx.reply("tpa.cancelled", Ph.of("amount", cancelled.size()));
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? players(ctx) : List.of();
        }
    }

    /** /tptoggle and /tpauto [player]: switch accepting requests / auto-accepting /tpa. */
    static final class Toggle extends AbstractCommand {

        private final boolean auto;

        Toggle(AppleCorePlugin plugin, boolean auto) {
            super(plugin, auto ? "tpauto" : "tptoggle");
            this.auto = auto;
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player who = ctx.targetOrSelf(0);
            User user = plugin.users().user(who);
            boolean value;
            if (auto) {
                value = !user.teleportAuto();
                user.teleportAuto(value);
            } else {
                value = !user.teleportRequests();
                user.teleportRequests(value);
            }
            String key = "commands." + name() + "." + (value ? "turned-on" : "turned-off");
            plugin.messages().send(who, key);
            if (!ctx.isSelf(who)) {
                ctx.reply("commands." + name() + ".changed-other", Ph.of("player", who.getName()),
                        Ph.of("state", plugin.messages().bool(ctx.sender(), value)));
            }
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? othersIfAllowed(ctx) : List.of();
        }
    }

    private static List<String> names(List<UUID> ids) {
        List<String> out = new ArrayList<>();
        for (UUID id : ids) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                out.add(player.getName());
            }
        }
        return out;
    }

    static List<AbstractCommand> all(AppleCorePlugin plugin, TeleportRequests requests) {
        return List.of(
                new Tpa(plugin, requests, false),
                new Tpa(plugin, requests, true),
                new TpaAll(plugin, requests),
                new TpAccept(plugin, requests),
                new TpDeny(plugin, requests),
                new TpaCancel(plugin, requests),
                new Toggle(plugin, false),
                new Toggle(plugin, true));
    }
}
