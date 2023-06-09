package net.netcoding.nifty.ranks.cache;

import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.minecraft.OfflinePlayer;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.common.mojang.MinecraftMojangCache;
import net.netcoding.nifty.core.mojang.MojangProfile;
import net.netcoding.nifty.core.util.ListUtil;
import net.netcoding.nifty.core.util.StringUtil;
import net.netcoding.nifty.core.util.concurrent.Concurrent;
import net.netcoding.nifty.core.util.concurrent.ConcurrentMap;
import net.netcoding.nifty.core.util.concurrent.ConcurrentSet;
import net.netcoding.nifty.ranks.NiftyRanks;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRankData extends MinecraftMojangCache<MinecraftMojangProfile> {

	private static final transient ConcurrentSet<UserRankData> CACHE = Concurrent.newSet();
	private final ConcurrentMap<String, List<String>> ranks = Concurrent.newMap();

	public UserRankData(MinecraftPlugin plugin, MinecraftMojangProfile profile) {
		this(plugin, profile, true);
	}

	private UserRankData(MinecraftPlugin plugin, MinecraftMojangProfile profile, boolean addToCache) {
		super(plugin, profile);

		if (addToCache)
			CACHE.add(this);
	}

	public static ConcurrentSet<UserRankData> getCache() {
		CACHE.stream().filter(data -> !data.isOnlineLocally()).forEach(CACHE::remove);
		return CACHE;
	}

	public static UserRankData getCache(MinecraftMojangProfile profile) {
		for (UserRankData data : getCache()) {
			if (data.getProfile().equals(profile))
				return data;
		}

		return new UserRankData(Nifty.getPluginManager().getPlugin(NiftyRanks.class), profile, false);
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
			} catch (SQLException ignore) { }
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
		return NiftyRanks.getSQL().query(StringUtil.format("SELECT server, rank FROM {0} WHERE uuid = ? AND (server = ? OR server = ?) ORDER BY _submitted DESC;", Config.USER_TABLE), result -> {
			ConcurrentMap<String, List<String>> found = Concurrent.newMap();

			while (result.next()) {
				String server = result.getString("server");

				if (!found.containsKey(server))
					found.put(server, new ArrayList<>());

				found.get(server).add(result.getString("rank"));
			}

			if (found.isEmpty()) found.put(getServerName(), Collections.singletonList("default"));
			return found;
		}, profile.getUniqueId().toString(), "*", getServerName());
	}

	private static String getServerName() {
		return Nifty.getBungeeHelper().getDetails().isDetected() ? Nifty.getBungeeHelper().getServerName() : "*";
	}

	public boolean hasRank(String rank) {
		return this.getRanks().contains(rank);
	}

	public static boolean rankExists(String rank) throws SQLException {
		return NiftyRanks.getSQL().query(StringUtil.format("SELECT * FROM {0} WHERE rank = ?;", Config.RANK_TABLE), ResultSet::next, rank);
	}

	public static void removeCache(MinecraftMojangProfile profile) {
		for (UserRankData data : CACHE) {
			if (data.getProfile().equals(profile)) {
				CACHE.remove(data);
				break;
			}
		}
	}

	@SuppressWarnings("deprecation")
	public void saveVaultRanks() {
		OfflinePlayer oPlayer = this.getProfile().getOfflinePlayer();

		try {
			String[] uuidGroups = Nifty.getPermissions().getPlayerGroups(null, oPlayer);

			if (ListUtil.notEmpty(uuidGroups)) {
				for (String group : uuidGroups)
					Nifty.getPermissions().playerRemoveGroup(null, oPlayer, group);
			}
		} catch (Exception ignore) { }

		try {
			String[] nameGroups = Nifty.getPermissions().getPlayerGroups((String)null, oPlayer.getName());

			if (ListUtil.notEmpty(nameGroups)) {
				for (String group : nameGroups)
					Nifty.getPermissions().playerRemoveGroup((String)null, oPlayer.getName(), group);
			}
		} catch (Exception ignore) { }

		if (Nifty.getPermissions().hasGroupSupport()) {
			boolean notDefault = false;

			for (String rank : this.getRanks()) {
				if (!"default".equalsIgnoreCase(rank)) {
					notDefault = true;
					break;
				}
			}

			if (!NiftyRanks.getPexOverride().isEnabled() || NiftyRanks.getPexOverride().getVersionUUID() == 0) {
				if (notDefault) {
					for (String rank : this.getRanks())
						Nifty.getPermissions().playerAddGroup(null, oPlayer, rank);
				}
			} else {
				if (notDefault) {
					for (String rank : this.getRanks())
						Nifty.getPermissions().playerAddGroup((String)null, oPlayer.getName(), rank);
				}
			}
		}
	}

	public void updateRanks() throws SQLException {
		this.ranks.clear();
		this.ranks.putAll(_getRanks(this.getProfile()));
	}

}