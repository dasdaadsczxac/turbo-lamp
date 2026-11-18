package fun.applecube.core.permission;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TrustEssentials names differ from the commands; the table must keep the exact nodes TrustEssentials checks. */
class TrustEssentialsPermissionsTest {

    @Test
    void renamedCommands() {
        assertEquals(List.of("trustessentials.createhome"), TrustEssentialsPermissions.map("applecore.sethome"));
        assertEquals(List.of("trustessentials.createwarp"), TrustEssentialsPermissions.map("applecore.setwarp"));
        assertEquals(List.of("trustessentials.carttable"), TrustEssentialsPermissions.map("applecore.cartographytable"));
        assertEquals(List.of("trustessentials.nickname"), TrustEssentialsPermissions.map("applecore.nick"));
        assertEquals(List.of("trustessentials.baltop"), TrustEssentialsPermissions.map("applecore.balancetop"));
    }

    @Test
    void otherPlayersUseDotOther() {
        assertEquals(List.of("trustessentials.fly.other"), TrustEssentialsPermissions.map("applecore.fly.others"));
        assertEquals(List.of("trustessentials.delwarp.other"), TrustEssentialsPermissions.map("applecore.delwarp.others"));
        assertEquals(List.of("trustessentials.speed.other", "trustessentials.flyspeed.other"),
                TrustEssentialsPermissions.map("applecore.speed.others"));
    }

    @Test
    void cooldownBypasses() {
        assertEquals(List.of("trustessentials.time.cooldown.bypass"), TrustEssentialsPermissions.map("applecore.day.cooldown.bypass"));
        assertEquals(List.of("trustessentials.weather.cooldown.bypass"), TrustEssentialsPermissions.map("applecore.storm.cooldown.bypass"));
    }

    @Test
    void noAdminPowersFromPlayerNodes() {
        // trustessentials.systemwarp only lists server warps; it must not manage them.
        assertTrue(TrustEssentialsPermissions.map("applecore.serverwarp").isEmpty());
        assertTrue(TrustEssentialsPermissions.map("applecore.ranks.manage").isEmpty());
        assertTrue(TrustEssentialsPermissions.map("applecore.playerwarps.unlimited").isEmpty());
        assertTrue(TrustEssentialsPermissions.map("trustessentials.fly").isEmpty());
    }
}
