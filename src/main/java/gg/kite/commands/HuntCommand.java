package gg.kite.commands;

import gg.kite.config.MessageConfig;
import gg.kite.managers.TreasureManager;
import gg.kite.managers.Team;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Command executor for managing treasure hunts and competitions with tab completion.
 */
public class HuntCommand extends AbstractCommand implements TabExecutor {
    private final TreasureManager treasureManager;

    /**
     * Constructs a HuntCommand with the specified dependencies.
     *
     * @param treasureManager The treasure manager for hunt operations.
     * @param messageConfig The message configuration for command messages.
     */
    public HuntCommand(@NotNull TreasureManager treasureManager, @NotNull MessageConfig messageConfig) {
        super(messageConfig, "treasurehunt.hunt");
        this.treasureManager = treasureManager;
    }

    /**
     * Executes the hunt command with subcommands: start, competition, leaderboard, progress.
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
            player.sendMessage(messageConfig.getMessage("usage-hunt-error"));
            return false;
        }

        try {
            switch (args[0].toLowerCase()) {
                case "start" -> treasureManager.startHunt(player);
                case "competition" -> handleCompetition(player, args);
                case "leaderboard" -> showLeaderboard(player);
                case "progress" -> showProgress(player);
                default -> {
                    player.sendMessage(messageConfig.getMessage("invalid-hunt-command"));
                    return false;
                }
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(messageConfig.getMessage("error", "%s", e.getMessage()));
        }
        return true;
    }

    /**
     * Handles the competition subcommand to start a team competition.
     *
     * @param player The player executing the command.
     * @param args The command arguments (expected: competition start).
     */
    private void handleCompetition(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (args.length != 2 || !args[1].equalsIgnoreCase("start")) {
            player.sendMessage(messageConfig.getMessage("usage-hunt-competition"));
            return;
        }
        if (!player.hasPermission("treasurehunt.admin")) {
            player.sendMessage(messageConfig.getMessage("no-permission"));
            return;
        }
        treasureManager.startCompetition();
    }

    /**
     * Displays the leaderboard of top teams.
     *
     * @param player The player requesting the leaderboard.
     */
    private void showLeaderboard(@NotNull Player player) {
        List<Team> leaderboard = treasureManager.getLeaderboard();
        if (leaderboard.isEmpty()) {
            player.sendMessage(messageConfig.getMessage("no-teams"));
            return;
        }
        player.sendMessage(messageConfig.getMessage("team-list"));
        for (int i = 0; i < leaderboard.size(); i++) {
            Team team = leaderboard.get(i);
            player.sendMessage(String.format("%d. %s (Score: %d)", i + 1, team.getName(), team.getScore()));
        }
    }

    /**
     * Displays the player's current hunt progress.
     *
     * @param player The player requesting their progress.
     */
    private void showProgress(@NotNull Player player) {
        Set<String> solvedClues = treasureManager.getPlayerClueProgress(player.getUniqueId());
        Set<String> foundTreasures = treasureManager.getPlayerTreasureProgress(player.getUniqueId());
        player.sendMessage(messageConfig.getMessage("progress-header"));
        player.sendMessage("Clues Solved: " + (solvedClues.isEmpty() ? "None" : solvedClues.size()));
        player.sendMessage("Treasures Found: " + (foundTreasures.isEmpty() ? "None" : foundTreasures.size()));
    }

    /**
     * Provides tab completion suggestions for the hunt command.
     *
     * @param sender The command sender.
     * @param command The command instance.
     * @param alias The command alias.
     * @param args The command arguments.
     * @return A list of tab completion suggestions.
     */
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(Arrays.asList("start", "competition", "leaderboard", "progress"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("competition")) {
            completions.add("start");
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