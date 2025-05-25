package gg.kite.commands;

import gg.kite.config.MessageConfig;
import gg.kite.managers.TreasureManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles hunt-related commands for starting treasure hunts and competitions.
 */
public class HuntCommand implements CommandExecutor {
    private final TreasureManager treasureManager;
    private final MessageConfig messageConfig;

    /**
     * Constructs a HuntCommand with dependencies.
     *
     * @param treasureManager The treasure manager.
     * @param messageConfig The message configuration.
     */
    public HuntCommand(TreasureManager treasureManager, MessageConfig messageConfig) {
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
                case "start" -> treasureManager.startHunt(player);
                case "competition" -> handleCompetition(player, args);
                default -> { return false; }
            }
        } catch (Exception e) {
            player.sendMessage(messageConfig.getMessage("error", "%s", e.getMessage()));
        }
        return true;
    }

    /**
     * Handles the competition subcommand.
     *
     * @param player The player issuing the command.
     * @param args The command arguments.
     */
    private void handleCompetition(Player player, String[] args) {
        if (args.length != 2 || !args[1].equalsIgnoreCase("start")) {
            player.sendMessage(messageConfig.getMessage("usage-hunt-competition"));
            return;
        }
        treasureManager.startCompetition();
    }
}