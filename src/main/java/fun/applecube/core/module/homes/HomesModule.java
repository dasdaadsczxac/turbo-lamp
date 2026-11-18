package fun.applecube.core.module.homes;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.command.AbstractCommand;
import fun.applecube.core.module.Module;

/** Homes (stage 2). Limits: applecore.homes.limit.<n> or EssentialsX sethome-multiple sets. */
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
