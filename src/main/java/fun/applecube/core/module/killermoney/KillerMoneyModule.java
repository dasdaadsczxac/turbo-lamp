package fun.applecube.core.module.killermoney;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.config.ConfigFile;
import fun.applecube.core.hook.EconomyService;
import fun.applecube.core.module.Module;
import fun.applecube.core.module.ranks.RanksModule;
import fun.applecube.core.text.Ph;
import fun.applecube.core.util.Durations;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * KillerMoney (stage 5): money for killing mobs (ranges per type, anti-farm limits) and a share of the victim's
 * balance for PvP kills. Rewards are multiplied by the player's donation rank (ranks.yml killermoney: x1.5) or
 * applecore.killermoney.multiplier.<x>, and by global-multiplier.
 */
public final class KillerMoneyModule extends Module implements Listener {

    private final ConfigFile file;
    private final Map<EntityType, Reward> rewards = new EnumMap<>(EntityType.class);
    private final Map<String, Window> chunkKills = new ConcurrentHashMap<>();
    private final Map<String, Long> pvpKills = new ConcurrentHashMap<>();
    private @Nullable Reward fallback;
    private YamlConfiguration cfg = new YamlConfiguration();

    public KillerMoneyModule(AppleCorePlugin plugin) {
        super(plugin);
        this.file = new ConfigFile(plugin, "killermoney.yml", true);
    }

    @Override
    public String id() {
        return "killermoney";
    }

    @Override
    public void enable() {
        load();
        listen(this);
        command(new KillerMoneyCommand(plugin, this));
    }

    @Override
    public void reload() {
        load();
    }

    private void load() {
        cfg = file.load().yaml();
        rewards.clear();
        fallback = null;
        ConfigurationSection mobs = cfg.getConfigurationSection("mobs");
        if (mobs != null) {
            for (String key : mobs.getKeys(false)) {
                Reward reward = Reward.parse(mobs.getString(key));
                if (key.equalsIgnoreCase("default")) {
                    fallback = reward;
                    continue;
                }
                NamespacedKey id = NamespacedKey.fromString(key.toLowerCase(Locale.ROOT));
                EntityType type = id == null ? null : Registry.ENTITY_TYPE.get(id);
                if (type == null) {
                    plugin.getLogger().warning("killermoney.yml: unknown mob '" + key + "'");
                } else if (reward != null) {
                    rewards.put(type, reward);
                }
            }
        }
    }

    YamlConfiguration config() {
        return cfg;
    }

    /** Rank multiplier or applecore.killermoney.multiplier.<x> (the larger), times global-multiplier. */
    public double multiplier(Player player) {
        RanksModule ranks = plugin.module(RanksModule.class);
        double rank = ranks == null ? 1 : ranks.killerMoney(player);
        double perm = plugin.permissions().highestDecimal(player, "applecore.killermoney.multiplier.", 1);
        return Math.max(rank, perm) * RanksModule.parseMultiplier(cfg.getString("global-multiplier", "1"));
    }

    private boolean worldAllowed(LivingEntity entity) {
        return !cfg.getStringList("disabled-worlds").contains(entity.getWorld().getName());
    }

