package fun.applecube.core.module.world;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.command.AbstractCommand;
import fun.applecube.core.command.CommandContext;
import fun.applecube.core.command.CommandException;
import fun.applecube.core.module.Module;
import fun.applecube.core.text.Ph;
import org.bukkit.Bukkit;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Time and weather (stage 3): /day, /night, /sun, /storm, /rain change the world (donation perks, optional
 * broadcast, allowed worlds); /ptime and /pweather change only what the player sees. Vanilla /time and /weather
 * stay untouched for console and datapacks.
 */
public final class WorldModule extends Module {

    public WorldModule(AppleCorePlugin plugin) {
        super(plugin);
    }

    @Override
    public String id() {
        return "world";
    }

    @Override
    public void enable() {
        command(new WorldChange("day", w -> w.setTime(1000)));
        command(new WorldChange("night", w -> w.setTime(13000)));
        command(new WorldChange("sun", w -> {
            w.setStorm(false);
            w.setThundering(false);
            w.setClearWeatherDuration(20 * 60 * 10);
        }));
        command(new WorldChange("rain", w -> {
            w.setStorm(true);
            w.setThundering(false);
        }));
        command(new WorldChange("storm", w -> {
            w.setStorm(true);
            w.setThundering(true);
        }));
        command(new PlayerTime());
        command(new PlayerWeather());
    }

    /** A world-wide change by a player, limited to world-commands.worlds (empty = every world). */
    private final class WorldChange extends AbstractCommand {

        private final Consumer<World> change;

        WorldChange(String name, Consumer<World> change) {
            super(WorldModule.this.plugin, name);
            this.change = change;
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            World world;
            if (ctx.has(0)) {
                world = Bukkit.getWorld(ctx.arg(0));
                if (world == null) {
                    throw new CommandException("errors.world-not-found", Ph.of("world", ctx.arg(0)));
                }
            } else {
                world = ctx.player().getWorld();
            }
            List<String> allowed = plugin.getConfig().getStringList("world-commands.worlds");
            if (!allowed.isEmpty() && !allowed.contains(world.getName()) && !ctx.hasPermission("applecore.world-commands.any-world")) {
                throw new CommandException("commands." + name() + ".world-denied", Ph.of("world", world.getName()));
            }
            change.accept(world);
            ctx.replyCmd("done", Ph.of("world", world.getName()));
            if (plugin.getConfig().getBoolean("world-commands.broadcast", true) && ctx.isPlayer()) {
                plugin.messages().broadcast("commands." + name() + ".broadcast",
                        p -> !p.equals(ctx.sender()) && p.getWorld().equals(world),
                        Ph.of("player", ((Player) ctx.sender()).getName()), Ph.of("world", world.getName()));
            }
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? Bukkit.getWorlds().stream().map(World::getName).toList() : List.of();
        }
    }

    /** /ptime <day|night|noon|midnight|sunrise|sunset|reset|ticks> [player]. */
    private final class PlayerTime extends AbstractCommand {

        PlayerTime() {
            super(WorldModule.this.plugin, "ptime", "playertime");
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            if (!ctx.has(0)) {
                throw ctx.usage();
            }
            Player who = ctx.targetOrSelf(1);
            String raw = ctx.arg(0).toLowerCase(Locale.ROOT);
            if (raw.equals("reset")) {
                who.resetPlayerTime();
                plugin.messages().send(who, "commands.ptime.reset");
            } else {
                long ticks = switch (raw) {
                    case "day", "день" -> 1000;
                    case "noon", "полдень" -> 6000;
                    case "sunset", "закат" -> 12000;
                    case "night", "ночь" -> 13000;
                    case "midnight", "полночь" -> 18000;
                    case "sunrise", "рассвет" -> 23000;
                    default -> {
                        try {
                            yield Math.floorMod(Long.parseLong(raw), 24000L);
                        } catch (NumberFormatException e) {
                            throw ctx.usage();
                        }
                    }
                };
                who.setPlayerTime(ticks, false);
                plugin.messages().send(who, "commands.ptime.done", Ph.of("time", raw));
            }
            if (!ctx.isSelf(who)) {
                ctx.replyCmd("done-other", Ph.of("player", who.getName()), Ph.of("time", raw));
            }
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            if (ctx.size() == 1) {
                return List.of("day", "noon", "sunset", "night", "midnight", "sunrise", "reset");
            }
            return ctx.size() == 2 ? othersIfAllowed(ctx) : List.of();
        }
    }

    /** /pweather <sun|rain|reset> [player]. */
    private final class PlayerWeather extends AbstractCommand {

        PlayerWeather() {
            super(WorldModule.this.plugin, "pweather", "playerweather");
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            if (!ctx.has(0)) {
                throw ctx.usage();
            }
            Player who = ctx.targetOrSelf(1);
            String raw = ctx.arg(0).toLowerCase(Locale.ROOT);
            switch (raw) {
                case "reset" -> who.resetPlayerWeather();
                case "sun", "clear", "солнце", "ясно" -> who.setPlayerWeather(WeatherType.CLEAR);
                case "rain", "storm", "downfall", "дождь" -> who.setPlayerWeather(WeatherType.DOWNFALL);
                default -> throw ctx.usage();
            }
            plugin.messages().send(who, raw.equals("reset") ? "commands.pweather.reset" : "commands.pweather.done",
                    Ph.of("weather", raw));
            if (!ctx.isSelf(who)) {
                ctx.replyCmd("done-other", Ph.of("player", who.getName()), Ph.of("weather", raw));
            }
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            if (ctx.size() == 1) {
                return List.of("sun", "rain", "reset");
            }
            return ctx.size() == 2 ? othersIfAllowed(ctx) : List.of();
        }
    }
}
