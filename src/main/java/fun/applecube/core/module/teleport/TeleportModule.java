package fun.applecube.core.module.teleport;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.command.AbstractCommand;
import fun.applecube.core.module.Module;

/** Teleport commands and requests (stage 2). */
public final class TeleportModule extends Module {

    public TeleportModule(AppleCorePlugin plugin) {
        super(plugin);
    }

    @Override
    public String id() {
        return "teleport";
    }

    @Override
    public void enable() {
        TeleportRequests requests = new TeleportRequests(plugin);
        listen(requests);
        for (AbstractCommand command : TeleportCommands.all(plugin)) {
            command(command);
        }
        for (AbstractCommand command : RequestCommands.all(plugin, requests)) {
            command(command);
        }
    }
}