    private double round(double amount) {
        double scale = Math.pow(10, Math.max(0, Math.min(4, cfg.getInt("decimals", 2))));
        return Math.floor(amount * scale) / scale;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player || !cfg.getBoolean("enabled", true)) {
            return;
        }
        Player killer = entity.getKiller();
        if (killer == null || !worldAllowed(entity)) {
            return;
        }
        if (killer.getGameMode() == GameMode.CREATIVE && !cfg.getBoolean("anti-farm.creative", false)) {
            return;
        }
        Reward reward = rewards.getOrDefault(entity.getType(), fallback);
        if (reward == null || reward.max() <= 0) {
            return;
        }
        CreatureSpawnEvent.SpawnReason reason = entity.getEntitySpawnReason();
        if ((reason == CreatureSpawnEvent.SpawnReason.SPAWNER || reason == CreatureSpawnEvent.SpawnReason.TRIAL_SPAWNER)
                && !cfg.getBoolean("anti-farm.spawner-mobs", false)) {
            return;
        }
        if (reason == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG && !cfg.getBoolean("anti-farm.spawn-egg-mobs", false)) {
            return;
        }
        double chance = cfg.getDouble("chance", 100);
        if (chance < 100 && ThreadLocalRandom.current().nextDouble(100) >= chance) {
            return;
        }
        if (!chunkAllows(killer, entity)) {
            return;
        }
        EconomyService economy = plugin.hooks().economy();
        if (!economy.available()) {
            return;
        }
        double multiplier = multiplier(killer);
        double amount = round(reward.roll() * multiplier);
        if (amount <= 0 || !economy.deposit(killer, amount)) {
            return;
        }
        Component mob = entity.customName() != null ? entity.customName() : Component.translatable(entity.getType().translationKey());
        Component bonus = multiplier > 1.0001
                ? plugin.messages().get(killer, "killermoney.multiplier", Ph.of("value", KillerMoneyCommand.format(multiplier)))
                : Component.empty();
        String notify = cfg.getString("notify", "actionbar").toLowerCase(Locale.ROOT);
        if (notify.equals("chat")) {
            plugin.messages().send(killer, "killermoney.mob", Ph.of("amount", economy.format(amount)), Ph.of("mob", mob), Ph.of("multiplier", bonus));
        } else if (!notify.equals("none")) {
            plugin.messages().actionBar(killer, "killermoney.mob", Ph.of("amount", economy.format(amount)), Ph.of("mob", mob), Ph.of("multiplier", bonus));
        }
    }

    /** At most anti-farm.chunk-limit rewards per player and chunk within anti-farm.period. */
    private boolean chunkAllows(Player killer, LivingEntity entity) {
        int limit = cfg.getInt("anti-farm.chunk-limit", 40);
        if (limit <= 0) {
            return true;
        }
        long period = Durations.parseOr(cfg.getString("anti-farm.period", "10m"), 600_000L);
        long now = System.currentTimeMillis();
        String key = killer.getUniqueId() + ":" + entity.getWorld().getName() + ":" + (entity.getLocation().getBlockX() >> 4)
                + ":" + (entity.getLocation().getBlockZ() >> 4);
        Window window = chunkKills.compute(key, (k, w) -> w == null || now - w.start() > period ? new Window(now, 1) : new Window(w.start(), w.count() + 1));
        return window.count() <= limit;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKill(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim) || !cfg.getBoolean("enabled", true) || !cfg.getBoolean("pvp.enabled", true)
                || !worldAllowed(victim)) {
            return;
        }
        if (cfg.getBoolean("pvp.ignore-same-ip", true) && sameAddress(killer, victim)) {
            return;
        }
        long now = System.currentTimeMillis();
        long cooldown = Durations.parseOr(cfg.getString("pvp.same-victim-cooldown", "10m"), 600_000L);
        String pair = killer.getUniqueId() + ">" + victim.getUniqueId();
        Long last = pvpKills.get(pair);
        if (last != null && now - last < cooldown) {
            plugin.messages().send(killer, "killermoney.pvp-cooldown", Ph.of("player", victim.getName()),
                    Ph.of("time", plugin.messages().duration(killer, cooldown - (now - last))));
            return;
        }
        EconomyService economy = plugin.hooks().economy();
        if (!economy.available()) {
            return;
        }
        double balance = economy.balance(victim);
        double amount = balance * cfg.getDouble("pvp.steal-percent", 5) / 100.0;
        if (cfg.getBoolean("pvp.apply-multiplier", false)) {
            amount *= multiplier(killer);
        }
        amount = Math.min(amount, cfg.getDouble("pvp.max", 5000));
        amount = round(Math.min(balance, amount));
        if (amount < cfg.getDouble("pvp.min", 1) || amount <= 0) {
            return;
        }
        if (!economy.withdraw(victim, amount)) {
            return;
        }
        if (!economy.deposit(killer, amount)) {
            economy.deposit(victim, amount);
            return;
        }
        pvpKills.put(pair, now);
        plugin.messages().send(killer, "killermoney.pvp-killer", Ph.of("amount", economy.format(amount)), Ph.of("player", victim.getName()));
        plugin.messages().send(victim, "killermoney.pvp-victim", Ph.of("amount", economy.format(amount)), Ph.of("player", killer.getName()));
        plugin.pack().play(killer, "success");
    }

    private static boolean sameAddress(Player a, Player b) {
        InetSocketAddress x = a.getAddress();
        InetSocketAddress y = b.getAddress();
        return x != null && y != null && x.getAddress() != null && x.getAddress().equals(y.getAddress());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        String prefix = event.getPlayer().getUniqueId() + ":";
        chunkKills.keySet().removeIf(key -> key.startsWith(prefix));
    }

    /** Reward per mob type for /killermoney: the configured ones, sorted by the maximum. */
    List<Map.Entry<EntityType, Reward>> table() {
        return rewards.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue().max(), a.getValue().max()))
                .toList();
    }

    private record Window(long start, int count) {
    }

    /** A fixed amount or a "min-max" range. */
    record Reward(double min, double max) {

        static @Nullable Reward parse(@Nullable String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            String s = raw.trim().replace(',', '.');
            int dash = s.indexOf('-', 1);
            try {
                if (dash > 0) {
                    double a = Double.parseDouble(s.substring(0, dash).trim());
                    double b = Double.parseDouble(s.substring(dash + 1).trim());
                    return new Reward(Math.min(a, b), Math.max(a, b));
                }
                double v = Double.parseDouble(s);
                return new Reward(v, v);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        double roll() {
            return max <= min ? min : ThreadLocalRandom.current().nextDouble(min, max);
        }
    }
}
