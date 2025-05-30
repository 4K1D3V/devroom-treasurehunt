package gg.kite.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import gg.kite.TreasureHunt;
import gg.kite.commands.CommandHandler;
import gg.kite.config.MessageConfig;
import gg.kite.managers.DatabaseManager;
import gg.kite.managers.TreasureManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Guice module for configuring dependency injection in the TreasureHunt plugin.
 */
public class TreasureHuntModule extends AbstractModule {
    private final TreasureHunt plugin;

    /**
     * Constructs a TreasureHuntModule with the specified plugin instance.
     *
     * @param plugin The plugin instance providing configuration and context.
     */
    public TreasureHuntModule(@NotNull TreasureHunt plugin) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin must not be null");
    }

    /**
     * Configures bindings for dependency injection.
     */
    @Override
    protected void configure() {
        bind(JavaPlugin.class).toInstance(plugin);
        bind(TreasureHunt.class).toInstance(plugin);
        bind(FileConfiguration.class).toProvider(() -> plugin.getConfig());
        bind(MessageConfig.class).in(Scopes.SINGLETON);
        bind(DatabaseManager.class).in(Scopes.SINGLETON);
        bind(TreasureManager.class).in(Scopes.SINGLETON);
        bind(CommandHandler.class).in(Scopes.SINGLETON);
    }

    /**
     * Provides a MongoDB client instance for database operations.
     *
     * @param plugin The plugin instance to access configuration.
     * @return The MongoDB client instance.
     */
    @Provides
    @NotNull
    public MongoClient provideMongoClient(@NotNull TreasureHunt plugin) {
        String connectionString = plugin.getConfig().getString("mongodb.connection-string", "mongodb://localhost:27017");
        return MongoClients.create(connectionString);
    }
}