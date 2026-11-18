package fun.applecube.core.command;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.text.Ph;
import fun.applecube.core.util.Players;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

/** Arguments and helpers for one command execution or tab-completion request. */
public final class CommandContext {

    private final AppleCorePlugin plugin;
    private final AbstractCommand command;
    private final CommandSender sender;
    private final String[] args;

    public CommandContext(AppleCorePlugin plugin, AbstractCommand command, CommandSender sender, String[] args) {
        this.plugin = plugin;
        this.command = command;
        this.sender = sender;
        this.args = args;
    }

    public CommandSender sender() {
        return sender;
    }

    public String[] args() {
        return args;
    }

    public int size() {
        return args.length;
    }

    public boolean has(int index) {
        return args.length > index;
    }

    public String arg(int index) {
        return args[index];
    }

    /** Arguments from {@code index} joined with spaces (message text, descriptions …). */
    public String joinFrom(int index) {
        return index >= args.length ? "" : String.join(" ", Arrays.copyOfRange(args, index, args.length));
    }

    public boolean isPlayer() {
        return sender instanceof Player;
    }

    public Player player() throws CommandException {
        if (sender instanceof Player player) {
            return player;
        }
        throw new CommandException("errors.players-only");
    }

    public boolean hasPermission(String permission) {
        return sender.hasPermission(permission);
    }

    public void require(String permission) throws CommandException {
        if (!sender.hasPermission(permission)) {
            throw CommandException.noPermission(permission);
        }
    }

    /** {@code <command permission>.others}, the Essentials convention for acting on other players. */
    public String othersPermission() {
        return command.permission() + ".others";
    }

    /** Online player named by {@code args[index]}; vanished players stay hidden. */
    public Player onlinePlayer(int index) throws CommandException {
        if (!has(index)) {
            throw usage();
        }
        Player player = Players.find(sender, args[index]);
        if (player == null) {
            throw new CommandException("errors.player-not-found", Ph.of("player", args[index]));
        }
        return player;
    }

    /** The player in {@code args[index]} (needs {@code .others}) or the sender. */
    public Player targetOrSelf(int index) throws CommandException {
        if (has(index)) {
            Player target = onlinePlayer(index);
            if (!target.equals(sender)) {
                require(othersPermission());
            }
            return target;
        }
        if (sender instanceof Player player) {
            return player;
        }
        throw usage();
    }

    /** A player who has joined before, by exact name (offline lookups use the server cache only). */
    @SuppressWarnings("deprecation")
    public OfflinePlayer knownPlayer(int index) throws CommandException {
        if (!has(index)) {
            throw usage();
        }
        Player online = Players.find(sender, args[index]);
        if (online != null) {
            return online;
        }
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(args[index]);
        if (cached == null || !cached.hasPlayedBefore() && !cached.isOnline()) {
            throw new CommandException("errors.player-never-joined", Ph.of("player", args[index]));
        }
        return cached;
    }

    public int integer(int index, int min, int max) throws CommandException {
        if (!has(index)) {
            throw usage();
        }
        try {
            int value = Integer.parseInt(args[index]);
            if (value < min || value > max) {
                throw new CommandException("errors.number-range", Ph.of("min", min), Ph.of("max", max));
            }
            return value;
        } catch (NumberFormatException e) {
            throw new CommandException("errors.invalid-number", Ph.of("input", args[index]));
        }
    }

    public double decimal(int index, double min, double max) throws CommandException {
        if (!has(index)) {
            throw usage();
        }
        try {
            double value = Double.parseDouble(args[index].replace(',', '.'));
            if (Double.isNaN(value) || Double.isInfinite(value) || value < min || value > max) {
                throw new CommandException("errors.number-range", Ph.of("min", min), Ph.of("max", max));
            }
            return value;
        } catch (NumberFormatException e) {
            throw new CommandException("errors.invalid-number", Ph.of("input", args[index]));
        }
    }

    /** The usage message of this command ({@code commands.<name>.usage}). */
    public CommandException usage() {
        return new CommandException("commands." + command.name() + ".usage");
    }

    public void reply(String key, TagResolver... placeholders) {
        plugin.messages().send(sender, key, placeholders);
    }

    /** Message key under this command: {@code commands.<name>.<suffix>}. */
    public void replyCmd(String suffix, TagResolver... placeholders) {
        plugin.messages().send(sender, "commands." + command.name() + "." + suffix, placeholders);
    }

    public boolean isSelf(Player player) {
        return player.equals(sender);
    }

    public AbstractCommand command() {
        return command;
    }
}
