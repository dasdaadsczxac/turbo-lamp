package fun.applecube.core.hook;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Only loaded when Vault is present. Tracks economy providers that register after AppleCore. */
final class VaultEconomyBridge implements Listener {

    private Economy economy;

    void refresh() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        economy = rsp == null ? null : rsp.getProvider();
    }

    boolean ready() {
        return economy != null && economy.isEnabled();
    }

    String name() {
        return economy == null ? "-" : economy.getName();
    }

    double balance(OfflinePlayer player) {
        return economy.getBalance(player);
    }

    boolean has(OfflinePlayer player, double amount) {
        return economy.has(player, amount);
    }

    boolean withdraw(OfflinePlayer player, double amount) {
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    boolean deposit(OfflinePlayer player, double amount) {
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    @EventHandler
    public void onRegister(ServiceRegisterEvent event) {
        if (event.getProvider().getService() == Economy.class) {
            refresh();
        }
    }

    @EventHandler
    public void onUnregister(ServiceUnregisterEvent event) {
        if (event.getProvider().getService() == Economy.class) {
            refresh();
        }
    }
}
