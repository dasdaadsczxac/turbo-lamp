package fun.applecube.core.hook;

import fun.applecube.core.AppleCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Economy facade over Vault. No Vault type appears in this class, so it loads safely without Vault;
 * the actual calls live in {@link VaultEconomyBridge}.
 */
public final class EconomyService {

    private final AppleCorePlugin plugin;
    private @Nullable VaultEconomyBridge bridge;

    public EconomyService(AppleCorePlugin plugin) {
        this.plugin = plugin;
    }

    void setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().info("Vault not found: money features (killer money, /pay, /balance) are disabled");
            return;
        }
        bridge = new VaultEconomyBridge();
        bridge.refresh();
        Bukkit.getPluginManager().registerEvents(bridge, plugin);
    }

    public boolean available() {
        return bridge != null && bridge.ready();
    }

    public String providerName() {
        return bridge == null ? "-" : bridge.name();
    }

    public double balance(OfflinePlayer player) {
        return available() ? bridge.balance(player) : 0;
    }

    public boolean has(OfflinePlayer player, double amount) {
        return available() && bridge.has(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        return amount <= 0 || available() && bridge.withdraw(player, amount);
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        return amount <= 0 || available() && bridge.deposit(player, amount);
    }

    /** Sets the balance by depositing/withdrawing the difference (Vault has no "set"). */
    public boolean set(OfflinePlayer player, double amount) {
        if (!available()) {
            return false;
        }
        double current = balance(player);
        return amount >= current ? deposit(player, amount - current) : withdraw(player, current - amount);
    }

    /** Formats money with the pattern from config ({@code economy.format}), e.g. "12 345.50". */
    public String format(double amount) {
        String pattern = plugin.getConfig().getString("economy.format", "#,##0.##");
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.ROOT);
        String separator = plugin.getConfig().getString("economy.grouping-separator", " ");
        if (!separator.isEmpty()) {
            symbols.setGroupingSeparator(separator.charAt(0));
        }
        DecimalFormat format = new DecimalFormat(pattern, symbols);
        return format.format(BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP));
    }
}
