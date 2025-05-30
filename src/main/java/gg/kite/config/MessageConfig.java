package gg.kite.config;

import gg.kite.TreasureHunt;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Manages configurable messages for the plugin.
 */
public class MessageConfig {
    private final FileConfiguration config;
    private final TreasureHunt plugin;

    /**
     * Constructs a MessageConfig instance.
     *
     * @param plugin The plugin instance.
     */
    public MessageConfig(@NotNull TreasureHunt plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        // Validate messages section
        if (!config.contains("messages") || config.getConfigurationSection("messages") == null) {
            plugin.getLogger().warning("No 'messages' section found in config.yml. Commands may show missing message errors.");
        }
    }

    /**
     * Retrieves a message from the configuration, with a fallback if not found.
     *
     * @param key The message key.
     * @return The formatted message.
     */
    public String getMessage(String key) {
        String message = config.getString("messages." + key);
        if (message == null) {
            plugin.getLogger().log(Level.WARNING, "Message key '{0}' not found in config.yml.", key);
            return "&cMissing message: " + key;
        }
        return message;
    }

    /**
     * Retrieves a message and replaces placeholders.
     *
     * @param key The message key.
     * @param replacements Varargs of placeholder and value pairs.
     * @return The formatted message with replacements.
     */
    public String getMessage(String key, String @NotNull ... replacements) {
        String message = config.getString("messages." + key);
        if (message == null) {
            plugin.getLogger().log(Level.WARNING, "Message key '{0}' not found in config.yml.", key);
            message = "&cMissing message: " + key;
        }
        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return message;
    }
}