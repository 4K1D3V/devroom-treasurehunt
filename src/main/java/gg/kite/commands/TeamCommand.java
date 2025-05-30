package gg.kite.commands;

import gg.kite.config.MessageConfig;
import gg.kite.managers.TreasureManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Handles team-related commands with tab completion.
 */
public class TeamCommand implements CommandExecutor, TabCompleter {
    private final TreasureManager treasureManager;
    private final MessageConfig messageConfig;

    public TeamCommand(TreasureManager treasureManager, MessageConfig messageConfig) {
        this.treasureManager = treasureManager;
        this.messageConfig = messageConfig;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageConfig.getMessage("player-only"));
            return true;
        }
        if (!player.hasPermission("treasurehunt.team")) {
            player.sendMessage(messageConfig.getMessage("no-permission"));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(messageConfig.getMessage("usage-team-error"));
            return false;
        }

        try {
            switch (args[0].toLowerCase()) {
                case "create" -> createTeam(player, args);
                case "invite" -> invitePlayer(player, args);
                case "kick" -> kickPlayer(player, args);
                case "list" -> listTeams(player);
                default -> {
                    player.sendMessage(messageConfig.getMessage("invalid-team-command"));
                    return false;
                }
            }
        } catch (Exception e) {
            player.sendMessage(messageConfig.getMessage("error", "%s", e.getMessage()));
        }
        return true;
    }

    private void createTeam(Player player, String @NotNull [] args) {
        if (args.length != 2) {
            player.sendMessage(messageConfig.getMessage("usage-team-create"));
            return;
        }
        boolean created = treasureManager.createTeam(args[1], player);
        player.sendMessage(messageConfig.getMessage(created ? "team-created" : "team-exists", "%s", args[1]));
    }

    private void invitePlayer(Player player, String @NotNull [] args) {
        if (args.length != 2) {
            player.sendMessage(messageConfig.getMessage("usage-team-invite"));
            return;
        }
        Player invited = player.getServer().getPlayer(args[1]);
        if (invited == null) {
            player.sendMessage(messageConfig.getMessage("player-not-found", "%s", args[1]));
            return;
        }
        // Find the sender's team
        var team = treasureManager.getTeams().stream()
                .filter(t -> t.getMembers().contains(player.getUniqueId()))
                .findFirst()
                .orElse(null);
        if (team == null) {
            player.sendMessage(messageConfig.getMessage("team-not-found"));
            return;
        }
        boolean invitedSuccessfully = treasureManager.invitePlayer(team.getName(), invited);
        if (invitedSuccessfully) {
            player.sendMessage(messageConfig.getMessage("team-invited", "%s", args[1]));
            invited.sendMessage(messageConfig.getMessage("team-invite-received"));
        } else {
            player.sendMessage(messageConfig.getMessage("team-invite-failed", "%s", args[1]));
        }
    }

    private void kickPlayer(Player player, String @NotNull [] args) {
        if (args.length != 2) {
            player.sendMessage(messageConfig.getMessage("usage-team-kick"));
            return;
        }
        Player kicked = player.getServer().getPlayer(args[1]);
        if (kicked == null) {
            player.sendMessage(messageConfig.getMessage("player-not-found", "%s", args[1]));
            return;
        }
        // Find the sender's team
        var team = treasureManager.getTeams().stream()
                .filter(t -> t.getMembers().contains(player.getUniqueId()))
                .findFirst()
                .orElse(null);
        if (team == null) {
            player.sendMessage(messageConfig.getMessage("team-not-found"));
            return;
        }
        boolean kickedSuccessfully = treasureManager.kickPlayer(team.getName(), kicked);
        if (kickedSuccessfully) {
            player.sendMessage(messageConfig.getMessage("team-kicked", "%s", args[1]));
            kicked.sendMessage(messageConfig.getMessage("team-kick-received"));
        } else {
            player.sendMessage(messageConfig.getMessage("team-kick-failed", "%s", args[1]));
        }
    }

    private void listTeams(Player player) {
        var teams = treasureManager.getTeams();
        if (teams.isEmpty()) {
            player.sendMessage(messageConfig.getMessage("no-teams"));
        } else {
            player.sendMessage(messageConfig.getMessage("team-list"));
            for (var team : teams) {
                player.sendMessage(team.getName() + ": " + team.getMembers().stream()
                        .map(uuid -> player.getServer().getOfflinePlayer(uuid).getName())
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(", ")));
            }
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "invite", "kick", "list"));
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick"))) {
            completions.addAll(sender.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList());
        }

        if (args.length > 0) {
            String lastArg = args[args.length - 1].toLowerCase();
            completions = completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(lastArg))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}