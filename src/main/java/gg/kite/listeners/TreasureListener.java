package gg.kite.listeners;

import gg.kite.config.MessageConfig;
import gg.kite.managers.TreasureManager;
// import com.sk89q.worldguard.WorldGuard; // Uncomment for WorldGuard integration
// import com.sk89q.worldguard.protection.flags.Flags;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Listener for player interactions to detect treasure discoveries.
 */
public class TreasureListener implements Listener {
    private final TreasureManager treasureManager;
    private final MessageConfig messageConfig;

    /**
     * Constructs a TreasureListener with the specified dependencies.
     *
     * @param treasureManager The treasure manager for treasure operations.
     * @param messageConfig The message configuration for sending formatted messages.
     */
    public TreasureListener(@NotNull TreasureManager treasureManager, @NotNull MessageConfig messageConfig) {
        this.treasureManager = treasureManager;
        this.messageConfig = messageConfig;
    }

    /**
     * Handles player block interactions to check for treasure discoveries.
     *
     * @param event The player interact event.
     */
    @EventHandler
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        var player = event.getPlayer();
        var clickedLocation = event.getClickedBlock().getLocation();

        // Optional: WorldGuard integration
        /*
        if (WorldGuard.getInstance() != null) {
            var localPlayer = WorldGuard.getInstance().getPlatform().getSessionManager().get(player);
            var regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
            var query = regionContainer.createQuery();
            if (!query.testState(localPlayer, clickedLocation, Flags.BUILD)) {
                player.sendMessage(messageConfig.getMessage("protected-region"));
                event.setCancelled(true);
                return;
            }
        }
        */

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
     * Checks if a clicked location is within the treasure's detection radius.
     *
     * @param treasureLocation The treasure's location.
     * @param clickedLocation The clicked block's location.
     * @return True if the clicked location is within the treasure's find radius, false otherwise.
     */
    private boolean isWithinFindRadius(@NotNull Location treasureLocation, @NotNull Location clickedLocation) {
        return treasureLocation.getWorld().equals(clickedLocation.getWorld()) &&
                treasureLocation.distanceSquared(clickedLocation) <= treasureManager.getTreasureFindRadius() * treasureManager.getTreasureFindRadius();
    }
}