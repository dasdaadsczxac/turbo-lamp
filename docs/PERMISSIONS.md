# AppleCore permissions

Every check accepts **three node sets** plus the player's donation rank:

1. **AppleCore**: `applecore.<command>`, `applecore.<command>.others`, …
2. **EssentialsX**: the exact node EssentialsX checks for the same thing. A LuckPerms setup made for
   EssentialsX keeps working without changes (`permissions.essentials-compat: true` in `config.yml`).
3. **TrustEssentials** (the plugin APPLECUBE ran before): `trustessentials.<command>` with its own names
   (`createhome`, `createwarp`, `carttable` …) and `.other`. Table at the end of this file
   (`permissions.trustessentials-compat: true`).
4. **Rank presets** (`ranks.yml`): members of a rank's LuckPerms group (or holders of `applecore.rank.<id>`) get every
   node of the preset, including inherited ranks, and its home / warp limits and KillerMoney multiplier.

Any of them grants access; an AppleCore node explicitly set to `false` in LuckPerms always denies. The mappings live in
`EssentialsPermissions` and `TrustEssentialsPermissions` and are unit-tested. EssentialsX's own rule is
`essentials.<command>` (with `/r` → `essentials.msg`), so most rows are a prefix swap; the exceptions are marked
**bold**. Nodes marked "—" are AppleCore-only; EssentialsX / TrustEssentials wildcards never grant them.

Defaults (`plugin.yml`): player basics are `true`, everything else `op`. Donation commands are unlocked by the rank
presets; groups that already have the EssentialsX or TrustEssentials nodes need nothing extra.

## General

| What | AppleCore | EssentialsX |
|---|---|---|
| `/applecore reload\|info` | `applecore.admin` | **`essentials.essentials`** |
| Ignore all command cooldowns | `applecore.cooldown.bypass` | **`essentials.commandcooldowns.bypass`** |
| Ignore one command's cooldown | `applecore.<command>.cooldown.bypass` | **`essentials.commandcooldowns.bypass.<command>`** |
| Teleport without warm-up | `applecore.teleport.bypass-delay` | **`essentials.teleport.timer.bypass`** |
| Shorter warm-up (seconds) | `applecore.teleport.delay.<n>` (lowest wins) | — |
| See vanished players | `applecore.vanish.see` | `essentials.vanish.see` |

## Teleport

| Command | AppleCore | EssentialsX |
|---|---|---|
| `/tp <player>` | `applecore.tp` | `essentials.tp` |
| `/tp <player> <player>` | `applecore.tp.others` | `essentials.tp.others` |
| `/tp [player] <x> <y> <z>` | `applecore.tp.position` | `essentials.tp.position` |
| `/tpo`, `/tpohere` | `applecore.tpo`, `applecore.tpohere` | `essentials.tpo`, `essentials.tpohere` |
| Reach players with `/tptoggle` off | `applecore.tptoggle.bypass` | **`essentials.tpo`** |
| `/tphere` (`/s`) | `applecore.tphere` | `essentials.tphere` |
| `/tppos` | `applecore.tppos` (+ `.others`) | `essentials.tppos` (+ `.others`) |
| `/tpall` | `applecore.tpall` | `essentials.tpall` |
| `/back` | `applecore.back` (+ `.others`) | `essentials.back` (+ `.others`) |
| `/back` to death point | `applecore.back.ondeath` | `essentials.back.ondeath` |
| `/top` | `applecore.top` | `essentials.top` |
| `/jump` | `applecore.jump` | `essentials.jump` |

## Teleport requests

| Command | AppleCore | EssentialsX |
|---|---|---|
| `/tpa` | `applecore.tpa` | `essentials.tpa` |
| `/tpahere` | `applecore.tpahere` | `essentials.tpahere` |
| `/tpaall` | `applecore.tpaall` | `essentials.tpaall` |
| `/tpaccept` (also needed to receive requests) | `applecore.tpaccept` | `essentials.tpaccept` |
| `/tpdeny` | `applecore.tpdeny` | `essentials.tpdeny` |
| `/tpacancel` | `applecore.tpacancel` | `essentials.tpacancel` |
| `/tptoggle` | `applecore.tptoggle` (+ `.others`) | `essentials.tptoggle` (+ `.others`) |
| `/tpauto` | `applecore.tpauto` (+ `.others`) | `essentials.tpauto` (+ `.others`) |

