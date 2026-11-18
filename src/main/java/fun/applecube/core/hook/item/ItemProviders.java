package fun.applecube.core.hook.item;

import fun.applecube.core.AppleCorePlugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Registry of the custom-item plugins found on the server. */
public final class ItemProviders {

    private final AppleCorePlugin plugin;
    private final List<ItemProvider> providers = new ArrayList<>();

    public ItemProviders(AppleCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void detect() {
        providers.clear();
        add(ReflectiveItemProviders.itemsAdder());
        add(ReflectiveItemProviders.nexo());
        add(ReflectiveItemProviders.oraxen());
        add(ReflectiveItemProviders.craftEngine());
        add(ReflectiveItemProviders.headDatabase());
        if (!providers.isEmpty()) {
            plugin.getLogger().info("Custom items: " + String.join(", ", names()));
        }
    }

    private void add(@Nullable ItemProvider provider) {
        if (provider != null) {
            providers.add(provider);
        }
    }

    public @Nullable ItemProvider byPrefix(String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        for (ItemProvider provider : providers) {
            if (provider.prefixes().contains(lower)) {
                return provider;
            }
        }
        return null;
    }

    public List<ItemProvider> all() {
        return Collections.unmodifiableList(providers);
    }

    public List<String> names() {
        return providers.stream().map(ItemProvider::name).toList();
    }
}
