package fun.applecube.core.module.economy;

import fun.applecube.core.AppleCorePlugin;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Vault provider backed by {@link BalanceStore}. Only loaded when Vault is installed. Registered with the
 * priority from {@code economy.provider}: "auto" uses Lowest, so any dedicated economy plugin wins.
 */
final class AppleEconomy implements Economy {

    private static final String NO_BANKS = "AppleCore has no bank support";

    private final AppleCorePlugin plugin;
    private final BalanceStore store;

    AppleEconomy(AppleCorePlugin plugin, BalanceStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    private OfflinePlayer byName(String name) {
        UUID id = store.byName(name);
        if (id != null) {
            return Bukkit.getOfflinePlayer(id);
        }
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        if (cached != null) {
            return cached;
        }
        return Bukkit.getOfflinePlayer(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)));
    }

    private EconomyResponse result(boolean ok, double amount, OfflinePlayer player, String error) {
        return new EconomyResponse(amount, store.balance(player),
                ok ? EconomyResponse.ResponseType.SUCCESS : EconomyResponse.ResponseType.FAILURE, ok ? null : error);
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    @Override
    public String getName() {
        return "AppleCore";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double amount) {
        return plugin.hooks().economy().format(amount);
    }

    @Override
    public String currencyNamePlural() {
        return plugin.getConfig().getString("economy.currency-plural", "монет");
    }

    @Override
    public String currencyNameSingular() {
        return plugin.getConfig().getString("economy.currency-singular", "монета");
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return store.hasAccount(player.getUniqueId());
    }

    @Override
    public boolean hasAccount(String playerName) {
        return hasAccount(byName(playerName));
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return store.balance(player);
    }

    @Override
    public double getBalance(String playerName) {
        return getBalance(byName(playerName));
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return store.balance(player) + 1e-9 >= amount;
    }

    @Override
    public boolean has(String playerName, double amount) {
        return has(byName(playerName), amount);
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return result(false, amount, player, "Cannot withdraw a negative amount");
        }
        return result(store.withdraw(player, amount), amount, player, "Insufficient funds");
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return withdrawPlayer(byName(playerName), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return result(false, amount, player, "Cannot deposit a negative amount");
        }
        return result(store.deposit(player, amount), amount, player, "Deposit failed");
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return depositPlayer(byName(playerName), amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    private static EconomyResponse noBanks() {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, NO_BANKS);
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return noBanks();
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return noBanks();
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return noBanks();
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return noBanks();
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return noBanks();
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return noBanks();
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return noBanks();
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return noBanks();
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return noBanks();
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return noBanks();
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return noBanks();
    }

    @Override
    public List<String> getBanks() {
        return List.of();
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        store.create(player);
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return createPlayerAccount(byName(playerName));
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }
}