## Homes

| What | AppleCore | EssentialsX |
|---|---|---|
| `/home`, `/homes` | `applecore.home` | `essentials.home` |
| `/home player:home`, `/homes player` | `applecore.home.others` | `essentials.home.others` |
| `/playerhome` | `applecore.playerhome` | **`essentials.home.others`** |
| `/sethome` | `applecore.sethome` (+ `.others`) | `essentials.sethome` (+ `.others`) |
| `/delhome` | `applecore.delhome` (+ `.others`) | `essentials.delhome` (+ `.others`) |
| Home limit | `applecore.homes.limit.<n>` (highest wins) | `essentials.sethome.multiple` + `.multiple.<set>` (sets in `homes.sethome-multiple`, same as EssentialsX `sethome-multiple`) |
| No home limit | `applecore.homes.unlimited` | **`essentials.sethome.multiple.unlimited`** |
| `/sethome` in a disabled world | `applecore.sethome.bypass-world` | — |

The effective home limit is the larger of the two systems; players with neither get `homes.default-limit`.

## Spawn

| Command | AppleCore | EssentialsX |
|---|---|---|
| `/spawn` | `applecore.spawn` (+ `.others`) | `essentials.spawn` (+ `.others`) |
| `/setspawn` | `applecore.setspawn` | `essentials.setspawn` |

## Player abilities

| Command | AppleCore | EssentialsX |
|---|---|---|
| `/fly` | `applecore.fly` (+ `.others`) | `essentials.fly` (+ `.others`) |
| `/god` | `applecore.god` (+ `.others`) | `essentials.god` (+ `.others`) |
| `/heal`, `/feed`, `/ext` | `applecore.heal\|feed\|ext` (+ `.others`) | `essentials.heal\|feed\|ext` (+ `.others`) |
| `/heal`, `/feed` cooldown | `applecore.heal.cooldown.bypass` | `essentials.heal.cooldown.bypass` **or** `essentials.commandcooldowns.bypass.heal` |
| `/burn`, `/suicide` | `applecore.burn`, `applecore.suicide` | `essentials.burn`, `essentials.suicide` |
| `/speed`, `/flyspeed`, `/walkspeed` | `applecore.speed` + `.fly` / `.walk` / `.others` / `.bypass` | `essentials.speed` + same sub-nodes |
| `/gamemode`, `/gmc`, `/gms`, `/gma`, `/gmsp` | `applecore.gamemode` + `.<mode>` or `.all`, `.others` | `essentials.gamemode` + same sub-nodes |

## Information

| Command | AppleCore | EssentialsX |
|---|---|---|
| `/ping`, `/playtime`, `/getpos` | `applecore.<cmd>` (+ `.others`) | `essentials.<cmd>` (+ `.others`) |
| `/depth`, `/compass`, `/list`, `/realname` | `applecore.<cmd>` | `essentials.<cmd>` |
| `/near` | `applecore.near` (+ `.others`, `.maxexempt`) | `essentials.near` (+ same) |
| Hidden from `/near` (explicit grant only) | `applecore.near.exclude` | `essentials.near.exclude` |
| `/whois` | `applecore.whois` (+ `.ip`) | `essentials.whois` (+ `.ip`) |

## Items and workstations

