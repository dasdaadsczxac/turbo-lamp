# AppleCore

Essentials-style core plugin for the **APPLECUBE** Minecraft server.

| | |
|---|---|
| Server | Paper **1.21.11** (`api-version: 1.21`) |
| Java | 21 |
| Build | Maven (`mvn package` → `target/AppleCore-<version>.jar`) |
| Optional | Vault + economy, PlaceholderAPI, LuckPerms, ItemsAdder / Nexo / Oraxen / CraftEngine / HeadDatabase |

## What it does

- **Essentials-style commands and permissions**: every command is guarded by `applecore.<command>`,
  acting on others needs `applecore.<command>.others`, numeric limits use nodes such as
  `applecore.homes.limit.5` (highest wins). The command set follows TrustEssentials (names and aliases),
  minus `/ec` and kits, which belong to AppleEnderChest and AppleKits.
- **GUI warps**: two tabs, server warps (set by staff) and player warps (created by players, limited per rank),
  drawn with the APPLECUBE "bordo" resource pack. Icons can be ItemsAdder, Nexo, Oraxen, CraftEngine or
  HeadDatabase items, vanilla items, `item_model` components or textured heads.
- **Donation ranks**: guard → caesar (+ the seasonal pumpkin) are permission presets, so a donation shop only
  has to put the player into a LuckPerms group. Players without access see which rank unlocks a command.
- **KillerMoney**: money for killing mobs and players, with a multiplier per donation rank.
- **Fully localized**: `lang/ru_ru.yml` (default) and `lang/en_us.yml`; MiniMessage and `&` codes;
  optional per-player language.
- **Resource-pack aware**: with the pack, menus use glyph backgrounds, `item_model` buttons, pack sounds,
  chat icons and the bold menu font. Players without the pack (or with `resource-pack.enabled: false`)
  get a clean vanilla design from the same config.

## Install

1. Build or download `AppleCore-<version>.jar` and drop it into `plugins/`.
2. Install the APPLECUBE bordo v2 pack through ItemsAdder (`/iazip`); it already contains the AppleCore warp menus.
3. Start the server once, then edit `plugins/AppleCore/config.yml`, `commands.yml` and `lang/*.yml`.
4. `/applecore reload` applies config and language changes; enabling/disabling commands or modules needs a restart.

## Documentation

- [docs/ROADMAP.md](docs/ROADMAP.md): development stages, what is done and what is next.
- [AGENTS.md](AGENTS.md): architecture and conventions, for whoever (human or AI) continues the work.
- `docs/PERMISSIONS.md` and `docs/RESOURCE_PACK.md` are added by the later stages.

## Development

```bash
mvn package            # compile, run unit tests, build the jar
```

Tests check the text pipeline, duration parsing and that both language files contain the same keys and every
key used in code.
