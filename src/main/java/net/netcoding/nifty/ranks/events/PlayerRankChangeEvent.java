package net.netcoding.nifty.ranks.events;

import net.netcoding.nifty.common.minecraft.event.player.PlayerEvent;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.ranks.cache.UserRankData;

import java.util.List;

public final class PlayerRankChangeEvent implements PlayerEvent {

	private final transient UserRankData data;

	public PlayerRankChangeEvent(UserRankData data) {
		this.data = data;
	}

	public String getPrimaryRank() {
		return this.getRanks().get(0);
	}

	@Override
	public MinecraftMojangProfile getProfile() {
		return this.data.getProfile();
	}

	public List<String> getRanks() {
		return this.data.getRanks();
	}

}