package fun.applecube.core.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure text helpers used by the message pipeline. Covered by unit tests. */
public final class Text {

    private static final Pattern UNICODE_ESCAPE = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
    private static final Pattern PACK_SECTION = Pattern.compile("<pack>(.*?)</pack>", Pattern.DOTALL);
    private static final Pattern PLAIN_SECTION = Pattern.compile("<plain>(.*?)</plain>", Pattern.DOTALL);
    private static final String LEGACY_CODES = "0123456789abcdefklmnor";
    private static final String[] LEGACY_TAGS = {
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray",
            "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white",
            "obfuscated", "bold", "strikethrough", "underlined", "italic", "reset"
    };

    private Text() {
    }

    /** Turns literal backslash-u-XXXX sequences (single-quoted YAML) into the real characters. */
    public static String unescapeUnicode(String input) {
        if (input.indexOf("\\u") < 0) {
            return input;
        }
        Matcher matcher = UNICODE_ESCAPE.matcher(input);
        StringBuilder out = new StringBuilder(input.length());
        while (matcher.find()) {
            char c = (char) Integer.parseInt(matcher.group(1), 16);
            matcher.appendReplacement(out, Matcher.quoteReplacement(String.valueOf(c)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /**
     * Keeps {@code <pack>…</pack>} content when the viewer uses the resource pack and
     * {@code <plain>…</plain>} content otherwise; the other kind of section is removed.
     */
    public static String applyPackSections(String input, boolean pack) {
        if (input.indexOf("<pack>") < 0 && input.indexOf("<plain>") < 0) {
            return input;
        }
        Pattern keep = pack ? PACK_SECTION : PLAIN_SECTION;
        Pattern drop = pack ? PLAIN_SECTION : PACK_SECTION;
        String dropped = drop.matcher(input).replaceAll("");
        return keep.matcher(dropped).replaceAll(result -> Matcher.quoteReplacement(result.group(1)));
    }

    /**
     * Converts legacy formatting ({@code &a}, {@code &l}, {@code &#RRGGBB}, {@code &x&R&R&G&G&B&B}, and the
     * same with {@code §}) into MiniMessage tags. A colour after a decoration emits {@code <reset>} first,
     * mirroring how the vanilla client treats legacy codes.
     */
    public static String legacyToMiniMessage(String input) {
        if (input.indexOf('&') < 0 && input.indexOf('§') < 0) {
            return input;
        }
        StringBuilder out = new StringBuilder(input.length() + 16);
        boolean decorated = false;
        int length = input.length();
        int i = 0;
        while (i < length) {
            char c = input.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < length) {
                char next = Character.toLowerCase(input.charAt(i + 1));
                if (next == '#' && i + 8 <= length && isHex(input, i + 2, 6)) {
                    if (decorated) {
                        out.append("<reset>");
                        decorated = false;
                    }
                    out.append("<#").append(input, i + 2, i + 8).append('>');
                    i += 8;
                    continue;
                }
                if (next == 'x' && i + 14 <= length && isSpigotHex(input, i + 2)) {
                    if (decorated) {
                        out.append("<reset>");
                        decorated = false;
                    }
                    out.append("<#");
                    for (int k = 0; k < 6; k++) {
                        out.append(input.charAt(i + 3 + k * 2));
                    }
                    out.append('>');
                    i += 14;
                    continue;
                }
                int index = LEGACY_CODES.indexOf(next);
                if (index >= 0) {
                    if (index < 16) {
                        if (decorated) {
                            out.append("<reset>");
                            decorated = false;
                        }
                    } else if (index < 21) {
                        decorated = true;
                    } else {
                        decorated = false;
                    }
                    out.append('<').append(LEGACY_TAGS[index]).append('>');
                    i += 2;
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static boolean isHex(String s, int from, int count) {
        for (int i = from; i < from + count; i++) {
            if (Character.digit(s.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSpigotHex(String s, int from) {
        for (int k = 0; k < 6; k++) {
            char marker = s.charAt(from + k * 2);
            if ((marker != '&' && marker != '§') || Character.digit(s.charAt(from + k * 2 + 1), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    /** Menu items render italic by default; this keeps explicit styles and removes the implicit italic. */
    public static Component noItalic(Component component) {
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
