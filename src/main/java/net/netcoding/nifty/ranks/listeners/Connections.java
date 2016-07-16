package net.netcoding.nifty.ranks.listeners;

import net.netcoding.nifty.common.api.plugin.Event;
import net.netcoding.nifty.common.api.plugin.MinecraftListener;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.minecraft.event.player.PlayerJoinEvent;
import net.netcoding.nifty.common.minecraft.event.player.PlayerLoginEvent;
import net.netcoding.nifty.common.minecraft.event.player.PlayerQuitEvent;
import net.netcoding.nifty.ranks.cache.UserRankData;

public class Connections extends MinecraftListener {

	public Connections(MinecraftPlugin plugin) {
		super(plugin);
	}

	@Event
	public void onProfileQuit(PlayerQuitEvent event) {
		UserRankData.removeCache(event.getProfile());
	}

	@Event(priority = Event.Priority.LOWEST)
	public void onPlayerLogin(PlayerLoginEvent event) {
		if (PlayerLoginEvent.Result.KICK_FULL == event.getResult() && this.hasPermissions(event.getPlayer(), "joinfullserver"))
			event.setResult(PlayerLoginEvent.Result.ALLOWED);
	}

	@Event(priority = Event.Priority.LOWEST)
	public void onProfileJoin(PlayerJoinEvent event) {
		UserRankData userData = new UserRankData(this.getPlugin(), event.getProfile());

		try {
			userData.updateRanks();
			userData.saveVaultRanks();
		} catch (Exception ex) {
			this.getLog().console(ex);
		}
	}

}