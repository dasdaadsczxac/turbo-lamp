package fun.applecube.core.pack;

import fun.applecube.core.AppleCorePlugin;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.sound.Sound;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resource-pack integration (APPLECUBE "bordo" pack via ItemsAdder): decides per viewer whether the
 * pack design (glyph backgrounds, item models, custom sounds) or the plain vanilla design is used,
 * and can optionally send a pack itself when ItemsAdder is not hosting one.
 */
public final class PackStyle implements Listener {

    private final AppleCorePlugin plugin;
    private final Set<UUID> plainPlayers = ConcurrentHashMap.newKeySet();
    private final Map<String, SoundSpec> sounds = new HashMap<>();
    private boolean enabled;
    private boolean plainForDeclined;
    private @Nullable NamespacedKey tooltipStyle;
    private @Nullable ResourcePackRequest request;

    public PackStyle(AppleCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        ConfigurationSection c = plugin.getConfig().getConfigurationSection("resource-pack");
        enabled = c == null || c.getBoolean("enabled", true);
        plainForDeclined = c == null || c.getBoolean("plain-for-declined", true);
        tooltipStyle = c == null ? null : NamespacedKey.fromString(c.getString("tooltip-style", ""));
        request = c == null ? null : buildRequest(c.getConfigurationSection("send"));

        sounds.clear();
        ConfigurationSection soundSection = plugin.getConfig().getConfigurationSection("sounds");
        if (soundSection != null) {
            for (String id : soundSection.getKeys(false)) {
                ConfigurationSection s = soundSection.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                Key pack = key(s.getString("pack"));
                Key plain = key(s.getString("plain"));
                sounds.put(id, new SoundSpec(pack != null ? pack : plain, plain != null ? plain : pack,
                        (float) s.getDouble("volume", 1.0), (float) s.getDouble("pitch", 1.0)));
            }
        }
    }

    private @Nullable ResourcePackRequest buildRequest(@Nullable ConfigurationSection send) {
        if (send == null || !send.getBoolean("enabled", false)) {
            return null;
        }
        String url = send.getString("url", "");
        if (url.isBlank()) {
            plugin.getLogger().warning("resource-pack.send.enabled is true but no url is set");
            return null;
        }
        UUID id = UUID.nameUUIDFromBytes(("applecore:" + url).getBytes(StandardCharsets.UTF_8));
        ResourcePackInfo info = ResourcePackInfo.resourcePackInfo(id, URI.create(url), send.getString("hash", ""));
        return ResourcePackRequest.resourcePackRequest()
                .packs(info)
                .required(send.getBoolean("required", false))
                .prompt(plugin.messages().get(null, "resource-pack.prompt"))
                .replace(false)
                .build();
    }

    private @Nullable Key key(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Key.key(raw);
        } catch (RuntimeException e) {
            plugin.getLogger().warning("Invalid sound key: " + raw);
            return null;
        }
    }

    /** Whether the pack design should be used for this viewer. Console always gets the plain style. */
    public boolean usesPack(@Nullable CommandSender viewer) {
        if (!enabled) {
            return false;
        }
        if (viewer == null) {
            return true;
        }
        if (viewer instanceof Player player) {
            return !plainPlayers.contains(player.getUniqueId());
        }
        return false;
    }

    public boolean enabled() {
        return enabled;
    }

    public @Nullable NamespacedKey tooltipStyle() {
        return tooltipStyle;
    }

    /** Plays a configured sound ({@code sounds.<id>} in config.yml), pack or vanilla variant. */
    public void play(Player player, String id) {
        SoundSpec spec = sounds.get(id);
        if (spec != null) {
            Key key = usesPack(player) ? spec.pack() : spec.plain();
            if (key != null) {
                player.playSound(Sound.sound(key, Sound.Source.MASTER, spec.volume(), spec.pitch()));
            }
        }
    }

    @EventHandler
    public void onStatus(PlayerResourcePackStatusEvent event) {
        if (!plainForDeclined) {
            return;
        }
        UUID id = event.getPlayer().getUniqueId();
        switch (event.getStatus()) {
            case SUCCESSFULLY_LOADED -> plainPlayers.remove(id);
            case DECLINED, FAILED_DOWNLOAD, INVALID_URL -> plainPlayers.add(id);
            default -> {
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (request != null) {
            event.getPlayer().sendResourcePacks(request);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plainPlayers.remove(event.getPlayer().getUniqueId());
    }

    public record SoundSpec(@Nullable Key pack, @Nullable Key plain, float volume, float pitch) {
    }
}
