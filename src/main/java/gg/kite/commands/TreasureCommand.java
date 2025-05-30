package gg.kite.commands;

import gg.kite.config.MessageConfig;
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

/**
 * Command executor for managing treasures with tab completion support.
 */
public class TreasureCommand extends AbstractCommand implements TabExecutor {
    private final TreasureManager treasureManager;

    /**
     * Constructs a TreasureCommand with the specified dependencies.
     *
     * @param treasureManager The treasure manager for treasure operations.
     * @param messageConfig The message configuration for command messages.
     */
    public TreasureCommand(@NotNull TreasureManager treasureManager, @NotNull MessageConfig messageConfig) {
        super(messageConfig, "treasurehunt.treasure");
        this.treasureManager = treasureManager;
    }

    /**
     * Executes the treasure command with subcommands: create, delete, list.
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

        if (args.length == 1) {
            player.sendMessage(messageConfig.getMessage("usage-treasure-error"));
            return false;
        }

        try {
            switch (args[0].toLowerCase()) {
                case "create" -> createTreasure(player, args);
                case "delete" -> deleteTreasure(player, args);
                case "list" -> listTreasures(player);
                default -> {
                    player.sendMessage(messageConfig.getMessage("invalid-treasure-command"));
                    return false;
                }
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(messageConfig.getMessage("error", "%s", e.getMessage()));
        }
        return true;
    }

    /**
     * Creates a new treasure at the player's location.
     *
     * @param player The player executing the command.
     * @param args The command arguments (expected: create <name> <rarity>).
     */
    private void createTreasure(@NotNull Player player, @NotNull String[] args) {
        if (args.length != 3) {
            player.sendMessage(messageConfig.getMessage("usage-treasure-create"));
            return;
        }
        if (args[1].length() > 32 || !args[1].matches("[a-zA-Z0-9_-]+")) {
            throw new IllegalArgumentException("Treasure name must be alphanumeric and up to 32 characters");
        }
        int rarity;
        try {
            rarity = Integer.parseInt(args[2]);
            if (rarity < 1) {
                player.sendMessage(messageConfig.getMessage("invalid-rarity"));
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(messageConfig.getMessage("invalid-rarity-format"));
            return;
        }
        boolean created = treasureManager.createTreasure(args[1], player.getLocation(), rarity);
        player.sendMessage(messageConfig.getMessage(created ? "treasure-created" : "treasure-exists",
                "%s", args[1], "%r", String.valueOf(rarity)));
    }

    /**
     * Deletes a specified treasure.
     *
     * @param player The player executing the command.
     * @param args The command arguments (expected: delete <name>).
     */
    private void deleteTreasure(@NotNull Player player, @NotNull String[] args) {
        if (args.length != 2) {
            player.sendMessage(messageConfig.getMessage("usage-treasure-delete"));
            return;
        }
        boolean deleted = treasureManager.deleteTreasure(args[1]);
        player.sendMessage(messageConfig.getMessage(deleted ? "treasure-deleted" : "treasure-not-found", "%s", args[1]));
    }

    /**
     * Lists all available treasures.
     *
     * @param player The player executing the command.
     */
    private void listTreasures(@NotNull Player player) {
        var treasures = treasureManager.getTreasures();
        if (treasures.isEmpty()) {
            player.sendMessage(messageConfig.getMessage("no-treasures"));
        } else {
            player.sendMessage(messageConfig.getMessage("treasure-list"));
            for (var t : treasures) {
                Location loc = t.getLocation();
                int clueCount = t.getClues().size();
                player.sendMessage(String.format("%s (Rarity: %d, Clues: %d, Location: %.1f, %.1f, %.1f)",
                        t.getName(), t.getRarity(), clueCount, loc.getX(), loc.getY(), loc.getZ()));
            }
        }
    }

    /**
     * Provides tab completion suggestions for the treasure command.
     *
     * @param sender The command sender.
     * @param command The command instance.
     * @param label The command alias.
     * @param args The command arguments.
     * @return A list of tab completion suggestions.
     */
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "delete", "list"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            completions.addAll(treasureManager.getTreasures().stream()
                    .map(Treasure::getName)
                    .toList());
        }
        if (args.length > 0) {
            String lastArg = args[args.length - 1].toLowerCase();
            completions = completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(lastArg))
                    .toList();
        }
        return completions;
    }
}