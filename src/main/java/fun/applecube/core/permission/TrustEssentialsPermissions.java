package fun.applecube.core.permission;

import java.util.List;
import java.util.Map;

/**
 * The third permission set: TrustEssentials nodes, so LuckPerms groups made for TrustEssentials keep working.
 * TrustEssentials names differ from the commands ({@code /sethome} → {@code createhome}, {@code /setwarp} →
 * {@code createwarp}) and uses {@code .other} only for some commands, so the table is explicit. It was taken from the
 * TrustEssentials executors; a few rows grant what TrustEssentials gave with the base node (its {@code /back} always
 * returns to the death point, {@code /repair all} includes armour). {@code trustessentials.systemwarp} only lists server
 * warps there, so it does not grant {@code applecore.serverwarp}. Reference: docs/PERMISSIONS.md.
 */
public final class TrustEssentialsPermissions {

    private static final Map<String, List<String>> NODES = Map.ofEntries(
            Map.entry("applecore.admin", List.of("trustessentials.admin")),
            Map.entry("applecore.tp", List.of("trustessentials.tp")),
            Map.entry("applecore.tp.position", List.of("trustessentials.tppos")),
            Map.entry("applecore.tphere", List.of("trustessentials.tphere")),
            Map.entry("applecore.tppos", List.of("trustessentials.tppos")),
            Map.entry("applecore.tppos.others", List.of("trustessentials.tppos.other")),
            Map.entry("applecore.tpall", List.of("trustessentials.tpall")),
            Map.entry("applecore.tptoggle.bypass", List.of("trustessentials.tptoggle.bypass")),
            Map.entry("applecore.back", List.of("trustessentials.back")),
            Map.entry("applecore.back.ondeath", List.of("trustessentials.back")),
            Map.entry("applecore.top", List.of("trustessentials.top")),
            Map.entry("applecore.tpa", List.of("trustessentials.tpa")),
            Map.entry("applecore.tpaccept", List.of("trustessentials.tpaccept")),
            Map.entry("applecore.tpdeny", List.of("trustessentials.tpcancel")),
            Map.entry("applecore.tptoggle", List.of("trustessentials.tptoggle")),
            Map.entry("applecore.home", List.of("trustessentials.home")),
            Map.entry("applecore.home.others", List.of("trustessentials.playerhome")),
            Map.entry("applecore.sethome", List.of("trustessentials.createhome")),
            Map.entry("applecore.delhome", List.of("trustessentials.delhome")),
            Map.entry("applecore.playerhome", List.of("trustessentials.playerhome")),
            Map.entry("applecore.spawn", List.of("trustessentials.spawn")),
            Map.entry("applecore.spawn.others", List.of("trustessentials.spawn.other")),
            Map.entry("applecore.setspawn", List.of("trustessentials.setspawn")),
            Map.entry("applecore.fly", List.of("trustessentials.fly")),
            Map.entry("applecore.fly.others", List.of("trustessentials.fly.other")),
            Map.entry("applecore.god", List.of("trustessentials.god")),
            Map.entry("applecore.god.others", List.of("trustessentials.god.other")),
            Map.entry("applecore.heal", List.of("trustessentials.heal")),
            Map.entry("applecore.heal.others", List.of("trustessentials.heal.other")),
            Map.entry("applecore.feed", List.of("trustessentials.feed")),
            Map.entry("applecore.feed.others", List.of("trustessentials.feed.other")),
            Map.entry("applecore.ext", List.of("trustessentials.ext")),
            Map.entry("applecore.ext.others", List.of("trustessentials.ext.other")),
            Map.entry("applecore.burn", List.of("trustessentials.burn")),
            Map.entry("applecore.suicide", List.of("trustessentials.suicide")),
            Map.entry("applecore.speed", List.of("trustessentials.speed", "trustessentials.flyspeed")),
            Map.entry("applecore.speed.fly", List.of("trustessentials.flyspeed")),
            Map.entry("applecore.speed.walk", List.of("trustessentials.speed")),
            Map.entry("applecore.speed.others", List.of("trustessentials.speed.other", "trustessentials.flyspeed.other")),
            Map.entry("applecore.gamemode", List.of("trustessentials.gamemode")),
            Map.entry("applecore.gamemode.survival", List.of("trustessentials.gamemode.survival")),
            Map.entry("applecore.gamemode.creative", List.of("trustessentials.gamemode.creative")),
            Map.entry("applecore.gamemode.adventure", List.of("trustessentials.gamemode.adventure")),
            Map.entry("applecore.gamemode.spectator", List.of("trustessentials.gamemode.spectator")),
            Map.entry("applecore.gamemode.others", List.of("trustessentials.gamemode.other")),
            Map.entry("applecore.ping", List.of("trustessentials.ping")),
            Map.entry("applecore.getpos", List.of("trustessentials.getpos")),
            Map.entry("applecore.getpos.others", List.of("trustessentials.getpos.other")),
            Map.entry("applecore.compass", List.of("trustessentials.compass")),
            Map.entry("applecore.near", List.of("trustessentials.near")),
            Map.entry("applecore.list", List.of("trustessentials.list")),
            Map.entry("applecore.realname", List.of("trustessentials.realname")),
            Map.entry("applecore.hat", List.of("trustessentials.hat")),
            Map.entry("applecore.repair", List.of("trustessentials.repair")),
            Map.entry("applecore.repair.all", List.of("trustessentials.repair.all")),
            Map.entry("applecore.repair.armor", List.of("trustessentials.repair.all")),
            Map.entry("applecore.itemdb", List.of("trustessentials.itemdb")),
            Map.entry("applecore.itemname", List.of("trustessentials.itemrename")),
            Map.entry("applecore.itemname.color", List.of("trustessentials.itemrename")),
            Map.entry("applecore.more", List.of("trustessentials.dupe")),
            Map.entry("applecore.skull", List.of("trustessentials.head")),
            Map.entry("applecore.skull.others", List.of("trustessentials.head")),
            Map.entry("applecore.enchant", List.of("trustessentials.enchant")),
            Map.entry("applecore.enchant.unsafe", List.of("trustessentials.enchant")),
            Map.entry("applecore.clearinventory", List.of("trustessentials.clearinventory")),
            Map.entry("applecore.clearinventory.others", List.of("trustessentials.clearinventory.other")),
            Map.entry("applecore.workbench", List.of("trustessentials.workbench")),
            Map.entry("applecore.anvil", List.of("trustessentials.anvil")),
            Map.entry("applecore.stonecutter", List.of("trustessentials.stonecutter")),
            Map.entry("applecore.grindstone", List.of("trustessentials.grindstone")),
            Map.entry("applecore.loom", List.of("trustessentials.loom")),
            Map.entry("applecore.cartographytable", List.of("trustessentials.carttable")),
            Map.entry("applecore.smithingtable", List.of("trustessentials.smithtable")),
            Map.entry("applecore.enchantingtable", List.of("trustessentials.etable")),
            Map.entry("applecore.nick", List.of("trustessentials.nickname")),
            Map.entry("applecore.nick.others", List.of("trustessentials.nickname.other")),
            Map.entry("applecore.nick.color", List.of("trustessentials.nickname.colors")),
            Map.entry("applecore.vanish", List.of("trustessentials.vanish")),
            Map.entry("applecore.vanish.others", List.of("trustessentials.vanish.other")),
            Map.entry("applecore.vanish.see", List.of("trustessentials.vanish.see")),
            Map.entry("applecore.invsee", List.of("trustessentials.invsee")),
            Map.entry("applecore.invsee.modify", List.of("trustessentials.invsee.change")),
            Map.entry("applecore.day", List.of("trustessentials.day")),
            Map.entry("applecore.night", List.of("trustessentials.night")),
            Map.entry("applecore.sun", List.of("trustessentials.sun")),
            Map.entry("applecore.rain", List.of("trustessentials.rain")),
            Map.entry("applecore.storm", List.of("trustessentials.storm")),
            Map.entry("applecore.ptime", List.of("trustessentials.ptime")),
            Map.entry("applecore.pweather", List.of("trustessentials.pweather")),
            Map.entry("applecore.balance", List.of("trustessentials.balance")),
            Map.entry("applecore.balance.others", List.of("trustessentials.balance.other")),
            Map.entry("applecore.pay", List.of("trustessentials.pay")),
            Map.entry("applecore.balancetop", List.of("trustessentials.baltop")),
            Map.entry("applecore.warp", List.of("trustessentials.warp")),
            Map.entry("applecore.warpinfo", List.of("trustessentials.warpinfo")),
            Map.entry("applecore.setwarp", List.of("trustessentials.createwarp")),
            Map.entry("applecore.delwarp", List.of("trustessentials.delwarp")),
            Map.entry("applecore.delwarp.others", List.of("trustessentials.delwarp.other")),
            // Cooldown bypasses: TrustEssentials has them only for /time, /weather and /back.
            Map.entry("applecore.day.cooldown.bypass", List.of("trustessentials.time.cooldown.bypass")),
            Map.entry("applecore.night.cooldown.bypass", List.of("trustessentials.time.cooldown.bypass")),
            Map.entry("applecore.sun.cooldown.bypass", List.of("trustessentials.weather.cooldown.bypass")),
            Map.entry("applecore.rain.cooldown.bypass", List.of("trustessentials.weather.cooldown.bypass")),
            Map.entry("applecore.storm.cooldown.bypass", List.of("trustessentials.weather.cooldown.bypass")),
            Map.entry("applecore.back.cooldown.bypass", List.of("trustessentials.back.cooldown.bypass")));

    private TrustEssentialsPermissions() {
    }

    /** The TrustEssentials nodes that grant the same as an AppleCore node (any of them), or an empty list. */
    public static List<String> map(String node) {
        return NODES.getOrDefault(node, List.of());
    }
}
