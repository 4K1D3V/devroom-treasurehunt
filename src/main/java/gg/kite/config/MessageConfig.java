package gg.kite.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manages configurable messages for the plugin.
 */
public class MessageConfig {
    private final FileConfiguration config;

    /**
     * Constructs a MessageConfig instance.
     *
     * @param plugin The plugin instance.
     */
    public MessageConfig(JavaPlugin plugin) {
        this.config = plugin.getConfig();
    }

    /**
     * Retrieves a message from the configuration, with a default if not found.
     *
     * @param key The message key.
     * @return The formatted message.
     */
    public String getMessage(String key) {
        return config.getString("messages." + key, "Message not found: " + key);
    }

    /**
     * Retrieves a message and replaces placeholders.
     *
     * @param key The message key.
     * @param replacements Varargs of placeholder and value pairs.
     * @return The formatted message with replacements.
     */
    public String getMessage(String key, String... replacements) {
        String message = getMessage(key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return message;
    }
}