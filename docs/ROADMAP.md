# AppleCore: development roadmap

Each stage is one commit (tagged `stage-N`) that builds, passes `mvn package`, and was loaded on a real
Paper 1.21.11 server. A later stage may refine earlier code; the notes below say what each stage delivered and
what is still open, so a different developer or AI model can pick up from any tag.

Legend: ✅ done · 🔜 next · ⏳ planned

## Stage 1: Foundation ✅

- Maven project, Paper API 1.21.11, Java 21; `plugin.yml` with soft dependencies.
- `AppleCorePlugin`: service getters plus a module list (`createModules()`); modules can be turned off in `config.yml`.
- Config: `config.yml` (theme colours, sounds, teleport, resource pack), `commands.yml` (per-command enable/aliases/cooldown).
- Messages: MiniMessage plus `&` codes, PlaceholderAPI, AppleCore tags (`<prefix:*>`, `<icon:*>`, `<menu-font>`,
  theme colours, `<pack>`/`<plain>` sections). (Stage 1 shipped ru/en language files; stage 2 replaced them with a
  single Russian `messages.yml` at the owner's request.)
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

## Stage 2: Russian-only messages, EssentialsX permissions, teleport, homes, spawn ✅

- Owner requests applied: the localization system is gone (no `lang/`, no English, no locale switching); all texts
  are Russian in `messages.yml`, config comments are Russian. A second permission set: every check also accepts the
  exact EssentialsX node (`PermissionService.has` + `EssentialsPermissions`, table in `docs/PERMISSIONS.md`),
  including EssentialsX `sethome-multiple` home sets, timer/cooldown bypasses and `essentials.tpo`.
- Teleport: `tp` (players, coordinates with `~`, selectors fall back to `minecraft:tp`), `tphere (s)`, `tpo`,
  `tpohere`, `tppos`, `tpall`, `back (return)` incl. death point, `top`, `jump (j, jumpto)`.
- Requests: `tpa (call, tpask)`, `tpahere`, `tpaall`, `tpaccept (tpyes)`, `tpdeny (tpno, tpcancel)`, `tpacancel`,
  `tptoggle`, `tpauto`; clickable accept/deny buttons, expiry, destination resolved when the warm-up ends.
- Homes: `home`, `homes`, `sethome (createhome)`, `delhome`, `playerhome (phome)`, `player:home` syntax for staff,
  clickable home list, limits, disabled worlds.
- Spawn: `spawn`, `setspawn` (`spawn.yml`), first-join/join teleport, respawn at spawn, void rescue.
- Verified on Paper 1.21.11 + LuckPerms with two bot players: limits from `essentials.sethome.multiple.vip`,
  `/back` via `essentials.back`, `/tpahere` via `essentials.tpahere`, warm-up cancel on move/damage, TPA flow.

## Stage 3: Essentials utilities, world perks, economy (no chat) ✅

Chat is out of scope at the owner's request: no /msg, /r, /msgtoggle, /ignore, /socialspy, /broadcast, staff or
donate chat. Everything else is EssentialsX-named, so the EssentialsX nodes apply automatically.

- Abilities: `fly`, `god`, `heal`, `feed (eat)`, `ext`, `burn`, `suicide`, `speed`/`flyspeed`/`walkspeed`,
  `gamemode (gm)` + `gmc`/`gms`/`gma`/`gmsp`; fly, god, nick and vanish persist and are restored on join.
- Information: `ping`, `playtime`, `depth`, `getpos`, `compass`, `near`, `list`, `whois`, `realname`.
- Items: `hat`, `repair (fix) [all]`, `itemdb`, `itemname`, `more`, `skull`, `enchant`, `fireball [type]`, `beezooka`,
  `clearinventory (ci, clear)` (vanilla `clear` syntax falls through to `minecraft:clear`), `recipe` (GUI).
- Workstations: `workbench (craft)`, `anvil`, `stonecutter`, `grindstone`, `loom`, `cartographytable`, `smithingtable`,
  `enchantingtable (etable)`, `disposal (trash)`.
- Staff: `nick`, `vanish`, `invsee` (view-only unless `.modify`).
- World perks: `day`, `night`, `sun`, `rain`, `storm` (allowed worlds, broadcast, 60 s cooldown), `ptime`, `pweather`.
  Vanilla `/time` and `/weather` are left alone.
- Economy: AppleCore accounts (`data/economy.json`) registered in Vault with Lowest priority by default, so a dedicated
  economy plugin still wins; `balance`, `pay` (optional commission), `paytoggle`, `balancetop`,
  `eco give|take|set|reset` with the EssentialsX console syntax used by the donation menus.
- Verified on Paper 1.21.11 + LuckPerms + Vault with bot players: every donor command denied, then unlocked by its
  EssentialsX node only; cooldowns; workstation windows; `/eco take` from the console; `/pay`, `/paytoggle`, `/baltop`.

## Stage 4: GUI warps ✅

- `module/warps`: `Warp` (JSON model), `WarpStore` (`data/warps.json`, atomic writes on the IO thread), `WarpMenu`
  (server / players / my warps), `IconPickerMenu`, `WarpCommands`, `ServerWarpCommand`.
- Menu layout `menus/warps.yml` follows the pack's `README_warps.txt`: tabs 0-3 / 5-8 (invisible `ui_invisible`
  items over the drawn tabs), info 4, 28 warp slots, back 45, my warps 47, pages 48-50, create 51, close 53.
  Titles `<white>\uEB40\uEB98</white>` (server) and `\uEB99` (players). Without the pack: the same slots with vanilla
  fallbacks, a glass frame and a glowing selected tab.
