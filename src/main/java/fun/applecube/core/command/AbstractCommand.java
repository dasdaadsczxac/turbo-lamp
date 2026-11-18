package fun.applecube.core.command;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.lang.Messages;
import fun.applecube.core.util.Players;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Base class for every AppleCore command, modelled on TrustEssentials' {@code TrustCommand}: a name,
 * default aliases, a permission ({@code applecore.<name>} unless changed) and flags. Commands are
 * registered through Paper's command API by {@link CommandRegistry}; {@code commands.yml} can disable
 * a command, replace its aliases or add a cooldown without code changes.
 */
public abstract class AbstractCommand {

    protected final AppleCorePlugin plugin;
    private final String name;
    private final List<String> aliases;
    private @Nullable String permission;
    private boolean playerOnly;

    protected AbstractCommand(AppleCorePlugin plugin, String name, String... aliases) {
        this.plugin = plugin;
        this.name = name;
        this.aliases = List.of(aliases);
        this.permission = "applecore." + name;
    }

    public abstract void execute(CommandContext ctx) throws CommandException;

    /** Suggestions for the last argument; the registry filters them by what was typed. */
    public List<String> suggest(CommandContext ctx) {
        return List.of();
    }

    public String name() {
        return name;
    }

    public List<String> defaultAliases() {
        return aliases;
    }

    public @Nullable String permission() {
        return permission;
    }

    /** Changes the base permission; {@code null} makes the command available to everyone. */
    protected void permission(@Nullable String permission) {
        this.permission = permission;
    }

    public boolean playerOnly() {
        return playerOnly;
    }

    protected void playerOnly(boolean playerOnly) {
        this.playerOnly = playerOnly;
    }

    protected Messages messages() {
        return plugin.messages();
    }

    /** Visible online players, for suggestions. */
    protected List<String> players(CommandContext ctx) {
        return Players.names(ctx.sender());
    }

    /** Visible players when the sender may target others, otherwise nothing. */
    protected List<String> othersIfAllowed(CommandContext ctx) {
        return ctx.hasPermission(ctx.othersPermission()) ? players(ctx) : List.of();
    }
}
