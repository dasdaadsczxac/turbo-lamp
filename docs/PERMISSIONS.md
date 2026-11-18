# AppleCore permissions

Every check accepts **two node sets**:

1. **AppleCore**: `applecore.<command>`, `applecore.<command>.others`, …
2. **EssentialsX**: the exact node EssentialsX checks for the same thing. A LuckPerms setup made for
   EssentialsX keeps working without changes (`permissions.essentials-compat: true` in `config.yml`).

Either node grants access. The mapping lives in `EssentialsPermissions` and is unit-tested. EssentialsX's own
rule is `essentials.<command>` (with `/r` → `essentials.msg`), so most rows are a prefix swap; the exceptions are
marked **bold**. Nodes marked "—" are AppleCore-only. EssentialsX wildcards (`essentials.*`) never grant them.

Defaults (`plugin.yml`): player basics are `true`, everything else `op`. Donation ranks (stage 5) grant the
AppleCore nodes; groups that already have the EssentialsX nodes need nothing extra.

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
| Player warp limit (highest wins, default `warps.player-limit`) | `applecore.playerwarps.limit.<n>`, `applecore.playerwarps.unlimited` | — |
| No creation cost / any world | `applecore.setwarp.free`, `applecore.setwarp.bypass-world` | — |
| `/delwarp` own (+ other players' warps) | `applecore.delwarp` (+ `.others`) | `essentials.delwarp` (+ `.others`) |
| `/warpicon`, `/warpdesc` own (+ others) | `applecore.warpicon`, `applecore.warpdesc` (+ `.others`) | — |
| Colours in `/warpdesc` | `applecore.warpdesc.color` | — |
| Use player warps their owners closed | `applecore.warp.bypass-closed` | — |
| `/serverwarp`; deleting and editing server warps (`/delwarp`, `/warpicon`, `/warpdesc`, menu) | `applecore.serverwarp` | **`essentials.setwarp`** |

In EssentialsX `/setwarp` creates the shared warps, so holders of `essentials.setwarp` manage AppleCore's server
warps. In AppleCore `/setwarp` creates the player's own warp (limited per rank); server warps are created with
`/serverwarp set <name>`. A server warp can also require any node of your choice: `/serverwarp perm <warp> <node>`.

Operators get every EssentialsX node automatically (Bukkit grants undeclared nodes to ops), exactly as with EssentialsX.
That is why opt-in flags such as `near.exclude` and `invsee.preventmodify` count only when explicitly granted.