- Server warps (`/serverwarp set|delete|rename|title|icon|desc|order|perm|list`): MiniMessage titles and descriptions,
  sort order, optional permission, per-warp permissions like EssentialsX. Staff can also change icons and delete
  them from the menu (RMB, Shift+RMB twice).
- Player warps (`/setwarp`, `/delwarp`, `/warpicon`, `/warpdesc`, `/warpinfo`): limit `warps.player-limit` or
  `applecore.playerwarps.limit.<n>`, optional creation cost, disabled worlds, visits, likes (RMB), open/closed
  (Shift+LMB), two-step delete (Shift+RMB twice), sorting by likes/visits/newest/name.
- Icons: palette from `menus/warps.yml`, the item in hand (ItemsAdder / Nexo / Oraxen / CraftEngine / HeadDatabase
  ids are kept, otherwise material + `item_model` / custom model data / head texture), `owner-head`, or
  `/serverwarp icon <warp> <item> [fallback]` with any item id.
- EssentialsX: `essentials.warp`, `essentials.warp.others`, `essentials.warps.<warp>`, `essentials.delwarp`;
  `essentials.setwarp` manages server warps (see docs/PERMISSIONS.md).
- Verified on Paper 1.21.11 + LuckPerms with bot players: pack and plain layouts (titles, models, fallbacks, glass
  frame, tab glow), pagination over 32 warps, tab switching, icon picker, likes, closing, two-step delete, limits,
  name validation, `/serverwarp` denied and then allowed by `essentials.setwarp`, `/warp <warp> <player>` by
  `essentials.warp.others`, console `/serverwarp list`.

## Stage 5: Donation ranks, KillerMoney, TrustEssentials permissions ✅

- `ranks.yml`, taken from the server's DeluxeMenus `donate.yml`: guard (Страж) → khignt → prince → king → imperator →
  triton → gercog → caesar, plus the seasonal pumpkin. Each rank: LuckPerms `group`, name, badge glyph (from the
  server pack), price, `inherits`, command permissions from the donate menu, `homes`, `warps`, optional
  `teleport-delay`, `killermoney: x1.5`. A player holds a rank through the LuckPerms group (`group.<group>`, explicit
  only, so operators don't get every rank) or `applecore.rank.<id>`; the preset then counts in `PermissionService.has`
  and in the numeric limits. `applecore.rank.<id>` is also registered as a Bukkit permission with the preset as children.
- Denied commands name the cheapest rank that has them ("Доступно с привилегии <badge>"); `/setwarp` without warps
  does the same. `/ranks` (list, prices, limits, multiplier), `/ranks info [player]`, `/ranks give <player> <rank> [time]`
  and `/ranks take` for donation shops (configurable console commands, LuckPerms by default; announcement + title).
- KillerMoney (`killermoney.yml`): reward ranges per mob, chance, spawner / spawn-egg mobs excluded, per-chunk limit,
  no rewards in creative; PvP takes `steal-percent` of the victim's balance (min/max, same-victim cooldown, same-IP
  check). Multiplier = max(rank, `applecore.killermoney.multiplier.<x>`) × `global-multiplier`. `/killermoney`.
- PlaceholderAPI: `%applecore_rank%`, `rank_name`, `rank_badge`, `rank_display`, `rank_price`, `killermoney_x`,
  `killermoney_multiplier`, `homes_limit`, `warps`, `warps_limit`, `balance`, `balance_raw`.
- Third permission set: TrustEssentials (`TrustEssentialsPermissions`, 98 nodes taken from its executors, unit-tested);
  `permissions.trustessentials-compat`. Server warps no longer accept `essentials.setwarp` (a donor perk here).
- Explicitly negated AppleCore nodes deny even when a rank or a compat node would grant them.
- Verified on Paper 1.21.11 + LuckPerms + Vault + PlaceholderAPI 2.12.3 with bot players: hints for /feed, /fly,
  /setwarp; LuckPerms group `king` unlocks the inherited guard → king perks, homes 20, warps 15, x1.5; negation;
  mob rewards with and without x1.5; PvP 5% (50 of 1000) and the cooldown; `/ranks give Helper prince 30d` from the
  console (announcement, title, limits) and `/ranks take`; wrong rank / time errors; `trustessentials.fly` and
  `trustessentials.workbench` unlock /fly and /craft, `trustessentials.systemwarp` does not unlock /serverwarp;
  every placeholder.

## Stage 6: Polish and handoff ⏳

Audit that every permission checked in code is declared, final documentation pass, handoff notes.
