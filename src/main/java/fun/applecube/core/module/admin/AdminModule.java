package fun.applecube.core.module.admin;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.module.Module;

/** Plugin administration: /applecore reload | info | version. Always enabled. */
public final class AdminModule extends Module {

    public AdminModule(AppleCorePlugin plugin) {
        super(plugin);
    }

    @Override
    public String id() {
        return "admin";
    }

    @Override
    public boolean required() {
        return true;
    }

    @Override
    public void enable() {
        command(new AppleCoreCommand(plugin));
    }
}
