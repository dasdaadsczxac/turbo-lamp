package fun.applecube.core.message;

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

/** Keeps messages.yml complete: every key used in code exists and every command has description/usage. */
class MessagesFileTest {

    private static final Pattern KEY_LITERAL = Pattern.compile(
            "\"((?:errors|teleport|common|time|prefix|icons|resource-pack|commands|menus|warps|homes|ranks|killermoney|"
                    + "economy|chat|spawn|vanish|tpa)\\.[a-z0-9.\\-]+[a-z0-9])\"");

    private static YamlConfiguration resource(String path) throws IOException {
        try (InputStream in = MessagesFileTest.class.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }

    @Test
    void messagesFileIsNotEmpty() throws IOException {
        YamlConfiguration messages = resource("/messages.yml");
        if (messages == null) {
            fail("messages.yml is missing");
        }
        assertTrue(messages.getKeys(true).size() > 50, "messages.yml looks empty");
    }

    @Test
    void everyCommandHasDescriptionAndUsage() throws IOException {
        YamlConfiguration messages = resource("/messages.yml");
        var commands = messages.getConfigurationSection("commands");
        assertTrue(commands != null, "commands section missing");
        Set<String> broken = new TreeSet<>();
        for (String name : commands.getKeys(false)) {
            if (!messages.isString("commands." + name + ".description") || !messages.contains("commands." + name + ".usage")) {
                broken.add(name);
            }
        }
        assertTrue(broken.isEmpty(), "commands without description/usage: " + broken);
    }

    @Test
    void keysUsedInCodeExist() throws IOException {
        YamlConfiguration messages = resource("/messages.yml");
        YamlConfiguration[] configs = {
                resource("/config.yml"), resource("/warps.yml"), resource("/ranks.yml"),
                resource("/killermoney.yml"), resource("/menus/warps.yml")};
        Set<String> missing = new TreeSet<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.endsWith("EssentialsPermissions.java")).toList()) {
                Matcher matcher = KEY_LITERAL.matcher(Files.readString(file));
                while (matcher.find()) {
                    String key = matcher.group(1);
                    if (key.endsWith(".yml") || isConfigKey(configs, key)) {
                        continue;
                    }
                    if (!messages.contains(key)) {
                        missing.add(key + " (" + file.getFileName() + ")");
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "message keys used in code but missing in messages.yml: " + missing);
    }

    /** Config paths share section names with message keys (teleport.delay vs teleport.warmup). */
    private static boolean isConfigKey(YamlConfiguration[] configs, String key) {
        for (YamlConfiguration config : configs) {
            if (config != null && config.contains(key)) {
                return true;
            }
        }
        return false;
    }
}
