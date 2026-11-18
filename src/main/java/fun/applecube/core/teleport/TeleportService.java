package fun.applecube.core.teleport;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.storage.StoredLocation;
import fun.applecube.core.text.Ph;
import fun.applecube.core.user.User;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * All AppleCore teleports go through here: warm-up with boss bar and particles (cancelled by moving
 * or taking damage), chunk pre-loading, safe-spot search, short invulnerability, sound, and /back.
 */
public final class TeleportService implements Listener {

    private static final Set<PlayerTeleportEvent.TeleportCause> BACK_CAUSES = EnumSet.of(
            PlayerTeleportEvent.TeleportCause.COMMAND, PlayerTeleportEvent.TeleportCause.PLUGIN,
            PlayerTeleportEvent.TeleportCause.UNKNOWN);

    private final AppleCorePlugin plugin;
    private final Map<UUID, Warmup> warmups = new HashMap<>();

    public TeleportService(AppleCorePlugin plugin) {
        this.plugin = plugin;
    }

    /** Teleports after the player's warm-up delay. {@code onArrive} runs on success only. */
    public void teleport(Player player, Location target, @Nullable Runnable onArrive) {
        teleport(player, target, delaySeconds(player), onArrive);
    }

    public void teleport(Player player, Location target, int delaySeconds, @Nullable Runnable onArrive) {
        Location fixed = target.clone();
        teleport(player, () -> fixed, delaySeconds, onArrive);
    }

    /**
     * Teleports to a destination resolved when the warm-up ends (e.g. another player's current position).
     * A null destination aborts with {@code teleport.failed}.
     */
    public void teleport(Player player, Supplier<@Nullable Location> target, int delaySeconds, @Nullable Runnable onArrive) {
        cancel(player.getUniqueId());
        if (delaySeconds <= 0) {
            finish(player, target, onArrive);
            return;
        }
        Warmup warmup = new Warmup(player, target, delaySeconds, onArrive);
        warmups.put(player.getUniqueId(), warmup);
        warmup.start();
        plugin.messages().send(player, "teleport.warmup", Ph.of("seconds", delaySeconds));
    }

    /** Warm-up in seconds: config default, lowered by {@code applecore.teleport.delay.<n>}. */
    public int delaySeconds(Player player) {
        if (plugin.permissions().has(player, "applecore.teleport.bypass-delay")) {
            return 0;
        }
        int base = Math.max(0, plugin.getConfig().getInt("teleport.delay", 3));
        return Math.max(0, plugin.permissions().lowest(player, "applecore.teleport.delay.", base));
    }

    public boolean isWarmingUp(Player player) {
        return warmups.containsKey(player.getUniqueId());
    }

    public void cancel(UUID id) {
        Warmup warmup = warmups.remove(id);
        if (warmup != null) {
            warmup.stop();
        }
    }

    public void cancelAll() {
        for (Warmup warmup : warmups.values()) {
            warmup.stop();
        }
        warmups.clear();
    }

    private void finish(Player player, Supplier<@Nullable Location> destinationSupplier, @Nullable Runnable onArrive) {
        Location target = destinationSupplier.get();
        if (target == null || target.getWorld() == null) {
            plugin.messages().send(player, "teleport.failed");
            return;
        }
        boolean checkSafety = plugin.getConfig().getBoolean("teleport.safe-location", true)
                && !player.getAllowFlight()
                && (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE);
        target.getWorld().getChunkAtAsync(target).thenAccept(chunk -> {
            if (!player.isOnline()) {
                return;
            }
            Location destination = checkSafety ? SafeLocation.find(target) : target;
            if (destination == null) {
                plugin.messages().send(player, "teleport.unsafe");
                plugin.pack().play(player, "error");
                return;
            }
            player.teleportAsync(destination, PlayerTeleportEvent.TeleportCause.COMMAND).thenAccept(success -> {
                if (!success) {
                    plugin.messages().send(player, "teleport.failed");
                    return;
                }
                int ticks = plugin.getConfig().getInt("teleport.invulnerability-ticks", 60);
                if (ticks > 0) {
                    player.setNoDamageTicks(ticks);
                }
                plugin.pack().play(player, "teleport");
                if (onArrive != null) {
                    onArrive.run();
                }
            });
        });
    }

