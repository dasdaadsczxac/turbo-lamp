package fun.applecube.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DurationsTest {

    @Test
    void parsesUnits() {
        assertEquals(90_000, Durations.parse("90"));
        assertEquals(30_000, Durations.parse("30s"));
        assertEquals(5_400_000, Durations.parse("1h30m"));
        assertEquals(172_800_000, Durations.parse("2d"));
        assertEquals(250, Durations.parse("250ms"));
    }

    @Test
    void parsesRussianUnits() {
        assertEquals(5_400_000, Durations.parse("1ч30м"));
        assertEquals(10_000, Durations.parse("10с"));
        assertEquals(86_400_000, Durations.parse("1д"));
    }

    @Test
    void rejectsGarbage() {
        assertThrows(IllegalArgumentException.class, () -> Durations.parse("soon"));
        assertThrows(IllegalArgumentException.class, () -> Durations.parse(""));
        assertThrows(IllegalArgumentException.class, () -> Durations.parse("5x"));
    }

    @Test
    void lenientConfigValues() {
        assertEquals(2_500, Durations.parseOr(2.5, 0));
        assertEquals(60_000, Durations.parseOr("1m", 0));
        assertEquals(7, Durations.parseOr("bad", 7));
    }
}
