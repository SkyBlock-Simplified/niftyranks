package net.netcoding.nifty.ranks.commands;

import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.Command;
import net.netcoding.nifty.common.api.plugin.MinecraftListener;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.minecraft.OfflinePlayer;
import net.netcoding.nifty.common.minecraft.command.CommandSource;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.core.api.color.ChatColor;
import net.netcoding.nifty.core.mojang.MojangProfile;
import net.netcoding.nifty.core.mojang.exceptions.ProfileNotFoundException;
import net.netcoding.nifty.core.util.ListUtil;
import net.netcoding.nifty.core.util.StringUtil;
import net.netcoding.nifty.ranks.NiftyRanks;
import net.netcoding.nifty.ranks.cache.Config;
import net.netcoding.nifty.ranks.cache.UserRankData;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Rank extends MinecraftListener {

	public Rank(MinecraftPlugin plugin) {
		super(plugin);
	}

	@Command(name = "rank",
			playerTabComplete = true,
			usages = {
					@Command.Usage(match = "list"),
					@Command.Usage(match = "(add|rem(ove)?)", replace = "[player] <rank> [server]"),
					@Command.Usage(match = "check", replace = "<player>"),
					@Command.Usage(match = "(crea|dele)te", replace = "<rank>"),
					@Command.Usage(match = "import", replace = "groups|users")
			}
	)
	public void onCommand(CommandSource source, String alias, String args[]) throws Exception {
		final String action = (Config.isLocked() && !args[0].matches("^add|remove|check|list$")) ? "" : args[0];

		if ("import".equalsIgnoreCase(action)) {
			if (this.hasPermissions(source, "rank", "import")) {
				if (args.length < 2) {
					this.showUsage(source);
					return;
				}

				if (!Nifty.hasVault()) {
					this.getLog().error(source, "You have no vault plugin to import with!");
					return;
				}

				if (!Nifty.getPermissions().hasGroupSupport()) {
					this.getLog().error(source, "Your permissions manager has no group support!");
					return;
				}

				try {
					if ("groups".equalsIgnoreCase(args[1])) {
						for (String group : Nifty.getPermissions().getGroups()) {
							if ("default".equalsIgnoreCase(group)) continue;
							NiftyRanks.getSQL().update(StringUtil.format("INSERT IGNORE INTO {0} (rank) VALUES (?);", Config.RANK_TABLE), group);
						}

						this.getLog().message(source, "Groups successfully imported!");
					} else if ("users".equalsIgnoreCase(args[1])) {
						for (OfflinePlayer oplayer : this.getPlugin().getServer().getOfflinePlayers()) {
							String[] playerGroups = Nifty.getPermissions().getPlayerGroups(null, oplayer);
							List<String> groupList = ListUtil.notEmpty(playerGroups) ? Arrays.asList(playerGroups) : new ArrayList<>();
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
										Nifty.getPermissions().playerRemoveGroup(null, oplayer, group);
								}
							}
						}

						this.getLog().message(source, "Users successfully imported!");
					} else
						this.showUsage(source);
				} catch (Exception ex) {
					this.getLog().console(ex);
				}
			} else
				this.getLog().error(source, "You do not have permission to import existing players and groups");
		} else if ("list".equalsIgnoreCase(action)) {
			if (this.hasPermissions(source, "rank", "list")) {
				NiftyRanks.getSQL().queryAsync(StringUtil.format("SELECT * FROM {0};", Config.RANK_TABLE), result -> {
					List<String> ranks = new ArrayList<>();
					while (result.next()) ranks.add(result.getString("rank"));
					getLog().message(source, "The ranks are {{0}}.", StringUtil.implode(ChatColor.GRAY + ", " + ChatColor.RED, ranks));
				});
			} else
				this.getLog().error(source, "You do not have permission to list all ranks!");
		} else if (action.matches("^add|remove$")) {
			if (args.length >= 2 && args.length <= 4) {
				String server = Config.getServerNameFromArgs(args, args.length > 2);
				if (server.equalsIgnoreCase(args[args.length - 1]))
					args = StringUtil.split(",", StringUtil.implode(",", args, 0, args.length - 1));
				server = Config.isLocked() ? Nifty.getBungeeHelper().getServerName() : server;

				final String rank = args[args.length - 1];
				if (!UserRankData.rankExists(rank)) {
					this.getLog().error(source, "The rank {{0}} does not exist!", rank);
					return;
				}
				args = StringUtil.split(",", StringUtil.implode(",", args, 0, args.length - 1));

				String user = action.equalsIgnoreCase(args[args.length - 1]) ? source.getName() : args[args.length - 1];
				String complete = "now has the rank of";
				String location = ("*".equals(server) ? "" : StringUtil.format(" in {{0}}", server));
				final MojangProfile profile;

				try {
					profile = Nifty.getMojangRepository().searchByUsername(user);
				} catch (ProfileNotFoundException pnfe) {
					this.getLog().error(source, "Unable to locate the profile of {{0}}!", user);
					return;
				}

				if ("add".equalsIgnoreCase(action))
					NiftyRanks.getSQL().updateAsync(StringUtil.format("INSERT INTO {0} (uuid, rank, server) VALUES (?, ?, ?);", Config.USER_TABLE), profile.getUniqueId(), rank, server);
				else if ("remove".equalsIgnoreCase(action)) {
					NiftyRanks.getSQL().updateAsync(StringUtil.format("DELETE FROM {0} WHERE uuid = ? AND rank = ? AND server = ?;", Config.USER_TABLE), profile.getUniqueId(), rank, server);
					complete  = "no longer has the rank of";
				}

				this.getLog().message(source, "{{0}} {1} {{2}}{3}.", profile.getName(), complete, rank, location);
			} else
				this.showUsage(source);
		} else if (action.matches("^create|delete$")) {
			if (args.length == 2) {
				final String rank = args[1];

				if ("delete".equalsIgnoreCase(action) && "default".equalsIgnoreCase(rank)) {
					this.getLog().error(source, "You cannot delete the default rank!");
					return;
				}

				if ("create".equalsIgnoreCase(action)) {
					try {
						NiftyRanks.getSQL().update(StringUtil.format("INSERT INTO {0} (rank) VALUES (?);", Config.RANK_TABLE), rank);
					} catch (SQLIntegrityConstraintViolationException ex) {
						this.getLog().error(source, "The rank {{0}} already exists!", rank);
					}
				} else
					NiftyRanks.getSQL().update(StringUtil.format("DELETE FROM {0} WHERE rank = ?;", Config.RANK_TABLE), rank);

				this.getLog().message(source, "The rank {{0}} has been {1}d.", rank, action);
			} else
				this.showUsage(source);
		} else if ("check".equalsIgnoreCase(action)) {
			if (this.hasPermissions(source, "rank", "check")) {
				if (args.length == 2) {
					String user = args[1];

					try {
						MinecraftMojangProfile profile = Nifty.getMojangRepository().searchByUsername(user);
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
							this.getLog().message(source, "{{0}} is a member of the following:", profile.getName());

							for (String server : allRanks.keySet())
								this.getLog().message(source, "{{0}}: {{1}}.", ("*".equals(server) ? "Global" : server), StringUtil.implode(ChatColor.GRAY + ", " + ChatColor.RED, allRanks.get(server)));
						} else
							this.getLog().message(source, "{{0}} is a member of the following: {{1}}.", profile.getName(), StringUtil.implode(ChatColor.GRAY + ", " + ChatColor.RED, rankData.getRanks()));
					} catch (ProfileNotFoundException pnfe) {
						this.getLog().error(source, "Unable to locate the profile of {{0}}!", user);
					}
				} else
					this.showUsage(source);
			} else
				this.getLog().error(source, "You do not have permission to list a players ranks!");
		} else {
			if (Config.isLocked()) {
				this.getLog().error(source, "You do not have permission to run this command!");
				return;
			}

			if (args.length <= 3) {
				final String server = Config.getServerNameFromArgs(args, args.length > 2);
				if (server.equalsIgnoreCase(args[args.length - 1]))
					args = StringUtil.split(",", StringUtil.implode(",", args, 0, args.length - 1));

				if (args.length == 0) {
					this.getLog().error(source, "You have not specified a rank!");
					return;
				}

				final String rank = args[args.length - 1];
				if (!UserRankData.rankExists(rank)) {
					this.getLog().error(source, "The rank {{0}} does not exist!", rank);
					return;
				}

				args = StringUtil.split(",", StringUtil.implode(",", args, 0, args.length - 1));
				String user = (args.length == 1 ? args[args.length - 1] : source.getName());
				String location = ("*".equals(server) ? "" : StringUtil.format(" in {{0}}", server));
				final MojangProfile profile;

				if (isConsole(user)) {
					this.getLog().error(source, "Changing ranks requires a player name when used by the console!");
					return;
				}

				try {
					profile = Nifty.getMojangRepository().searchByUsername(user);
				} catch (ProfileNotFoundException pnfe) {
					this.getLog().error(source, "Unable to locate the profile of {{0}}!", user);
					return;
				}

				NiftyRanks.getSQL().update(StringUtil.format("DELETE FROM {0} WHERE uuid = ? AND server = ?;", Config.USER_TABLE), profile.getUniqueId(), server);
				NiftyRanks.getSQL().update(StringUtil.format("INSERT INTO {0} (uuid, rank, server) VALUES (?, ?, ?);", Config.USER_TABLE), profile.getUniqueId(), rank, server);
				getLog().message(source, "{{0}} has been ranked to {{1}}{2}.", profile.getName(), rank, location);
			} else
				this.showUsage(source);
		}
	}

}