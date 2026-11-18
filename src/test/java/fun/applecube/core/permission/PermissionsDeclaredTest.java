package fun.applecube.core.permission;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every applecore.* node checked in code is declared in plugin.yml (default and description for LuckPerms). */
class PermissionsDeclaredTest {

    /** Whole nodes only: dynamic prefixes such as "applecore.homes.limit." end with a dot and are skipped. */
    private static final Pattern NODE = Pattern.compile("\"(applecore\\.[a-z0-9.\\-]*[a-z0-9])\"");
    /** Mapping tables list nodes of other plugins' commands that AppleCore builds at runtime. */
    private static final Set<String> TABLES = Set.of("EssentialsPermissions.java", "TrustEssentialsPermissions.java");

    @Test
    void checkedNodesAreDeclared() throws IOException {
        // SnakeYAML directly: Bukkit's YamlConfiguration splits "applecore.x.y" on dots, plugin.yml keys are literal.
        Map<?, ?> permissions;
        try (InputStream in = PermissionsDeclaredTest.class.getResourceAsStream("/plugin.yml")) {
            assertNotNull(in, "plugin.yml is missing");
            Map<?, ?> plugin = new Yaml().load(new InputStreamReader(in, StandardCharsets.UTF_8));
            permissions = (Map<?, ?>) plugin.get("permissions");
        }
        Set<String> missing = new TreeSet<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !TABLES.contains(p.getFileName().toString())).toList()) {
                Matcher matcher = NODE.matcher(Files.readString(file));
                while (matcher.find()) {
                    String node = matcher.group(1);
                    if (!(permissions.get(node) instanceof Map<?, ?> entry) || !entry.containsKey("description")) {
                        missing.add(node + " (" + file.getFileName() + ")");
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "permissions checked in code but not declared in plugin.yml: " + missing);
    }
}
