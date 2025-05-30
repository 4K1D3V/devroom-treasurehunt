package gg.kite.commands;

import gg.kite.config.MessageConfig;
import gg.kite.managers.TreasureManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Command executor for managing teams with tab completion support.
 */
public class TeamCommand extends AbstractCommand implements TabExecutor {
    private final TreasureManager treasureManager;

    /**
     * Constructs a TeamCommand with the specified dependencies.
     *
     * @param treasureManager The treasure manager for team operations.
     * @param messageConfig The message configuration for command messages.
     */
    public TeamCommand(@NotNull TreasureManager treasureManager, @NotNull MessageConfig messageConfig) {
        super(messageConfig, "treasurehunt.team");
        this.treasureManager = treasureManager;
    }

    /**
     * Executes the team command with subcommands: create, invite, kick, list.
     *
     * @param sender The command sender.
     * @param command The command instance.
     * @param label The command label.
     * @param args The command arguments.
     * @return True if the command was processed successfully, false otherwise.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!checkSender(sender)) return true;
        Player player = (Player) sender;

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
        } catch (IllegalArgumentException e) {
            player.sendMessage(messageConfig.getMessage("error", "%s", e.getMessage()));
        }
        return true;
    }

    /**
     * Creates a new team with the specified name.
     *
     * @param player The player executing the command.
     * @param args The command arguments (expected: create <name>).
     */
    private void createTeam(@NotNull Player player, @NotNull String[] args) {
        if (args.length != 2) {
            player.sendMessage(messageConfig.getMessage("usage-team-create"));
            return;
        }
        if (args[1].length() > 16 || !args[1].matches("[a-zA-Z0-9_-]+")) {
            throw new IllegalArgumentException("Team name must be alphanumeric and up to 16 characters");
        }
        boolean created = treasureManager.createTeam(args[1], player);
        player.sendMessage(messageConfig.getMessage(created ? "team-created" : "team-exists", "%s", args[1]));
    }

    /**
     * Invites a player to the sender's team.
     *
     * @param player The player executing the command.
     * @param args The command arguments (expected: invite <player>).
     */
    private void invitePlayer(@NotNull Player player, @NotNull String[] args) {
        if (args.length != 2) {
            player.sendMessage(messageConfig.getMessage("usage-team-invite"));
            return;
        }
        Player invited = player.getServer().getPlayer(args[1]);
        if (invited == null) {
            player.sendMessage(messageConfig.getMessage("player-not-found", "%s", args[1]));
            return;
        }
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

    /**
     * Kicks a player from the sender's team.
     *
     * @param player The player executing the command.
     * @param args The command arguments (expected: kick <player>).
     */
    private void kickPlayer(@NotNull Player player, @NotNull String[] args) {
        if (args.length != 2) {
            player.sendMessage(messageConfig.getMessage("usage-team-kick"));
            return;
        }
        Player kicked = player.getServer().getPlayer(args[1]);
        if (kicked == null) {
            player.sendMessage(messageConfig.getMessage("player-not-found", "%s", args[1]));
            return;
        }
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

    /**
     * Lists all teams and their members.
     *
     * @param player The player executing the command.
     */
    private void listTeams(@NotNull Player player) {
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

    /**
     * Provides tab completion suggestions for the team command.
     *
     * @param sender The command sender.
     * @param command The command instance.
     * @param alias The command.
     * @param args The command arguments.
     * @return A list of tab completion suggestions.
     */
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
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