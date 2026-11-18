package fun.applecube.core.module.ranks;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankTest {

    @Test
    void multiplierAcceptsXNotation() {
        assertEquals(1.5, RanksModule.parseMultiplier("x1.5"));
        assertEquals(2, RanksModule.parseMultiplier("X2"));
        assertEquals(2.25, RanksModule.parseMultiplier("2,25"));
        assertEquals(1.75, RanksModule.parseMultiplier("х1.75"), "Cyrillic х");
        assertEquals(1, RanksModule.parseMultiplier("abc"));
        assertEquals(1, RanksModule.parseMultiplier("-3"));
        assertEquals(1, RanksModule.parseMultiplier(null));
    }

    @Test
    void presetGrantsExactAndWildcardNodes() {
        Rank rank = new Rank("king", "king", "Король", null, 140, true, 1.5,
                Set.of("applecore.fly", "applecore.fireball.*", "applecore.homes.limit.20"));
        assertTrue(rank.grants("applecore.fly"));
        assertTrue(rank.grants("applecore.fireball.small"));
        assertFalse(rank.grants("applecore.fly.others"));
        assertFalse(rank.grants("applecore.heal"));
    }

    @Test
    void displayFallsBackToNameWithoutBadge() {
        Rank plain = new Rank("guard", "guard", "<red>Страж", null, 60, true, 1, Set.of());
        assertEquals("<red>Страж", plain.display());
        Rank badge = new Rank("guard", "guard", "<red>Страж", "\uF823", 60, true, 1, Set.of());
        assertEquals("<pack><white>\uF823</white></pack><plain><red>Страж</plain>", badge.display());
    }
}
