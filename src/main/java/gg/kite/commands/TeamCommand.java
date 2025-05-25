package gg.kite.commands;

import gg.kite.config.MessageConfig;
import gg.kite.managers.TreasureManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Handles team-related commands.
 */
public class TeamCommand implements CommandExecutor {
    private final TreasureManager treasureManager;
    private final MessageConfig messageConfig;

    public TeamCommand(TreasureManager treasureManager, MessageConfig messageConfig) {
        this.treasureManager = treasureManager;
        this.messageConfig = messageConfig;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageConfig.getMessage("player-only"));
            return true;
        }
        if (args.length == 0) {
            return false;
        }

        try {
            switch (args[0].toLowerCase()) {
                case "create" -> createTeam(player, args);
                case "invite" -> invitePlayer(player, args);
                case "kick" -> kickPlayer(player, args);
                case "list" -> listTeams(player);
                default -> { return false; }
            }
        } catch (Exception e) {
            player.sendMessage(messageConfig.getMessage("error", "%s", e.getMessage()));
        }
        return true;
    }

    private void createTeam(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(messageConfig.getMessage("usage-team-create"));
            return;
        }
        boolean created = treasureManager.createTeam(args[1], player);
        player.sendMessage(messageConfig.getMessage(created ? "team-created" : "team-exists", "%s", args[1]));
    }

    private void invitePlayer(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(messageConfig.getMessage("usage-team-invite"));
            return;
        }
        Player invited = player.getServer().getPlayer(args[1]);
        if (invited == null) {
            player.sendMessage(messageConfig.getMessage("player-not-found", "%s", args[1]));
            return;
        }
        boolean invitedSuccessfully = treasureManager.invitePlayer(args[1], invited);
        if (invitedSuccessfully) {
            player.sendMessage(messageConfig.getMessage("team-invited", "%s", args[1]));
            invited.sendMessage(messageConfig.getMessage("team-invite-received"));
        } else {
            player.sendMessage(messageConfig.getMessage("team-not-found"));
        }
    }

    private void kickPlayer(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(messageConfig.getMessage("usage-team-kick"));
            return;
        }
        Player kicked = player.getServer().getPlayer(args[1]);
        if (kicked == null) {
            player.sendMessage(messageConfig.getMessage("player-not-found", "%s", args[1]));
            return;
        }
        boolean kickedSuccessfully = treasureManager.kickPlayer(args[1], kicked);
        if (kickedSuccessfully) {
            player.sendMessage(messageConfig.getMessage("team-kicked", "%s", args[1]));
            kicked.sendMessage(messageConfig.getMessage("team-kick-received"));
        } else {
            player.sendMessage(messageConfig.getMessage("team-not-found"));
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
}