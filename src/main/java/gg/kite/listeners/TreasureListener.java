package gg.kite.listeners;

import gg.kite.config.MessageConfig;
import gg.kite.managers.TreasureManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for player interactions to detect treasure finds.
 */
public class TreasureListener implements Listener {
    private final TreasureManager treasureManager;
    private final MessageConfig messageConfig;

    /**
     * Constructs a TreasureListener with dependencies.
     *
     * @param treasureManager The treasure manager.
     * @param messageConfig The message configuration.
     */
    public TreasureListener(TreasureManager treasureManager, MessageConfig messageConfig) {
        this.treasureManager = treasureManager;
        this.messageConfig = messageConfig;
    }

    /**
     * Handles player block interactions to check for treasure finds.
     *
     * @param event The player interact event.
     */
    @EventHandler
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        var player = event.getPlayer();
        var clickedLocation = event.getClickedBlock().getLocation();

        for (var treasure : treasureManager.getTreasures()) {
            if (isWithinFindRadius(treasure.getLocation(), clickedLocation)) {
                if (treasureManager.markTreasureFound(player, treasure.getName())) {
                    event.setCancelled(false);
                } else {
                    player.sendMessage(messageConfig.getMessage("no-clue-solved", "%d", String.valueOf(treasureManager.getMinCluesRequired())));
                }
                return;
            }
        }
    }

    /**
     * Checks if a clicked location is within the treasure's find radius.
     *
     * @param treasureLocation The treasure's location.
     * @param clickedLocation The clicked location.
     * @return True if within radius.
     */
    private boolean isWithinFindRadius(@NotNull Location treasureLocation, @NotNull Location clickedLocation) {
        return treasureLocation.getWorld().equals(clickedLocation.getWorld()) &&
                treasureLocation.distance(clickedLocation) <= treasureManager.getTreasureFindRadius();
    }

    /**
     * Gets the minimum number of clues required to find a treasure.
     *
     * @return The minimum clues required.
     */
    public int getMinCluesRequired() {
        return treasureManager.getMinCluesRequired();
    }
}