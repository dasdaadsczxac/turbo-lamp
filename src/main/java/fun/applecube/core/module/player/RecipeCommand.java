package fun.applecube.core.module.player;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.command.AbstractCommand;
import fun.applecube.core.command.CommandContext;
import fun.applecube.core.command.CommandException;
import fun.applecube.core.gui.Menu;
import fun.applecube.core.item.ItemBuilder;
import fun.applecube.core.text.Ph;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.inventory.StonecuttingRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** /recipe [item]: shows how to craft the held or named item; several recipes are paged. */
final class RecipeCommand extends AbstractCommand {

    private static final int[] GRID = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int RESULT = 24;

    RecipeCommand(AppleCorePlugin plugin) {
        super(plugin, "recipe", "formula", "method", "recipes");
        playerOnly(true);
    }

    @Override
    public void execute(CommandContext ctx) throws CommandException {
        Player self = ctx.player();
        ItemStack target;
        if (ctx.has(0)) {
            Material material = Material.matchMaterial(ctx.arg(0));
            if (material == null || !material.isItem()) {
                throw new CommandException("commands.recipe.unknown", Ph.of("input", ctx.arg(0)));
            }
            target = new ItemStack(material);
        } else {
            target = ItemCommands.hand(self).clone();
            target.setAmount(1);
        }
        List<Recipe> recipes = new ArrayList<>(Bukkit.getRecipesFor(target));
        recipes.removeIf(r -> !(r instanceof ShapedRecipe || r instanceof ShapelessRecipe || r instanceof CookingRecipe<?>
                || r instanceof StonecuttingRecipe || r instanceof SmithingTransformRecipe));
        if (recipes.isEmpty()) {
            throw new CommandException("commands.recipe.none", Ph.of("item", ItemCommands.itemName(target)));
        }
        new RecipeMenu(plugin, self, target, recipes, 0).open();
    }

    @Override
    public List<String> suggest(CommandContext ctx) {
        if (ctx.size() != 1 || ctx.arg(0).length() < 2) {
            return List.of();
        }
        String typed = ctx.arg(0).toLowerCase();
        List<String> out = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material.isItem() && !material.isLegacy() && material.getKey().getKey().startsWith(typed)) {
                out.add(material.getKey().getKey());
                if (out.size() >= 40) {
                    break;
                }
            }
        }
        return out;
    }

    private static final class RecipeMenu extends Menu {

        private final ItemStack target;
        private final List<Recipe> recipes;
        private final int index;

        RecipeMenu(AppleCorePlugin plugin, Player viewer, ItemStack target, List<Recipe> recipes, int index) {
            super(plugin, viewer);
            this.target = target;
            this.recipes = recipes;
            this.index = index;
        }

        @Override
        protected Component title() {
            return plugin.messages().get(viewer, "commands.recipe.title", Ph.of("item", ItemCommands.itemName(target)),
                    Ph.of("page", index + 1), Ph.of("pages", recipes.size()));
        }

        @Override
        protected int rows() {
            return 6;
        }

        @Override
        protected void onOpen() {
            plugin.pack().play(viewer, "menu-open");
        }

        @Override
        protected void render() {
            ItemStack filler = ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
            for (int slot = 0; slot < 54; slot++) {
                set(slot, filler);
            }
            for (int slot : GRID) {
                set(slot, new ItemStack(Material.AIR));
            }
            Recipe recipe = recipes.get(index);
            set(RESULT, recipe.getResult());
            set(23, ItemBuilder.of(Material.ARROW).name(plugin.messages().get(viewer, "commands.recipe.arrow")).build());
            switch (recipe) {
                case ShapedRecipe shaped -> {
                    String[] shape = shaped.getShape();
                    Map<Character, RecipeChoice> choices = shaped.getChoiceMap();
                    for (int row = 0; row < shape.length; row++) {
                        for (int col = 0; col < shape[row].length(); col++) {
                            set(GRID[row * 3 + col], display(choices.get(shape[row].charAt(col))));
                        }
                    }
                }
                case ShapelessRecipe shapeless -> {
                    List<RecipeChoice> choices = shapeless.getChoiceList();
                    for (int i = 0; i < choices.size() && i < GRID.length; i++) {
                        set(GRID[i], display(choices.get(i)));
                    }
                }
                case CookingRecipe<?> cooking -> {
                    set(GRID[4], display(cooking.getInputChoice()));
                    set(GRID[7], new ItemStack(Material.COAL));
                }
                case StonecuttingRecipe cutting -> set(GRID[4], display(cutting.getInputChoice()));
                case SmithingTransformRecipe smithing -> {
                    set(GRID[3], display(smithing.getTemplate()));
                    set(GRID[4], display(smithing.getBase()));
                    set(GRID[5], display(smithing.getAddition()));
                }
                default -> {
                }
            }
            if (index > 0) {
                set(48, ItemBuilder.of(Material.ARROW).name(plugin.messages().get(viewer, "commands.recipe.previous")).build(),
                        click -> reopen(index - 1));
            }
            if (index < recipes.size() - 1) {
                set(50, ItemBuilder.of(Material.ARROW).name(plugin.messages().get(viewer, "commands.recipe.next")).build(),
                        click -> reopen(index + 1));
            }
            set(49, ItemBuilder.of(Material.BARRIER).name(plugin.messages().get(viewer, "commands.recipe.close")).build(),
                    click -> viewer.closeInventory());
        }

        private void reopen(int newIndex) {
            plugin.pack().play(viewer, "click");
            new RecipeMenu(plugin, viewer, target, recipes, newIndex).open();
        }

        private static ItemStack display(RecipeChoice choice) {
            if (choice instanceof RecipeChoice.MaterialChoice materials && !materials.getChoices().isEmpty()) {
                return new ItemStack(materials.getChoices().getFirst());
            }
            if (choice instanceof RecipeChoice.ExactChoice exact && !exact.getChoices().isEmpty()) {
                return exact.getChoices().getFirst().clone();
            }
            return new ItemStack(Material.AIR);
        }
    }
}
