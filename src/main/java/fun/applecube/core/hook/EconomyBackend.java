package fun.applecube.core.hook;

import org.bukkit.OfflinePlayer;

/** AppleCore's own account storage, used when no other economy plugin provides Vault. */
public interface EconomyBackend {

    double balance(OfflinePlayer player);

    boolean withdraw(OfflinePlayer player, double amount);

    boolean deposit(OfflinePlayer player, double amount);

    void set(OfflinePlayer player, double amount);
}
