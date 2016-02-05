package net.netcoding.niftyranks.commands;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.mojang.BukkitMojangProfile;
import net.netcoding.niftycore.database.factory.callbacks.VoidResultCallback;
import net.netcoding.niftycore.mojang.MojangProfile;
import net.netcoding.niftycore.mojang.exceptions.ProfileNotFoundException;
import net.netcoding.niftycore.util.ListUtil;
import net.netcoding.niftycore.util.StringUtil;
import net.netcoding.niftyranks.NiftyRanks;
import net.netcoding.niftyranks.cache.Config;
import net.netcoding.niftyranks.cache.UserRankData;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Rank extends BukkitCommand {

	public Rank(JavaPlugin plugin) {
		super(plugin, "rank");
		this.editUsage(1, "list", "");
		this.editUsage(1, "add", "[player] <rank> [server]");
		this.editUsage(1, "remove", "[player] <rank> [server]");
		this.editUsage(1, "check", "<player>");
		this.editUsage(1, "create", "<rank>");
		this.editUsage(1, "delete", "<rank>");
		this.editUsage(1, "import", "groups|users");
	}

	@Override
	public void onCommand(final CommandSender sender, String alias, String args[]) throws Exception {
		final String action = (Config.isLocked() && !args[0].matches("^add|remove|check|list$")) ? "" : args[0];

		if ("import".equalsIgnoreCase(action)) {
			if (this.hasPermissions(sender, "rank", "import")) {
				if (args.length < 2) {
					this.showUsage(sender);
					return;
				}

				if (!NiftyBukkit.hasVault()) {
					this.getLog().error(sender, "You have no vault plugin to import with!");
					return;
				}

				if (!NiftyBukkit.getPermissions().hasGroupSupport()) {
					this.getLog().error(sender, "Your permissions manager has no group support!");
					return;
				}

				try {
					if ("groups".equalsIgnoreCase(args[1])) {
						for (String group : NiftyBukkit.getPermissions().getGroups()) {
							if ("default".equalsIgnoreCase(group)) continue;
							NiftyRanks.getSQL().update(StringUtil.format("INSERT IGNORE INTO {0} (rank) VALUES (?);", Config.RANK_TABLE), group);
						}

						this.getLog().message(sender, "Groups successfully imported!");
					} else if ("users".equalsIgnoreCase(args[1])) {
						for (OfflinePlayer oplayer : this.getPlugin().getServer().getOfflinePlayers()) {
							String[] playerGroups = NiftyBukkit.getPermissions().getPlayerGroups(null, oplayer);
							List<String> groupList = ListUtil.notEmpty(playerGroups) ? Arrays.asList(playerGroups) : new ArrayList<String>();
							Collections.reverse(groupList);
							playerGroups = ListUtil.toArray(groupList, String.class);

							if (ListUtil.notEmpty(playerGroups)) {
								for (String group : playerGroups) {
									boolean remove = false;

									if ("default".equalsIgnoreCase(group))
										remove = true;
									else {
										if (NiftyRanks.getSQL().update(StringUtil.format("INSERT IGNORE INTO {0} (uuid, rank, server) VALUES (?, ?, ?);", Config.USER_TABLE), oplayer.getUniqueId(), group, "*"))
											remove = true;
									}

									if (remove)
										NiftyBukkit.getPermissions().playerRemoveGroup(null, oplayer, group);
								}
							}
						}

						this.getLog().message(sender, "Users successfully imported!");
					} else
						this.showUsage(sender);
				} catch (Exception ex) {
					this.getLog().console(ex);
				}
			} else
				this.getLog().error(sender, "You do not have permission to import existing players and groups");
		} else if ("list".equalsIgnoreCase(action)) {
			if (this.hasPermissions(sender, "rank", "list")) {
				NiftyRanks.getSQL().queryAsync(StringUtil.format("SELECT * FROM {0};", Config.RANK_TABLE), new VoidResultCallback() {
					@Override
					public void handle(ResultSet result) throws SQLException {
						List<String> ranks = new ArrayList<>();
						while (result.next()) ranks.add(result.getString("rank"));
						getLog().message(sender, "The ranks are {{0}}.", StringUtil.implode(ChatColor.GRAY + ", " + ChatColor.RED, ranks));
					}
				});
			} else
				this.getLog().error(sender, "You do not have permission to list all ranks!");
		} else if (action.matches("^add|remove$")) {
			if (args.length >= 2 && args.length <= 4) {
				String server = Config.getServerNameFromArgs(args, args.length > 2);
				if (server.equalsIgnoreCase(args[args.length - 1]))
					args = StringUtil.split(",", StringUtil.implode(",", args, 0, args.length - 1));
				server = Config.isLocked() ? NiftyBukkit.getBungeeHelper().getServerName() : server;

				final String rank = args[args.length - 1];
				if (!UserRankData.rankExists(rank)) {
					this.getLog().error(sender, "The rank {{0}} does not exist!", rank);
					return;
				}
				args = StringUtil.split(",", StringUtil.implode(",", args, 0, args.length - 1));

				String user = action.equalsIgnoreCase(args[args.length - 1]) ? sender.getName() : args[args.length - 1];
				String complete = "now has the rank of";
				String location = ("*".equals(server) ? "" : StringUtil.format(" in {{0}}", server));
				final MojangProfile profile;

				try {
					profile = NiftyBukkit.getMojangRepository().searchByUsername(user);
				} catch (ProfileNotFoundException pnfe) {
					this.getLog().error(sender, "Unable to locate the profile of {{0}}!", user);
					return;
				}

				if ("add".equalsIgnoreCase(action))
					NiftyRanks.getSQL().updateAsync(StringUtil.format("INSERT INTO {0} (uuid, rank, server) VALUES (?, ?, ?);", Config.USER_TABLE), profile.getUniqueId(), rank, server);
				else if ("remove".equalsIgnoreCase(action)) {
					NiftyRanks.getSQL().updateAsync(StringUtil.format("DELETE FROM {0} WHERE uuid = ? AND rank = ? AND server = ?;", Config.USER_TABLE), profile.getUniqueId(), rank, server);
					complete  = "no longer has the rank of";
				}

				this.getLog().message(sender, "{{0}} {1} {{2}}{3}.", profile.getName(), complete, rank, location);
			} else
				this.showUsage(sender);
		} else if (action.matches("^create|delete$")) {
			if (args.length == 2) {
				final String rank = args[1];

				if ("delete".equalsIgnoreCase(action) && "default".equalsIgnoreCase(rank)) {
					this.getLog().error(sender, "You cannot delete the default rank!");
					return;
				}

				if ("create".equalsIgnoreCase(action)) {
					try {
						NiftyRanks.getSQL().update(StringUtil.format("INSERT INTO {0} (rank) VALUES (?);", Config.RANK_TABLE), rank);
					} catch (SQLIntegrityConstraintViolationException ex) {
						this.getLog().error(sender, "The rank {{0}} already exists!", rank);
					}
				} else
					NiftyRanks.getSQL().update(StringUtil.format("DELETE FROM {0} WHERE rank = ?;", Config.RANK_TABLE), rank);

				this.getLog().message(sender, "The rank {{0}} has been {1}d.", rank, action);
			} else
				this.showUsage(sender);
		} else if ("check".equalsIgnoreCase(action)) {
			if (this.hasPermissions(sender, "rank", "check")) {
				if (args.length == 2) {
					String user = args[1];

					try {
						BukkitMojangProfile profile = NiftyBukkit.getMojangRepository().searchByUsername(user);
						UserRankData rankData = UserRankData.getCache(profile);
						Map<String, List<String>> allRanks = rankData.getAllRanks();
						boolean hasNonGlobal = false;

						for (String server : allRanks.keySet()) {
							if (!"*".equals(server)) {
								hasNonGlobal = true;
								break;
							}
						}

						if (hasNonGlobal) {
							this.getLog().message(sender, "{{0}} is a member of the following:", profile.getName());

							for (String server : allRanks.keySet())
								this.getLog().message(sender, "{{0}}: {{1}}.", ("*".equals(server) ? "Global" : server), StringUtil.implode(ChatColor.GRAY + ", " + ChatColor.RED, allRanks.get(server)));
						} else
							this.getLog().message(sender, "{{0}} is a member of the following: {{1}}.", profile.getName(), StringUtil.implode(ChatColor.GRAY + ", " + ChatColor.RED, rankData.getRanks()));
					} catch (ProfileNotFoundException pnfe) {
						this.getLog().error(sender, "Unable to locate the profile of {{0}}!", user);
					}
				} else
					this.showUsage(sender);
			} else
				this.getLog().error(sender, "You do not have permission to list a players ranks!");
		} else {
			if (Config.isLocked()) {
				this.getLog().error(sender, "You do not have permission to run this command!");
				return;
			}

			if (args.length <= 3) {
				final String server = Config.getServerNameFromArgs(args, args.length > 2);
				if (server.equalsIgnoreCase(args[args.length - 1]))
					args = StringUtil.split(",", StringUtil.implode(",", args, 0, args.length - 1));

				if (args.length == 0) {
					this.getLog().error(sender, "You have not specified a rank!");
					return;
				}

				final String rank = args[args.length - 1];
				if (!UserRankData.rankExists(rank)) {
					this.getLog().error(sender, "The rank {{0}} does not exist!", rank);
					return;
				}

				args = StringUtil.split(",", StringUtil.implode(",", args, 0, args.length - 1));
				String user = (args.length == 1 ? args[args.length - 1] : sender.getName());
				String location = ("*".equals(server) ? "" : StringUtil.format(" in {{0}}", server));
				final MojangProfile profile;

				if (isConsole(user)) {
					this.getLog().error(sender, "Changing ranks requires a player name when used by the console!");
					return;
				}

				try {
					profile = NiftyBukkit.getMojangRepository().searchByUsername(user);
				} catch (ProfileNotFoundException pnfe) {
					this.getLog().error(sender, "Unable to locate the profile of {{0}}!", user);
					return;
				}

				NiftyRanks.getSQL().update(StringUtil.format("DELETE FROM {0} WHERE uuid = ? AND server = ?;", Config.USER_TABLE), profile.getUniqueId(), server);
				NiftyRanks.getSQL().update(StringUtil.format("INSERT INTO {0} (uuid, rank, server) VALUES (?, ?, ?);", Config.USER_TABLE), profile.getUniqueId(), rank, server);
				getLog().message(sender, "{{0}} has been ranked to {{1}}{2}.", profile.getName(), rank, location);
			} else
				this.showUsage(sender);
		}
	}

}