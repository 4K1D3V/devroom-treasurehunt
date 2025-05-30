package gg.kite;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import gg.kite.commands.CommandHandler;
import gg.kite.config.MessageConfig;
import gg.kite.listeners.ClueListener;
import gg.kite.listeners.TreasureListener;
import gg.kite.managers.DatabaseManager;
import gg.kite.managers.TreasureManager;
import gg.kite.modules.TreasureHuntModule;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main plugin class for the TreasureHunt plugin, responsible for initialization and lifecycle management.
 */
public class TreasureHunt extends JavaPlugin {
    private Injector injector;
    private MongoClient mongoClient;
    private ScheduledExecutorService scheduler;

    /**
     * Called when the plugin is enabled. Initializes configuration, dependencies, and registers components.
     */
    @Override
    public void onEnable() {
        saveDefaultConfig();
        configureMongoLogging();
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            getLogger().warning("config.yml not found! Creating default config.");
            saveDefaultConfig();
        }

        injector = Guice.createInjector(new TreasureHuntModule(this));
        MessageConfig messageConfig = injector.getInstance(MessageConfig.class);

        try {

            mongoClient = initializeMongoClient();
            DatabaseManager databaseManager = injector.getInstance(DatabaseManager.class);
            TreasureManager treasureManager = injector.getInstance(TreasureManager.class);

            injector.getInstance(CommandHandler.class).registerCommands();

            getServer().getPluginManager().registerEvents(
                    new TreasureListener(treasureManager, messageConfig), this);
            getServer().getPluginManager().registerEvents(
                    new ClueListener(treasureManager, messageConfig, getConfig().getDouble("treasure.clue-find-radius", 3.0)), this);

            scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> treasureManager.cleanupStaleData(), 1, 10, TimeUnit.MINUTES);

            getLogger().info(messageConfig.getMessage("plugin-enabled"));
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize plugin", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void configureMongoLogging() {
        Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
        mongoLogger.setLevel(Level.WARNING);

        for (Handler handler : mongoLogger.getParent().getHandlers()) {
            handler.setLevel(Level.WARNING);
        }
    }

    /**
     * Called when the plugin is disabled. Cleans up resources and connections.
     */
    @Override
    public void onDisable() {
        if (mongoClient != null) {
            mongoClient.close();
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
        MessageConfig messageConfig = injector.getInstance(MessageConfig.class);
        getLogger().info(messageConfig.getMessage("plugin-disabled"));
    }

    /**
     * Initializes the MongoDB client with retry logic.
     *
     * @return The initialized MongoClient.
     * @throws IllegalStateException if connection fails after retries.
     */
    private @NotNull MongoClient initializeMongoClient() {
        String connectionString = getConfig().getString("mongodb.connection-string", "mongodb://localhost:27017");
        int retries = 3;
        for (int i = 0; i < retries; i++) {
            try {
                return MongoClients.create(connectionString);
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "MongoDB connection attempt {0} failed: {1}", new Object[]{i + 1, e.getMessage()});
                if (i < retries - 1) {
                    try {
                        Thread.sleep(1000L * (i + 1)); // Exponential backoff
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
        throw new IllegalStateException("Failed to connect to MongoDB after " + retries + " attempts");
    }

    /**
     * Gets the Guice injector for dependency injection.
     *
     * @return The injector instance.
     */
    public Injector getInjector() {
        return injector;
    }
}