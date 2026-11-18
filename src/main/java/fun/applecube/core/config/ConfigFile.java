package fun.applecube.core.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * A YAML file in the plugin folder, created from the bundled copy on first start.
 * With {@code bundledDefaults} enabled, keys missing from the user's file fall back to the bundled
 * values, so new settings and messages work after an update without overwriting edits.
 */
public final class ConfigFile {

    private final JavaPlugin plugin;
    private final String path;
    private final boolean bundledDefaults;
    private final File file;
    private YamlConfiguration yaml = new YamlConfiguration();

    public ConfigFile(JavaPlugin plugin, String path, boolean bundledDefaults) {
        this.plugin = plugin;
        this.path = path;
        this.bundledDefaults = bundledDefaults;
        this.file = new File(plugin.getDataFolder(), path);
    }

    public ConfigFile load() {
        if (!file.exists() && plugin.getResource(path) != null) {
            plugin.saveResource(path, false);
        }
        yaml = YamlConfiguration.loadConfiguration(file);
        if (bundledDefaults) {
            YamlConfiguration defaults = bundled(plugin, path);
            if (defaults != null) {
                yaml.setDefaults(defaults);
            }
        }
        return this;
    }

    public static YamlConfiguration bundled(JavaPlugin plugin, String path) {
        try (InputStream in = plugin.getResource(path)) {
            if (in == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not read bundled " + path, e);
            return null;
        }
    }

    public void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + path, e);
        }
    }

    public YamlConfiguration yaml() {
        return yaml;
    }

    public ConfigurationSection section(String key) {
        ConfigurationSection section = yaml.getConfigurationSection(key);
        return section != null ? section : yaml.createSection(key);
    }

    public File file() {
        return file;
    }
}
