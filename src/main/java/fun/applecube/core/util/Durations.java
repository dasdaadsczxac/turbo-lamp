package fun.applecube.core.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses durations like {@code 90}, {@code 30s}, {@code 5m}, {@code 1h30m}, {@code 2d} and the Russian
 * units {@code 1ч30м}, {@code 10с}, {@code 2д}. A bare number means seconds.
 */
public final class Durations {

    private static final Pattern PART = Pattern.compile("(\\d+)\\s*(ms|мс|d|д|h|ч|m|м|s|с)?");

    private Durations() {
    }

    /** Milliseconds; throws {@link IllegalArgumentException} for anything unparsable. */
    public static long parse(String input) {
        String s = input.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) {
            throw new IllegalArgumentException("empty duration");
        }
        Matcher matcher = PART.matcher(s);
        long total = 0;
        int position = 0;
        while (matcher.find()) {
            if (matcher.start() != position && !s.substring(position, matcher.start()).isBlank()) {
                throw new IllegalArgumentException("bad duration: " + input);
            }
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2) == null ? "s" : matcher.group(2);
            total += switch (unit) {
                case "ms", "мс" -> value;
                case "d", "д" -> value * 86_400_000L;
                case "h", "ч" -> value * 3_600_000L;
                case "m", "м" -> value * 60_000L;
                default -> value * 1_000L;
            };
            position = matcher.end();
        }
        if (position != s.length() && !s.substring(position).isBlank() || position == 0) {
            throw new IllegalArgumentException("bad duration: " + input);
        }
        return total;
    }

    /** Lenient variant for config values: numbers are seconds; invalid values return {@code def}. */
    public static long parseOr(Object value, long def) {
        if (value == null) {
            return def;
        }
        if (value instanceof Number number) {
            return (long) (number.doubleValue() * 1000);
        }
        try {
            return parse(String.valueOf(value));
        } catch (IllegalArgumentException e) {
            return def;
        }
    }
}
