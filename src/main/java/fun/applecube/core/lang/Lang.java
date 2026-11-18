package fun.applecube.core.lang;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.config.ConfigFile;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Language files in {@code plugins/AppleCore/lang/*.yml}. Bundled languages are copied on first start;
 * any extra file (e.g. {@code uk_ua.yml}) is picked up automatically. Missing keys fall back to the
 * bundled file of the same language, then to the default language.
 */
public final class Lang {

    public static final List<String> BUNDLED = List.of("ru_ru", "en_us");
    private static final String FALLBACK = "ru_ru";

    private final AppleCorePlugin plugin;
    private final Map<String, YamlConfiguration> languages = new HashMap<>();
    private final Set<String> reportedMissing = ConcurrentHashMap.newKeySet();
    private String defaultId = FALLBACK;
    private boolean perPlayer;

    public Lang(AppleCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        languages.clear();
        reportedMissing.clear();
        File dir = new File(plugin.getDataFolder(), "lang");
        for (String id : BUNDLED) {
            if (!new File(dir, id + ".yml").exists()) {
                plugin.saveResource("lang/" + id + ".yml", false);
            }
        }
        defaultId = plugin.getConfig().getString("language.default", FALLBACK).toLowerCase(Locale.ROOT);
        perPlayer = plugin.getConfig().getBoolean("language.per-player", false);
        String fallbackId = BUNDLED.contains(defaultId) ? defaultId : FALLBACK;

        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String id = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                YamlConfiguration defaults = ConfigFile.bundled(plugin, "lang/" + id + ".yml");
                if (defaults == null) {
                    defaults = ConfigFile.bundled(plugin, "lang/" + fallbackId + ".yml");
                }
                if (defaults != null) {
                    yaml.setDefaults(defaults);
                }
                languages.put(id, yaml);
            }
        }
        if (!languages.containsKey(defaultId)) {
            plugin.getLogger().warning("Language '" + defaultId + "' not found in lang/, using " + FALLBACK);
            defaultId = FALLBACK;
        }
    }

    public YamlConfiguration of(@Nullable CommandSender viewer) {
        if (perPlayer && viewer instanceof Player player) {
            YamlConfiguration own = forLocale(player.locale());
            if (own != null) {
                return own;
            }
        }
        return languages.get(defaultId);
    }

    private @Nullable YamlConfiguration forLocale(Locale locale) {
        YamlConfiguration exact = languages.get(locale.toString().toLowerCase(Locale.ROOT));
        if (exact != null) {
            return exact;
        }
        String prefix = locale.getLanguage().toLowerCase(Locale.ROOT) + "_";
        for (Map.Entry<String, YamlConfiguration> entry : languages.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public @Nullable Object value(@Nullable CommandSender viewer, String key) {
        Object value = of(viewer).get(key);
        if (value == null && reportedMissing.add(key)) {
            plugin.getLogger().warning("Missing language key: " + key);
        }
        return value;
    }

    /** The value as a single string; lists are joined with new lines. Missing keys return the key itself. */
    public String string(@Nullable CommandSender viewer, String key) {
        Object value = value(viewer, key);
        if (value == null) {
            return key;
        }
        if (value instanceof List<?> list) {
            return String.join("\n", list.stream().map(String::valueOf).toList());
        }
        return String.valueOf(value);
    }

    /** The value as lines; a plain string is split on new lines. */
    public List<String> list(@Nullable CommandSender viewer, String key) {
        Object value = value(viewer, key);
        if (value == null) {
            return List.of(key);
        }
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        String s = String.valueOf(value);
        return s.isEmpty() ? List.of() : List.of(s.split("\n", -1));
    }

    public boolean has(@Nullable CommandSender viewer, String key) {
        return of(viewer).contains(key);
    }

    public String defaultId() {
        return defaultId;
    }

    public Set<String> ids() {
        return Collections.unmodifiableSet(languages.keySet());
    }
}
