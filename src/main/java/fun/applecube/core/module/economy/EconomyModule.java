package fun.applecube.core.module.economy;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.command.AbstractCommand;
import fun.applecube.core.command.CommandContext;
import fun.applecube.core.command.CommandException;
import fun.applecube.core.hook.EconomyService;
import fun.applecube.core.module.Module;
import fun.applecube.core.text.Ph;
import fun.applecube.core.user.User;
import fun.applecube.core.util.Durations;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Money (stage 3): AppleCore accounts as a Vault provider (economy.provider), and the EssentialsX-style
 * commands /balance, /pay, /paytoggle, /balancetop and /eco give|take|set|reset — the console syntax
 * donation menus use ("eco take %player_name% 35000").
 */
public final class EconomyModule extends Module implements Listener {

    private @Nullable BalanceStore store;
    private List<TopEntry> cachedTop = List.of();
    private long topBuiltAt;

    record TopEntry(String name, double balance) {
    }

    public EconomyModule(AppleCorePlugin plugin) {
        super(plugin);
    }

    @Override
    public String id() {
        return "economy";
    }

    @Override
    public void enable() {
        String provider = plugin.getConfig().getString("economy.provider", "auto").toLowerCase(Locale.ROOT);
        if (!provider.equals("none")) {
            store = new BalanceStore(plugin);
            store.load();
            plugin.hooks().economy().internal(store);
            if (plugin.hooks().economy().vaultPresent()) {
                VaultRegistration.register(plugin, store,
                        provider.equals("applecore") ? ServicePriority.Highest : ServicePriority.Lowest);
            }
            listen(this);
        }
        command(new Balance());
        command(new Pay());
        command(new PayToggle());
        command(new BalanceTop());
        command(new Eco());
    }

    @Override
    public void disable() {
        if (store != null) {
            store.shutdown();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (store != null) {
            store.create(event.getPlayer());
        }
    }

    private EconomyService economy() throws CommandException {
        EconomyService economy = plugin.hooks().economy();
        if (!economy.available()) {
            throw new CommandException("errors.economy-unavailable");
        }
        return economy;
    }

    private String money(double amount) {
        return plugin.hooks().economy().format(amount);
    }

    // ---- commands ----------------------------------------------------------------------------------

    private final class Balance extends AbstractCommand {

        Balance() {
            super(EconomyModule.this.plugin, "balance", "bal", "money");
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            EconomyService economy = economy();
            if (ctx.has(0)) {
                OfflinePlayer target = ctx.knownPlayer(0);
                if (!target.getUniqueId().equals(ctx.isPlayer() ? ((Player) ctx.sender()).getUniqueId() : null)) {
                    ctx.require(ctx.othersPermission());
                    ctx.replyCmd("other", Ph.of("player", String.valueOf(target.getName())),
                            Ph.of("amount", money(economy.balance(target))));
                    return;
                }
            }
            Player self = ctx.player();
            ctx.replyCmd("self", Ph.of("amount", money(economy.balance(self))));
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? othersIfAllowed(ctx) : List.of();
        }
    }

    private final class Pay extends AbstractCommand {

        Pay() {
            super(EconomyModule.this.plugin, "pay");
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            if (ctx.size() < 2) {
                throw ctx.usage();
            }
            EconomyService economy = economy();
            Player self = ctx.player();
            OfflinePlayer target = ctx.knownPlayer(0);
            if (target.getUniqueId().equals(self.getUniqueId())) {
                throw new CommandException("commands.pay.self");
            }
            Player online = target.getPlayer();
            if (online == null && !ctx.hasPermission("applecore.pay.offline")) {
                throw new CommandException("errors.player-not-found", Ph.of("player", ctx.arg(0)));
            }
            if (online != null && !plugin.users().user(online).payments()) {
                throw new CommandException("commands.pay.disabled", Ph.of("player", online.getName()));
            }
            double min = plugin.getConfig().getDouble("economy.pay.min", 1);
            double amount = Math.floor(ctx.decimal(1, min, 1_000_000_000_000d) * 100) / 100;
            if (!economy.has(self, amount)) {
                throw new CommandException("errors.not-enough-money", Ph.of("amount", money(amount)));
            }
            double commission = Math.floor(amount * plugin.getConfig().getDouble("economy.pay.commission-percent", 0)) / 100;
            if (!economy.withdraw(self, amount)) {
                throw new CommandException("errors.not-enough-money", Ph.of("amount", money(amount)));
            }
            economy.deposit(target, amount - commission);
            ctx.replyCmd(commission > 0 ? "sent-commission" : "sent", Ph.of("player", String.valueOf(target.getName())),
                    Ph.of("amount", money(amount)), Ph.of("commission", money(commission)),
                    Ph.of("received", money(amount - commission)));
            plugin.pack().play(self, "success");
            if (online != null) {
                plugin.messages().send(online, "commands.pay.received", Ph.of("player", self.getName()),
                        Ph.of("amount", money(amount - commission)));
                plugin.pack().play(online, "success");
            }
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? players(ctx) : ctx.size() == 2 ? List.of("100", "1000") : List.of();
        }
    }

    private final class PayToggle extends AbstractCommand {

        PayToggle() {
            super(EconomyModule.this.plugin, "paytoggle", "payoff", "payon");
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player who = ctx.targetOrSelf(0);
            User user = plugin.users().user(who);
            boolean value = !user.payments();
            user.payments(value);
            plugin.messages().send(who, value ? "commands.paytoggle.turned-on" : "commands.paytoggle.turned-off");
            if (!ctx.isSelf(who)) {
                ctx.replyCmd("changed-other", Ph.of("player", who.getName()),
                        Ph.of("state", plugin.messages().bool(ctx.sender(), value)));
            }
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? othersIfAllowed(ctx) : List.of();
        }
    }

    /** /balancetop [page]: AppleCore accounts directly, or a cached snapshot of another economy plugin. */
    private final class BalanceTop extends AbstractCommand {

        BalanceTop() {
            super(EconomyModule.this.plugin, "balancetop", "baltop", "topbal");
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            EconomyService economy = economy();
            int page = ctx.has(0) ? ctx.integer(0, 1, 1000) : 1;
            List<TopEntry> top = top(economy);
            int perPage = 10;
            int pages = Math.max(1, (top.size() + perPage - 1) / perPage);
            page = Math.min(page, pages);
            ctx.replyCmd("header", Ph.of("page", page), Ph.of("pages", pages));
            for (int i = (page - 1) * perPage; i < Math.min(top.size(), page * perPage); i++) {
                TopEntry entry = top.get(i);
                ctx.replyCmd("entry", Ph.of("place", i + 1), Ph.of("player", entry.name()), Ph.of("amount", money(entry.balance())));
            }
            if (top.isEmpty()) {
                ctx.replyCmd("empty");
            }
        }

        private List<TopEntry> top(EconomyService economy) {
            long refresh = Durations.parseOr(plugin.getConfig().get("economy.baltop-refresh"), 300_000L);
            if (System.currentTimeMillis() - topBuiltAt < refresh && !cachedTop.isEmpty()) {
                return cachedTop;
            }
            List<TopEntry> list = new ArrayList<>();
            if (economy.usesInternal() && store != null) {
                for (Map.Entry<UUID, BalanceStore.Account> entry : store.top()) {
                    list.add(new TopEntry(entry.getValue().name(), entry.getValue().balance()));
                }
            } else {
                for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                    if (player.getName() != null) {
                        list.add(new TopEntry(player.getName(), economy.balance(player)));
                    }
                }
                list.sort((a, b) -> Double.compare(b.balance(), a.balance()));
            }
            cachedTop = list;
            topBuiltAt = System.currentTimeMillis();
            return list;
        }
    }

