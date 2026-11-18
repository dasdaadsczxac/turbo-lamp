package fun.applecube.core.command;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.config.ConfigFile;
import fun.applecube.core.text.Ph;
import fun.applecube.core.text.Text;
import fun.applecube.core.util.Durations;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

/**
 * Collects commands from modules and registers them with Paper's Brigadier API. Per-command settings
 * come from {@code commands.yml}: {@code enabled}, {@code aliases}, {@code cooldown}. Enabling or
 * disabling a command or changing aliases needs a restart; cooldowns reload with /applecore reload.
 */
public final class CommandRegistry {

    private final AppleCorePlugin plugin;
    private final ConfigFile file;
    private final Map<String, AbstractCommand> commands = new LinkedHashMap<>();
    private final Map<String, Long> cooldowns = new LinkedHashMap<>();
    private boolean hideUnpermitted;

    public CommandRegistry(AppleCorePlugin plugin, ConfigFile file) {
        this.plugin = plugin;
        this.file = file;
    }

    public void add(AbstractCommand command) {
        commands.put(command.name(), command);
    }

    public void reloadSettings() {
        hideUnpermitted = plugin.getConfig().getBoolean("commands.hide-unpermitted", false);
        cooldowns.clear();
        for (AbstractCommand command : commands.values()) {
            ConfigurationSection section = file.yaml().getConfigurationSection(command.name());
            long cooldown = section == null ? 0 : Durations.parseOr(section.get("cooldown"), 0);
            if (cooldown > 0) {
                cooldowns.put(command.name(), cooldown);
            }
        }
    }

    public boolean enabled(String name) {
        ConfigurationSection section = file.yaml().getConfigurationSection(name);
        return commands.containsKey(name) && (section == null || section.getBoolean("enabled", true));
    }

    public List<String> aliases(AbstractCommand command) {
        ConfigurationSection section = file.yaml().getConfigurationSection(command.name());
        if (section != null && section.isList("aliases")) {
            return section.getStringList("aliases").stream().map(a -> a.toLowerCase(Locale.ROOT)).toList();
        }
        return command.defaultAliases();
    }

    public Collection<AbstractCommand> all() {
        return Collections.unmodifiableCollection(commands.values());
    }

    public int size() {
        return commands.size();
    }

    /** Hooks registration into Paper's command lifecycle (runs at startup and on datapack reloads). */
    public void registerWithPaper() {
        reloadSettings();
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands registrar = event.registrar();
            int registered = 0;
            for (AbstractCommand command : commands.values()) {
                if (!enabled(command.name())) {
                    continue;
                }
                String description = Text.plain(plugin.messages().get(null, "commands." + command.name() + ".description"));
                registrar.register(command.name(), description, aliases(command), new Bridge(command));
                registered++;
            }
            plugin.getLogger().info("Registered " + registered + " commands");
        });
    }

    void dispatch(AbstractCommand command, CommandSender sender, String[] args) {
        CommandContext ctx = new CommandContext(plugin, command, sender, args);
        try {
            if (command.playerOnly() && !(sender instanceof Player)) {
                throw new CommandException("errors.players-only");
            }
            String permission = command.permission();
            if (permission != null && !sender.hasPermission(permission)) {
                throw CommandException.noPermission(permission);
            }
            Long cooldown = cooldowns.get(command.name());
            boolean tracked = cooldown != null && sender instanceof Player player
                    && !player.hasPermission("applecore.cooldown.bypass")
                    && !player.hasPermission("applecore." + command.name() + ".cooldown.bypass");
            if (tracked) {
                long left = plugin.cooldowns().remaining(((Player) sender).getUniqueId(), "cmd:" + command.name());
                if (left > 0) {
                    throw new CommandException("errors.cooldown", Ph.of("time", plugin.messages().duration(sender, left)));
                }
            }
            command.execute(ctx);
            if (tracked) {
                plugin.cooldowns().start(((Player) sender).getUniqueId(), "cmd:" + command.name(), cooldown);
            }
        } catch (CommandException e) {
            if (e.permission() != null) {
                plugin.permissions().denied(sender, e.permission());
            } else {
                plugin.messages().send(sender, e.key(), e.placeholders());
            }
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, "Error in /" + command.name() + " " + String.join(" ", args), e);
            plugin.messages().send(sender, "errors.internal");
        }
    }

    private List<String> complete(AbstractCommand command, CommandSender sender, String[] args) {
        String permission = command.permission();
        if (permission != null && !sender.hasPermission(permission)) {
            return List.of();
        }
        String[] normalized = args.length == 0 ? new String[]{""} : args;
        try {
            String typed = normalized[normalized.length - 1].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (String option : command.suggest(new CommandContext(plugin, command, sender, normalized))) {
                if (option.toLowerCase(Locale.ROOT).startsWith(typed)) {
                    out.add(option);
                }
            }
            return out;
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private final class Bridge implements BasicCommand {

        private final AbstractCommand command;

        private Bridge(AbstractCommand command) {
            this.command = command;
        }

        @Override
        public void execute(CommandSourceStack source, String[] args) {
            dispatch(command, source.getSender(), args);
        }

        @Override
        public Collection<String> suggest(CommandSourceStack source, String[] args) {
            return complete(command, source.getSender(), args);
        }

        @Override
        public boolean canUse(CommandSender sender) {
            String permission = command.permission();
            return !hideUnpermitted || permission == null || sender.hasPermission(permission);
        }

        @Override
        public @Nullable String permission() {
            return null;
        }
    }
}
