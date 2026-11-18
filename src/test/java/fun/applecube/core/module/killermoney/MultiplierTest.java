package fun.applecube.core.module.killermoney;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MultiplierTest {

    @Test
    void acceptsXNotation() {
        assertEquals(1.5, KillerMoneyModule.parseMultiplier("x1.5"));
        assertEquals(2, KillerMoneyModule.parseMultiplier("X2"));
        assertEquals(2.25, KillerMoneyModule.parseMultiplier("2,25"));
        assertEquals(1.75, KillerMoneyModule.parseMultiplier("х1.75"), "Cyrillic х");
    }

    @Test
    void invalidValuesMeanNoBonus() {
        assertEquals(1, KillerMoneyModule.parseMultiplier("abc"));
        assertEquals(1, KillerMoneyModule.parseMultiplier("-3"));
        assertEquals(1, KillerMoneyModule.parseMultiplier("0"));
        assertEquals(1, KillerMoneyModule.parseMultiplier(null));
    }
}
