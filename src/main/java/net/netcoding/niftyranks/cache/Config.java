package net.netcoding.niftyranks.cache;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftycore.database.MySQL;
import net.netcoding.niftycore.yaml.ConfigSection;
import net.netcoding.niftycore.yaml.SQLConfig;
import net.netcoding.niftycore.yaml.exceptions.InvalidConfigurationException;

import org.bukkit.plugin.java.JavaPlugin;

public class Config extends SQLConfig<MySQL> {

	private static final String TABLE_PREFIX = "niftyranks_";
	public static final String RANK_TABLE = TABLE_PREFIX + "list";
	public static final String USER_TABLE = TABLE_PREFIX + "users";
	private static final Boolean SERVER_LOCKED = false;

	public Config(JavaPlugin plugin) {
		super(plugin.getDataFolder(), "config");
	}

	public static String getServerNameFromArgs(String[] args, boolean check) {
		if (NiftyBukkit.getBungeeHelper().isDetected()) {
			if (check) {
				if (NiftyBukkit.getBungeeHelper().getServer(args[args.length - 1]) != null)
					return args[args.length - 1];
			}
		}

		return "*";
	}

	public static boolean isLocked() {
		return NiftyBukkit.getBungeeHelper().isDetected() ? SERVER_LOCKED : false;
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
