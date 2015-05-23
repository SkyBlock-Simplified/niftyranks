package net.netcoding.niftyranks.managers;

import net.netcoding.niftybukkit.minecraft.BukkitHelper;
import net.netcoding.niftycore.util.StringUtil;
import net.netcoding.niftycore.util.VersionUtil;

import org.bukkit.plugin.java.JavaPlugin;

public class PexOverride extends BukkitHelper {

	private static final VersionUtil VERSION_UUID = new VersionUtil("1.23");
	//private static final VersionUtil VERSION_NAME = new VersionUtil("1.21");
	public static final String PACKAGE_NAME = "PermissionsEx";
	public static final String PACKAGE_PATH = StringUtil.format("ru.tehkode.permissions.bukkit.{0}", PACKAGE_NAME);
	private final boolean enabled;
	private VersionUtil version;

	public PexOverride(JavaPlugin plugin) {
		super(plugin);
		this.enabled = this.locatePackage();

		if (this.isEnabled()) {
			String pexVersion = this.getPexPlugin().getDescription().getVersion();
			pexVersion = pexVersion.replace("-SNAPSHOT", "");
			version = new VersionUtil(pexVersion);
		}
	}

	public JavaPlugin getPexPlugin() {
		return (JavaPlugin)this.getPlugin().getServer().getPluginManager().getPlugin(PACKAGE_NAME);
	}

	public String getVersion() {
		return this.version.toString();
	}

	public int getVersionUUID() {
		return version.compareTo(VERSION_UUID);
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	private boolean locatePackage() {
		try {
			Class.forName(PACKAGE_PATH);
			return this.getPexPlugin().isEnabled();
		} catch (Exception ex) {
			return false;
		}
	}

}