package gg.kite.commands;

import gg.kite.config.MessageConfig;
import gg.kite.managers.TreasureManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

/**
 * Handles clue-related commands.
 */
public class CluesCommand implements CommandExecutor {
    private final TreasureManager treasureManager;
    private final MessageConfig messageConfig;

    public CluesCommand(TreasureManager treasureManager, MessageConfig messageConfig) {
        this.treasureManager = treasureManager;
        this.messageConfig = messageConfig;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            return false;
        }

        try {
            switch (args[0].toLowerCase()) {
                case "create" -> createClue(sender, args);
                case "delete" -> deleteClues(sender, args);
                case "list" -> listClues(sender, args);
                case "solve" -> solveClue(sender, args);
                default -> { return false; }
            }
        } catch (Exception e) {
            sender.sendMessage(messageConfig.getMessage("error", "%s", e.getMessage()));
        }
        return true;
    }

    private void createClue(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageConfig.getMessage("player-only"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(messageConfig.getMessage("usage-clue-create"));
            return;
        }
        String description = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        boolean created = treasureManager.createClue(args[1], description, player.getLocation());
        sender.sendMessage(messageConfig.getMessage(created ? "clue-created" : "clue-invalid", "%s", args[1]));
    }

    private void deleteClues(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(messageConfig.getMessage("usage-clue-delete"));
            return;
        }
        boolean deleted = treasureManager.deleteClues(args[1]);
        sender.sendMessage(messageConfig.getMessage(deleted ? "clues-deleted" : "treasure-not-found", "%s", args[1]));
    }

    private void listClues(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(messageConfig.getMessage("usage-clue-list"));
            return;
        }
        var clues = treasureManager.getClues(args[1]);
        if (clues.isEmpty()) {
            sender.sendMessage(messageConfig.getMessage("no-clues", "%s", args[1]));
        } else {
            sender.sendMessage(messageConfig.getMessage("clue-list", "%s", args[1]));
            for (int i = 0; i < clues.size(); i++) {
                var clue = clues.get(i);
                sender.sendMessage((i + 1) + ". " + clue.getDescription() + " (Difficulty: " + clue.getDifficulty() + ")");
            }
        }
    }

    private void solveClue(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageConfig.getMessage("player-only"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(messageConfig.getMessage("usage-clue-solve"));
            return;
        }
        String description = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        boolean solved = treasureManager.markClueSolved(player, args[1], description);
        sender.sendMessage(messageConfig.getMessage(solved ? "clue-solved" : "clue-not-found", "%s", args[1]));
    }
}