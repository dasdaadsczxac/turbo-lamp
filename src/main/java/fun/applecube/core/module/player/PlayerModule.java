package fun.applecube.core.module.player;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.command.AbstractCommand;
import fun.applecube.core.command.CommandContext;
import fun.applecube.core.command.CommandException;
import fun.applecube.core.module.Module;
import fun.applecube.core.text.Ph;
import fun.applecube.core.text.Text;
import fun.applecube.core.user.User;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Essentials-style player utilities (stage 3): abilities, information, items, workstations, nick, vanish,
 * invsee and /recipe. Chat features are intentionally not part of AppleCore.
 */
public final class PlayerModule extends Module implements Listener {

    static NamespacedKey SPAWNED_KEY;

    private final Map<UUID, Long> fallProtection = new ConcurrentHashMap<>();
    private final Map<UUID, InvseeSession> invsee = new ConcurrentHashMap<>();
    private @Nullable BukkitTask vanishTask;

    public PlayerModule(AppleCorePlugin plugin) {
        super(plugin);
        SPAWNED_KEY = new NamespacedKey(plugin, "spawned");
    }

    @Override
    public String id() {
        return "player";
    }

    @Override
    public void enable() {
        listen(this);
        List<AbstractCommand> commands = new ArrayList<>();
        commands.addAll(AbilityCommands.all(plugin));
        commands.addAll(InfoCommands.all(plugin));
        commands.addAll(ItemCommands.all(plugin));
        commands.addAll(WorkstationCommands.all(plugin));
        commands.add(new Nick());
        commands.add(new Vanish());
        commands.add(new Invsee());
        commands.add(new RecipeCommand(plugin));
        commands.forEach(this::command);
        vanishTask = Bukkit.getScheduler().runTaskTimer(plugin, this::remindVanished, 40L, 40L);
    }

    @Override
    public void disable() {
        if (vanishTask != null) {
            vanishTask.cancel();
        }
    }

    // ---- fall protection ---------------------------------------------------------------------------

    void protectFall(Player player) {
        fallProtection.put(player.getUniqueId(), System.currentTimeMillis() + 15_000);
    }

    // ---- nick --------------------------------------------------------------------------------------

    private Component nickComponent(String raw) {
        return Component.text(plugin.getConfig().getString("nick.prefix", "~")).append(Text.userFormatted(raw));
    }

    private void applyNick(Player player, User user) {
        String nick = user.nickname();
        if (nick == null) {
            player.displayName(null);
            player.playerListName(null);
            return;
        }
        Component name = nickComponent(nick);
        player.displayName(name);
        if (plugin.getConfig().getBoolean("nick.tab-list", true)) {
            player.playerListName(name);
        }
    }

    /** /nick [player] <nick|off>; colours need applecore.nick.color (essentials.nick.color). */
    private final class Nick extends AbstractCommand {

