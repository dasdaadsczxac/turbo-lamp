package fun.applecube.core.module.killermoney;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.command.AbstractCommand;
import fun.applecube.core.command.CommandContext;
import fun.applecube.core.command.CommandException;
import fun.applecube.core.text.Ph;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/** /killermoney [player]: the player's multiplier, PvP share and the best-paying mobs. */
final class KillerMoneyCommand extends AbstractCommand {

    private final KillerMoneyModule module;

    KillerMoneyCommand(AppleCorePlugin plugin, KillerMoneyModule module) {
        super(plugin, "killermoney", "km", "killmoney");
        this.module = module;
    }

    @Override
    public void execute(CommandContext ctx) throws CommandException {
        Player target = ctx.targetOrSelf(0);
        ctx.reply("killermoney.info", Ph.of("player", target.getName()),
                Ph.of("multiplier", format(module.multiplier(target))),
                Ph.of("percent", format(module.config().getDouble("pvp.steal-percent", 5))));
        List<Map.Entry<EntityType, KillerMoneyModule.Reward>> table = module.table();
        int shown = 0;
        for (Map.Entry<EntityType, KillerMoneyModule.Reward> entry : table) {
            if (shown++ >= 8) {
                break;
            }
            KillerMoneyModule.Reward reward = entry.getValue();
            String range = reward.min() == reward.max() ? format(reward.min()) : format(reward.min()) + "–" + format(reward.max());
            ctx.reply("killermoney.info-mob", Ph.of("mob", Component.translatable(entry.getKey().translationKey())),
                    Ph.of("amount", range));
        }
    }

    static String format(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(Math.round(value * 100) / 100.0);
    }

    @Override
    public List<String> suggest(CommandContext ctx) {
        return ctx.size() == 1 ? othersIfAllowed(ctx) : List.of();
    }
}
