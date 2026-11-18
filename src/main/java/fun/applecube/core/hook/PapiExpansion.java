package fun.applecube.core.hook;

import fun.applecube.core.AppleCorePlugin;
import fun.applecube.core.module.killermoney.KillerMoneyModule;
import fun.applecube.core.module.ranks.Rank;
import fun.applecube.core.module.ranks.RanksModule;
import fun.applecube.core.module.warps.WarpsModule;
import fun.applecube.core.permission.PermissionService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * %applecore_…% placeholders for TAB, scoreboards and menus: rank, rank_name, rank_badge, rank_display, rank_price,
 * killermoney_multiplier, killermoney_x, homes_limit, warps, warps_limit, balance, balance_raw.
 * Only loaded when PlaceholderAPI is installed.
 */
public final class PapiExpansion extends PlaceholderExpansion {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private final AppleCorePlugin plugin;

    private PapiExpansion(AppleCorePlugin plugin) {
        this.plugin = plugin;
    }

    public static void register(AppleCorePlugin plugin) {
        new PapiExpansion(plugin).register();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "applecore";
    }

    @Override
    public @NotNull String getAuthor() {
        return "APPLECUBE";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer offline, @NotNull String params) {
        String key = params.toLowerCase(Locale.ROOT);
        if (offline == null) {
            return "";
        }
        if (key.equals("balance") || key.equals("balance_raw")) {
            EconomyService economy = plugin.hooks().economy();
            if (!economy.available()) {
                return "";
            }
            double balance = economy.balance(offline);
            return key.equals("balance") ? economy.format(balance) : String.valueOf(balance);
        }
        Player player = offline.getPlayer();
        if (player == null) {
            return "";
        }
        return switch (key) {
            case "rank", "rank_name", "rank_badge", "rank_display", "rank_price" -> rank(player, key);
            case "killermoney_multiplier", "killermoney_x" -> {
                KillerMoneyModule km = plugin.module(KillerMoneyModule.class);
                double value = km == null ? 1 : km.multiplier(player);
                String text = value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(Math.round(value * 100) / 100.0);
                yield key.endsWith("_x") ? "x" + text : text;
            }
            case "homes_limit" -> limit(plugin.permissions().homeLimit(player));
            case "warps", "warps_limit" -> {
                WarpsModule warps = plugin.module(WarpsModule.class);
                if (warps == null) {
                    yield "0";
                }
                yield key.equals("warps") ? String.valueOf(warps.store().countOwned(player.getUniqueId())) : limit(warps.limit(player));
            }
            default -> null;
        };
    }

    private String rank(Player player, String key) {
        RanksModule ranks = plugin.module(RanksModule.class);
        Rank rank = ranks == null ? null : ranks.top(player);
        if (rank == null) {
            return "";
        }
        return switch (key) {
            case "rank" -> rank.id();
            case "rank_name" -> LEGACY.serialize(plugin.messages().parse(player, rank.name()));
            case "rank_badge" -> rank.badge() == null ? "" : rank.badge();
            case "rank_display" -> LEGACY.serialize(plugin.messages().parse(player, rank.display()));
            default -> String.valueOf((long) rank.price());
        };
    }

    private String limit(int value) {
        return value == PermissionService.UNLIMITED ? "∞" : String.valueOf(value);
    }
}