| Command | AppleCore | EssentialsX |
|---|---|---|
| `/hat` | `applecore.hat` (+ `.ignore-binding`) | `essentials.hat` (+ same) |
| `/repair` (`/fix`) | `applecore.repair`, `.all`, `.armor`, `.enchanted` | `essentials.repair`, same sub-nodes |
| `/itemdb`, `/more`, `/recipe` | `applecore.<cmd>` | `essentials.<cmd>` |
| `/itemname` | `applecore.itemname` (+ `.color`) | `essentials.itemname` (+ `.color`) |
| `/skull` | `applecore.skull` (+ `.others`) | `essentials.skull` (+ `.others`) |
| `/enchant` | `applecore.enchant` (+ `.unsafe`) | `essentials.enchant` |
| `/fireball [type]` | `applecore.fireball` + `applecore.fireball.<type>` | `essentials.fireball` + `essentials.fireball.<type>` |
| `/beezooka` | `applecore.beezooka` | `essentials.beezooka` |
| `/clear` (`/ci`) | `applecore.clearinventory` (+ `.others`) | `essentials.clearinventory` (+ `.others`) |
| `/craft`, `/anvil`, `/stonecutter`, `/grindstone`, `/loom`, `/cartographytable`, `/smithingtable` | `applecore.workbench`, `applecore.anvil`, … | `essentials.workbench`, `essentials.anvil`, … |
| `/enchantingtable` | `applecore.enchantingtable` | — |
| `/trash` | `applecore.disposal` | `essentials.disposal` |

## Nick, vanish, invsee

| Command | AppleCore | EssentialsX |
|---|---|---|
| `/nick` | `applecore.nick` (+ `.others`, `.color`) | `essentials.nick` (+ same) |
| `/vanish` | `applecore.vanish` (+ `.others`) | `essentials.vanish` (+ `.others`) |
| `/invsee` | `applecore.invsee` (+ `.modify`) | `essentials.invsee` (+ `.modify`) |
| Inventory protected from `/invsee` edits (explicit grant only) | `applecore.invsee.preventmodify` | `essentials.invsee.preventmodify` |

## Time and weather

| Command | AppleCore | EssentialsX |
|---|---|---|
| `/day`, `/night` | `applecore.day`, `applecore.night` | **`essentials.time.set`** |
| `/sun`, `/rain`, `/storm` | `applecore.sun\|rain\|storm` | **`essentials.weather`** |
| Outside `world-commands.worlds` | `applecore.world-commands.any-world` | — |
| `/ptime`, `/pweather` | `applecore.ptime\|pweather` (+ `.others`) | `essentials.ptime\|pweather` (+ `.others`) |

## Economy

| Command | AppleCore | EssentialsX |
|---|---|---|
| `/balance` | `applecore.balance` (+ `.others`) | `essentials.balance` (+ `.others`) |
| `/pay` | `applecore.pay` (+ `.offline`) | `essentials.pay` (+ `.offline`) |
| `/paytoggle` | `applecore.paytoggle` (+ `.others`) | `essentials.paytoggle` (+ `.others`) |
| `/baltop` | `applecore.balancetop` | `essentials.balancetop` |
| `/eco give\|take\|set\|reset` | `applecore.eco` | `essentials.eco` |

## Warps

