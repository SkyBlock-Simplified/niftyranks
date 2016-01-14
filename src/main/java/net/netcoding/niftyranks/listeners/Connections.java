package net.netcoding.niftyranks.listeners;

import net.netcoding.niftybukkit.minecraft.BukkitListener;
import net.netcoding.niftybukkit.minecraft.events.PlayerDisconnectEvent;
import net.netcoding.niftybukkit.minecraft.events.PlayerPostLoginEvent;
import net.netcoding.niftyranks.cache.UserRankData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.plugin.java.JavaPlugin;

public class Connections extends BukkitListener {

	public Connections(JavaPlugin plugin) {
		super(plugin);
	}

	@EventHandler
	public void onPlayerDisconnect(PlayerDisconnectEvent event) {
		UserRankData.removeCache(event.getProfile());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerLogin(PlayerLoginEvent event) {
		if (Result.KICK_FULL.equals(event.getResult()) && this.hasPermissions(event.getPlayer(), "joinfullserver"))
			event.setResult(Result.ALLOWED);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerPostLogin(PlayerPostLoginEvent event) {
		UserRankData userData = new UserRankData(this.getPlugin(), event.getProfile());

		try {
			userData.updateRanks();
			userData.saveVaultRanks();
		} catch (Exception ex) {
			this.getLog().console(ex);
		}
	}

}