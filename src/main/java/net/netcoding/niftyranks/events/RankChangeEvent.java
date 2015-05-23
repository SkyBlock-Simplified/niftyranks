package net.netcoding.niftyranks.events;

import java.util.List;

import net.netcoding.niftybukkit.mojang.BukkitMojangProfile;
import net.netcoding.niftyranks.cache.UserRankData;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RankChangeEvent extends Event {

	private static final transient HandlerList handlers = new HandlerList();
	private final transient UserRankData data;

	public RankChangeEvent(UserRankData data) {
		this.data = data;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public String getPrimaryRank() {
		return this.getRanks().get(0);
	}

	public BukkitMojangProfile getProfile() {
		return this.data.getProfile();
	}

	public List<String> getRanks() {
		return this.data.getRanks();
	}

}