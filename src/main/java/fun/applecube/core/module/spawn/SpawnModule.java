package fun.applecube.core.module.spawn;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.command.AbstractCommand;
import fun.applecube.core.command.CommandContext;
import fun.applecube.core.command.CommandException;
import fun.applecube.core.config.ConfigFile;
import fun.applecube.core.module.Module;
import fun.applecube.core.text.Ph;
import fun.applecube.core.util.Locations;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** /spawn, /setspawn and spawn handling on first join, join, respawn and falling into the void. */
public final class SpawnModule extends Module implements Listener {

    private final ConfigFile file;

    public SpawnModule(AppleCorePlugin plugin) {
        super(plugin);
        this.file = new ConfigFile(plugin, "spawn.yml", false);
    }

    @Override
    public String id() {
        return "spawn";
    }

    @Override
    public void enable() {
        file.load();
        listen(this);
        command(new SpawnCommand());
        command(new SetSpawnCommand());
    }

    @Override
    public void reload() {
        file.load();
    }

    /** The /setspawn location, or the main world's spawn if none was set. */
    public @Nullable Location spawn() {
        ConfigurationSection s = file.yaml().getConfigurationSection("location");
        if (s != null) {
            World world = Bukkit.getWorld(s.getString("world", ""));
            if (world != null) {
                return new Location(world, s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                        (float) s.getDouble("yaw"), (float) s.getDouble("pitch"));
            }
        }
        List<World> worlds = Bukkit.getWorlds();
        return worlds.isEmpty() ? null : worlds.getFirst().getSpawnLocation();
    }

    public void setSpawn(Location location) {
        YamlConfiguration yaml = file.yaml();
        yaml.set("location.world", location.getWorld().getName());
        yaml.set("location.x", location.getX());
        yaml.set("location.y", location.getY());
        yaml.set("location.z", location.getZ());
        yaml.set("location.yaw", (double) location.getYaw());
        yaml.set("location.pitch", (double) location.getPitch());
        file.save();
        if (plugin.getConfig().getBoolean("spawn.set-world-spawn", true)) {
            location.getWorld().setSpawnLocation(location);
        }
    }

    // ---- events ------------------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean first = !player.hasPlayedBefore();
        if (first && plugin.getConfig().getBoolean("spawn.teleport-on-first-join", true)
                || plugin.getConfig().getBoolean("spawn.teleport-on-join", false)) {
            Location spawn = spawn();
            if (spawn != null) {
                Bukkit.getScheduler().runTask(plugin, () -> player.teleportAsync(spawn, PlayerTeleportEvent.TeleportCause.PLUGIN));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    @SuppressWarnings("deprecation")
    public void onRespawn(PlayerRespawnEvent event) {
        boolean bed = event.isBedSpawn() || event.isAnchorSpawn();
        boolean force = plugin.getConfig().getBoolean("spawn.force-respawn-at-spawn", false);
        if (force || !bed && plugin.getConfig().getBoolean("spawn.respawn-at-spawn", true)) {
            Location spawn = spawn();
            if (spawn != null) {
                event.setRespawnLocation(spawn);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onVoid(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("spawn.void.enabled", false)) {
            return;
        }
        Location to = event.getTo();
        if (to.getY() >= plugin.getConfig().getDouble("spawn.void.y", -64)
                || !plugin.getConfig().getStringList("spawn.void.worlds").contains(to.getWorld().getName())) {
            return;
        }
        Location spawn = spawn();
        if (spawn != null) {
            event.getPlayer().setFallDistance(0);
            event.getPlayer().teleportAsync(spawn, PlayerTeleportEvent.TeleportCause.PLUGIN);
            plugin.messages().send(event.getPlayer(), "commands.spawn.void");
        }
    }

    // ---- commands ----------------------------------------------------------------------------------

    private final class SpawnCommand extends AbstractCommand {

        SpawnCommand() {
            super(SpawnModule.this.plugin, "spawn");
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player who = ctx.targetOrSelf(0);
            Location spawn = spawn();
            if (spawn == null) {
                throw new CommandException("commands.spawn.not-set");
            }
            boolean self = ctx.isSelf(who);
            plugin.teleports().teleport(who, spawn, self ? plugin.teleports().delaySeconds(who) : 0, () -> {
                plugin.messages().send(who, "commands.spawn.done");
                if (!self) {
                    ctx.replyCmd("done-other", Ph.of("player", who.getName()));
                }
            });
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? othersIfAllowed(ctx) : List.of();
        }
    }

    private final class SetSpawnCommand extends AbstractCommand {

        SetSpawnCommand() {
            super(SpawnModule.this.plugin, "setspawn", "set-spawn");
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Location here = ctx.player().getLocation();
            setSpawn(here);
            ctx.replyCmd("done", Locations.placeholders(here));
        }
    }
}
