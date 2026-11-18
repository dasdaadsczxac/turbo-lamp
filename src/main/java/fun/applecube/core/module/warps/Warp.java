package fun.applecube.core.module.warps;

import fun.applecube.core.item.ItemSpec;
import fun.applecube.core.storage.StoredLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** A server warp (set by staff) or a player warp (owned, limited per rank). Stored in data/warps.json. */
public final class Warp {

    public enum Type { SERVER, PLAYER }

    private String name;
    private String displayName;
    private Type type;
    private @Nullable UUID owner;
    private @Nullable String ownerName;
    private StoredLocation location;
    private @Nullable ItemSpec icon;
    private List<String> description = new ArrayList<>();
    private int order;
    private @Nullable String permission;
    private boolean closed;
    private long created;
    private int visits;
    private Set<UUID> likes = new HashSet<>();

    @SuppressWarnings("unused")
    private Warp() {
        // Gson
    }

    Warp(String name, Type type, StoredLocation location, @Nullable UUID owner, @Nullable String ownerName) {
        this.name = name.toLowerCase(Locale.ROOT);
        this.displayName = name;
        this.type = type;
        this.location = location;
        this.owner = owner;
        this.ownerName = ownerName;
        this.created = System.currentTimeMillis();
    }

    void normalize() {
        if (description == null) {
            description = new ArrayList<>();
        }
        if (likes == null) {
            likes = new HashSet<>();
        }
        if (displayName == null) {
            displayName = name;
        }
    }

    /** Lower-case key, unique across both types. */
    public String name() {
        return name;
    }

    public String displayName() {
        return displayName;
    }

    void displayName(String displayName) {
        this.displayName = displayName;
    }

    void rename(String newName) {
        this.name = newName.toLowerCase(Locale.ROOT);
        this.displayName = newName;
    }

    public Type type() {
        return type;
    }

    public boolean isServer() {
        return type == Type.SERVER;
    }

    public @Nullable UUID owner() {
        return owner;
    }

    public @Nullable String ownerName() {
        return ownerName;
    }

    void ownerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public boolean isOwner(UUID player) {
        return player.equals(owner);
    }

    public StoredLocation location() {
        return location;
    }

    void location(StoredLocation location) {
        this.location = location;
    }

    public @Nullable ItemSpec icon() {
        return icon;
    }

    void icon(@Nullable ItemSpec icon) {
        this.icon = icon;
    }

    public List<String> description() {
        return description;
    }

    void description(List<String> lines) {
        this.description = new ArrayList<>(lines);
    }

    public int order() {
        return order;
    }

    void order(int order) {
        this.order = order;
    }

    public @Nullable String permission() {
        return permission;
    }

    void permission(@Nullable String permission) {
        this.permission = permission;
    }

    public boolean closed() {
        return closed;
    }

    void closed(boolean closed) {
        this.closed = closed;
    }

    public long created() {
        return created;
    }

    public int visits() {
        return visits;
    }

    void visit() {
        visits++;
    }

    public int likes() {
        return likes.size();
    }

    public boolean likedBy(UUID player) {
        return likes.contains(player);
    }

    /** Toggles the like; returns true if the player now likes the warp. */
    boolean toggleLike(UUID player) {
        if (!likes.remove(player)) {
            likes.add(player);
            return true;
        }
        return false;
    }
}
