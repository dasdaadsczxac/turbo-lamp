package fun.applecube.core.module.killermoney;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RewardTest {

    @Test
    void parsesFixedAmountsAndRanges() {
        assertEquals(new KillerMoneyModule.Reward(5, 5), KillerMoneyModule.Reward.parse("5"));
        assertEquals(new KillerMoneyModule.Reward(1, 3), KillerMoneyModule.Reward.parse("1-3"));
        assertEquals(new KillerMoneyModule.Reward(0.5, 1), KillerMoneyModule.Reward.parse("0,5 - 1"));
        assertEquals(new KillerMoneyModule.Reward(2, 4), KillerMoneyModule.Reward.parse("4-2"));
        assertNull(KillerMoneyModule.Reward.parse("lots"));
        assertNull(KillerMoneyModule.Reward.parse(""));
    }

    @Test
    void rollStaysInRange() {
        KillerMoneyModule.Reward reward = KillerMoneyModule.Reward.parse("1-3");
        for (int i = 0; i < 1000; i++) {
            double value = reward.roll();
            assertTrue(value >= 1 && value <= 3, "out of range: " + value);
        }
        assertEquals(7, KillerMoneyModule.Reward.parse("7").roll());
    }
}
