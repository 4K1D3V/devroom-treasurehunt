package gg.kite.managers;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a treasure with a location and rarity.
 */
public class Treasure {
    private final String name;
    private final Location location;
    private final int rarity;
    private final List<Clue> clues;

    public Treasure(String name, Location location, int rarity) {
        this.name = name;
        this.location = location;
        this.rarity = Math.max(1, rarity);
        this.clues = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public int getRarity() {
        return rarity;
    }

    public List<Clue> getClues() {
        return new ArrayList<>(clues);
    }

    public void addClue(Clue clue) {
        clues.add(clue);
    }

    public void clearClues() {
        clues.clear();
    }
}