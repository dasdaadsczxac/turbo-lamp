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
