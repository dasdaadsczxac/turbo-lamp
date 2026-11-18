package fun.applecube.core.message;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.config.ConfigFile;
import fun.applecube.core.text.Ph;
import fun.applecube.core.text.Text;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * All player-facing text comes from {@code messages.yml} (Russian). Keys missing from the server's copy
 * fall back to the bundled file, so updates never show raw keys.
 * <p>
 * Rendering pipeline: backslash-u escapes → {@code <pack>/<plain>} sections → PlaceholderAPI (if installed)
 * → legacy {@code &} codes → MiniMessage with AppleCore tags: theme colours ({@code <accent>}, {@code <text>} …),
 * {@code <prefix:ok|error|warn|info>}, {@code <icon:lmb|rmb|mmb|bar|dot|apple>} and {@code <menu-font>}.
 * The viewer only matters for the resource-pack style and PlaceholderAPI.
 */
public final class Messages {

    private final AppleCorePlugin plugin;
    private final ConfigFile file;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final Set<String> reportedMissing = ConcurrentHashMap.newKeySet();
    private TagResolver theme = TagResolver.empty();
    private Key menuFont = Key.key("bordo_menu", "bold");

    public Messages(AppleCorePlugin plugin) {
        this.plugin = plugin;
        this.file = new ConfigFile(plugin, "messages.yml", true);
    }

    public void reload() {
        file.load();
        reportedMissing.clear();
        TagResolver.Builder builder = TagResolver.builder();
        ConfigurationSection colors = plugin.getConfig().getConfigurationSection("style.colors");
        if (colors != null) {
            for (String name : colors.getKeys(false)) {
                TextColor color = TextColor.fromHexString(String.valueOf(colors.getString(name)));
                if (color == null) {
                    plugin.getLogger().warning("style.colors." + name + " is not a #RRGGBB colour");
                    continue;
                }
                builder.resolver(TagResolver.resolver(name, Tag.styling(color)));
            }
        }
        theme = builder.build();
        try {
            menuFont = Key.key(plugin.getConfig().getString("resource-pack.menu-font", "bordo_menu:bold"));
        } catch (RuntimeException e) {
            plugin.getLogger().warning("resource-pack.menu-font is not a valid key, using bordo_menu:bold");
            menuFont = Key.key("bordo_menu", "bold");
        }
    }

    // ---- raw values ------------------------------------------------------------------------------

    private @Nullable Object value(String key) {
        Object value = file.yaml().get(key);
        if (value == null && reportedMissing.add(key)) {
            plugin.getLogger().warning("Missing message: " + key);
        }
        return value;
    }

    /** The message as one string (lists are joined with new lines); a missing key returns the key. */
    public String raw(String key) {
        Object value = value(key);
        if (value == null) {
            return key;
        }
        if (value instanceof List<?> list) {
            return String.join("\n", list.stream().map(String::valueOf).toList());
        }
        return String.valueOf(value);
    }

    /** The message as lines; a plain string is split on new lines. */
    public List<String> rawList(String key) {
        Object value = value(key);
        if (value == null) {
            return List.of(key);
        }
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        String s = String.valueOf(value);
        return s.isEmpty() ? List.of() : List.of(s.split("\n", -1));
    }

    public boolean has(String key) {
        return file.yaml().contains(key);
    }

    // ---- rendering -------------------------------------------------------------------------------

