package gg.kite.commands;

import gg.kite.config.MessageConfig;
import gg.kite.managers.TreasureManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;

/**
 * Handles treasure-related commands.
 */
public class TreasureCommand implements CommandExecutor {
    private final TreasureManager treasureManager;
    private final MessageConfig messageConfig;

    public TreasureCommand(TreasureManager treasureManager, MessageConfig messageConfig) {
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
                case "create" -> createTreasure(player, args);
                case "delete" -> deleteTreasure(player, args);
                case "list" -> listTreasures(player);
                default -> { return false; }
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
            player.sendMessage(messageConfig.getMessage("treasure-list") + treasures.stream()
                    .map(t -> t.getName() + " (Rarity: " + t.getRarity() + ")")
                    .collect(Collectors.joining(", ")));
        }
    }
}