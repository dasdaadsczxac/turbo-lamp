# AppleCore

Essentials-style core plugin for the **APPLECUBE** Minecraft server.

| | |
|---|---|
| Server | Paper **1.21.11** (`api-version: 1.21`) |
| Java | 21 |
| Build | Maven (`mvn package` → `target/AppleCore-<version>.jar`) |
| Optional | Vault + economy, PlaceholderAPI, LuckPerms, ItemsAdder / Nexo / Oraxen / CraftEngine / HeadDatabase |

## What it does

- **Essentials-style commands, three permission sets**: every command is guarded by `applecore.<command>`
  (`.others` to act on other players, numeric limits like `applecore.homes.limit.5`) **and** by the exact
  EssentialsX node (`essentials.<command>`, `essentials.sethome.multiple.<set>` …) **and** by the TrustEssentials
  node (`trustessentials.<command>`, `.other`). A LuckPerms setup made for either plugin works as is.
  Table: [docs/PERMISSIONS.md](docs/PERMISSIONS.md).
  The command set follows TrustEssentials and EssentialsX (names and aliases), minus `/ec` and kits, which
  belong to AppleEnderChest and AppleKits.
- **GUI warps**: two tabs, server warps (set by staff) and player warps (created by players, limited by LuckPerms
  permissions), drawn with the APPLECUBE "bordo" resource pack. Icons can be ItemsAdder, Nexo, Oraxen, CraftEngine or
  HeadDatabase items, vanilla items, `item_model` components or textured heads.
- **Limits only from LuckPerms**: home and warp limits, the KillerMoney multiplier and the teleport warm-up are numeric
  permission nodes on the donation groups (`applecore.homes.limit.20`, `applecore.playerwarps.limit.10`,
  `applecore.killermoney.multiplier.1.5` …), highest wins. AppleCore has no rank system and no limit tables.
- **KillerMoney** (`killermoney.yml`): money for killing mobs and a share of the victim's balance for PvP kills, with
  anti-farm limits and a multiplier per LuckPerms group.
- **PlaceholderAPI**: `%applecore_killermoney_x%`, `%applecore_homes_limit%`, `%applecore_warps%`,
  `%applecore_warps_limit%`, `%applecore_balance%` …
- **Russian only**: every text lives in one editable file, `messages.yml` (MiniMessage and `&` codes).
  There is no language switching.
- **Resource-pack aware**: with the pack, menus use glyph backgrounds, `item_model` buttons, pack sounds,
  chat icons and the bold menu font. Players without the pack (or with `resource-pack.enabled: false`)
  get a clean vanilla design from the same config.

## Install

1. Build or download `AppleCore-<version>.jar` and drop it into `plugins/`.
2. Install the APPLECUBE bordo v2 pack through ItemsAdder (`/iazip`); it already contains the AppleCore warp menus.
3. Start the server once, then edit `plugins/AppleCore/config.yml`, `commands.yml`, `messages.yml` and
   `menus/warps.yml` (warp menu layout and icon palette).
4. `/applecore reload` applies config and message changes; enabling/disabling commands or modules needs a restart.

## Documentation

- [docs/ROADMAP.md](docs/ROADMAP.md): development stages, what is done and what is next.
- [docs/PERMISSIONS.md](docs/PERMISSIONS.md): every AppleCore node and its EssentialsX twin.
- [AGENTS.md](AGENTS.md): architecture and conventions, for whoever (human or AI) continues the work.

## Development

```bash
mvn package            # compile, run unit tests, build the jar
```

Tests check the text pipeline, duration parsing, the EssentialsX and TrustEssentials permission mappings, KillerMoney
parsing, that every message key used in code exists in `messages.yml`, and that every permission checked in code is
declared in `plugin.yml`.
