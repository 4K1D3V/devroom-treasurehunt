package gg.kite.commands;

import gg.kite.config.MessageConfig;
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

/**
 * Handles treasure-related commands with tab completion.
 */
public class TreasureCommand implements CommandExecutor, TabCompleter {
    private final TreasureManager treasureManager;
    private final MessageConfig messageConfig;

    public TreasureCommand(TreasureManager treasureManager, MessageConfig messageConfig) {
        this.treasureManager = treasureManager;
        this.messageConfig = messageConfig;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageConfig.getMessage("player-only"));
            return true;
        }
        if (!player.hasPermission("treasurehunt.treasure")) {
            player.sendMessage(messageConfig.getMessage("no-permission"));
            return true;
        }
        if (args.length == 0) {
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
        } catch (Exception e) {
            player.sendMessage(messageConfig.getMessage("error", "%s", e.getMessage()));
        }
        return true;
    }

    private void createTreasure(Player player, String[] args) {
        if (args.length != 3) {
            player.sendMessage(messageConfig.getMessage("usage-treasure-create"));
            return;
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

    private void deleteTreasure(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(messageConfig.getMessage("usage-treasure-delete"));
            return;
        }
        boolean deleted = treasureManager.deleteTreasure(args[1]);
        player.sendMessage(messageConfig.getMessage(deleted ? "treasure-deleted" : "treasure-not-found", "%s", args[1]));
    }

    private void listTreasures(Player player) {
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

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
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