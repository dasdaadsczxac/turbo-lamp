package fun.applecube.core.hook.item;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.Function;

/**
 * Providers implemented through reflection, so AppleCore compiles and runs without any of these
 * plugins. Each factory returns null when the plugin is missing or its API changed.
 */
final class ReflectiveItemProviders {

    private ReflectiveItemProviders() {
    }

    static @Nullable ItemProvider itemsAdder() {
        if (!enabled("ItemsAdder")) {
            return null;
        }
        try {
            Class<?> customStack = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Method getInstance = customStack.getMethod("getInstance", String.class);
            Method getItemStack = customStack.getMethod("getItemStack");
            Method byItemStack = customStack.getMethod("byItemStack", ItemStack.class);
            Method getId = customStack.getMethod("getNamespacedID");
            return new Simple("ItemsAdder", List.of("itemsadder", "ia"),
                    id -> {
                        Object stack = invoke(getInstance, null, id);
                        return stack == null ? null : (ItemStack) invoke(getItemStack, stack);
                    },
                    item -> {
                        Object stack = invoke(byItemStack, null, item);
                        return stack == null ? null : (String) invoke(getId, stack);
                    });
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    static @Nullable ItemProvider nexo() {
        if (!enabled("Nexo")) {
            return null;
        }
        try {
            Class<?> items = Class.forName("com.nexomc.nexo.api.NexoItems");
            Object target = staticTarget(items);
            Method fromId = items.getMethod("itemFromId", String.class);
            Method idFromItem = items.getMethod("idFromItem", ItemStack.class);
            Object owner = Modifier.isStatic(fromId.getModifiers()) ? null : target;
            return new Simple("Nexo", List.of("nexo"),
                    id -> build(invoke(fromId, owner, id)),
                    item -> (String) invoke(idFromItem, owner, item));
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    static @Nullable ItemProvider oraxen() {
        if (!enabled("Oraxen")) {
            return null;
        }
        try {
            Class<?> items = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
            Method byId = items.getMethod("getItemById", String.class);
            Method idOf = items.getMethod("getIdByItem", ItemStack.class);
            return new Simple("Oraxen", List.of("oraxen"),
                    id -> build(invoke(byId, null, id)),
                    item -> (String) invoke(idOf, null, item));
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    static @Nullable ItemProvider craftEngine() {
        if (!enabled("CraftEngine")) {
            return null;
        }
        try {
            Class<?> items = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineItems");
            Class<?> keyClass = Class.forName("net.momirealms.craftengine.core.util.Key");
            Method keyOf = firstStatic(keyClass, "of", "from", "fromString");
            Method byId = items.getMethod("byId", keyClass);
            Method idOf = items.getMethod("getCustomItemId", ItemStack.class);
            return new Simple("CraftEngine", List.of("craftengine", "ce"),
                    id -> {
                        Object custom = invoke(byId, null, invoke(keyOf, null, id));
                        if (custom == null) {
                            return null;
                        }
                        Method build = custom.getClass().getMethod("buildItemStack");
                        return (ItemStack) build.invoke(custom);
                    },
                    item -> {
                        Object key = invoke(idOf, null, item);
                        return key == null ? null : key.toString();
                    });
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    static @Nullable ItemProvider headDatabase() {
        if (!enabled("HeadDatabase")) {
            return null;
        }
        try {
            Class<?> api = Class.forName("me.arcaniax.hdb.api.HeadDatabaseAPI");
            Object instance = api.getConstructor().newInstance();
            Method head = api.getMethod("getItemHead", String.class);
            Method idOf = api.getMethod("getItemID", ItemStack.class);
            return new Simple("HeadDatabase", List.of("hdb"),
                    id -> (ItemStack) invoke(head, instance, id),
                    item -> (String) invoke(idOf, instance, item));
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    // ---- helpers ----------------------------------------------------------------------------------

    private static boolean enabled(String plugin) {
        return Bukkit.getPluginManager().getPlugin(plugin) != null;
    }

    private static @Nullable Object staticTarget(Class<?> type) {
        try {
            return type.getField("INSTANCE").get(null);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Method firstStatic(Class<?> type, String... names) throws NoSuchMethodException {
        for (String name : names) {
            try {
                Method method = type.getMethod(name, String.class);
                if (Modifier.isStatic(method.getModifiers())) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
                // try the next name
            }
        }
        throw new NoSuchMethodException(type.getName() + " has none of " + String.join(", ", names));
    }

    private static @Nullable ItemStack build(@Nullable Object builder) throws ReflectiveOperationException {
        if (builder == null) {
            return null;
        }
        return (ItemStack) builder.getClass().getMethod("build").invoke(builder);
    }

    private static @Nullable Object invoke(Method method, @Nullable Object target, Object... args)
            throws ReflectiveOperationException {
        return method.invoke(target, args);
    }

    @FunctionalInterface
    interface ReflectiveFunction<T, R> {
        R apply(T value) throws ReflectiveOperationException;
    }

    private record Simple(String name, List<String> prefixes,
                          ReflectiveFunction<String, ItemStack> creator,
                          ReflectiveFunction<ItemStack, String> identifier) implements ItemProvider {

        @Override
        public @Nullable ItemStack create(String id) {
            return safely(creator::apply, id, stack -> stack == null ? null : stack.clone());
        }

        @Override
        public @Nullable String idOf(ItemStack stack) {
            return safely(identifier::apply, stack, Function.identity());
        }

        private static <T, R> @Nullable R safely(ReflectiveFunction<T, R> call, T value, Function<R, R> post) {
            try {
                return post.apply(call.apply(value));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return null;
            }
        }
    }
}