    public Component parse(@Nullable CommandSender viewer, String raw, TagResolver... placeholders) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        boolean pack = plugin.pack().usesPack(viewer);
        TagResolver resolver = TagResolver.builder()
                .resolver(theme)
                .resolver(fontTag(pack))
                .resolver(fragmentTag("prefix", "prefix.", "info", viewer, pack))
                .resolver(fragmentTag("icon", "icons.", "dot", viewer, pack))
                .resolvers(placeholders)
                .build();
        return mini.deserialize(prepare(viewer, raw, pack), resolver);
    }

    private String prepare(@Nullable CommandSender viewer, String raw, boolean pack) {
        String s = Text.unescapeUnicode(raw);
        s = Text.applyPackSections(s, pack);
        if (viewer instanceof Player player) {
            s = plugin.hooks().papi().apply(player, s);
        }
        return Text.legacyToMiniMessage(s);
    }

    private TagResolver fontTag(boolean pack) {
        return TagResolver.resolver("menu-font", pack
                ? Tag.styling(style -> style.font(menuFont))
                : Tag.styling(style -> style.decoration(TextDecoration.BOLD, true)));
    }

    /** {@code <prefix:ok>} → {@code prefix.ok}; fragments get theme colours but no nested fragments. */
    private TagResolver fragmentTag(String tag, String keyPrefix, String fallback, @Nullable CommandSender viewer,
                                    boolean pack) {
        return TagResolver.resolver(tag, (args, context) -> {
            String name = args.hasNext() ? args.pop().lowerValue() : fallback;
            return Tag.inserting(mini.deserialize(prepare(viewer, raw(keyPrefix + name), pack),
                    TagResolver.resolver(theme, fontTag(pack))));
        });
    }

    public Component get(@Nullable CommandSender viewer, String key, TagResolver... placeholders) {
        return parse(viewer, raw(key), placeholders);
    }

    /** Lines for item lore; every line is rendered separately and loses the implicit italic. */
    public List<Component> lines(@Nullable CommandSender viewer, String key, TagResolver... placeholders) {
        List<Component> out = new ArrayList<>();
        for (String line : rawList(key)) {
            out.add(Text.noItalic(parse(viewer, line, placeholders)));
        }
        return out;
    }

    // ---- delivery --------------------------------------------------------------------------------

    /** Sends a message; an empty string in messages.yml disables it. */
    public void send(CommandSender to, String key, TagResolver... placeholders) {
        String raw = raw(key);
        if (!raw.isEmpty()) {
            to.sendMessage(parse(to, raw, placeholders));
        }
    }

    public void sendRaw(CommandSender to, String raw, TagResolver... placeholders) {
        if (raw != null && !raw.isEmpty()) {
            to.sendMessage(parse(to, raw, placeholders));
        }
    }

    public void actionBar(Player player, String key, TagResolver... placeholders) {
        String raw = raw(key);
        if (!raw.isEmpty()) {
            player.sendActionBar(parse(player, raw, placeholders));
        }
    }

    /** Shows {@code <key>.title} / {@code <key>.subtitle}. */
    public void title(Player player, String key, Duration fadeIn, Duration stay, Duration fadeOut,
                      TagResolver... placeholders) {
        Component title = get(player, key + ".title", placeholders);
        Component subtitle = get(player, key + ".subtitle", placeholders);
        player.showTitle(Title.title(title, subtitle, Title.Times.times(fadeIn, stay, fadeOut)));
    }

    /** Sends to every online player (each in their own pack style) and the console. */
    public void broadcast(String key, TagResolver... placeholders) {
        broadcast(key, player -> true, placeholders);
    }

    public void broadcast(String key, Predicate<Player> filter, TagResolver... placeholders) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (filter.test(player)) {
                send(player, key, placeholders);
            }
        }
        send(Bukkit.getConsoleSender(), key, placeholders);
    }

    // ---- formatting helpers ----------------------------------------------------------------------

    /** Duration such as "1 ч. 5 мин."; always shows at least seconds. */
    public Component duration(@Nullable CommandSender viewer, long millis) {
        long total = Math.max(0, (millis + 999) / 1000);
        long days = total / 86_400;
        long hours = total % 86_400 / 3_600;
        long minutes = total % 3_600 / 60;
        long seconds = total % 60;
        List<Component> parts = new ArrayList<>();
        if (days > 0) {
            parts.add(get(viewer, "time.days", Ph.of("amount", days)));
        }
        if (hours > 0) {
            parts.add(get(viewer, "time.hours", Ph.of("amount", hours)));
        }
        if (minutes > 0) {
            parts.add(get(viewer, "time.minutes", Ph.of("amount", minutes)));
        }
        if (seconds > 0 || parts.isEmpty()) {
            parts.add(get(viewer, "time.seconds", Ph.of("amount", seconds)));
        }
        return Component.join(JoinConfiguration.separator(Component.space()), parts);
    }

    public Component bool(@Nullable CommandSender viewer, boolean value) {
        return get(viewer, value ? "common.enabled" : "common.disabled");
    }

    public MiniMessage mini() {
        return mini;
    }
}
