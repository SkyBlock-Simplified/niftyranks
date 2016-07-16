package net.netcoding.nifty.ranks.cache;

import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.yaml.BukkitSQLConfig;
import net.netcoding.nifty.core.database.MySQL;
import net.netcoding.nifty.core.yaml.ConfigSection;
import net.netcoding.nifty.core.yaml.exceptions.InvalidConfigurationException;

public class Config extends BukkitSQLConfig<MySQL> {

	private static final String TABLE_PREFIX = "niftyranks_";
	public static final String RANK_TABLE = TABLE_PREFIX + "list";
	public static final String USER_TABLE = TABLE_PREFIX + "users";
	private static final Boolean SERVER_LOCKED = false;

	public Config(MinecraftPlugin plugin) {
		super(plugin.getDataFolder(), "config");
	}

	public static String getServerNameFromArgs(String[] args, boolean check) {
		if (Nifty.getBungeeHelper().getDetails().isDetected()) {
			if (check) {
				if (Nifty.getBungeeHelper().getServer(args[args.length - 1]) != null)
					return args[args.length - 1];
			}
		}

		return "*";
	}

	public static boolean isLocked() {
		return Nifty.getBungeeHelper().getDetails().isDetected() ? SERVER_LOCKED : false;
	}

	@Override
	public boolean update(ConfigSection section) throws InvalidConfigurationException {
		if (section.has("mysql")) {
			ConfigSection mysql = section.get("mysql");
			section.remove("mysql");
			this.driver = "mysql";
			this.hostname = mysql.get("host");
			this.username = mysql.get("user");
			this.password = mysql.get("pass");
			this.port = mysql.get("port");
			this.schema = mysql.get("schema");

			return true;
		}

		return false;
	}

}
