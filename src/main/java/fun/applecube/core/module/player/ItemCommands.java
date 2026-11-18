package fun.applecube.core.module.player;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.command.AbstractCommand;
import fun.applecube.core.command.CommandContext;
import fun.applecube.core.command.CommandException;
import fun.applecube.core.item.ItemSpec;
import fun.applecube.core.text.Ph;
import fun.applecube.core.text.Text;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Bee;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.entity.Trident;
import org.bukkit.entity.WitherSkull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** hat, repair, itemdb, itemname, more, skull, enchant, fireball, beezooka, clearinventory. */
final class ItemCommands {

    private ItemCommands() {
    }

    static ItemStack hand(Player player) throws CommandException {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            throw new CommandException("errors.empty-hand");
        }
        return item;
    }

    static Component itemName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.displayName();
        }
        return Component.translatable(item.getType().translationKey());
    }

    static final class Hat extends AbstractCommand {

        Hat(AppleCorePlugin plugin) {
            super(plugin, "hat");
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player self = ctx.player();
            PlayerInventory inventory = self.getInventory();
            ItemStack hand = hand(self);
            ItemStack helmet = inventory.getHelmet();
            if (helmet != null && helmet.containsEnchantment(Enchantment.BINDING_CURSE)
                    && !ctx.hasPermission("applecore.hat.ignore-binding")) {
                throw new CommandException("commands.hat.binding");
            }
            inventory.setHelmet(hand);
            inventory.setItemInMainHand(helmet);
            ctx.replyCmd("done", Ph.of("item", itemName(hand)));
        }
    }

    static boolean isRepairable(ItemStack item) {
        return item != null && !item.getType().isAir() && item.getItemMeta() instanceof Damageable damageable
                && damageable.hasDamage();
    }

    /** /repair [hand|all] (alias /fix). Enchanted items follow repair.allow-enchanted like EssentialsX. */
    static final class Repair extends AbstractCommand {

        Repair(AppleCorePlugin plugin) {
            super(plugin, "repair", "fix");
            playerOnly(true);
        }

        private boolean allowed(CommandContext ctx, ItemStack item) {
            return item.getEnchantments().isEmpty() || plugin.getConfig().getBoolean("repair.allow-enchanted", true)
                    || ctx.hasPermission("applecore.repair.enchanted");
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player self = ctx.player();
            boolean all = ctx.has(0) && ctx.arg(0).equalsIgnoreCase("all");
            if (all) {
                ctx.require("applecore.repair.all");
                List<ItemStack> items = new ArrayList<>();
                for (ItemStack item : self.getInventory().getStorageContents()) {
                    items.add(item);
                }
                items.add(self.getInventory().getItemInOffHand());
                if (ctx.hasPermission("applecore.repair.armor")) {
                    items.addAll(List.of(self.getInventory().getArmorContents()));
                }
                List<ItemStack> todo = items.stream().filter(ItemCommands::isRepairable).filter(i -> allowed(ctx, i)).toList();
                if (todo.isEmpty()) {
                    throw new CommandException("commands.repair.nothing");
                }
                charge(ctx, self, "repair.cost.all");
                todo.forEach(ItemCommands::repair);
                ctx.replyCmd("all", Ph.of("amount", todo.size()));
            } else {
                ItemStack item = hand(self);
                if (!isRepairable(item)) {
                    throw new CommandException("commands.repair.not-damaged");
                }
                if (!allowed(ctx, item)) {
                    throw new CommandException("commands.repair.enchanted");
                }
                charge(ctx, self, "repair.cost.hand");
                repair(item);
                ctx.replyCmd("hand", Ph.of("item", itemName(item)));
            }
            plugin.pack().play(self, "success");
        }

        private void charge(CommandContext ctx, Player player, String path) throws CommandException {
            double cost = plugin.getConfig().getDouble(path, 0);
            if (cost <= 0 || ctx.hasPermission("applecore.repair.free")) {
                return;
            }
            if (!plugin.hooks().economy().available()) {
                throw new CommandException("errors.economy-unavailable");
            }
            if (!plugin.hooks().economy().withdraw(player, cost)) {
                throw new CommandException("errors.not-enough-money", Ph.of("amount", plugin.hooks().economy().format(cost)));
            }
            ctx.reply("commands.repair.paid", Ph.of("amount", plugin.hooks().economy().format(cost)));
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 && ctx.hasPermission("applecore.repair.all") ? List.of("hand", "all") : List.of();
        }
    }

    static void repair(ItemStack item) {
        item.editMeta(Damageable.class, meta -> meta.setDamage(0));
    }

    static final class ItemDb extends AbstractCommand {

        ItemDb(AppleCorePlugin plugin) {
            super(plugin, "itemdb", "dura", "durability", "itemno", "db");
            playerOnly(true);
        }

        @Override
        @SuppressWarnings("deprecation")
        public void execute(CommandContext ctx) throws CommandException {
            ItemStack item = hand(ctx.player());
            ItemSpec spec = plugin.items().describe(item);
            ItemMeta meta = item.getItemMeta();
            ctx.replyCmd("item", Ph.of("item", itemName(item)), Ph.of("material", item.getType().getKey().toString()),
                    Ph.of("amount", item.getAmount()));
            if (meta instanceof Damageable damageable && item.getType().getMaxDurability() > 0) {
                int max = damageable.hasMaxDamage() ? damageable.getMaxDamage() : item.getType().getMaxDurability();
                ctx.replyCmd("durability", Ph.of("left", max - damageable.getDamage()), Ph.of("max", max));
            }
            if (spec.item().contains(":")) {
                ctx.replyCmd("custom", Ph.of("id", spec.item()));
            }
            if (meta != null && meta.hasItemModel() && meta.getItemModel() != null) {
                ctx.replyCmd("model", Ph.of("model", meta.getItemModel().toString()));
            }
            if (meta != null && meta.hasCustomModelData()) {
                ctx.replyCmd("custom-model-data", Ph.of("value", meta.getCustomModelData()));
            }
        }
    }

    /** /itemname [name…]; colours need applecore.itemname.color (essentials.itemname.color). No name resets. */
    static final class ItemName extends AbstractCommand {

        ItemName(AppleCorePlugin plugin) {
            super(plugin, "itemname", "iname", "itemrename", "irename", "rename");
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            ItemStack item = hand(ctx.player());
            if (!ctx.has(0)) {
                item.editMeta(meta -> meta.displayName(null));
                ctx.replyCmd("reset");
                return;
            }
            String raw = ctx.joinFrom(0);
            int max = plugin.getConfig().getInt("itemname.max-length", 50);
            Component name = ctx.hasPermission("applecore.itemname.color")
                    ? Text.userFormatted(raw)
                    : Component.text(raw);
            if (Text.plain(name).length() > max) {
                throw new CommandException("commands.itemname.too-long", Ph.of("max", max));
            }
            item.editMeta(meta -> meta.displayName(Text.noItalic(name)));
            ctx.replyCmd("done", Ph.of("name", name));
        }
    }

    static final class More extends AbstractCommand {

        More(AppleCorePlugin plugin) {
            super(plugin, "more");
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            ItemStack item = hand(ctx.player());
            item.setAmount(item.getMaxStackSize());
            ctx.replyCmd("done", Ph.of("amount", item.getAmount()));
        }
    }

    /** /skull [player]: a player head; other players' heads need applecore.skull.others. */
    static final class Skull extends AbstractCommand {

        Skull(AppleCorePlugin plugin) {
            super(plugin, "skull", "playerskull", "head");
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player self = ctx.player();
            String owner = ctx.has(0) ? ctx.arg(0) : self.getName();
            if (!owner.equalsIgnoreCase(self.getName())) {
                ctx.require("applecore.skull.others");
            }
            if (!owner.matches("[A-Za-z0-9_]{1,16}")) {
                throw new CommandException("errors.player-never-joined", Ph.of("player", owner));
            }
            var profile = Bukkit.createProfile(owner);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                profile.complete();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    head.editMeta(SkullMeta.class, meta -> meta.setPlayerProfile(profile));
                    self.getInventory().addItem(head).values()
                            .forEach(rest -> self.getWorld().dropItemNaturally(self.getLocation(), rest));
                    ctx.replyCmd("done", Ph.of("player", owner));
                });
            });
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 && ctx.hasPermission("applecore.skull.others") ? players(ctx) : List.of();
        }
    }

    /** /enchant <enchantment> [level]; level 0 removes; beyond vanilla rules needs applecore.enchant.unsafe. */
    static final class Enchant extends AbstractCommand {

        Enchant(AppleCorePlugin plugin) {
            super(plugin, "enchant", "enchantment");
            playerOnly(true);
        }

        private static Registry<Enchantment> registry() {
            return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            ItemStack item = hand(ctx.player());
            if (!ctx.has(0)) {
                throw ctx.usage();
            }
            NamespacedKey key = NamespacedKey.fromString(ctx.arg(0).toLowerCase(Locale.ROOT));
            Enchantment enchantment = key == null ? null : registry().get(key);
            if (enchantment == null) {
                throw new CommandException("commands.enchant.unknown", Ph.of("input", ctx.arg(0)));
            }
            int level = ctx.has(1) ? ctx.integer(1, 0, 255) : enchantment.getMaxLevel();
            if (level == 0) {
                item.removeEnchantment(enchantment);
                ctx.replyCmd("removed", Ph.of("enchantment", enchantment.displayName(1)));
                return;
            }
            boolean safe = level <= enchantment.getMaxLevel() && enchantment.canEnchantItem(item);
            if (!safe && !ctx.hasPermission("applecore.enchant.unsafe")) {
                throw new CommandException("commands.enchant.unsafe");
            }
            item.addUnsafeEnchantment(enchantment, level);
            ctx.replyCmd("done", Ph.of("enchantment", enchantment.displayName(level)));
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            if (ctx.size() == 1) {
                return registry().stream().map(e -> e.getKey().getKey()).sorted().toList();
            }
            return ctx.size() == 2 ? List.of("1", "2", "3", "4", "5") : List.of();
        }
    }

    /** /fireball [type] [speed]; each type needs applecore.fireball.<type>, like EssentialsX. */
    static final class FireballCommand extends AbstractCommand {

        static final Map<String, Class<? extends Projectile>> TYPES = new LinkedHashMap<>();

        static {
            TYPES.put("fireball", Fireball.class);
            TYPES.put("small", SmallFireball.class);
            TYPES.put("large", LargeFireball.class);
            TYPES.put("arrow", Arrow.class);
            TYPES.put("skull", WitherSkull.class);
            TYPES.put("egg", Egg.class);
            TYPES.put("snowball", Snowball.class);
            TYPES.put("expbottle", ThrownExpBottle.class);
            TYPES.put("dragon", DragonFireball.class);
            TYPES.put("trident", Trident.class);
        }

        FireballCommand(AppleCorePlugin plugin) {
            super(plugin, "fireball", "fireentity", "fireskull");
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player self = ctx.player();
            String type = ctx.has(0) ? ctx.arg(0).toLowerCase(Locale.ROOT) : "fireball";
            Class<? extends Projectile> projectileType = TYPES.get(type);
            if (projectileType == null) {
                throw new CommandException("commands.fireball.unknown", Ph.of("input", type));
            }
            ctx.require("applecore.fireball." + type);
            double speed = ctx.has(1) ? ctx.decimal(1, 0.1, 5) : 2;
            Vector direction = self.getEyeLocation().getDirection().multiply(speed);
            Location spawn = self.getEyeLocation().add(direction.clone().normalize());
            Projectile projectile = self.getWorld().spawn(spawn, projectileType, p -> {
                p.setShooter(self);
                p.setVelocity(direction);
                p.getPersistentDataContainer().set(PlayerModule.SPAWNED_KEY, PersistentDataType.BYTE, (byte) 1);
            });
            if (projectile instanceof Arrow arrow) {
                arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
            }
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            if (ctx.size() == 1) {
                return TYPES.keySet().stream().filter(t -> ctx.hasPermission("applecore.fireball." + t)).toList();
            }
            return ctx.size() == 2 ? List.of("1", "2", "3", "4", "5") : List.of();
        }
    }

    /** /beezooka: a bee flies forward and bursts (no block or player damage). */
    static final class Beezooka extends AbstractCommand {

        Beezooka(AppleCorePlugin plugin) {
            super(plugin, "beezooka", "beecannon");
            playerOnly(true);
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            Player self = ctx.player();
            Vector direction = self.getEyeLocation().getDirection().multiply(1.6);
            Location spawn = self.getEyeLocation().add(direction.clone().normalize());
            Bee bee = self.getWorld().spawn(spawn, Bee.class, b -> {
                b.setAI(false);
                b.setGravity(false);
                b.setInvulnerable(true);
                b.setPersistent(false);
                b.getPersistentDataContainer().set(PlayerModule.SPAWNED_KEY, PersistentDataType.BYTE, (byte) 1);
            });
            final int[] ticks = {0};
            Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                if (!bee.isValid() || ++ticks[0] > 20) {
                    Location at = bee.getLocation();
                    bee.remove();
                    at.getWorld().createExplosion(at, 0f, false, false);
                    task.cancel();
                    return;
                }
                bee.teleport(bee.getLocation().add(direction));
            }, 1L, 1L);
        }
    }

    /** /clearinventory [player]; item filters (vanilla syntax) are passed to minecraft:clear. */
    static final class ClearInventory extends AbstractCommand {

        ClearInventory(AppleCorePlugin plugin) {
            super(plugin, "clearinventory", "ci", "clean", "clear", "clearinvent");
        }

        @Override
        public void execute(CommandContext ctx) throws CommandException {
            if (ctx.size() > 1 || ctx.has(0) && ctx.arg(0).startsWith("@")) {
                Bukkit.dispatchCommand(ctx.sender(), "minecraft:clear " + String.join(" ", ctx.args()));
                return;
            }
            Player who = ctx.targetOrSelf(0);
            PlayerInventory inventory = who.getInventory();
            int removed = 0;
            ItemStack[] contents = inventory.getStorageContents();
            for (int i = 0; i < contents.length; i++) {
                if (contents[i] != null && !contents[i].getType().isAir()) {
                    removed += contents[i].getAmount();
                    contents[i] = null;
                }
            }
            inventory.setStorageContents(contents);
            if (!inventory.getItemInOffHand().getType().isAir()) {
                removed += inventory.getItemInOffHand().getAmount();
                inventory.setItemInOffHand(null);
            }
            plugin.messages().send(who, "commands.clearinventory.done", Ph.of("amount", removed));
            if (!ctx.isSelf(who)) {
                ctx.replyCmd("done-other", Ph.of("player", who.getName()), Ph.of("amount", removed));
            }
        }

        @Override
        public List<String> suggest(CommandContext ctx) {
            return ctx.size() == 1 ? othersIfAllowed(ctx) : List.of();
        }
    }

    static List<AbstractCommand> all(AppleCorePlugin plugin) {
        return List.of(new Hat(plugin), new Repair(plugin), new ItemDb(plugin), new ItemName(plugin), new More(plugin),
                new Skull(plugin), new Enchant(plugin), new FireballCommand(plugin), new Beezooka(plugin),
                new ClearInventory(plugin));
    }

    static boolean spawnedByCommand(Entity entity) {
        return entity.getPersistentDataContainer().has(PlayerModule.SPAWNED_KEY, PersistentDataType.BYTE);
    }
}
