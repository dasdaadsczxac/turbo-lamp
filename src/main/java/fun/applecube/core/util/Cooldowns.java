package fun.applecube.core.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory cooldowns keyed by (player, action). They survive relogs but not restarts. */
public final class Cooldowns {

    private final Map<String, Map<UUID, Long>> expiries = new ConcurrentHashMap<>();

    /** Milliseconds left, or 0 when the action is available. */
    public long remaining(UUID player, String key) {
        Map<UUID, Long> map = expiries.get(key);
        if (map == null) {
            return 0;
        }
        Long until = map.get(player);
        if (until == null) {
            return 0;
        }
        long left = until - System.currentTimeMillis();
        if (left <= 0) {
            map.remove(player);
            return 0;
        }
        return left;
    }

    public void start(UUID player, String key, long millis) {
        if (millis > 0) {
            expiries.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(player, System.currentTimeMillis() + millis);
        }
    }

    public void reset(UUID player, String key) {
        Map<UUID, Long> map = expiries.get(key);
        if (map != null) {
            map.remove(player);
        }
    }
}
