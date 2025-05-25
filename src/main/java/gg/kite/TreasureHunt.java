package gg.kite;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import gg.kite.commands.CluesCommand;
import gg.kite.commands.HuntCommand;
import gg.kite.commands.TeamCommand;
import gg.kite.commands.TreasureCommand;
import gg.kite.config.MessageConfig;
import gg.kite.listeners.TreasureListener;
import gg.kite.managers.DatabaseManager;
import gg.kite.managers.TreasureManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

/**
 * Main plugin class for TreasureHunt, initializing all components and managing lifecycle.
 */
public class TreasureHunt extends JavaPlugin {
    private TreasureManager treasureManager;
    private DatabaseManager databaseManager;
    private MessageConfig messageConfig;
    private MongoClient mongoClient;

    @Override
    public void onEnable() {
        // Initialize configuration
        saveDefaultConfig();
        messageConfig = new MessageConfig(this);

        // Initialize MongoDB
        try {
            String connectionString = getConfig().getString("mongodb.connection-string", "mongodb://localhost:27017");
            mongoClient = MongoClients.create(connectionString);
            databaseManager = new DatabaseManager(this, mongoClient);
            treasureManager = new TreasureManager(databaseManager, getConfig(), messageConfig);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize MongoDB", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register commands
        registerCommands();

        // Register event listeners
        getServer().getPluginManager().registerEvents(new TreasureListener(treasureManager, messageConfig), this);

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
     * Registers all plugin commands.
     */
    private void registerCommands() {
        Objects.requireNonNull(getCommand("treasure")).setExecutor(new TreasureCommand(treasureManager, messageConfig));
        Objects.requireNonNull(getCommand("clue")).setExecutor(new CluesCommand(treasureManager, messageConfig));
        Objects.requireNonNull(getCommand("team")).setExecutor(new TeamCommand(treasureManager, messageConfig));
        Objects.requireNonNull(getCommand("hunt")).setExecutor(new HuntCommand(treasureManager, messageConfig));
    }

    /**
     * Gets the message configuration manager.
     *
     * @return The MessageConfig instance.
     */
    public MessageConfig getMessageConfig() {
        return messageConfig;
    }
}