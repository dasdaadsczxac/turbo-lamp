package fun.applecube.core.lang;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Keeps the bundled languages complete and in sync with the code. */
class LangFilesTest {

    private static final Pattern KEY_LITERAL = Pattern.compile(
            "\"((?:errors|teleport|common|time|prefix|icons|resource-pack|commands|menus|warps|homes|ranks|killermoney|"
                    + "economy|chat|spawn|vanish|tpa)\\.[a-z0-9.\\-]+[a-z0-9])\"");

    private static YamlConfiguration load(String id) throws IOException {
        try (InputStream in = LangFilesTest.class.getResourceAsStream("/lang/" + id + ".yml")) {
            if (in == null) {
                fail("missing bundled language " + id);
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }

    private static Set<String> leaves(YamlConfiguration yaml) {
        Set<String> keys = new TreeSet<>();
        for (String key : yaml.getKeys(true)) {
            if (!yaml.isConfigurationSection(key)) {
                keys.add(key);
            }
        }
        return keys;
    }

    @Test
    void bundledLanguagesHaveTheSameKeys() throws IOException {
        Set<String> ru = leaves(load("ru_ru"));
        Set<String> en = leaves(load("en_us"));
        Set<String> onlyRu = new TreeSet<>(ru);
        onlyRu.removeAll(en);
        Set<String> onlyEn = new TreeSet<>(en);
        onlyEn.removeAll(ru);
        assertTrue(onlyRu.isEmpty() && onlyEn.isEmpty(), "only in ru_ru: " + onlyRu + "\nonly in en_us: " + onlyEn);
        assertTrue(ru.size() > 20, "language file looks empty");
    }

    @Test
    void everyCommandHasDescriptionAndUsage() throws IOException {
        YamlConfiguration ru = load("ru_ru");
        var commands = ru.getConfigurationSection("commands");
        assertTrue(commands != null, "commands section missing");
        Set<String> broken = new TreeSet<>();
        for (String name : commands.getKeys(false)) {
            if (!ru.isString("commands." + name + ".description") || !ru.contains("commands." + name + ".usage")) {
                broken.add(name);
            }
        }
        assertTrue(broken.isEmpty(), "commands without description/usage: " + broken);
    }

    @Test
    void keysUsedInCodeExist() throws IOException {
        YamlConfiguration ru = load("ru_ru");
        YamlConfiguration config;
        try (InputStream in = LangFilesTest.class.getResourceAsStream("/config.yml")) {
            config = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        Set<String> missing = new TreeSet<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher matcher = KEY_LITERAL.matcher(Files.readString(file));
                while (matcher.find()) {
                    String key = matcher.group(1);
                    // Config paths share section names with language keys; file names are not keys.
                    if (key.endsWith(".yml") || config.contains(key) || isConfigFileKey(key)) {
                        continue;
                    }
                    if (!ru.contains(key)) {
                        missing.add(key + " (" + file.getFileName() + ")");
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "language keys used in code but missing in ru_ru.yml: " + missing);
    }

    /** Keys read from the module config files (warps.yml, ranks.yml, killermoney.yml …). */
    private static boolean isConfigFileKey(String key) throws IOException {
        for (String file : new String[]{"/warps.yml", "/ranks.yml", "/killermoney.yml", "/menus/warps.yml"}) {
            try (InputStream in = LangFilesTest.class.getResourceAsStream(file)) {
                if (in != null && YamlConfiguration.loadConfiguration(
                        new InputStreamReader(in, StandardCharsets.UTF_8)).contains(key)) {
                    return true;
                }
            }
        }
        return false;
    }
}
