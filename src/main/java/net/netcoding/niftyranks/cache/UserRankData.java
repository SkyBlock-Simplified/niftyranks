package net.netcoding.niftyranks.cache;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.mojang.BukkitMojangCache;
import net.netcoding.niftybukkit.mojang.BukkitMojangProfile;
import net.netcoding.niftycore.database.factory.callbacks.ResultCallback;
import net.netcoding.niftycore.mojang.MojangProfile;
import net.netcoding.niftycore.util.ListUtil;
import net.netcoding.niftycore.util.StringUtil;
import net.netcoding.niftycore.util.concurrent.ConcurrentSet;
import net.netcoding.niftyranks.NiftyRanks;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

public class UserRankData extends BukkitMojangCache {

	private static final transient ConcurrentSet<UserRankData> CACHE = new ConcurrentSet<>();
	private ConcurrentHashMap<String, List<String>> ranks = new ConcurrentHashMap<>();

	public UserRankData(JavaPlugin plugin, BukkitMojangProfile profile) {
		this(plugin, profile, true);
	}

	private UserRankData(JavaPlugin plugin, BukkitMojangProfile profile, boolean addToCache) {
		super(plugin, profile);
		if (addToCache) CACHE.add(this);
	}

	public static ConcurrentSet<UserRankData> getCache() {
		for (UserRankData data : CACHE) {
			if (!data.isOnlineLocally())
				CACHE.remove(data);
		}

		return CACHE;
	}

	public static UserRankData getCache(BukkitMojangProfile profile) {
		for (UserRankData data : getCache()) {
			if (data.getProfile().equals(profile))
				return data;
		}

		return new UserRankData(NiftyRanks.getPlugin(NiftyRanks.class), profile, false);
	}

	public String getPrimaryRank() {
		return this.getRanks().get(0);
	}

	public Map<String, List<String>> getAllRanks() {
		return Collections.unmodifiableMap(this.getAllRanks(false));
	}

	private Map<String, List<String>> getAllRanks(boolean fetch) {
		if (fetch || !this.isOnlineLocally()) {
			try {
				this.updateRanks();
			} catch (SQLException ex) { }
		}

		return this.ranks;
	}

	public List<String> getRanks() {
		return Collections.unmodifiableList(this.getRanks(false));
	}

	private List<String> getRanks(boolean fetch) {
		Map<String, List<String>> allRanks = this.getAllRanks(fetch);
		List<String> ranks = new ArrayList<>();

		if (allRanks.containsKey(getServerName()))
			ranks.addAll(allRanks.get(getServerName()));

		if (allRanks.containsKey("*"))
			ranks.addAll(allRanks.get("*"));

		return ranks;
	}

	private static Map<String, List<String>> _getRanks(final MojangProfile profile) throws SQLException {
		return NiftyRanks.getSQL().query(StringUtil.format("SELECT server, rank FROM {0} WHERE uuid = ? AND (server = ? OR server = ?) ORDER BY _submitted DESC;", Config.USER_TABLE), new ResultCallback<Map<String, List<String>>>() {
			@Override
			public HashMap<String, List<String>> handle(ResultSet result) throws SQLException {
				HashMap<String, List<String>> found = new HashMap<>();

				while (result.next()) {
					String server = result.getString("server");

					if (!found.containsKey(server))
						found.put(server, new ArrayList<String>());

					found.get(server).add(result.getString("rank"));
				}

				if (found.size() == 0) found.put(getServerName(), Arrays.asList("default"));
				return found;
			}
		}, profile.getUniqueId().toString(), "*", getServerName());
	}

	private static String getServerName() {
		return NiftyBukkit.getBungeeHelper().isDetected() ? NiftyBukkit.getBungeeHelper().getServerName() : "*";
	}

	public boolean hasRank(String rank) {
		return this.getRanks().contains(rank);
	}

	public static boolean rankExists(String rank) throws SQLException {
		return NiftyRanks.getSQL().query(StringUtil.format("SELECT * FROM {0} WHERE rank = ?;", Config.RANK_TABLE), new ResultCallback<Boolean>() {
			@Override
			public Boolean handle(ResultSet result) throws SQLException {
				return result.next();
			}
		}, rank);
	}

	public void saveVaultRanks() {
		OfflinePlayer oPlayer = this.getProfile().getOfflinePlayer();

		try {
			String[] uuidGroups = NiftyBukkit.getPermissions().getPlayerGroups(null, oPlayer);

			if (ListUtil.notEmpty(uuidGroups)) {
				for (String group : uuidGroups)
					NiftyBukkit.getPermissions().playerRemoveGroup((String)null, oPlayer, group);
			}
		} catch (Exception ex) { }

		try {
			String[] nameGroups = NiftyBukkit.getPermissions().getPlayerGroups((String)null, oPlayer.getName());

			if (ListUtil.notEmpty(nameGroups)) {
				for (String group : nameGroups)
					NiftyBukkit.getPermissions().playerRemoveGroup((String)null, oPlayer.getName(), group);
			}
		} catch (Exception ex) { }

		if (NiftyBukkit.getPermissions().hasGroupSupport()) {
			boolean notDefault = false;

			for (String rank : this.getRanks()) {
				if (!rank.equalsIgnoreCase("default")) {
					notDefault = true;
					break;
				}
			}

			if (!NiftyRanks.getPexOverride().isEnabled() || NiftyRanks.getPexOverride().getVersionUUID() == 0) {
				if (notDefault) {
					for (String rank : this.getRanks())
						NiftyBukkit.getPermissions().playerAddGroup(null, oPlayer, rank);
				}
			} else {
				if (notDefault) {
					for (String rank : this.getRanks())
						NiftyBukkit.getPermissions().playerAddGroup((String)null, oPlayer.getName(), rank);
				}
			}
		}
	}

	public void updateRanks() throws SQLException {
		this.ranks.clear();
		this.ranks.putAll(_getRanks(this.getProfile()));
	}

}