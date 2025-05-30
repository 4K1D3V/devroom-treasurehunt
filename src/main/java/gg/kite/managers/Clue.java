package gg.kite.managers;

import org.bukkit.Location;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a clue with a description, location, and a difficulty level.
 */
public record Clue(@NotNull String description, @NotNull Location location, @NotNull String difficulty, long createdAt) {
    /**
     * Constructs a Clue with the specified parameters, using the current timestamp.
     *
     * @param description The clue's description.
     * @param location The clue's location in the world.
     * @param difficulty The clue's difficulty (e.g., easy, medium, hard).
     */
    @Contract(pure = true)
    public Clue(@NotNull String description, @NotNull Location location, @NotNull String difficulty) {
        this(description, location, difficulty.toLowerCase(), System.currentTimeMillis());
    }
}