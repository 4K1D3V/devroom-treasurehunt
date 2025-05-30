package gg.kite.managers;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a treasure with a name, location, rarity, and associated clues.
 */
public class Treasure {
    private final String name;
    private final Location location;
    private final int rarity;
    private final List<Clue> clues;
    private final long createdAt;

    /**
     * Constructs a Treasure with the specified parameters.
     *
     * @param name The unique name of the treasure.
     * @param location The location of the treasure in the world.
     * @param rarity The rarity level of the treasure (minimum 1).
     */
    public Treasure(@NotNull String name, @NotNull Location location, int rarity) {
        this.name = name;
        this.location = location;
        this.rarity = Math.max(1, rarity);
        this.clues = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Gets the treasure name.
     *
     * @return The treasure name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the treasure location.
     *
     * @return The treasure location.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Gets the treasure rarity.
     *
     * @return The rarity value.
     */
    public int getRarity() {
        return rarity;
    }

    /**
     * Gets a copy of the treasure's clues.
     *
     * @return A list of clues associated with the treasure.
     */
    public List<Clue> getClues() {
        return new ArrayList<>(clues);
    }

    /**
     * Adds a clue to the treasure.
     *
     * @param clue The clue to add.
     */
    public void addClue(@NotNull Clue clue) {
        clues.add(clue);
    }

    /**
     * Clears all clues associated with the treasure.
     */
    public void clearClues() {
        clues.clear();
    }

    /**
     * Gets the creation timestamp of the treasure.
     *
     * @return The creation timestamp in milliseconds.
     */
    public long getCreatedAt() {
        return createdAt;
    }
}