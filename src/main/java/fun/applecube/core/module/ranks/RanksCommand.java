package fun.applecube.core.module.ranks;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.command.AbstractCommand;
import fun.applecube.core.command.CommandContext;
import fun.applecube.core.command.CommandException;
import fun.applecube.core.module.warps.WarpsModule;
import fun.applecube.core.permission.PermissionService;
import fun.applecube.core.text.Ph;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * /ranks: the rank list with prices and perks, /ranks info [player], and /ranks give|take for donation shops
 * (runs the console commands from ranks.yml, then announces the purchase).
 */
final class RanksCommand extends AbstractCommand {

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{2,16}");
    private static final Pattern TIME = Pattern.compile("[0-9]+(s|m|h|d|w|mo|y)");

    private final RanksModule module;

    RanksCommand(AppleCorePlugin plugin, RanksModule module) {
        super(plugin, "ranks", "donate", "privileges");
        this.module = module;
    }

    @Override
    public void execute(CommandContext ctx) throws CommandException {
        String action = ctx.has(0) ? ctx.arg(0).toLowerCase(Locale.ROOT) : "list";
        switch (action) {
            case "list" -> list(ctx);
            case "info" -> info(ctx);
            case "give" -> give(ctx);
            case "take" -> take(ctx);
            default -> throw ctx.usage();
        }
    }

    private Component name(CommandSender viewer, Rank rank) {
        return plugin.messages().parse(viewer, rank.display());
    }

    private void list(CommandContext ctx) {
        ctx.reply("ranks.list-header");
        Player self = ctx.isPlayer() ? (Player) ctx.sender() : null;
        for (Rank rank : module.all()) {
            boolean held = self != null && module.has(self, rank);
            ctx.reply(held ? "ranks.list-entry-held" : "ranks.list-entry",
                    Ph.of("rank", name(ctx.sender(), rank)),
                    Ph.of("id", rank.id()),
                    Ph.of("price", format(rank.price())),
                    Ph.of("homes", limit(rank, "applecore.homes.limit.")),
                    Ph.of("warps", limit(rank, "applecore.playerwarps.limit.")),
                    Ph.of("multiplier", format(rank.killerMoney())));
        }
        ctx.reply("ranks.list-footer");
    }

    private void info(CommandContext ctx) throws CommandException {
        Player target;
        if (ctx.has(1)) {
            target = ctx.onlinePlayer(1);
            if (!ctx.isSelf(target)) {
                ctx.require("applecore.ranks.others");
            }
        } else {
            target = ctx.player();
        }
        Rank top = module.top(target);
        PermissionService perms = plugin.permissions();
        int homes = perms.homeLimit(target);
        WarpsModule warps = plugin.module(WarpsModule.class);
        int warpLimit = warps == null ? 0 : warps.limit(target);
        ctx.reply("ranks.info", Ph.of("player", target.getName()),
                Ph.of("rank", top == null ? plugin.messages().get(ctx.sender(), "ranks.none") : name(ctx.sender(), top)),
                Ph.of("homes", homes == PermissionService.UNLIMITED ? plugin.messages().get(ctx.sender(), "common.unlimited") : Component.text(homes)),
                Ph.of("warps", warpLimit == PermissionService.UNLIMITED ? plugin.messages().get(ctx.sender(), "common.unlimited") : Component.text(warpLimit)),
                Ph.of("multiplier", format(module.killerMoney(target))));
    }

    private Rank rankArg(CommandContext ctx, int index) throws CommandException {
        if (!ctx.has(index)) {
            throw ctx.usage();
        }
        Rank rank = module.get(ctx.arg(index));
        if (rank == null) {
            throw new CommandException("ranks.unknown", Ph.of("rank", ctx.arg(index)));
        }
        return rank;
    }

    private String nameArg(CommandContext ctx) throws CommandException {
        if (!ctx.has(1)) {
            throw ctx.usage();
        }
        String name = ctx.arg(1);
        if (!NAME.matcher(name).matches()) {
            throw new CommandException("errors.player-never-joined", Ph.of("player", name));
        }
        return name;
    }

    private void give(CommandContext ctx) throws CommandException {
        ctx.require("applecore.ranks.manage");
        String player = nameArg(ctx);
        Rank rank = rankArg(ctx, 2);
        String time = ctx.has(3) ? ctx.arg(3).toLowerCase(Locale.ROOT) : null;
        if (time != null && !TIME.matcher(time).matches()) {
            throw new CommandException("ranks.bad-time", Ph.of("time", time));
        }
        run(time == null ? "give-commands" : "give-temp-commands", player, rank, time);
        ctx.reply("ranks.given", Ph.of("player", player), Ph.of("rank", name(ctx.sender(), rank)));
        if (module.file().yaml().getBoolean("announce", true)) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                plugin.messages().send(online, "ranks.announce", Ph.of("player", player), Ph.of("rank", name(online, rank)));
            }
            Player buyer = Bukkit.getPlayerExact(player);
            if (buyer != null) {
                buyer.showTitle(Title.title(plugin.messages().get(buyer, "ranks.title", Ph.of("rank", name(buyer, rank))),
                        plugin.messages().get(buyer, "ranks.subtitle", Ph.of("rank", name(buyer, rank))),
                        Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(700))));
                plugin.pack().play(buyer, "success");
            }
        }
    }

    private void take(CommandContext ctx) throws CommandException {
        ctx.require("applecore.ranks.manage");
        String player = nameArg(ctx);
        Rank rank = rankArg(ctx, 2);
        run("take-commands", player, rank, null);
        ctx.reply("ranks.taken", Ph.of("player", player), Ph.of("rank", name(ctx.sender(), rank)));
    }

    private void run(String key, String player, Rank rank, String time) {
        for (String template : module.file().yaml().getStringList(key)) {
            String command = template.replace("<player>", player).replace("<group>", rank.group())
                    .replace("<rank>", rank.id()).replace("<time>", time == null ? "" : time).trim();
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    private static String limit(Rank rank, String prefix) {
        int best = 0;
        for (String node : rank.nodes()) {
            if (node.startsWith(prefix)) {
                try {
                    best = Math.max(best, Integer.parseInt(node.substring(prefix.length())));
                } catch (NumberFormatException ignored) {
                    // not a number
                }
            }
        }
        return best == 0 ? "—" : String.valueOf(best);
    }

    static String format(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(Math.round(value * 100) / 100.0);
    }

    @Override
    public List<String> suggest(CommandContext ctx) {
        boolean manage = ctx.hasPermission("applecore.ranks.manage");
        if (ctx.size() == 1) {
            List<String> out = new ArrayList<>(List.of("list", "info"));
            if (manage) {
                out.addAll(List.of("give", "take"));
            }
            return out;
        }
        String action = ctx.arg(0).toLowerCase(Locale.ROOT);
        if (ctx.size() == 2 && (manage || action.equals("info"))) {
            return players(ctx);
        }
        if (ctx.size() == 3 && manage && (action.equals("give") || action.equals("take"))) {
            return module.all().stream().map(Rank::id).toList();
        }
        if (ctx.size() == 4 && manage && action.equals("give")) {
            return List.of("30d", "90d", "1y");
        }
        return List.of();
    }
}
