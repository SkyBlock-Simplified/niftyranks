package net.netcoding.niftyranks.listeners;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import net.netcoding.niftybukkit.minecraft.BukkitHelper;
import net.netcoding.niftycore.database.factory.callbacks.VoidResultCallback;
import net.netcoding.niftycore.database.notifications.DatabaseListener;
import net.netcoding.niftycore.database.notifications.DatabaseNotification;
import net.netcoding.niftycore.database.notifications.TriggerEvent;
import net.netcoding.niftyranks.cache.Config;
import net.netcoding.niftyranks.cache.UserRankData;
import net.netcoding.niftyranks.events.RankChangeEvent;

import org.bukkit.plugin.java.JavaPlugin;

public class Notifications extends BukkitHelper implements DatabaseListener {

	public Notifications(JavaPlugin plugin) {
		super(plugin);
	}

	@Override
	public void onDatabaseNotification(final DatabaseNotification databaseNotification) throws SQLException {
		TriggerEvent event = databaseNotification.getEvent();
		String table = databaseNotification.getTable();

		if (table.equals(Config.RANK_TABLE)) {
			if (event.equals(TriggerEvent.DELETE)) {
				Map<String, Object> deletedData = databaseNotification.getDeletedData();
				String rank = String.valueOf(deletedData.get("rank"));

				for (UserRankData data : UserRankData.getCache()) {
					if (data.getRanks().contains(rank))
						data.updateRanks();
				}
			}
		} else if (table.equals(Config.USER_TABLE)) {
			if (event.equals(TriggerEvent.DELETE)) {
				Map<String, Object> deletedData = databaseNotification.getDeletedData();
				this.search(UUID.fromString((String)deletedData.get("uuid")));
			} else {
				databaseNotification.getUpdatedRow(new VoidResultCallback() {
					@Override
					public void handle(ResultSet result) throws SQLException {
						if (result.next())
							search(UUID.fromString(result.getString("uuid")));
					}
				});
			}

		}
	}

	private void search(UUID uniqueId) throws SQLException {
		for (UserRankData rankData : UserRankData.getCache()) {
			if (rankData.getProfile().getUniqueId().equals(uniqueId)) {
				rankData.updateRanks();
				rankData.saveVaultRanks();
				this.getPlugin().getServer().getPluginManager().callEvent(new RankChangeEvent(rankData));
				break;
			}
		}
	}

}