package gg.kite.config;

import com.google.inject.Inject;
import gg.kite.TreasureHunt;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Manages configurable messages with color code translation and placeholder support.
 */
public class MessageConfig {
    private final FileConfiguration config;
    private final TreasureHunt plugin;

    /**
     * Constructs a MessageConfig instance with the specified plugin.
     *
     * @param plugin The plugin providing configuration.
     */
    @Inject
    public MessageConfig(@NotNull TreasureHunt plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        if (!config.contains("messages") || config.getConfigurationSection("messages") == null) {
            plugin.getLogger().warning("No 'messages' section found in config.yml.");
        }
    }

    /**
     * Retrieves a message from the configuration with color codes translated.
     *
     * @param key The message key.
     * @return The formatted message with color codes applied.
     */
    public String getMessage(@NotNull String key) {
        String message = config.getString("messages." + key);
        if (message == null) {
            plugin.getLogger().log(Level.WARNING, "Message key '{0}' not found in config.yml.", key);
            return ChatColor.translateAlternateColorCodes('&', "&cMissing message: " + key);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Retrieves a message and replaces placeholders with provided values.
     *
     * @param key The message key.
     * @param replacements Varargs of placeholder and value pairs (e.g., "%s", "value").
     * @return The formatted message with placeholders replaced and color codes applied.
     */
    public String getMessage(@NotNull String key, @NotNull String... replacements) {
        String message = config.getString("messages." + key);
        if (message == null) {
            plugin.getLogger().log(Level.WARNING, "Message key '{0}' not found in config.yml.", key);
            message = "&cMissing message: " + key;
        }
        String finalMessage = message;
        for (int i = 0; i < replacements.length - 1; i += 2) {
            finalMessage = finalMessage.replace(replacements[i], replacements[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', finalMessage);
    }
}