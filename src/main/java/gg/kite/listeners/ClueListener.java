package gg.kite.listeners;

import gg.kite.config.MessageConfig;
import gg.kite.managers.Clue;
import gg.kite.managers.TreasureManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener for player movements to detect clue discoveries within a configurable radius.
 */
public class ClueListener implements Listener {
    private final TreasureManager treasureManager;
    private final MessageConfig messageConfig;
    private final double clueFindRadius;
    private final ConcurrentHashMap<Location, ClueData> spatialIndex;

    /**
     * Constructs a ClueListener with the specified dependencies.
     *
     * @param treasureManager The treasure manager for clue operations.
     * @param messageConfig The message configuration for sending formatted messages.
     * @param clueFindRadius The radius within which clues are detected.
     */
    public ClueListener(@NotNull TreasureManager treasureManager, @NotNull MessageConfig messageConfig, double clueFindRadius) {
        this.treasureManager = treasureManager;
        this.messageConfig = messageConfig;
        this.clueFindRadius = clueFindRadius;
        this.spatialIndex = new ConcurrentHashMap<>();
        buildSpatialIndex();
    }

    /**
     * Builds a spatial index of clues for efficient lookup during player movement.
     */
    private void buildSpatialIndex() {
        treasureManager.getTreasures().forEach(treasure ->
                treasure.getClues().forEach(clue ->
                        spatialIndex.put(clue.location(), new ClueData(treasure.getName(), clue))));
    }

    /**
     * Handles player movement events to detect nearby clues.
     *
     * @param event The player move event.
     */
    @EventHandler
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        var player = event.getPlayer();
        var playerLocation = player.getLocation();

        spatialIndex.forEach((clueLocation, clueData) -> {
            if (isWithinClueRadius(clueLocation, playerLocation)) {
                if (treasureManager.markClueSolved(player, clueData.treasureName, clueData.clue.description())) {
                    player.sendMessage(messageConfig.getMessage("clue-solved-auto", "%s", clueData.treasureName));
                }
            }
        });
    }

    /**
     * Checks if a player is within the detection radius of a clue.
     *
     * @param clueLocation The location of the clue.
     * @param playerLocation The player's current location.
     * @return True if the player is within the clue's detection radius, false otherwise.
     */
    private boolean isWithinClueRadius(@NotNull Location clueLocation, @NotNull Location playerLocation) {
        return clueLocation.getWorld().equals(playerLocation.getWorld()) &&
                clueLocation.distanceSquared(playerLocation) <= clueFindRadius * clueFindRadius;
    }

    /**
     * Record to store clue data for the spatial index.
     *
     * @param treasureName The name of the associated treasure.
     * @param clue The clue instance.
     */
    private record ClueData(String treasureName, Clue clue) {}
}