    /** /eco <give|take|set|reset> <player> [amount] — EssentialsX syntax, works from the console. */
    private final class Eco extends AbstractCommand {

        Eco() {
            super(EconomyModule.this.plugin, "eco", "economy");
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            if (ctx.size() < 2) {
                throw ctx.usage();
            }
            EconomyService economy = economy();
            String action = ctx.arg(0).toLowerCase(Locale.ROOT);
            OfflinePlayer target = ctx.knownPlayer(1);
            double amount = action.equals("reset") ? plugin.getConfig().getDouble("economy.starting-balance", 0)
                    : ctx.decimal(2, 0, 1_000_000_000_000d);
            boolean ok = switch (action) {
                case "give" -> economy.deposit(target, amount);
                case "take" -> {
                    if (!economy.has(target, amount)) {
                        throw new CommandException("commands.eco.insufficient", Ph.of("player", String.valueOf(target.getName())),
                                Ph.of("amount", money(economy.balance(target))));
                    }
                    yield economy.withdraw(target, amount);
                }
                case "set", "reset" -> economy.set(target, amount);
                default -> throw ctx.usage();
            };
            if (!ok) {
                throw new CommandException("commands.eco.failed");
            }
            double balance = economy.balance(target);
            ctx.replyCmd("done", Ph.of("player", String.valueOf(target.getName())), Ph.of("balance", money(balance)));
            Player online = target.getPlayer();
            if (online != null && !online.equals(ctx.sender()) && plugin.getConfig().getBoolean("economy.notify-eco", true)) {
                plugin.messages().send(online, "commands.eco.notify-" + (action.equals("reset") ? "set" : action),
                        Ph.of("amount", money(amount)), Ph.of("balance", money(balance)));
            }
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            if (ctx.size() == 1) {
                return List.of("give", "take", "set", "reset");
            }
            return ctx.size() == 2 ? players(ctx) : List.of();
        }
    }

    /** Keeps the Vault types out of EconomyModule so the module loads without Vault. */
    private static final class VaultRegistration {
        static void register(AppleCorePlugin plugin, BalanceStore store, ServicePriority priority) {
            Bukkit.getServicesManager().register(net.milkbowl.vault.economy.Economy.class,
                    new AppleEconomy(plugin, store), plugin, priority);
            plugin.getLogger().info("AppleCore economy registered in Vault (" + priority + ")");
        }
    }
}
