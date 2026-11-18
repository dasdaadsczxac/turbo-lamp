package fun.applecube.core.user;

import fun.applecube.core.storage.StoredLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent per-player data ({@code userdata/<uuid>.json}). Setters mark the user dirty; the
 * {@link UserManager} saves dirty users on quit and on the autosave timer.
 */
public final class User {

    private UUID uuid;
    private String name;
    private long firstJoin;
    private long lastSeen;
    private @Nullable String nickname;
    private Map<String, StoredLocation> homes = new LinkedHashMap<>();
    private Set<UUID> ignored = new HashSet<>();
    private @Nullable StoredLocation lastLocation;
    private @Nullable StoredLocation logoutLocation;
    private boolean god;
    private boolean fly;
    private boolean vanished;
    private boolean socialSpy;
    private boolean teleportRequests = true;
    private boolean teleportAuto;
    private boolean privateMessages = true;
    private boolean payments = true;

    private transient boolean dirty;

    @SuppressWarnings("unused")
    private User() {
        // for Gson
    }

    User(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.dirty = true;
    }

    /** Gson leaves fields missing from older files null; restore defaults. */
    void normalize(UUID id) {
        if (uuid == null) {
            uuid = id;
        }
        if (homes == null) {
            homes = new LinkedHashMap<>();
        }
        if (ignored == null) {
            ignored = new HashSet<>();
        }
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void name(String name) {
        if (!name.equals(this.name)) {
            this.name = name;
            dirty = true;
        }
    }

    public long firstJoin() {
        return firstJoin;
    }

    public void firstJoin(long time) {
        this.firstJoin = time;
        dirty = true;
    }

    public long lastSeen() {
        return lastSeen;
    }

    public void lastSeen(long time) {
        this.lastSeen = time;
        dirty = true;
    }

    public @Nullable String nickname() {
        return nickname;
    }

    public void nickname(@Nullable String nickname) {
        this.nickname = nickname;
        dirty = true;
    }

    // ---- homes ------------------------------------------------------------------------------------

    public Map<String, StoredLocation> homes() {
        return Collections.unmodifiableMap(homes);
    }

    public @Nullable StoredLocation home(String name) {
        return homes.get(name.toLowerCase(Locale.ROOT));
    }

    public void setHome(String name, StoredLocation location) {
        homes.put(name.toLowerCase(Locale.ROOT), location);
        dirty = true;
    }

    public boolean removeHome(String name) {
        boolean removed = homes.remove(name.toLowerCase(Locale.ROOT)) != null;
        dirty |= removed;
        return removed;
    }

    // ---- ignore list -------------------------------------------------------------------------------

    public Set<UUID> ignored() {
        return Collections.unmodifiableSet(ignored);
    }

    public boolean isIgnoring(UUID other) {
        return ignored.contains(other);
    }

    /** Toggles ignoring; returns true if the player is now ignored. */
    public boolean toggleIgnore(UUID other) {
        dirty = true;
        if (!ignored.remove(other)) {
            ignored.add(other);
            return true;
        }
        return false;
    }

    // ---- locations ---------------------------------------------------------------------------------

    public @Nullable StoredLocation lastLocation() {
        return lastLocation;
    }

    public void lastLocation(@Nullable StoredLocation location) {
        this.lastLocation = location;
        dirty = true;
    }

    public @Nullable StoredLocation logoutLocation() {
        return logoutLocation;
    }

    public void logoutLocation(@Nullable StoredLocation location) {
        this.logoutLocation = location;
        dirty = true;
    }

    // ---- toggles -----------------------------------------------------------------------------------

    public boolean god() {
        return god;
    }

    public void god(boolean value) {
        god = value;
        dirty = true;
    }

    public boolean fly() {
        return fly;
    }

    public void fly(boolean value) {
        fly = value;
        dirty = true;
    }

    public boolean vanished() {
        return vanished;
    }

    public void vanished(boolean value) {
        vanished = value;
        dirty = true;
    }

    public boolean socialSpy() {
        return socialSpy;
    }

    public void socialSpy(boolean value) {
        socialSpy = value;
        dirty = true;
    }

    /** Whether the player accepts /tpa and /tpahere requests. */
    public boolean teleportRequests() {
        return teleportRequests;
    }

    public void teleportRequests(boolean value) {
        teleportRequests = value;
        dirty = true;
    }

    /** Whether incoming /tpa requests are accepted automatically. */
    public boolean teleportAuto() {
        return teleportAuto;
    }

    public void teleportAuto(boolean value) {
        teleportAuto = value;
        dirty = true;
    }

    public boolean privateMessages() {
        return privateMessages;
    }

    public void privateMessages(boolean value) {
        privateMessages = value;
        dirty = true;
    }

    public boolean payments() {
        return payments;
    }

    public void payments(boolean value) {
        payments = value;
        dirty = true;
    }

    boolean dirty() {
        return dirty;
    }

    void clean() {
        dirty = false;
    }
}
