package net.netcoding.nifty.ranks;

import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;

import net.netcoding.nifty.core.database.factory.SQLWrapper;
import net.netcoding.nifty.core.util.StringUtil;
import net.netcoding.nifty.ranks.cache.Config;
import net.netcoding.nifty.ranks.commands.Rank;
import net.netcoding.nifty.ranks.listeners.Connections;
import net.netcoding.nifty.ranks.listeners.Notifications;

public class NiftyRanks extends MinecraftPlugin {

	private static transient Config PLUGIN_CONFIG;

	@Override
	public void onEnable() {
		try {
			this.getLog().console("Loading SQL Config");
			(PLUGIN_CONFIG = new Config(this)).init();

			if (PLUGIN_CONFIG.getSQL() == null) {
				this.getLog().console("Incomplete MySQL Configuration!");
				this.setEnabled(false);
				return;
			}
		} catch (Exception ex) {
			this.getLog().console("Invalid MySQL Configuration!", ex);
			this.setEnabled(false);
			return;
		}

		this.getLog().console("Updating MySQL Tables & Data");
		if (!this.setupTables()) {
			this.getLog().console("Unable to update MySQL Tables & Data!");
			this.setEnabled(false);
			return;
		}

		try {
			Notifications notifications = new Notifications(this);
			getSQL().addListener(Config.RANK_TABLE, notifications);
			getSQL().addListener(Config.USER_TABLE, notifications);
		} catch (Exception ex) {
			this.getLog().console(ex);
			this.setEnabled(false);
			return;
		}

		this.getLog().console("Registering Commands");
		new Rank(this);

		this.getLog().console("Registering Listeners");
		new Connections(this);
	}

	@Override
	public void onDisable() {
		if (getSQL() != null)
			getSQL().removeListeners();
	}

	public static SQLWrapper getSQL() {
		return PLUGIN_CONFIG.getSQL();
	}

	private boolean setupTables() {
		try {
			getSQL().createTable(Config.RANK_TABLE, "rank VARCHAR(50) NOT NULL PRIMARY KEY");
			getSQL().createTable(Config.USER_TABLE, StringUtil.format("uuid VARCHAR(37) NOT NULL, rank VARCHAR(50) NOT NULL, server VARCHAR(100) NOT NULL, _submitted TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, _expires TIMESTAMP NULL, PRIMARY KEY (uuid, rank), FOREIGN KEY (rank) REFERENCES {0}(rank) ON DELETE CASCADE", Config.RANK_TABLE));
			getSQL().updateAsync(StringUtil.format("INSERT IGNORE INTO {0} (rank) VALUES (?);", Config.RANK_TABLE), "default");
			getSQL().updateAsync(StringUtil.format("DELETE FROM {0} WHERE rank = ?;", Config.USER_TABLE), "default");

			if (!getSQL().checkColumnExists(Config.USER_TABLE, "server")) {
				getSQL().update(StringUtil.format("ALTER TABLE {0} ADD server VARCHAR(100) NOT NULL AFTER rank;", Config.USER_TABLE));
				getSQL().updateAsync(StringUtil.format("UPDATE {0} SET server = ?;", Config.USER_TABLE), "*");
			}

			return true;
		} catch (Exception ex) {
			this.getLog().console(ex);
			return false;
		}
	}

}