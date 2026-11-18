package fun.applecube.core.permission;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The EssentialsX node for each AppleCore node must be exactly what EssentialsX checks. */
class EssentialsPermissionsTest {

    @Test
    void commandsMapOneToOne() {
        assertEquals("essentials.fly", EssentialsPermissions.map("applecore.fly"));
        assertEquals("essentials.fly.others", EssentialsPermissions.map("applecore.fly.others"));
        assertEquals("essentials.tp.position", EssentialsPermissions.map("applecore.tp.position"));
        assertEquals("essentials.home.others", EssentialsPermissions.map("applecore.home.others"));
        assertEquals("essentials.back.ondeath", EssentialsPermissions.map("applecore.back.ondeath"));
        assertEquals("essentials.vanish.see", EssentialsPermissions.map("applecore.vanish.see"));
        assertEquals("essentials.warps.mine", EssentialsPermissions.map("applecore.warps.mine"));
    }

    @Test
    void essentialsSpecialNames() {
        assertEquals("essentials.msg", EssentialsPermissions.map("applecore.reply"));
        assertEquals("essentials.time.set", EssentialsPermissions.map("applecore.day"));
        assertEquals("essentials.weather", EssentialsPermissions.map("applecore.sun"));
        assertEquals("essentials.home.others", EssentialsPermissions.map("applecore.playerhome"));
        assertEquals("essentials.essentials", EssentialsPermissions.map("applecore.admin"));
        assertEquals("essentials.teleport.timer.bypass", EssentialsPermissions.map("applecore.teleport.bypass-delay"));
        assertEquals("essentials.tpo", EssentialsPermissions.map("applecore.tptoggle.bypass"));
        assertEquals("essentials.sethome.multiple.unlimited", EssentialsPermissions.map("applecore.homes.unlimited"));
        assertEquals("essentials.warp", EssentialsPermissions.map("applecore.warpinfo"));
        assertEquals("essentials.warp.others", EssentialsPermissions.map("applecore.warp.others"));
        assertEquals("essentials.delwarp", EssentialsPermissions.map("applecore.delwarp"));
    }

    @Test
    void cooldownBypass() {
        assertEquals("essentials.commandcooldowns.bypass", EssentialsPermissions.map("applecore.cooldown.bypass"));
        assertEquals("essentials.commandcooldowns.bypass.back", EssentialsPermissions.map("applecore.back.cooldown.bypass"));
    }

    @Test
    void appleCoreOnlyFeaturesHaveNoEssentialsNode() {
        assertNull(EssentialsPermissions.map("applecore.homes.limit.5"));
        assertNull(EssentialsPermissions.map("applecore.teleport.delay.1"));
        assertNull(EssentialsPermissions.map("applecore.rank.king"));
        assertNull(EssentialsPermissions.map("applecore.killermoney.multiplier"));
        assertNull(EssentialsPermissions.map("applecore.playerwarps.limit.3"));
        assertNull(EssentialsPermissions.map("applecore.warpicon"));
        // Server warps are staff-only; on APPLECUBE essentials.setwarp is a donor perk, so it must not grant them.
        assertNull(EssentialsPermissions.map("applecore.serverwarp"));
        assertNull(EssentialsPermissions.map("essentials.fly"));
    }
}
