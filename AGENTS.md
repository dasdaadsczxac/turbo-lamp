# Working on AppleCore

Read this before changing code. It is written for any developer or AI model that continues the project.

## Ground rules

- Target **Paper 1.21.11** (`io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT`), **Java 21**, **Maven**.
  Do not upgrade the API version unless asked.
- **No `/ec` and no kit system.** The server runs AppleEnderChest and AppleKits; AppleCore must not register
  `ec`, `enderchest`, `kit`, `kits` or anything kit-like.
- **Russian only, no localization system.** Every player-facing string lives in `src/main/resources/messages.yml`
  (one file, Russian). Do not add language files or locale switching. `MessagesFileTest` fails the build if a key
  used in code is missing or a command has no `description`/`usage`. Config comments are Russian too.
- **Three permission sets, no rank system.** Check permissions only through
  `plugin.permissions().has(sender, "applecore.x")` (or `ctx.require` / `ctx.hasPermission`); it also accepts the exact
  EssentialsX node and the TrustEssentials node. Name commands after their EssentialsX counterpart when one exists, so
  `applecore.<cmd>` maps to `essentials.<cmd>` automatically; add exceptions to `EssentialsPermissions`
  (+ `EssentialsPermissionsTest`) and AppleCore-only features to its `APPLECORE_ONLY` set. TrustEssentials nodes are an
  explicit table in `TrustEssentialsPermissions` (taken from its executors; add a row only with evidence). Never let a
  donor-level node grant a staff feature. Declare new AppleCore nodes in `plugin.yml` and list the equivalents in
  `docs/PERMISSIONS.md`.
- **Limits only from LuckPerms permissions** (owner's decision). Donation ranks are LuckPerms groups. Per-player
  numbers are numeric nodes read with `highest()` / `lowest()` / `highestDecimal()` (`applecore.homes.limit.<n>`,
  `applecore.playerwarps.limit.<n>`, `applecore.killermoney.multiplier.<x>`); a default for everyone is a
  `default: true` node in `plugin.yml` (`applecore.homes.limit.1`). Do not add limit tables, rank presets or group maps
  to the configs.
- **Config defaults:** `getConfig().getInt("path", def)` returns `def`, not the bundled value, when an older
  `config.yml` lacks the key, so `def` must equal the value in the bundled `config.yml`.
- Do not commit third-party jars (TrustEssentials) or the resource-pack archive. The pack is installed on the
  server through ItemsAdder; AppleCore only references its glyphs, item models and sounds.
- Run `mvn package` before every commit; it compiles and runs the tests.

## Layout

```
fun.applecube.core
├── AppleCorePlugin      service getters + createModules() (add new modules there)
├── command/             AbstractCommand, CommandContext, CommandException, CommandRegistry (Paper BasicCommand)
├── config/ConfigFile    YAML file with bundled defaults for missing keys
├── message/Messages     messages.yml: render/send/actionbar/title/duration
├── text/                Text (legacy→MiniMessage, <pack>/<plain>), Ph (safe placeholders)
├── pack/PackStyle       pack vs plain design per player, sounds, optional pack sending
├── hook/                Vault economy facade, PlaceholderAPI (PapiHook, PapiExpansion %applecore_*%),
│                        item/ (ItemsAdder, Nexo, Oraxen, CraftEngine, HDB)
├── item/                ItemSpec (config format), ItemFactory (spec → ItemStack), ItemBuilder
├── gui/                 Menu, MenuListener, Click
├── permission/          PermissionService (all node sets, numeric limits), EssentialsPermissions,
│                        TrustEssentialsPermissions (mappings)
├── teleport/            TeleportService (warm-up, safety, /back), SafeLocation
├── user/                User (JSON model), UserManager (load/save lifecycle)
├── storage/             AsyncIo, JsonFiles, StoredLocation
├── util/                Cooldowns, Durations, Players
└── module/<feature>/    one package per feature: a Module subclass + its commands/listeners/menus
```

## Adding a command

1. Create `class XCommand extends AbstractCommand` in the feature package:
   `super(plugin, "name", "alias1", "alias2")`; call `playerOnly(true)` if needed; permission defaults to
   `applecore.name`.
2. Implement `execute(CommandContext ctx)`. Use `ctx.targetOrSelf(i)` for "self or other player" commands; it enforces
   `.others`. Throw `ctx.usage()` or `new CommandException("key", Ph.of(...))` for errors.
3. Register it in the module's `enable()` with `command(new XCommand(plugin))`.
4. Add `commands.name.description`, `commands.name.usage` and the messages to `messages.yml` (Russian).
5. Add the permission to `plugin.yml` and `docs/PERMISSIONS.md` (with its EssentialsX node). Donation perks default to
   `op`; the server owner grants them to the LuckPerms donation groups.
6. Optionally list it in `commands.yml` (aliases/cooldown).

## Messages

- `plugin.messages().send(sender, "key", Ph.of("player", name))` reads `messages.yml`. Placeholders are inserted unparsed, so
  player input cannot inject formatting. Use `Ph.parsed` only for trusted config text.
- Tags available everywhere: `<prefix:ok|error|warn|info>`, `<icon:lmb|rmb|mmb|bar|dot|apple>`, `<menu-font>`,
  theme colours `<accent> <text> <muted> <highlight> <good> <bad> <warn> <info>`, and `<pack>…</pack>` /
  `<plain>…</plain>` for content that only makes sense with or without the resource pack.
- Never name a placeholder `pack` or `plain`, and never use YAML keys `on`, `off`, `yes`, `no` (YAML turns them into booleans).

## Menus

- A menu extends `gui/Menu`; its layout and items live in `src/main/resources/menus/<menu>.yml` (`slots`, `items` in
  the `ItemSpec` format `item` / `model` / `fallback`), its texts in `messages.yml` under `menus.<menu>.<key>.name|lore`.
  Follow the pack's slot map and give every pack button a vanilla `fallback`. Example: `module/warps/WarpMenu` with
  `menus/warps.yml`.

## Resource pack contract (APPLECUBE bordo v2)

- Chat icons: `\uEBA0` red, `\uEBA1` green, `\uEBA2` yellow, `\uEBA3` blue; always render them white.
- Menu font tokens: bar `\uEB00`, LMB `\uEB01`, RMB `\uEB02`, MMB `\uEB03`, apple `\uEB04`, dot `\uEB05`.
- Offset `\uEB40` (−8 px). Warp backgrounds: `\uEB98` server warps, `\uEB99` player warps (title `<white>\uEB40\uEB98</white>`).
  `\uEB9A`–`\uEB9F` are reserved for future AppleCore menus.
- Buttons: `PAPER` + `item_model bordo_menu:gui/<id>` (or ItemsAdder `bordo_menu:<id>`), tooltip style `bordo_menu:menu`.
- Sounds: `bordo_menu:menu.open_rtp`, `bordo_menu:auction.tick`, `bordo_menu:auction.success`, `bordo_menu:auction.cancel`.

## Testing on a real server

```bash
curl -sL "$(curl -s https://fill.papermc.io/v3/projects/paper/versions/1.21.11/builds/latest | grep -o '"url":"[^"]*jar"' | head -1 | cut -d'"' -f4)" -o paper.jar
echo eula=true > eula.txt && mkdir -p plugins && cp target/AppleCore-*.jar plugins/
java -jar paper.jar --nogui     # then run console commands, e.g. "applecore info"
```
