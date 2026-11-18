package fun.applecube.core;

import fun.applecube.core.command.CommandRegistry;
import fun.applecube.core.config.ConfigFile;
import fun.applecube.core.gui.MenuListener;
import fun.applecube.core.hook.Hooks;
import fun.applecube.core.item.ItemFactory;
import fun.applecube.core.message.Messages;
import fun.applecube.core.module.Module;
import fun.applecube.core.module.admin.AdminModule;
import fun.applecube.core.module.homes.HomesModule;
import fun.applecube.core.module.spawn.SpawnModule;
import fun.applecube.core.module.teleport.TeleportModule;
import fun.applecube.core.pack.PackStyle;
import fun.applecube.core.permission.PermissionService;
import fun.applecube.core.storage.AsyncIo;
import fun.applecube.core.teleport.TeleportService;
import fun.applecube.core.user.UserManager;
import fun.applecube.core.util.Cooldowns;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * AppleCore entry point. Services are created once in {@link #onEnable()} and exposed through
 * getters; features live in {@link Module}s registered in {@link #createModules()}.
 */
public final class AppleCorePlugin extends JavaPlugin {

    private final List<Module> modules = new ArrayList<>();
    private AsyncIo io;
    private ConfigFile commandsFile;
    private PackStyle pack;
    private Messages messages;
    private Hooks hooks;
    private ItemFactory items;
    private PermissionService permissions;
    private Cooldowns cooldowns;
    private UserManager users;
    private TeleportService teleports;
    private CommandRegistry commands;

    @Override
    public void onEnable() {
        long started = System.currentTimeMillis();
        io = new AsyncIo(getLogger());
        saveDefaultConfig();
        commandsFile = new ConfigFile(this, "commands.yml", true);
        pack = new PackStyle(this);
        messages = new Messages(this);
        hooks = new Hooks(this);
        items = new ItemFactory(this);
        permissions = new PermissionService(this);
        cooldowns = new Cooldowns();
        users = new UserManager(this);
        teleports = new TeleportService(this);
        commands = new CommandRegistry(this, commandsFile);
        loadConfiguration();
        hooks.enable();

        listen(users);
        listen(teleports);
        listen(pack);
        listen(new MenuListener());

        for (Module module : createModules()) {
            if (!module.required() && !getConfig().getBoolean("modules." + module.id(), true)) {
                getLogger().info("Module '" + module.id() + "' is disabled in config.yml");
                continue;
            }
            try {
                module.enable();
                modules.add(module);
            } catch (RuntimeException e) {
                getLogger().log(Level.SEVERE, "Module '" + module.id() + "' failed to start", e);
            }
        }
        commands.registerWithPaper();
        users.start();
        getLogger().info("AppleCore " + getPluginMeta().getVersion() + " enabled in "
                + (System.currentTimeMillis() - started) + " ms: modules " + moduleIds());
    }

    /** Every feature module, in start order. New stages add their modules here. */
    private List<Module> createModules() {
        List<Module> list = new ArrayList<>();
        list.add(new AdminModule(this));
        list.add(new TeleportModule(this));
        list.add(new HomesModule(this));
        list.add(new SpawnModule(this));
        return list;
    }

    @Override
    public void onDisable() {
        MenuListener.closeAll();
        if (teleports != null) {
            teleports.cancelAll();
        }
        for (Module module : modules) {
            try {
                module.disable();
            } catch (RuntimeException e) {
                getLogger().log(Level.SEVERE, "Module '" + module.id() + "' failed to stop cleanly", e);
            }
        }
        if (users != null) {
            users.shutdown();
        }
        if (io != null) {
            io.shutdown();
        }
    }

    private void loadConfiguration() {
        reloadConfig();
        commandsFile.load();
        messages.reload();
        pack.reload();
        permissions.reload();
    }

    /** /applecore reload: configs, messages, pack style, module settings, command cooldowns. */
    public void reload() {
        MenuListener.closeAll();
        loadConfiguration();
        commands.reloadSettings();
        for (Module module : modules) {
            module.reload();
        }
    }

    public List<String> moduleIds() {
        return modules.stream().map(Module::id).toList();
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T module(Class<T> type) {
        for (Module module : modules) {
            if (type.isInstance(module)) {
                return (T) module;
            }
        }
        return null;
    }

    private void listen(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    public AsyncIo io() {
        return io;
    }

    public PackStyle pack() {
        return pack;
    }

    public Messages messages() {
        return messages;
    }

    public Hooks hooks() {
        return hooks;
    }

    public ItemFactory items() {
        return items;
    }

    public PermissionService permissions() {
        return permissions;
    }

    public Cooldowns cooldowns() {
        return cooldowns;
    }

    public UserManager users() {
        return users;
    }

    public TeleportService teleports() {
        return teleports;
    }

    public CommandRegistry commands() {
        return commands;
    }
}