| Command / feature | AppleCore | EssentialsX |
|---|---|---|
| `/warp` (menu), `/warp <warp>`, `/playerwarps` | `applecore.warp` | `essentials.warp` |
| `/warp <warp> <player>` | `applecore.warp.others` | `essentials.warp.others` |
| One server warp (when `warps.per-warp-permission: true`) | `applecore.warps.<warp>` | `essentials.warps.<warp>` |
| `/warpinfo` | `applecore.warpinfo` | **`essentials.warp`** |
| `/setwarp` (own player warps) | `applecore.setwarp` | `essentials.setwarp` |
| Player warp limit (highest wins, default `warps.player-limit`, rank `warps:`) | `applecore.playerwarps.limit.<n>`, `applecore.playerwarps.unlimited` | — |
| No creation cost / any world | `applecore.setwarp.free`, `applecore.setwarp.bypass-world` | — |
| `/delwarp` own (+ other players' warps) | `applecore.delwarp` (+ `.others`) | `essentials.delwarp` (+ `.others`) |
| `/warpicon`, `/warpdesc` own (+ others) | `applecore.warpicon`, `applecore.warpdesc` (+ `.others`) | — |
| Colours in `/warpdesc` | `applecore.warpdesc.color` | — |
| Use player warps their owners closed | `applecore.warp.bypass-closed` | — |
| `/serverwarp`; deleting and editing server warps (`/delwarp`, `/warpicon`, `/warpdesc`, menu) | `applecore.serverwarp` | — |

Server warps are staff-only and AppleCore-only: on APPLECUBE `/setwarp` was a donor perk (from Prince), so neither
`essentials.setwarp` nor `trustessentials.createwarp` / `trustessentials.systemwarp` (which only lists them) grants
`/serverwarp`. Without a rank a player's warp limit is `warps.player-limit` (0), and `/setwarp` answers with the
cheapest rank that has warps. A server warp can also require any node: `/serverwarp perm <warp> <node>`.

## Ranks and KillerMoney

| What | AppleCore | Notes |
|---|---|---|
| Hold a rank | `applecore.rank.<id>` or LuckPerms group `group.<group>` | `ranks.yml`; the preset counts in every check |
| `/ranks`, `/ranks info` | `applecore.ranks` | `true` |
| `/ranks info <player>` | `applecore.ranks.others` | |
| `/ranks give\|take` (donation shop) | `applecore.ranks.manage` | runs `give-commands` / `take-commands` |
| `/killermoney` (+ `.others`) | `applecore.killermoney` | `true` |
| KillerMoney multiplier without a rank | `applecore.killermoney.multiplier.<x>` (`2`, `1_5`, `x1.5`) | highest of rank / node, times `global-multiplier` |

Operators get every EssentialsX node automatically (Bukkit grants undeclared nodes to ops), exactly as with EssentialsX.
That is why opt-in flags such as `near.exclude` and `invsee.preventmodify` count only when explicitly granted.

## TrustEssentials equivalents

Taken from the TrustEssentials executors (`TrustEssentialsPermissions`). Where TrustEssentials gave a capability with
its base node, the row says so: its `/back` always returns to the death point, `/repair all` includes armour,
`/tppos` is the coordinate form of `/tp`, `/playerhome` opens other players' homes. Cooldown bypasses exist only for
`/day` `/night` (`trustessentials.time.cooldown.bypass`), `/sun` `/rain` `/storm` (`trustessentials.weather.cooldown.bypass`)
and `/back` (`trustessentials.back.cooldown.bypass`). Home and warp limits were per-group settings in TrustEssentials;
in AppleCore they are the `homes:` / `warps:` values of `ranks.yml`.

| AppleCore | TrustEssentials (any of) |
|---|---|
| `applecore.admin` | `trustessentials.admin` |
| `applecore.tp` | `trustessentials.tp` |
| `applecore.tp.position` | `trustessentials.tppos` |
| `applecore.tphere` | `trustessentials.tphere` |
| `applecore.tppos` | `trustessentials.tppos` |
| `applecore.tppos.others` | `trustessentials.tppos.other` |
| `applecore.tpall` | `trustessentials.tpall` |
| `applecore.tptoggle.bypass` | `trustessentials.tptoggle.bypass` |
| `applecore.back` | `trustessentials.back` |
| `applecore.back.ondeath` | `trustessentials.back` |
| `applecore.top` | `trustessentials.top` |
| `applecore.tpa` | `trustessentials.tpa` |
| `applecore.tpaccept` | `trustessentials.tpaccept` |
| `applecore.tpdeny` | `trustessentials.tpcancel` |
| `applecore.tptoggle` | `trustessentials.tptoggle` |
| `applecore.home` | `trustessentials.home` |
| `applecore.home.others` | `trustessentials.playerhome` |
| `applecore.sethome` | `trustessentials.createhome` |
| `applecore.delhome` | `trustessentials.delhome` |
| `applecore.playerhome` | `trustessentials.playerhome` |
| `applecore.spawn` | `trustessentials.spawn` |
| `applecore.spawn.others` | `trustessentials.spawn.other` |
| `applecore.setspawn` | `trustessentials.setspawn` |
| `applecore.fly` | `trustessentials.fly` |
| `applecore.fly.others` | `trustessentials.fly.other` |
| `applecore.god` | `trustessentials.god` |
| `applecore.god.others` | `trustessentials.god.other` |
| `applecore.heal` | `trustessentials.heal` |
| `applecore.heal.others` | `trustessentials.heal.other` |
| `applecore.feed` | `trustessentials.feed` |
| `applecore.feed.others` | `trustessentials.feed.other` |
| `applecore.ext` | `trustessentials.ext` |
| `applecore.ext.others` | `trustessentials.ext.other` |
| `applecore.burn` | `trustessentials.burn` |
| `applecore.suicide` | `trustessentials.suicide` |
| `applecore.speed` | `trustessentials.speed`, `trustessentials.flyspeed` |
| `applecore.speed.fly` | `trustessentials.flyspeed` |
| `applecore.speed.walk` | `trustessentials.speed` |
| `applecore.speed.others` | `trustessentials.speed.other`, `trustessentials.flyspeed.other` |
| `applecore.gamemode` | `trustessentials.gamemode` |
| `applecore.gamemode.survival` | `trustessentials.gamemode.survival` |
| `applecore.gamemode.creative` | `trustessentials.gamemode.creative` |
| `applecore.gamemode.adventure` | `trustessentials.gamemode.adventure` |
| `applecore.gamemode.spectator` | `trustessentials.gamemode.spectator` |
| `applecore.gamemode.others` | `trustessentials.gamemode.other` |
| `applecore.ping` | `trustessentials.ping` |
| `applecore.getpos` | `trustessentials.getpos` |
| `applecore.getpos.others` | `trustessentials.getpos.other` |
| `applecore.compass` | `trustessentials.compass` |
| `applecore.near` | `trustessentials.near` |
| `applecore.list` | `trustessentials.list` |
| `applecore.realname` | `trustessentials.realname` |
| `applecore.hat` | `trustessentials.hat` |
| `applecore.repair` | `trustessentials.repair` |
| `applecore.repair.all` | `trustessentials.repair.all` |
| `applecore.repair.armor` | `trustessentials.repair.all` |
| `applecore.itemdb` | `trustessentials.itemdb` |
| `applecore.itemname` | `trustessentials.itemrename` |
| `applecore.itemname.color` | `trustessentials.itemrename` |
| `applecore.more` | `trustessentials.dupe` |
| `applecore.skull` | `trustessentials.head` |
| `applecore.skull.others` | `trustessentials.head` |
| `applecore.enchant` | `trustessentials.enchant` |
| `applecore.enchant.unsafe` | `trustessentials.enchant` |
| `applecore.clearinventory` | `trustessentials.clearinventory` |
| `applecore.clearinventory.others` | `trustessentials.clearinventory.other` |
| `applecore.workbench` | `trustessentials.workbench` |
| `applecore.anvil` | `trustessentials.anvil` |
| `applecore.stonecutter` | `trustessentials.stonecutter` |
| `applecore.grindstone` | `trustessentials.grindstone` |
| `applecore.loom` | `trustessentials.loom` |
| `applecore.cartographytable` | `trustessentials.carttable` |
| `applecore.smithingtable` | `trustessentials.smithtable` |
| `applecore.enchantingtable` | `trustessentials.etable` |
| `applecore.nick` | `trustessentials.nickname` |
| `applecore.nick.others` | `trustessentials.nickname.other` |
| `applecore.nick.color` | `trustessentials.nickname.colors` |
| `applecore.vanish` | `trustessentials.vanish` |
| `applecore.vanish.others` | `trustessentials.vanish.other` |
| `applecore.vanish.see` | `trustessentials.vanish.see` |
| `applecore.invsee` | `trustessentials.invsee` |
| `applecore.invsee.modify` | `trustessentials.invsee.change` |
| `applecore.day` | `trustessentials.day` |
| `applecore.night` | `trustessentials.night` |
| `applecore.sun` | `trustessentials.sun` |
| `applecore.rain` | `trustessentials.rain` |
| `applecore.storm` | `trustessentials.storm` |
| `applecore.ptime` | `trustessentials.ptime` |
| `applecore.pweather` | `trustessentials.pweather` |
| `applecore.balance` | `trustessentials.balance` |
| `applecore.balance.others` | `trustessentials.balance.other` |
| `applecore.pay` | `trustessentials.pay` |
| `applecore.balancetop` | `trustessentials.baltop` |
| `applecore.warp` | `trustessentials.warp` |
| `applecore.warpinfo` | `trustessentials.warpinfo` |
| `applecore.setwarp` | `trustessentials.createwarp` |
| `applecore.delwarp` | `trustessentials.delwarp` |
| `applecore.delwarp.others` | `trustessentials.delwarp.other` |
