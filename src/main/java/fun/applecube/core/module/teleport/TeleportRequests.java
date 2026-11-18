package fun.applecube.core.module.teleport;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.util.Durations;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Pending /tpa and /tpahere requests: target → (requester → request), newest last. */
public final class TeleportRequests implements Listener {

    /** {@code here}: the target should come to the requester (/tpahere). */
    public record Request(UUID from, UUID to, boolean here, long expiresAt) {
        boolean expired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    private final AppleCorePlugin plugin;
    private final Map<UUID, LinkedHashMap<UUID, Request>> incoming = new HashMap<>();

    public TeleportRequests(AppleCorePlugin plugin) {
        this.plugin = plugin;
    }

    public long timeoutMillis() {
        return Durations.parseOr(plugin.getConfig().get("teleport.requests.timeout"), 120_000L);
    }

    public Request put(Player from, Player to, boolean here) {
        LinkedHashMap<UUID, Request> map = incoming.computeIfAbsent(to.getUniqueId(), id -> new LinkedHashMap<>());
        map.remove(from.getUniqueId());
        Request request = new Request(from.getUniqueId(), to.getUniqueId(), here, System.currentTimeMillis() + timeoutMillis());
        map.put(from.getUniqueId(), request);
        return request;
    }

    /** Removes and returns the newest live request to {@code to} (from {@code from} if given). */
    public @Nullable Request take(Player to, @Nullable UUID from) {
        LinkedHashMap<UUID, Request> map = incoming.get(to.getUniqueId());
        if (map == null) {
            return null;
        }
        map.values().removeIf(Request::expired);
        Request request = null;
        if (from != null) {
            request = map.remove(from);
        } else {
            for (Request candidate : map.values()) {
                request = candidate;
            }
            if (request != null) {
                map.remove(request.from());
            }
        }
        if (map.isEmpty()) {
            incoming.remove(to.getUniqueId());
        }
        return request;
    }

    /** Removes the requester's outgoing requests (to {@code to} only, if given). */
    public List<Request> cancelOutgoing(UUID from, @Nullable UUID to) {
        List<Request> removed = new ArrayList<>();
        incoming.entrySet().removeIf(entry -> {
            if (to == null || entry.getKey().equals(to)) {
                Request request = entry.getValue().remove(from);
                if (request != null && !request.expired()) {
                    removed.add(request);
                }
            }
            return entry.getValue().isEmpty();
        });
        return removed;
    }

    public List<UUID> requesters(Player to) {
        LinkedHashMap<UUID, Request> map = incoming.get(to.getUniqueId());
        if (map == null) {
            return List.of();
        }
        return map.values().stream().filter(r -> !r.expired()).map(Request::from).toList();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        incoming.remove(id);
        incoming.values().forEach(map -> map.remove(id));
        incoming.values().removeIf(Map::isEmpty);
    }
}
