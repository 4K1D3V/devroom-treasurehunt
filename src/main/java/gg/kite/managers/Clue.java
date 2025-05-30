package gg.kite.managers;

import org.bukkit.Location;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a clue with a description, location, and difficulty.
 */
public record Clue(String description, Location location, String difficulty) {
    @Contract(pure = true)
    public Clue(String description, Location location, @NotNull String difficulty) {
        this.description = description;
        this.location = location;
        this.difficulty = difficulty.toLowerCase();
    }
}