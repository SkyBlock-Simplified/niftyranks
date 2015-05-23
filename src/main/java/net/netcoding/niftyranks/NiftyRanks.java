package net.netcoding.niftyranks;

import net.netcoding.niftybukkit.minecraft.BukkitPlugin;
import net.netcoding.niftycore.database.factory.SQLWrapper;
import net.netcoding.niftycore.util.StringUtil;
import net.netcoding.niftyranks.cache.Config;
import net.netcoding.niftyranks.commands.Rank;
import net.netcoding.niftyranks.listeners.Login;
import net.netcoding.niftyranks.listeners.Notifications;
import net.netcoding.niftyranks.managers.PexOverride;

public class NiftyRanks extends BukkitPlugin {

	private static transient Config pluginConfig;
	private static transient PexOverride pexOverride;

	@Override
	public void onEnable() {
		try {
			this.getLog().console("Loading SQL Config");
			(pluginConfig = new Config(this)).init();

			if (pluginConfig.getSQL() == null) {
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
		new Login(this);

		if ((pexOverride = new PexOverride(this)).isEnabled() && pexOverride.getVersionUUID() != 0)
			this.getLog().console("Warning, you are using an unsupported version of {0} ({1}). Currently, the only working UUID version is 1.23!", PexOverride.PACKAGE_NAME, pexOverride.getVersion());
	}

	@Override
	public void onDisable() {
		if (getSQL() != null)
			getSQL().removeListeners();
	}

	public final static PexOverride getPexOverride() {
		return pexOverride;
	}

	public final static SQLWrapper getSQL() {
		return pluginConfig.getSQL();
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