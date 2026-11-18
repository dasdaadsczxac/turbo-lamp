package fun.applecube.core.module.player;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.command.AbstractCommand;
import fun.applecube.core.command.CommandContext;
import fun.applecube.core.command.CommandException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

/** Portable workstations (donation perks) and /disposal (/trash). */
final class WorkstationCommands {

    private WorkstationCommands() {
    }

    static final class Workstation extends AbstractCommand {

        private final Consumer<Player> opener;

        Workstation(AppleCorePlugin plugin, String name, Consumer<Player> opener, String... aliases) {
            super(plugin, name, aliases);
            this.opener = opener;
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            opener.accept(ctx.player());
        }
    }

    /** Items put into the disposal window are destroyed when it closes. */
    static final class TrashHolder implements InventoryHolder {

        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    static final class Disposal extends AbstractCommand {

        Disposal(AppleCorePlugin plugin) {
            super(plugin, "disposal", "trash");
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player self = ctx.player();
            TrashHolder holder = new TrashHolder();
            holder.inventory = Bukkit.createInventory(holder, 36, plugin.messages().get(self, "commands.disposal.title"));
            self.openInventory(holder.inventory);
        }
    }

    @SuppressWarnings("deprecation")
    static List<AbstractCommand> all(AppleCorePlugin plugin) {
        return List.of(
                new Workstation(plugin, "workbench", p -> p.openWorkbench(null, true), "craft", "wb", "wbench"),
                new Workstation(plugin, "anvil", p -> p.openAnvil(null, true)),
                new Workstation(plugin, "stonecutter", p -> p.openStonecutter(null, true)),
                new Workstation(plugin, "grindstone", p -> p.openGrindstone(null, true)),
                new Workstation(plugin, "loom", p -> p.openLoom(null, true)),
                new Workstation(plugin, "cartographytable", p -> p.openCartographyTable(null, true), "carttable", "cartography"),
                new Workstation(plugin, "smithingtable", p -> p.openSmithingTable(null, true), "smithtable"),
                new Workstation(plugin, "enchantingtable", p -> p.openEnchanting(null, true), "etable", "enchanttable"),
                new Disposal(plugin));
    }
}
