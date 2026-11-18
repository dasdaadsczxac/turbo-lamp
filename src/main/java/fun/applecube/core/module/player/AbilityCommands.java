package fun.applecube.core.module.player;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.command.AbstractCommand;
import fun.applecube.core.command.CommandContext;
import fun.applecube.core.command.CommandException;
import fun.applecube.core.text.Ph;
import fun.applecube.core.user.User;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectTypeCategory;

import java.util.List;
import java.util.Locale;

/** fly, god, heal, feed, ext, burn, suicide, speed/flyspeed/walkspeed, gamemode/gmc/gms/gma/gmsp. */
final class AbilityCommands {

    private AbilityCommands() {
    }

    /** "on"/"off" argument, or null to toggle. */
    static Boolean state(String raw) {
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "on", "enable", "true", "вкл" -> Boolean.TRUE;
            case "off", "disable", "false", "выкл" -> Boolean.FALSE;
            default -> null;
        };
    }

    /** Shared shape of /fly and /god: {@code /cmd [player] [on|off]}. */
    abstract static class Toggle extends AbstractCommand {

        Toggle(AppleCorePlugin plugin, String name, String... aliases) {
            super(plugin, name, aliases);
        }

        abstract boolean current(Player player, User user);

        abstract void apply(Player player, User user, boolean value) throws CommandException;

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player who;
            Boolean forced = null;
            if (!ctx.has(0)) {
                who = ctx.player();
            } else if (!ctx.has(1) && state(ctx.arg(0)) != null) {
                who = ctx.player();
                forced = state(ctx.arg(0));
            } else {
                who = ctx.targetOrSelf(0);
                if (ctx.has(1)) {
                    forced = state(ctx.arg(1));
                    if (forced == null) {
                        throw ctx.usage();
                    }
                }
            }
            User user = plugin.users().user(who);
            boolean value = forced != null ? forced : !current(who, user);
            apply(who, user, value);
            plugin.messages().send(who, "commands." + name() + (value ? ".turned-on" : ".turned-off"));
            if (!ctx.isSelf(who)) {
                ctx.reply("commands." + name() + ".changed-other", Ph.of("player", who.getName()),
                        Ph.of("state", plugin.messages().bool(ctx.sender(), value)));
            }
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            if (ctx.size() == 1) {
                List<String> out = new java.util.ArrayList<>(othersIfAllowed(ctx));
                out.add("on");
                out.add("off");
                return out;
            }
            return ctx.size() == 2 ? List.of("on", "off") : List.of();
        }
    }

    static final class Fly extends Toggle {

        Fly(AppleCorePlugin plugin) {
            super(plugin, "fly");
        }

        @Override
        boolean current(Player player, User user) {
            return player.getAllowFlight();
        }

        @Override
        void apply(Player player, User user, boolean value) throws CommandException {
            if (value && plugin.getConfig().getStringList("fly.disabled-worlds").contains(player.getWorld().getName())
                    && !plugin.permissions().has(player, "applecore.fly.bypass-world")) {
                throw new CommandException("commands.fly.world-disabled");
            }
            setFlight(plugin, player, value);
            user.fly(value);
        }
    }

    /** Turns flight on/off; turning it off mid-air protects from the fall. */
    static void setFlight(AppleCorePlugin plugin, Player player, boolean value) {
        boolean wasFlying = player.isFlying();
        player.setAllowFlight(value);
        if (!value && wasFlying) {
            player.setFlying(false);
            PlayerModule module = plugin.module(PlayerModule.class);
            if (module != null) {
                module.protectFall(player);
            }
        }
    }

    static final class God extends Toggle {

        God(AppleCorePlugin plugin) {
            super(plugin, "god", "godmode", "tgm");
        }

        @Override
        boolean current(Player player, User user) {
            return user.god();
        }

        @Override
        void apply(Player player, User user, boolean value) {
            user.god(value);
            if (value) {
                heal(player);
            }
        }
    }

    static void heal(Player player) {
        AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
        player.setHealth(max != null ? max.getValue() : 20.0);
        player.setFoodLevel(20);
        player.setSaturation(10f);
        player.setFireTicks(0);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().getCategory() == PotionEffectTypeCategory.HARMFUL) {
                player.removePotionEffect(effect.getType());
            }
        }
    }

    /** /heal, /feed, /ext: {@code /cmd [player]}. */
    static final class Restore extends AbstractCommand {

        private final java.util.function.Consumer<Player> action;

        Restore(AppleCorePlugin plugin, String name, java.util.function.Consumer<Player> action, String... aliases) {
            super(plugin, name, aliases);
            this.action = action;
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player who = ctx.targetOrSelf(0);
            if (who.isDead()) {
                throw new CommandException("commands." + name() + ".dead", Ph.of("player", who.getName()));
            }
            action.accept(who);
            plugin.messages().send(who, "commands." + name() + ".done");
            if (!ctx.isSelf(who)) {
                ctx.replyCmd("done-other", Ph.of("player", who.getName()));
            }
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? othersIfAllowed(ctx) : List.of();
        }
    }

    static final class Burn extends AbstractCommand {

        Burn(AppleCorePlugin plugin) {
            super(plugin, "burn");
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player target = ctx.onlinePlayer(0);
            int seconds = ctx.has(1) ? ctx.integer(1, 1, 600) : 5;
            target.setFireTicks(seconds * 20);
            ctx.replyCmd("done", Ph.of("player", target.getName()), Ph.of("seconds", seconds));
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? players(ctx) : ctx.size() == 2 ? List.of("5", "10", "30") : List.of();
        }
    }

    static final class Suicide extends AbstractCommand {

        Suicide(AppleCorePlugin plugin) {
            super(plugin, "suicide");
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player self = ctx.player();
            self.setHealth(0);
            ctx.replyCmd("done");
        }
    }

    /**
     * /speed, /flyspeed, /walkspeed: {@code /speed <1-10> [player]}. Mode follows EssentialsX: the command alias or
     * the flying state, limited by {@code applecore.speed.fly} / {@code applecore.speed.walk}.
     */
    static final class Speed extends AbstractCommand {

        private final Boolean flyMode;

        Speed(AppleCorePlugin plugin, String name, Boolean flyMode, String... aliases) {
            super(plugin, name, aliases);
            permission("applecore.speed");
            this.flyMode = flyMode;
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            if (!ctx.has(0)) {
                throw ctx.usage();
            }
            float speed = (float) ctx.decimal(0, 0, 10);
            Player who = ctx.has(1) ? ctx.onlinePlayer(1) : ctx.player();
            if (!ctx.isSelf(who)) {
                ctx.require("applecore.speed.others");
            }
            boolean fly = flyMode != null ? flyMode : who.isFlying();
            boolean canFly = ctx.hasPermission("applecore.speed.fly");
            boolean canWalk = ctx.hasPermission("applecore.speed.walk");
            if (!(fly && canFly || !fly && canWalk || !canFly && !canWalk)) {
                fly = !canWalk;
            }
            float max = ctx.hasPermission("applecore.speed.bypass") ? 10f
                    : (float) plugin.getConfig().getDouble(fly ? "speed.max-fly" : "speed.max-walk", 10);
            float clamped = Math.min(speed, max);
            float base = fly ? 0.1f : 0.2f;
            float real = clamped <= 1 ? base * clamped : base + (clamped - 1) / 9f * (1f - base);
            if (fly) {
                who.setFlySpeed(Math.max(-1f, Math.min(1f, real)));
            } else {
                who.setWalkSpeed(Math.max(-1f, Math.min(1f, real)));
            }
            ctx.replyCmd(fly ? "fly" : "walk", Ph.of("speed", formatSpeed(clamped)), Ph.of("player", who.getName()));
        }

        private static String formatSpeed(float speed) {
            return speed == Math.rint(speed) ? String.valueOf((int) speed) : String.valueOf(speed);
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            if (ctx.size() == 1) {
                return List.of("1", "2", "3", "5", "10");
            }
            return ctx.size() == 2 && ctx.hasPermission("applecore.speed.others") ? players(ctx) : List.of();
        }
    }

    /**
     * /gamemode <mode> [player] and the shortcuts /gmc /gms /gma /gmsp [player]. Each mode needs
     * {@code applecore.gamemode.<mode>} or {@code applecore.gamemode.all}, like EssentialsX.
     */
    static final class Gamemode extends AbstractCommand {

        private final GameMode fixed;

        Gamemode(AppleCorePlugin plugin, String name, GameMode fixed, String... aliases) {
            super(plugin, name, aliases);
            permission("applecore.gamemode");
            this.fixed = fixed;
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            GameMode mode = fixed;
            int playerArg = 0;
            if (mode == null) {
                if (!ctx.has(0)) {
                    throw ctx.usage();
                }
                mode = parse(ctx.arg(0));
                if (mode == null) {
                    throw new CommandException("commands.gamemode.unknown", Ph.of("input", ctx.arg(0)));
                }
                playerArg = 1;
            }
            String modeName = mode.name().toLowerCase(Locale.ROOT);
            if (!ctx.hasPermission("applecore.gamemode.all") && !ctx.hasPermission("applecore.gamemode." + modeName)) {
                throw CommandException.noPermission("applecore.gamemode." + modeName);
            }
            Player who = ctx.has(playerArg) ? ctx.onlinePlayer(playerArg) : ctx.player();
            if (!ctx.isSelf(who)) {
                ctx.require("applecore.gamemode.others");
            }
            who.setGameMode(mode);
            net.kyori.adventure.text.Component modeText = plugin.messages().get(ctx.sender(), "commands.gamemode.modes." + modeName);
            plugin.messages().send(who, "commands.gamemode.done", Ph.of("mode", modeText));
            if (!ctx.isSelf(who)) {
                ctx.replyCmd("done-other", Ph.of("player", who.getName()), Ph.of("mode", modeText));
            }
        }

        static GameMode parse(String raw) {
            return switch (raw.toLowerCase(Locale.ROOT)) {
                case "0", "s", "survival", "выживание" -> GameMode.SURVIVAL;
                case "1", "c", "creative", "креатив" -> GameMode.CREATIVE;
                case "2", "a", "adventure", "приключение" -> GameMode.ADVENTURE;
                case "3", "sp", "spec", "spectator", "наблюдатель" -> GameMode.SPECTATOR;
                default -> null;
            };
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            if (fixed == null && ctx.size() == 1) {
                return List.of("survival", "creative", "adventure", "spectator");
            }
            int playerArg = fixed == null ? 2 : 1;
            return ctx.size() == playerArg && ctx.hasPermission("applecore.gamemode.others") ? players(ctx) : List.of();
        }
    }

    static List<AbstractCommand> all(AppleCorePlugin plugin) {
        return List.of(
                new Fly(plugin),
                new God(plugin),
                new Restore(plugin, "heal", AbilityCommands::heal),
                new Restore(plugin, "feed", p -> {
                    p.setFoodLevel(20);
                    p.setSaturation(10f);
                }, "eat", "food"),
                new Restore(plugin, "ext", p -> p.setFireTicks(0), "extinguish"),
                new Burn(plugin),
                new Suicide(plugin),
                new Speed(plugin, "speed", null),
                new Speed(plugin, "flyspeed", Boolean.TRUE, "fspeed"),
                new Speed(plugin, "walkspeed", Boolean.FALSE, "wspeed"),
                new Gamemode(plugin, "gamemode", null, "gm"),
                new Gamemode(plugin, "gmc", GameMode.CREATIVE, "creative"),
                new Gamemode(plugin, "gms", GameMode.SURVIVAL, "survival"),
                new Gamemode(plugin, "gma", GameMode.ADVENTURE, "adventure"),
                new Gamemode(plugin, "gmsp", GameMode.SPECTATOR, "spectator", "sp"));
    }
}
