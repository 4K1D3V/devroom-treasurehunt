package gg.kite.listeners;

import gg.kite.config.MessageConfig;
import gg.kite.managers.TreasureManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for player movements to detect clue finds.
 */
public class ClueListener implements Listener {
    private final TreasureManager treasureManager;
    private final MessageConfig messageConfig;
    private final double clueFindRadius;

    /**
     * Constructs a ClueListener with dependencies.
     *
     * @param treasureManager The treasure manager.
     * @param messageConfig The message configuration.
     * @param clueFindRadius The radius for detecting clues.
     */
    public ClueListener(@NotNull TreasureManager treasureManager, @NotNull MessageConfig messageConfig, double clueFindRadius) {
        this.treasureManager = treasureManager;
        this.messageConfig = messageConfig;
        this.clueFindRadius = clueFindRadius;
    }

    /**
     * Handles player movement to check for clue finds.
     *
     * @param event The player move event.
     */
    @EventHandler
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return; // Optimize: only check block changes
        var player = event.getPlayer();
        var playerLocation = player.getLocation();

        for (var treasure : treasureManager.getTreasures()) {
            for (var clue : treasure.getClues()) {
                if (isWithinClueRadius(clue.location(), playerLocation)) {
                    if (treasureManager.markClueSolved(player, treasure.getName(), clue.description())) {
                        player.sendMessage(messageConfig.getMessage("clue-solved-auto", "%s", treasure.getName()));
                    }
                }
            }
        }
    }

    /**
     * Checks if a player location is within the clue's find radius.
     *
     * @param clueLocation The clue's location.
     * @param playerLocation The player's location.
     * @return True if within radius.
     */
    private boolean isWithinClueRadius(@NotNull Location clueLocation, @NotNull Location playerLocation) {
        return clueLocation.getWorld().equals(playerLocation.getWorld()) &&
                clueLocation.distance(playerLocation) <= clueFindRadius;
    }
}