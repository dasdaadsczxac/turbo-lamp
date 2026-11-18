package fun.applecube.core.module;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.command.AbstractCommand;
import org.bukkit.event.Listener;

/**
 * A feature area (teleport, homes, warps …). Modules can be switched off in config.yml
 * ({@code modules.<id>: false}); a disabled module registers no commands or listeners.
 */
public abstract class Module {

    protected final AppleCorePlugin plugin;

    protected Module(AppleCorePlugin plugin) {
        this.plugin = plugin;
    }

    /** Config id under {@code modules:}. */
    public abstract String id();

    /** Register commands and listeners here. */
    public abstract void enable();

    /** Re-read module config after /applecore reload. */
    public void reload() {
    }

    /** Flush data on shutdown. */
    public void disable() {
    }

    /** Modules that can't be turned off (the admin command). */
    public boolean required() {
        return false;
    }

    protected void command(AbstractCommand command) {
        plugin.commands().add(command);
    }

    protected void listen(Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }
}
