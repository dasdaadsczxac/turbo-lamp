package fun.applecube.core.module.warps;

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.storage.JsonFiles;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** All warps in memory; changes are written to data/warps.json on the IO thread right away. */
public final class WarpStore {

    private final AppleCorePlugin plugin;
    private final Path file;
    private final Map<String, Warp> warps = new LinkedHashMap<>();

    public WarpStore(AppleCorePlugin plugin) {
        this.plugin = plugin;
        this.file = plugin.getDataFolder().toPath().resolve("data").resolve("warps.json");
    }

    public void load() {
        warps.clear();
        try {
            List<Warp> stored = JsonFiles.read(file, new TypeToken<List<Warp>>() { }.getType());
            if (stored != null) {
                for (Warp warp : stored) {
                    warp.normalize();
                    warps.put(warp.name(), warp);
                }
            }
        } catch (IOException | JsonParseException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not read " + file + " (file kept, warps start empty)", e);
            try {
                Files.copy(file, file.resolveSibling(file.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // best effort
            }
        }
    }

    /** Serialises now (main thread) and writes on the IO thread. */
    public void save() {
        String json = JsonFiles.GSON.toJson(new ArrayList<>(warps.values()));
        plugin.io().run(() -> {
            try {
                Files.createDirectories(file.getParent());
                Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
                Files.writeString(tmp, json, StandardCharsets.UTF_8);
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save " + file, e);
            }
        });
    }

    public @Nullable Warp get(String name) {
        return warps.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean exists(String name) {
        return warps.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public void add(Warp warp) {
        warps.put(warp.name(), warp);
        save();
    }

    public void remove(Warp warp) {
        warps.remove(warp.name());
        save();
    }

    public void rename(Warp warp, String newName) {
        warps.remove(warp.name());
        warp.rename(newName);
        warps.put(warp.name(), warp);
        save();
    }

    public Collection<Warp> all() {
        return warps.values();
    }

    /** Server warps by their configured order, then name. */
    public List<Warp> serverWarps() {
        return warps.values().stream().filter(Warp::isServer)
                .sorted(Comparator.comparingInt((Warp w) -> w.order()).thenComparing(Warp::name))
                .toList();
    }

    /** Player warps sorted by {@code warps.sort}: likes, visits, newest or name. */
    public List<Warp> playerWarps(@Nullable UUID owner) {
        String sort = plugin.getConfig().getString("warps.sort", "likes").toLowerCase(Locale.ROOT);
        Comparator<Warp> comparator = switch (sort) {
            case "visits" -> Comparator.comparingInt(Warp::visits).reversed();
            case "newest" -> Comparator.comparingLong(Warp::created).reversed();
            case "name" -> Comparator.comparing(Warp::name);
            default -> Comparator.comparingInt(Warp::likes).reversed().thenComparing(Comparator.comparingInt(Warp::visits).reversed());
        };
        return warps.values().stream()
                .filter(w -> !w.isServer())
                .filter(w -> owner == null || w.isOwner(owner))
                .sorted(comparator.thenComparing(Warp::name))
                .toList();
    }

    public long countOwned(UUID owner) {
        return warps.values().stream().filter(w -> w.isOwner(owner)).count();
    }
}
