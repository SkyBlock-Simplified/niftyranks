package net.netcoding.nifty.ranks.listeners;

import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.MinecraftHelper;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.core.database.notifications.DatabaseListener;
import net.netcoding.nifty.core.database.notifications.DatabaseNotification;
import net.netcoding.nifty.core.database.notifications.TriggerEvent;
import net.netcoding.nifty.ranks.cache.Config;
import net.netcoding.nifty.ranks.cache.UserRankData;
import net.netcoding.nifty.ranks.events.PlayerRankChangeEvent;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

public class Notifications extends MinecraftHelper implements DatabaseListener {

	public Notifications(MinecraftPlugin plugin) {
		super(plugin);
	}

	@Override
	public void onDatabaseNotification(final DatabaseNotification databaseNotification) throws SQLException {
		TriggerEvent event = databaseNotification.getEvent();
		String table = databaseNotification.getTable();

		if (table.equals(Config.RANK_TABLE)) {
			if (event == TriggerEvent.DELETE) {
				Map<String, Object> deletedData = databaseNotification.getDeletedData();
				String rank = String.valueOf(deletedData.get("rank"));

				for (UserRankData data : UserRankData.getCache()) {
					if (data.getRanks().contains(rank))
						data.updateRanks();
				}
			}
		} else if (table.equals(Config.USER_TABLE)) {
			if (event == TriggerEvent.DELETE) {
				Map<String, Object> deletedData = databaseNotification.getDeletedData();
				this.search(UUID.fromString((String)deletedData.get("uuid")));
			} else {
				databaseNotification.getUpdatedRow(result -> {
					if (result.next())
						search(UUID.fromString(result.getString("uuid")));
				});
			}

		}
	}

	private void search(UUID uniqueId) throws SQLException {
		for (UserRankData rankData : UserRankData.getCache()) {
			if (rankData.getProfile().getUniqueId().equals(uniqueId)) {
				rankData.updateRanks();
				rankData.saveVaultRanks();
				Nifty.getPluginManager().call(new PlayerRankChangeEvent(rankData));
				break;
			}
		}
	}

}