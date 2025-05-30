package gg.kite.commands;

import com.google.inject.Inject;
import gg.kite.config.MessageConfig;
import gg.kite.managers.TreasureManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized handler for registering and managing plugin commands.
 */
public class CommandHandler {
    private final JavaPlugin plugin;
    private final TreasureManager treasureManager;
    private final MessageConfig messageConfig;
    private final Map<String, CommandExecutor> commands;
    private final Map<String, TabCompleter> tabCompleters;

    /**
     * Constructs a CommandHandler with the specified dependencies.
     *
     * @param plugin The plugin instance.
     * @param treasureManager The treasure manager for command operations.
     * @param messageConfig The message configuration for command messages.
     */
    @Inject
    public CommandHandler(@NotNull JavaPlugin plugin, @NotNull TreasureManager treasureManager, @NotNull MessageConfig messageConfig) {
        this.plugin = plugin;
        this.treasureManager = treasureManager;
        this.messageConfig = messageConfig;
        this.commands = new HashMap<>();
        this.tabCompleters = new HashMap<>();
        registerCommand("treasure", new TreasureCommand(treasureManager, messageConfig));
        registerCommand("clue", new CluesCommand(treasureManager, messageConfig));
        registerCommand("team", new TeamCommand(treasureManager, messageConfig));
        registerCommand("hunt", new HuntCommand(treasureManager, messageConfig));
    }

    /**
     * Registers a command with its executor and optional tab completer.
     *
     * @param name The command name.
     * @param executor The command executor.
     */
    private void registerCommand(@NotNull String name, @NotNull CommandExecutor executor) {
        commands.put(name, executor);
        if (executor instanceof TabCompleter tabCompleter) {
            tabCompleters.put(name, tabCompleter);
        }
    }

    /**
     * Registers all commands with the plugin.
     */
    public void registerCommands() {
        commands.forEach((name, executor) -> {
            PluginCommand command = plugin.getCommand(name);
            if (command != null) {
                command.setExecutor(executor);
                command.setTabCompleter(tabCompleters.get(name));
            } else {
                plugin.getLogger().warning("Failed to register command: " + name);
            }
        });
    }
}