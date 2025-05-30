package gg.kite.commands;

import gg.kite.config.MessageConfig;
import gg.kite.managers.TreasureManager;
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
 * Handles hunt-related commands for starting treasure hunts and competitions with tab completion.
 */
public class HuntCommand implements CommandExecutor, TabCompleter {
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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageConfig.getMessage("player-only"));
            return true;
        }
        if (!player.hasPermission("treasurehunt.hunt")) {
            player.sendMessage(messageConfig.getMessage("no-permission"));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(messageConfig.getMessage("usage-hunt-error"));
            return false;
        }

        try {
            switch (args[0].toLowerCase()) {
                case "start" -> treasureManager.startHunt(player);
                case "competition" -> handleCompetition(player, args);
                default -> {
                    player.sendMessage(messageConfig.getMessage("invalid-hunt-command"));
                    return false;
                }
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
    private void handleCompetition(Player player, String @NotNull [] args) {
        if (args.length != 2 || !args[1].equalsIgnoreCase("start")) {
            player.sendMessage(messageConfig.getMessage("usage-hunt-competition"));
            return;
        }
        treasureManager.startCompetition();
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("start", "competition"));
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