    // ---- events ------------------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock() || !plugin.getConfig().getBoolean("teleport.cancel-on-move", true)) {
            return;
        }
        Warmup warmup = warmups.remove(event.getPlayer().getUniqueId());
        if (warmup != null) {
            warmup.stop();
            plugin.messages().send(event.getPlayer(), "teleport.cancelled-move");
            plugin.pack().play(event.getPlayer(), "error");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !plugin.getConfig().getBoolean("teleport.cancel-on-damage", true)) {
            return;
        }
        Warmup warmup = warmups.remove(player.getUniqueId());
        if (warmup != null) {
            warmup.stop();
            plugin.messages().send(player, "teleport.cancelled-damage");
            plugin.pack().play(player, "error");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer().getUniqueId());
    }

    /** Remembers where a player came from, for /back. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!BACK_CAUSES.contains(event.getCause()) || !plugin.getConfig().getBoolean("teleport.back.record-teleports", true)) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getWorld() == to.getWorld() && from.distanceSquared(to) < 4) {
            return;
        }
        plugin.users().user(event.getPlayer()).lastLocation(StoredLocation.of(from));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (plugin.getConfig().getBoolean("teleport.back.record-death", true)
                && plugin.permissions().has(player, "applecore.back.ondeath")) {
            User user = plugin.users().user(player);
            user.lastLocation(StoredLocation.of(player.getLocation()));
        }
    }

    // ---- warm-up -----------------------------------------------------------------------------------

    private final class Warmup {

        private final Player player;
        private final Supplier<@Nullable Location> target;
        private final long durationMillis;
        private final @Nullable Runnable onArrive;
        private final long startedAt = System.currentTimeMillis();
        private @Nullable BossBar bar;
        private @Nullable BukkitTask task;
        private long shownSeconds = -1;

        Warmup(Player player, Supplier<@Nullable Location> target, int seconds, @Nullable Runnable onArrive) {
            this.player = player;
            this.target = target;
            this.durationMillis = seconds * 1000L;
            this.onArrive = onArrive;
        }

        void start() {
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("teleport.bossbar");
            if (section == null || section.getBoolean("enabled", true)) {
                bar = BossBar.bossBar(net.kyori.adventure.text.Component.empty(), 1f,
                        color(section == null ? null : section.getString("color")),
                        overlay(section == null ? null : section.getString("overlay")));
                updateBar(durationMillis);
                player.showBossBar(bar);
            }
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 2L, 2L);
        }

        private void tick() {
            long left = durationMillis - (System.currentTimeMillis() - startedAt);
            if (left <= 0) {
                warmups.remove(player.getUniqueId(), this);
                stop();
                finish(player, target, onArrive);
                return;
            }
            updateBar(left);
            if (plugin.getConfig().getBoolean("teleport.particles", true)) {
                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 8, 0.4, 0.6, 0.4, 0.3);
            }
        }

        private void updateBar(long left) {
            if (bar == null) {
                return;
            }
            bar.progress(Math.max(0f, Math.min(1f, (float) left / durationMillis)));
            long seconds = (left + 999) / 1000;
            if (seconds != shownSeconds) {
                shownSeconds = seconds;
                bar.name(plugin.messages().get(player, "teleport.bossbar", Ph.of("seconds", seconds)));
            }
        }

        void stop() {
            if (task != null) {
                task.cancel();
            }
            if (bar != null) {
                player.hideBossBar(bar);
            }
        }

        private BossBar.Color color(@Nullable String raw) {
            try {
                return raw == null ? BossBar.Color.RED : BossBar.Color.valueOf(raw.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return BossBar.Color.RED;
            }
        }

        private BossBar.Overlay overlay(@Nullable String raw) {
            try {
                return raw == null ? BossBar.Overlay.PROGRESS : BossBar.Overlay.valueOf(raw.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return BossBar.Overlay.PROGRESS;
            }
        }
    }
}
