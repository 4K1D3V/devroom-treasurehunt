package gg.kite.managers;

import org.bukkit.Location; /**
 * Represents a clue with a description, location, and difficulty.
 */
public class Clue {
    private final String description;
    private final Location location;
    private final String difficulty;

    public Clue(String description, Location location, String difficulty) {
        this.description = description;
        this.location = location;
        this.difficulty = difficulty.toLowerCase();
    }

    public String getDescription() {
        return description;
    }

    public Location getLocation() {
        return location;
    }

    public String getDifficulty() {
        return difficulty;
    }
}
