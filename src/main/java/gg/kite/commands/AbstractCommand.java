package gg.kite.commands;

import gg.kite.config.MessageConfig;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class for command executors, providing common functionality like permission and player checks.
 */
public abstract class AbstractCommand {
    protected final MessageConfig messageConfig;
    protected final String permission;

    /**
     * Constructs an AbstractCommand with the given message configuration and permission.
     *
     * @param messageConfig The message configuration for sending formatted messages.
     * @param permission The permission node required to execute the command.
     */
    public AbstractCommand(@NotNull MessageConfig messageConfig, @NotNull String permission) {
        this.messageConfig = messageConfig;
        this.permission = permission;
    }

    /**
     * Checks if the sender is a player and has the required permission.
     *
     * @param sender The command sender to validate.
     * @return True if the sender is a player with the required permission, false otherwise.
     */
    protected boolean checkSender(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageConfig.getMessage("player-only"));
            return false;
        }
        if (!player.hasPermission(permission)) {
            player.sendMessage(messageConfig.getMessage("no-permission"));
            return false;
        }
        return true;
    }
}