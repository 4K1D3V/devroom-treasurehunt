package gg.kite.commands;

import gg.kite.config.MessageConfig;
import gg.kite.managers.Clue;
import gg.kite.managers.Treasure;
import gg.kite.managers.TreasureManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command executor for managing clues with tab completion support.
 */
public class CluesCommand extends AbstractCommand implements TabExecutor {
    private final TreasureManager treasureManager;

    /**
     * Constructs a CluesCommand with the specified dependencies.
     *
     * @param manager The treasure manager for clue operations.
     * @param messageConfig The message configuration for sending formatted messages.
     */
    public CluesCommand(@NotNull TreasureManager manager, @NotNull MessageConfig messageConfig) {
        super(messageConfig, "treasurehunt.clue");
        this.treasureManager = manager;
    }

    /**
     * Executes the clue command with subcommands: create, delete, list, solve.
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
        } catch (IllegalArgumentException e) {
            player.sendMessage(messageConfig.getMessage("error", "%s", e.getMessage()));
        }
        return true;
    }

    /**
     * Creates a new clue for a specified treasure.
     *
     * @param player The player executing the command.
     * @param args The command arguments (expected: create <treasure> <description>).
     */
    private void createClue(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (args.length < 2) {
            player.sendMessage(messageConfig.getMessage("usage-clue-create"));
            return;
        }
        String description = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (description.length() > 100) {
            throw new IllegalArgumentException("Clue description too long (max 100 characters)");
        }
        boolean created = treasureManager.createClue(args[1], description, player.getLocation());
        player.sendMessage(messageConfig.getMessage(created ? "clue-created" : "clue-invalid", "%s", args[1]));
    }

    /**
     * Deletes all clues for a specified treasure.
     *
     * @param player The player executing the command.
     * @param args The command arguments (expected: delete <treasure>).
     */
    private void deleteClues(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (args.length != 2) {
            player.sendMessage(messageConfig.getMessage("usage-clue-delete-error"));
            return;
        }
        boolean deleted = treasureManager.deleteClues(args[1]);
        player.sendMessage(messageConfig.getMessage(deleted ? "clues-deleted" : "treasure-not-found", "%s", args[1]));
    }

    /**
     * Lists all clues for a specified treasure.
     *
     * @param player The player executing the command.
     * @param args The command arguments (expected: list <treasure>).
     */
    private void listClues(@NotNull Player player, @NotNull String @NotNull [] args) {
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
                Clue clue = clues.get(i);
                Location loc = clue.location();
                player.sendMessage(String.format("%d. %s (Difficulty: %s, Location: %.1f, %.1f, %.1f)",
                        i + 1, clue.description(), clue.difficulty(), loc.getX(), loc.getY(), loc.getZ()));
            }
        }
    }

    /**
     * Marks a clue as solved for a specified treasure.
     *
     * @param player The player executing the command.
     * @param args The command arguments (expected: solve <treasure> <description>).
     */
    private void solveClue(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (args.length < 3) {
            player.sendMessage(messageConfig.getMessage("usage-clue-solve"));
            return;
        }
        String description = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        boolean solved = treasureManager.markClueSolved(player, args[1], description);
        player.sendMessage(messageConfig.getMessage(solved ? "clue-solved" : "clue-not-found", "%s", args[1]));
    }

    /**
     * Provides tab completion suggestions for the clue command.
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