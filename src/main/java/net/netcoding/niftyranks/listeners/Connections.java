package net.netcoding.niftyranks.listeners;

import net.netcoding.niftybukkit.minecraft.BukkitListener;
import net.netcoding.niftybukkit.minecraft.events.profile.ProfileQuitEvent;
import net.netcoding.niftybukkit.minecraft.events.profile.ProfileJoinEvent;
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
	public void onProfileQuit(ProfileQuitEvent event) {
		UserRankData.removeCache(event.getProfile());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerLogin(PlayerLoginEvent event) {
		if (Result.KICK_FULL == event.getResult() && this.hasPermissions(event.getPlayer(), "joinfullserver"))
			event.setResult(Result.ALLOWED);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onProfileJoin(ProfileJoinEvent event) {
		UserRankData userData = new UserRankData(this.getPlugin(), event.getProfile());

		try {
			userData.updateRanks();
			userData.saveVaultRanks();
		} catch (Exception ex) {
			this.getLog().console(ex);
		}
	}

}