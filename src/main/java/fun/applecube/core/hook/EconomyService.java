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
 * Money for every AppleCore feature. Uses the active Vault economy (any plugin, or AppleCore's own provider
 * registered by the economy module); without Vault it falls back to AppleCore's accounts directly.
 * No Vault type appears here, so this class loads safely without Vault.
 */
public final class EconomyService {

    private final AppleCorePlugin plugin;
    private @Nullable VaultEconomyBridge bridge;
    private @Nullable EconomyBackend internal;

    public EconomyService(AppleCorePlugin plugin) {
        this.plugin = plugin;
    }

    void setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().info("Vault not found: AppleCore money works only for its own commands and killer money");
            return;
        }
        bridge = new VaultEconomyBridge();
        bridge.refresh();
        Bukkit.getPluginManager().registerEvents(bridge, plugin);
    }

    /** Called by the economy module when AppleCore's own accounts are enabled. */
    public void internal(@Nullable EconomyBackend backend) {
        this.internal = backend;
    }

    public boolean vaultPresent() {
        return bridge != null;
    }

    private boolean viaVault() {
        return bridge != null && bridge.ready();
    }

    public boolean available() {
        return viaVault() || internal != null;
    }

    /** True when AppleCore's own accounts hold the money (directly or as the active Vault provider). */
    public boolean usesInternal() {
        return viaVault() ? "AppleCore".equals(bridge.name()) : internal != null;
    }

    public String providerName() {
        return viaVault() ? bridge.name() : internal != null ? "AppleCore" : "-";
    }

    public double balance(OfflinePlayer player) {
        if (viaVault()) {
            return bridge.balance(player);
        }
        return internal != null ? internal.balance(player) : 0;
    }

    public boolean has(OfflinePlayer player, double amount) {
        return balance(player) >= amount;
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (amount <= 0) {
            return true;
        }
        if (viaVault()) {
            return bridge.withdraw(player, amount);
        }
        return internal != null && internal.withdraw(player, amount);
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (amount <= 0) {
            return true;
        }
        if (viaVault()) {
            return bridge.deposit(player, amount);
        }
        return internal != null && internal.deposit(player, amount);
    }

    /** Sets the balance (through deposit/withdraw of the difference when another plugin owns the money). */
    public boolean set(OfflinePlayer player, double amount) {
        if (!viaVault() && internal != null) {
            internal.set(player, amount);
            return true;
        }
        if (!available()) {
            return false;
        }
        double current = balance(player);
        return amount >= current ? deposit(player, amount - current) : withdraw(player, current - amount);
    }

    /** Formats money with {@code economy.format}, e.g. "12 345.5". */
    public String format(double amount) {
        String pattern = plugin.getConfig().getString("economy.format", "#,##0.##");
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.ROOT);
        String separator = plugin.getConfig().getString("economy.grouping-separator", " ");
        if (!separator.isEmpty()) {
            symbols.setGroupingSeparator(separator.charAt(0));
        }
        DecimalFormat format = new DecimalFormat(pattern, symbols);
        return format.format(BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP))
                + plugin.getConfig().getString("economy.currency-suffix", " ⛃");
    }
}
