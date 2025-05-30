package gg.kite.commands;

import gg.kite.config.MessageConfig;
import gg.kite.managers.Clue;
import gg.kite.managers.Treasure;
import gg.kite.managers.TreasureManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles clue-related commands with tab completion.
 */
public class CluesCommand implements CommandExecutor, TabCompleter {
    private final TreasureManager treasureManager;
    private final MessageConfig messageConfig;

    public CluesCommand(TreasureManager manager, MessageConfig messageConfig) {
        this.treasureManager = manager;
        this.messageConfig = messageConfig;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageConfig.getMessage("player-only"));
            return true;
        }
        if (!player.hasPermission("treasurehunt.clue")) {
            player.sendMessage(messageConfig.getMessage("no-permission"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(messageConfig.getMessage("usage-clue-error"));
            return false;
        }

        try {
            switch (args[0].toLowerCase()) {
                case "create" -> createClue(player, args);
                case "delete" -> deleteClues(player, args);
                case "list" -> listClues(player, args);
                case "solve" -> solveClue(player, args);
                default -> {
                    player.sendMessage(messageConfig.getMessage("invalid-clue-command"));
                    return false;
                }
            }
        } catch (Exception e) {
            player.sendMessage(messageConfig.getMessage("error", "%s", e.getMessage()));
        }
        return true;
    }

    private void createClue(Player player, String @NotNull [] args) {
        if (args.length < 2) {
            player.sendMessage(messageConfig.getMessage("usage-clue-create"));
            return;
        }
        String description = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        boolean created = treasureManager.createClue(args[1], description, player.getLocation());
        player.sendMessage(messageConfig.getMessage(created ? "clue-created" : "clue-invalid", "%s", args[1]));
    }

    private void deleteClues(Player player, String @NotNull [] args) {
        if (args.length != 2) {
            player.sendMessage(messageConfig.getMessage("usage-clue-delete-error"));
            return;
        }
        boolean deleted = treasureManager.deleteClues(args[1]);
        player.sendMessage(messageConfig.getMessage(deleted ? "clues-deleted" : "treasure-not-found", "%s", args[1]));
    }

    private void listClues(Player player, String @NotNull [] args) {
        if (args.length != 2) {
            player.sendMessage(messageConfig.getMessage("usage-clue-list"));
            return;
        }
        var clues = treasureManager.getClues(args[1]);
        if (clues.isEmpty()) {
            player.sendMessage(messageConfig.getMessage("no-clues", "%s", args[1]));
        } else {
            player.sendMessage(messageConfig.getMessage("clue-list", "%s", args[1]));
            for (int i = 0; i < clues.size(); i++) {
                var clue = clues.get(i);
                Location loc = clue.location();
                player.sendMessage(String.format("%d. %s (Difficulty: %s, Location: %.1f, %.1f, %.1f)",
                        i + 1, clue.description(), clue.difficulty(), loc.getX(), loc.getY(), loc.getZ()));
            }
        }
    }

    private void solveClue(Player player, String @NotNull [] args) {
        if (args.length < 3) {
            player.sendMessage(messageConfig.getMessage("usage-clue-solve"));
            return;
        }
        String description = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        boolean solved = treasureManager.markClueSolved(player, args[1], description);
        player.sendMessage(messageConfig.getMessage(solved ? "clue-solved" : "clue-not-found", "%s", args[1]));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "delete", "list", "solve"));
        } else if (args.length == 2) {
            completions.addAll(treasureManager.getTreasures().stream()
                    .map(Treasure::getName)
                    .toList());
        } else if (args.length >= 3 && args[0].equalsIgnoreCase("solve")) {
            String treasureName = args[1];
            completions.addAll(treasureManager.getClues(treasureName).stream()
                    .map(Clue::description)
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