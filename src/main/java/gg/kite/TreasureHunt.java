package gg.kite;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import gg.kite.commands.CluesCommand;
import gg.kite.commands.HuntCommand;
import gg.kite.commands.TeamCommand;
import gg.kite.commands.TreasureCommand;
import gg.kite.config.MessageConfig;
import gg.kite.listeners.ClueListener;
import gg.kite.listeners.TreasureListener;
import gg.kite.managers.DatabaseManager;
import gg.kite.managers.TreasureManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;
import java.util.logging.Level;

/**
 * Main plugin class for TreasureHunt, initializing all components and managing lifecycle.
 */
public class TreasureHunt extends JavaPlugin {
    private TreasureManager treasureManager;
    private MessageConfig messageConfig;
    private MongoClient mongoClient;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            getLogger().warning("config.yml not found! Creating default config.");
            saveDefaultConfig();
        }
        messageConfig = new MessageConfig(this);

        try {
            String connectionString = getConfig().getString("mongodb.connection-string", "mongodb://localhost:27017");
            mongoClient = MongoClients.create(connectionString);
            DatabaseManager databaseManager = new DatabaseManager(this, mongoClient);
            treasureManager = new TreasureManager(databaseManager, getConfig(), messageConfig);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize MongoDB", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        registerCommands();

        getServer().getPluginManager().registerEvents(new TreasureListener(treasureManager, messageConfig), this);
        getServer().getPluginManager().registerEvents(new ClueListener(treasureManager, messageConfig,
                getConfig().getDouble("treasure.clue-find-radius", 3.0)), this);

        getLogger().info(messageConfig.getMessage("plugin-enabled"));
    }

    @Override
    public void onDisable() {
        if (mongoClient != null) {
            mongoClient.close();
        }
        getLogger().info(messageConfig.getMessage("plugin-disabled"));
    }

    /**
     * Registers all plugin commands and tab completer.
     */
    private void registerCommands() {
        CluesCommand cluesCommand = new CluesCommand(treasureManager, messageConfig);
        TreasureCommand treasureCommand = new TreasureCommand(treasureManager, messageConfig);
        HuntCommand huntCommand = new HuntCommand(treasureManager, messageConfig);
        TeamCommand teamCommand = new TeamCommand(treasureManager, messageConfig);
        Objects.requireNonNull(getCommand("treasure")).setExecutor(treasureCommand);
        Objects.requireNonNull(getCommand("treasure")).setTabCompleter(treasureCommand);
        Objects.requireNonNull(getCommand("clue")).setExecutor(cluesCommand);
        Objects.requireNonNull(getCommand("clue")).setTabCompleter(cluesCommand);
        Objects.requireNonNull(getCommand("team")).setExecutor(teamCommand);
        Objects.requireNonNull(getCommand("team")).setTabCompleter(teamCommand);
        Objects.requireNonNull(getCommand("hunt")).setExecutor(huntCommand);
        Objects.requireNonNull(getCommand("hunt")).setTabCompleter(huntCommand);
    }
}