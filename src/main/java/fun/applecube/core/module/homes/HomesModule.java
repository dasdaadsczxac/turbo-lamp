package fun.applecube.core.module.homes;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.command.AbstractCommand;
import fun.applecube.core.module.Module;

/** Homes (stage 2). Limit: LuckPerms nodes applecore.homes.limit.<n> (highest wins) or applecore.homes.unlimited. */
public final class HomesModule extends Module {

    public HomesModule(AppleCorePlugin plugin) {
        super(plugin);
    }

    @Override
    public String id() {
        return "homes";
    }

    @Override
    public void enable() {
        for (AbstractCommand command : HomeCommands.all(plugin)) {
            command(command);
        }
    }
}
