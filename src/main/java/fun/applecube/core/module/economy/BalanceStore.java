package fun.applecube.core.module.economy;

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.hook.EconomyBackend;
import fun.applecube.core.storage.JsonFiles;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * AppleCore's own accounts ({@code data/economy.json}), kept in memory and thread-safe because Vault callers
 * may run off the main thread. Saved every minute when changed and on shutdown.
 */
public final class BalanceStore implements EconomyBackend {

    public static final class Account {
        String name;
        double balance;

        Account(String name, double balance) {
            this.name = name;
            this.balance = balance;
        }

        public String name() {
            return name;
        }

        public double balance() {
            return balance;
        }
    }

    private final AppleCorePlugin plugin;
    private final Path file;
    private final Map<UUID, Account> accounts = new ConcurrentHashMap<>();
    private volatile boolean dirty;
    private @Nullable BukkitTask saver;

    public BalanceStore(AppleCorePlugin plugin) {
        this.plugin = plugin;
        this.file = plugin.getDataFolder().toPath().resolve("data").resolve("economy.json");
    }

    public void load() {
        try {
            Map<String, Account> stored = JsonFiles.read(file, new TypeToken<Map<String, Account>>() { }.getType());
            accounts.clear();
            if (stored != null) {
                stored.forEach((id, account) -> accounts.put(UUID.fromString(id), account));
            }
        } catch (IOException | JsonParseException | IllegalArgumentException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not read " + file + "; balances start empty (file kept)", e);
        }
        saver = Bukkit.getScheduler().runTaskTimer(plugin, () -> save(false), 1200L, 1200L);
    }

    public void shutdown() {
        if (saver != null) {
            saver.cancel();
        }
        save(true);
    }

    public void save(boolean now) {
        if (!dirty) {
            return;
        }
        dirty = false;
        Map<String, Account> snapshot = new LinkedHashMap<>();
        accounts.forEach((id, account) -> snapshot.put(id.toString(), new Account(account.name, account.balance)));
        Runnable write = () -> {
            try {
                JsonFiles.write(file, snapshot);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save " + file, e);
                dirty = true;
            }
        };
        if (now) {
            write.run();
        } else {
            plugin.io().run(write);
        }
    }

    private double starting() {
        return plugin.getConfig().getDouble("economy.starting-balance", 0);
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static String nameOf(OfflinePlayer player) {
        return player.getName() != null ? player.getName() : player.getUniqueId().toString();
    }

    public boolean hasAccount(UUID id) {
        return accounts.containsKey(id);
    }

    public void create(OfflinePlayer player) {
        accounts.computeIfAbsent(player.getUniqueId(), id -> {
            dirty = true;
            return new Account(nameOf(player), round(starting()));
        });
    }

    @Override
    public double balance(OfflinePlayer player) {
        Account account = accounts.get(player.getUniqueId());
        return account == null ? round(starting()) : account.balance;
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return false;
        }
        boolean[] ok = {false};
        accounts.compute(player.getUniqueId(), (id, account) -> {
            Account current = account != null ? account : new Account(nameOf(player), round(starting()));
            if (current.balance + 1e-9 >= amount) {
                current.balance = round(current.balance - amount);
                ok[0] = true;
            }
            return current;
        });
        dirty = true;
        return ok[0];
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return false;
        }
        accounts.compute(player.getUniqueId(), (id, account) -> {
            Account current = account != null ? account : new Account(nameOf(player), round(starting()));
            current.balance = round(current.balance + amount);
            if (player.getName() != null) {
                current.name = player.getName();
            }
            return current;
        });
        dirty = true;
        return true;
    }

    @Override
    public void set(OfflinePlayer player, double amount) {
        accounts.compute(player.getUniqueId(), (id, account) -> {
            Account current = account != null ? account : new Account(nameOf(player), 0);
            current.balance = round(Math.max(0, amount));
            return current;
        });
        dirty = true;
    }

    public @Nullable UUID byName(String name) {
        for (Map.Entry<UUID, Account> entry : accounts.entrySet()) {
            if (entry.getValue().name.equalsIgnoreCase(name)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** Accounts sorted by balance, richest first. */
    public List<Map.Entry<UUID, Account>> top() {
        List<Map.Entry<UUID, Account>> list = new ArrayList<>(accounts.entrySet());
        list.sort(Comparator.comparingDouble((Map.Entry<UUID, Account> e) -> e.getValue().balance).reversed()
                .thenComparing(e -> e.getValue().name.toLowerCase(Locale.ROOT)));
        return list;
    }
}
