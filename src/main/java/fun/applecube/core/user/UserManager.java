package fun.applecube.core.user;

import com.google.gson.JsonParseException;
import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.storage.JsonFiles;
import fun.applecube.core.storage.StoredLocation;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Loads users before join (async pre-login), keeps online users in memory, saves on quit and on a
 * timer. Saving serialises on the main thread and writes on the IO thread, so files never hold a
 * half-modified state.
 */
public final class UserManager implements Listener {

    private final AppleCorePlugin plugin;
    private final Path directory;
    private final Map<UUID, User> online = new ConcurrentHashMap<>();
    private final Map<UUID, User> preloaded = new ConcurrentHashMap<>();
    private final Map<UUID, User> saving = new ConcurrentHashMap<>();
    private @Nullable BukkitTask autosave;

    public UserManager(AppleCorePlugin plugin) {
        this.plugin = plugin;
        this.directory = plugin.getDataFolder().toPath().resolve("userdata");
    }

    public void start() {
        long minutes = Math.max(1, plugin.getConfig().getLong("storage.autosave-minutes", 5));
        autosave = Bukkit.getScheduler().runTaskTimer(plugin, this::saveDirty, minutes * 1200L, minutes * 1200L);
        for (Player player : Bukkit.getOnlinePlayers()) {
            join(player);
        }
    }

    public void shutdown() {
        if (autosave != null) {
            autosave.cancel();
        }
        for (User user : online.values()) {
            writeNow(user);
        }
    }

    // ---- access ------------------------------------------------------------------------------------

    /** The user of an online player; never null. */
    public User user(Player player) {
        return online.computeIfAbsent(player.getUniqueId(), id -> readOrCreate(id, player.getName()));
    }

    public @Nullable User online(UUID id) {
        return online.get(id);
    }

    public Collection<User> onlineUsers() {
        return Collections.unmodifiableCollection(online.values());
    }

    /** Loads any user (online users resolve immediately). Completes on the main thread. */
    public CompletableFuture<User> load(OfflinePlayer player) {
        User cached = online.get(player.getUniqueId());
        if (cached == null) {
            cached = saving.get(player.getUniqueId());
        }
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        CompletableFuture<User> result = new CompletableFuture<>();
        plugin.io().supply(() -> read(player.getUniqueId())).whenComplete((user, error) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (error != null) {
                        result.completeExceptionally(error);
                    } else {
                        result.complete(user);
                    }
                }));
        return result;
    }

    public void save(User user) {
        if (!user.dirty()) {
            return;
        }
        String json = JsonFiles.GSON.toJson(user);
        user.clean();
        UUID id = user.uuid();
        plugin.io().run(() -> write(id, json));
    }

    // ---- lifecycle ---------------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        User pending = saving.get(event.getUniqueId());
        preloaded.put(event.getUniqueId(), pending != null ? pending : readOrCreate(event.getUniqueId(), event.getName()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        join(event.getPlayer());
    }

    private void join(Player player) {
        User user = preloaded.remove(player.getUniqueId());
        if (user == null) {
            user = online.containsKey(player.getUniqueId()) ? online.get(player.getUniqueId())
                    : readOrCreate(player.getUniqueId(), player.getName());
        }
        user.name(player.getName());
        if (user.firstJoin() == 0) {
            user.firstJoin(System.currentTimeMillis());
        }
        online.put(player.getUniqueId(), user);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        User user = online.remove(player.getUniqueId());
        if (user == null) {
            return;
        }
        user.lastSeen(System.currentTimeMillis());
        user.logoutLocation(StoredLocation.of(player.getLocation()));
        saving.put(user.uuid(), user);
        String json = JsonFiles.GSON.toJson(user);
        user.clean();
        plugin.io().run(() -> write(user.uuid(), json))
                .whenComplete((ignored, error) -> saving.remove(user.uuid(), user));
    }

    private void saveDirty() {
        for (User user : online.values()) {
            save(user);
        }
    }

    // ---- files -------------------------------------------------------------------------------------

    private User readOrCreate(UUID id, String name) {
        User user = read(id);
        return user != null ? user : new User(id, name);
    }

    private @Nullable User read(UUID id) {
        Path file = file(id);
        try {
            User user = JsonFiles.read(file, User.class);
            if (user != null) {
                user.normalize(id);
            }
            return user;
        } catch (IOException | JsonParseException e) {
            plugin.getLogger().log(Level.SEVERE, "Corrupted user file " + file + ", starting fresh (backup kept)", e);
            try {
                Files.copy(file, file.resolveSibling(file.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // best effort
            }
            return null;
        }
    }

    private void writeNow(User user) {
        write(user.uuid(), JsonFiles.GSON.toJson(user));
        user.clean();
    }

    private void write(UUID id, String json) {
        Path file = file(id);
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save user " + id, e);
        }
    }

    private Path file(UUID id) {
        return directory.resolve(id + ".json");
    }
}