        Nick() {
            super(PlayerModule.this.plugin, "nick", "nickname");
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            if (!ctx.has(0)) {
                throw ctx.usage();
            }
            Player who = ctx.has(1) ? ctx.onlinePlayer(0) : ctx.player();
            if (!ctx.isSelf(who)) {
                ctx.require("applecore.nick.others");
            }
            String raw = ctx.has(1) ? ctx.arg(1) : ctx.arg(0);
            User user = plugin.users().user(who);
            if (raw.equalsIgnoreCase("off") || raw.equalsIgnoreCase("reset") || raw.equals(who.getName())) {
                user.nickname(null);
                applyNick(who, user);
                plugin.messages().send(who, "commands.nick.reset");
                if (!ctx.isSelf(who)) {
                    ctx.replyCmd("reset-other", Ph.of("player", who.getName()));
                }
                return;
            }
            boolean colors = ctx.hasPermission("applecore.nick.color");
            String stored = colors ? raw : Text.plain(Text.userFormatted(raw));
            String plain = Text.plain(Text.userFormatted(stored));
            int min = plugin.getConfig().getInt("nick.min-length", 3);
            int max = plugin.getConfig().getInt("nick.max-length", 16);
            Pattern allowed = Pattern.compile(plugin.getConfig().getString("nick.allowed", "[\\p{L}\\p{N}_]+"));
            if (plain.length() < min || plain.length() > max || !allowed.matcher(plain).matches()) {
                throw new CommandException("commands.nick.invalid", Ph.of("min", min), Ph.of("max", max));
            }
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (!other.equals(who) && (other.getName().equalsIgnoreCase(plain)
                        || Text.plain(other.displayName()).replace(plugin.getConfig().getString("nick.prefix", "~"), "")
                        .equalsIgnoreCase(plain))) {
                    throw new CommandException("commands.nick.taken", Ph.of("nick", plain));
                }
            }
            user.nickname(stored);
            applyNick(who, user);
            plugin.messages().send(who, "commands.nick.done", Ph.of("nick", who.displayName()));
            if (!ctx.isSelf(who)) {
                ctx.replyCmd("done-other", Ph.of("player", who.getName()), Ph.of("nick", who.displayName()));
            }
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 && ctx.hasPermission("applecore.nick.others") ? players(ctx) : List.of("off");
        }
    }

    // ---- vanish ------------------------------------------------------------------------------------

    private boolean seesVanished(Player viewer) {
        return plugin.permissions().has(viewer, "applecore.vanish.see");
    }

    void setVanished(Player player, boolean vanished) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) {
                continue;
            }
            if (vanished && !seesVanished(other)) {
                other.hidePlayer(plugin, player);
            } else {
                other.showPlayer(plugin, player);
            }
        }
    }

    private void remindVanished() {
        for (User user : plugin.users().onlineUsers()) {
            if (user.vanished()) {
                Player player = Bukkit.getPlayer(user.uuid());
                if (player != null) {
                    plugin.messages().actionBar(player, "commands.vanish.actionbar");
                }
            }
        }
    }

    private final class Vanish extends AbilityCommands.Toggle {

        Vanish() {
            super(PlayerModule.this.plugin, "vanish", "v");
        }

        @Override
        boolean current(Player player, User user) {
            return user.vanished();
        }

        @Override
        void apply(Player player, User user, boolean value) {
            user.vanished(value);
            setVanished(player, value);
        }
    }

    // ---- invsee ------------------------------------------------------------------------------------

    private record InvseeSession(UUID target, boolean canModify) {
    }

    /** /invsee <player>; editing needs applecore.invsee.modify, applecore.invsee.preventmodify protects the target. */
    private final class Invsee extends AbstractCommand {

        Invsee() {
            super(PlayerModule.this.plugin, "invsee");
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player self = ctx.player();
            Player target = ctx.onlinePlayer(0);
            if (target.equals(self)) {
                throw new CommandException("commands.invsee.self");
            }
            boolean modify = ctx.hasPermission("applecore.invsee.modify")
                    && !plugin.permissions().hasExplicit(target, "applecore.invsee.preventmodify");
            self.openInventory(target.getInventory());
            invsee.put(self.getUniqueId(), new InvseeSession(target.getUniqueId(), modify));
            ctx.replyCmd(modify ? "opened-edit" : "opened", Ph.of("player", target.getName()));
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? players(ctx) : List.of();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInvseeClick(InventoryClickEvent event) {
        InvseeSession session = invsee.get(event.getWhoClicked().getUniqueId());
        if (session != null && !session.canModify() && event.getView().getTopInventory() instanceof PlayerInventory) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInvseeDrag(InventoryDragEvent event) {
        InvseeSession session = invsee.get(event.getWhoClicked().getUniqueId());
        if (session != null && !session.canModify() && event.getView().getTopInventory() instanceof PlayerInventory) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        invsee.remove(event.getPlayer().getUniqueId());
        if (event.getInventory().getHolder(false) instanceof WorkstationCommands.TrashHolder) {
            int destroyed = 0;
            for (var item : event.getInventory().getContents()) {
                if (item != null) {
                    destroyed += item.getAmount();
                }
            }
            event.getInventory().clear();
            if (destroyed > 0 && event.getPlayer() instanceof Player player) {
                plugin.messages().send(player, "commands.disposal.done", Ph.of("amount", destroyed));
            }
        }
    }

    // ---- join / quit -------------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        User user = plugin.users().user(player);
        if (user.fly() && plugin.permissions().has(player, "applecore.fly")) {
            player.setAllowFlight(true);
            if (!player.isOnGround()) {
                player.setFlying(true);
            }
        }
        if (user.god() && !plugin.permissions().has(player, "applecore.god")) {
            user.god(false);
        }
        if (user.nickname() != null) {
            applyNick(player, user);
        }
        if (user.vanished()) {
            if (plugin.permissions().has(player, "applecore.vanish")) {
                setVanished(player, true);
                event.joinMessage(null);
            } else {
                user.vanished(false);
            }
        }
        if (!seesVanished(player)) {
            for (User other : plugin.users().onlineUsers()) {
                Player hidden = other.vanished() ? Bukkit.getPlayer(other.uuid()) : null;
                if (hidden != null && !hidden.equals(player)) {
                    player.hidePlayer(plugin, hidden);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        fallProtection.remove(event.getPlayer().getUniqueId());
        invsee.remove(event.getPlayer().getUniqueId());
        if (plugin.users().user(event.getPlayer()).vanished()) {
            event.quitMessage(null);
        }
    }

    // ---- god, fall, vanish side effects ------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (plugin.users().user(player).god()) {
            event.setCancelled(true);
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            Long until = fallProtection.remove(player.getUniqueId());
            if (until != null && until > System.currentTimeMillis()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && plugin.users().user(player).god()
                && event.getFoodLevel() < player.getFoodLevel()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && plugin.users().user(player).vanished()
                && plugin.getConfig().getBoolean("vanish.prevent-pickup", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player player && plugin.users().user(player).vanished()) {
            event.setCancelled(true);
        }
    }

    /** /fireball and /beezooka never break blocks unless fireball.block-damage is true. */
    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        if (ItemCommands.spawnedByCommand(event.getEntity()) && !plugin.getConfig().getBoolean("fireball.block-damage", false)) {
            event.blockList().clear();
        }
    }
}
