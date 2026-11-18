package fun.applecube.core.module.admin;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.command.AbstractCommand;
import fun.applecube.core.command.CommandContext;
import fun.applecube.core.command.CommandException;
import fun.applecube.core.text.Ph;

import java.util.List;
import java.util.Locale;

/** {@code /applecore <reload|info|version>} — permission {@code applecore.admin}. */
final class AppleCoreCommand extends AbstractCommand {

    AppleCoreCommand(AppleCorePlugin plugin) {
        super(plugin, "applecore", "acore", "ac");
        permission("applecore.admin");
    }

    @Override
    public void execute(CommandContext ctx) throws CommandException {
        String sub = ctx.has(0) ? ctx.arg(0).toLowerCase(Locale.ROOT) : "info";
        switch (sub) {
            case "reload" -> {
                long started = System.currentTimeMillis();
                plugin.reload();
                ctx.replyCmd("reloaded", Ph.of("ms", System.currentTimeMillis() - started));
            }
            case "info", "version" -> {
                ctx.replyCmd("info",
                        Ph.of("version", plugin.getPluginMeta().getVersion()),
                        Ph.of("language", plugin.lang().defaultId()),
                        Ph.of("resourcepack", plugin.messages().bool(ctx.sender(), plugin.pack().enabled())),
                        Ph.of("modules", String.join(", ", plugin.moduleIds())),
                        Ph.of("commands", plugin.commands().size()),
                        Ph.of("hooks", String.join(" | ", plugin.hooks().status())));
            }
            default -> throw ctx.usage();
        }
    }

    @Override
    public List<String> suggest(CommandContext ctx) {
        return ctx.size() == 1 ? List.of("reload", "info") : List.of();
    }
}
