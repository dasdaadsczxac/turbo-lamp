# AppleCore: development roadmap

Each stage is one commit (tagged `stage-N`) that builds, passes `mvn package`, and was loaded on a real
Paper 1.21.11 server. A later stage may refine earlier code; the notes below say what each stage delivered and
what is still open, so a different developer or AI model can pick up from any tag.

Legend: ✅ done · 🔜 next · ⏳ planned

## Stage 1: Foundation ✅

- Maven project, Paper API 1.21.11, Java 21; `plugin.yml` with soft dependencies.
- `AppleCorePlugin`: service getters plus a module list (`createModules()`); modules can be turned off in `config.yml`.
- Config: `config.yml` (theme colours, sounds, teleport, resource pack), `commands.yml` (per-command enable/aliases/cooldown).
- Localization: `lang/ru_ru.yml` and `lang/en_us.yml`; MiniMessage plus `&` codes, PlaceholderAPI, AppleCore tags
  (`<prefix:*>`, `<icon:*>`, `<menu-font>`, theme colours, `<pack>`/`<plain>` sections); optional per-player language.
- Commands: `AbstractCommand` (mirrors TrustEssentials' `TrustCommand`: name, aliases, permission, player-only),
  registered through Paper's Brigadier `BasicCommand` API; `.others` convention; cooldowns; the no-permission
  message can name the donation rank that unlocks the command (hook used in stage 5).
- Permissions: `PermissionService` with Essentials-style numeric nodes (`highest`/`lowest`).
- Storage: per-player JSON (`userdata/<uuid>.json`), loaded at async pre-login, atomic writes on a single IO thread.
- Teleports: warm-up with boss bar and particles, cancelled by moving or taking damage; chunk preloading, safe-spot
  search, invulnerability, sound, `/back` history (teleports and, with `applecore.back.ondeath`, deaths).
- GUI kit: `Menu`/`MenuListener` (every click cancelled), `ItemSpec`/`ItemFactory`/`ItemBuilder`; custom items from
  ItemsAdder, Nexo, Oraxen, CraftEngine and HeadDatabase via reflection (no compile-time dependency); `item_model`,
  `tooltip_style`, textured heads.
- Resource pack: `PackStyle` decides per player between the pack design and the plain design (players who decline
  the pack get the plain one); optional pack sending.
- Admin: `/applecore reload|info`.
- Tests: text pipeline, durations, language parity, and keys used in code.

## Stage 2: Teleport, homes, spawn 🔜

Commands (TrustEssentials names/aliases): `tp`, `tphere (s)`, `tppos`, `tpall`, `tpa (call)`, `tpahere`,
`tpaccept (tpyes)`, `tpdeny (tpno, tpcancel)`, `tpacancel`, `tptoggle`, `tpauto`, `back`, `top`, `jump`,
`spawn`, `setspawn`, `home (homes)`, `sethome (createhome)`, `delhome`, `playerhome (phome)`.
Home limits through `applecore.homes.limit.<n>`; spawn on join/respawn options.

## Stage 3: Essentials utilities, chat, economy ⏳

Player: `fly`, `god`, `heal`, `feed`, `ext`, `burn`, `speed`, `flyspeed`, `gamemode (gm, gmc/gms/gma/gmsp)`, `hat`,
`repair (fix)`, `workbench (craft, wb)`, `anvil`, `stonecutter`, `grindstone`, `loom`, `cartographytable`,
`smithingtable`, `enchantingtable`, `trash (disposal)`, `itemdb`, `itemrename`, `clear (ci)`, `invsee`, `near`,
`getpos`, `depth`, `compass`, `ptime`, `pweather`, `suicide`, `kill`, `nick`, `realname`, `whois`, `ping`, `list`,
`fireball`, `vanish`. World: `day`, `night`, `time`, `sun`, `rain`, `storm`, `weather`.
Chat: `msg`, `reply`, `msgtoggle`, `ignore`, `socialspy`, `broadcast (bc)`, `staffchat`.
Economy (Vault): `balance`, `pay`, `paytoggle`, `baltop`, `eco give|take|set|reset` (console syntax used by donation menus).

## Stage 4: GUI warps ⏳

Server and player warps in the bordo pack menus (`\uEB98` / `\uEB99` backgrounds, slot layout from the pack's
`README_warps.txt`), pagination, "My warps", create button, icons from custom-item plugins, visits and likes, player
warp limits per rank, per-warp permissions, plain-design fallback. Commands: `warp (warps)`, `setwarp`, `delwarp`,
`warpinfo`, `serverwarp`, `warpicon`, `warpdesc`.

## Stage 5: Donation ranks and KillerMoney ⏳

`ranks.yml`: guard, khignt, prince, king, imperator, triton, gercog, caesar, pumpkin; each rank is a permission preset
(registered as a Bukkit permission with children, so a LuckPerms group only needs `applecore.rank.<id>`), with display
name, prefix glyph, home/warp limits and teleport delay. `killermoney.yml`: mob rewards, PvP steal percentage,
anti-farm rules, multiplier per rank. PlaceholderAPI expansion `%applecore_*%`.

## Stage 6: Polish and handoff ⏳

Permission reference, resource-pack integration guide, final language review, live-server smoke test.
