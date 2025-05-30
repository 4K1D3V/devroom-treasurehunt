package gg.kite.managers;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import gg.kite.config.MessageConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Manages treasures, clues, teams, and game state with thread-safe operations.
 */
public class TreasureManager {
    private final DatabaseManager databaseManager;
    private final MessageConfig messageConfig;
    private final FileConfiguration config;
    private final ConcurrentHashMap<String, Treasure> treasures;
    private final ConcurrentHashMap<String, Team> teams;
    private final ConcurrentHashMap<UUID, Set<String>> playerProgress;
    private final ConcurrentHashMap<UUID, Set<String>> clueProgress;
    private final Cache<UUID, Long> commandCooldowns;
    private volatile boolean competitionActive;
    private final Random random;
    private final double maxClueDistance;
    private final String clueDifficulty;
    private final int minCluesRequired;
    private final double treasureFindRadius;
    private final int maxTreasuresPerHunt;
    private final Map<Integer, List<ItemStack>> rewardItems;

    /**
     * Constructs a TreasureManager with the specified dependencies and configuration.
     *
     * @param databaseManager The database manager for persistence.
     * @param config The plugin configuration.
     * @param messageConfig The message configuration for sending formatted messages.
     */
    @Inject
    public TreasureManager(@NotNull DatabaseManager databaseManager, @NotNull FileConfiguration config, @NotNull MessageConfig messageConfig) {
        this.databaseManager = databaseManager;
        this.config = config;
        this.messageConfig = messageConfig;
        this.treasures = new ConcurrentHashMap<>();
        this.teams = new ConcurrentHashMap<>();
        this.playerProgress = new ConcurrentHashMap<>();
        this.clueProgress = new ConcurrentHashMap<>();
        this.commandCooldowns = CacheBuilder.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .build();
        this.random = new Random();
        this.maxClueDistance = config.getDouble("treasure.max-clue-distance", 100.0);
        this.clueDifficulty = config.getString("treasure.clue-difficulty", "medium").toLowerCase();
        this.minCluesRequired = config.getInt("treasure.min-clues-required", 1);
        this.treasureFindRadius = config.getDouble("treasure.find-radius", 2.0);
        this.maxTreasuresPerHunt = config.getInt("treasure.max-treasures-per-hunt", 5);
        this.rewardItems = loadRewards(config);
        validateConfig();
        loadTreasures();
        loadTeams();
    }

    /**
     * Validates configuration values to ensure they are within acceptable ranges.
     *
     * @throws IllegalArgumentException if configuration values are invalid.
     */
    private void validateConfig() {
        if (treasureFindRadius <= 0 || maxClueDistance <= 0 || minCluesRequired < 0 || maxTreasuresPerHunt <= 0) {
            throw new IllegalArgumentException("Invalid configuration values in config.yml");
        }
    }

    /**
     * Loads reward items from the configuration.
     *
     * @param config The plugin configuration.
     * @return A map of rarity levels to lists of reward items.
     */
    private Map<Integer, List<ItemStack>> loadRewards(@NotNull FileConfiguration config) {
        Map<Integer, List<ItemStack>> rewards = new HashMap<>();
        // Placeholder for reward loading from config
        // Example: rewards.put(1, List.of(new ItemStack(Material.DIAMOND, 1)));
        return rewards;
    }

    /**
     * Cleans up stale data, such as empty teams, and saves to the database.
     */
    public void cleanupStaleData() {
        teams.entrySet().removeIf(entry -> entry.getValue().getMembers().isEmpty());
        teams.forEach((name, team) -> databaseManager.saveTeam(team));
    }

    /**
     * Creates a new treasure with the specified parameters.
     *
     * @param name The treasure name.
     * @param location The treasure location.
     * @param rarity The treasure rarity.
     * @return True if created successfully, false if name exists or invalid.
     */
    public boolean createTreasure(@NotNull String name, @NotNull Location location, int rarity) {
        if (name.length() > 32 || !name.matches("[a-zA-Z0-9_-]+") || location.getWorld() == null) {
            return false;
        }
        return treasures.computeIfAbsent(name, k -> {
            Treasure treasure = new Treasure(name, location, Math.max(1, rarity));
            databaseManager.saveTreasure(treasure);
            return treasure;
        }) != null;
    }

    /**
     * Deletes a treasure by name.
     *
     * @param name The treasure name.
     * @return True if deleted, false if not found.
     */
    public boolean deleteTreasure(@NotNull String name) {
        Treasure removed = treasures.remove(name);
        if (removed != null) {
            databaseManager.deleteTreasure(name);
            return true;
        }
        return false;
    }

    /**
     * Gets all treasures.
     *
     * @return A list of all treasures.
     */
    public List<Treasure> getTreasures() {
        return new ArrayList<>(treasures.values());
    }

    /**
     * Creates a clue for a specified treasure.
     *
     * @param treasureName The treasure name.
     * @param description The clue description.
     * @param location The clue location.
     * @return True if created successfully, false if invalid.
     */
    public boolean createClue(@NotNull String treasureName, @NotNull String description, @NotNull Location location) {
        var treasure = treasures.get(treasureName);
        if (treasure == null || !isValidClueLocation(treasure, location)) {
            return false;
        }
        var clue = new Clue(description, location, clueDifficulty);
        treasure.addClue(clue);
        databaseManager.saveClue(treasureName, clue);
        return true;
    }

    /**
     * Validates a clue's location relative to its treasure.
     *
     * @param treasure The treasure.
     * @param location The clue location.
     * @return True if the clue location is valid, false otherwise.
     */
    private boolean isValidClueLocation(@NotNull Treasure treasure, @NotNull Location location) {
        return location.getWorld() != null &&
                location.getWorld().equals(treasure.getLocation().getWorld()) &&
                location.distance(treasure.getLocation()) <= maxClueDistance;
    }

    /**
     * Deletes all clues for a specified treasure.
     *
     * @param treasureName The treasure name.
     * @return True if deleted, false if treasure not found.
     */
    public boolean deleteClues(@NotNull String treasureName) {
        var treasure = treasures.get(treasureName);
        if (treasure == null) {
            return false;
        }
        treasure.clearClues();
        databaseManager.deleteClues(treasureName);
        return true;
    }

    /**
     * Gets all clues for a specified treasure.
     *
     * @param treasureName The treasure name.
     * @return A list of clues, or empty list if treasure not found.
     */
    public List<Clue> getClues(@NotNull String treasureName) {
        var treasure = treasures.get(treasureName);
        return treasure != null ? treasure.getClues() : List.of();
    }

    /**
     * Marks a clue as solved by a player.
     *
     * @param player The player solving the clue.
     * @param treasureName The treasure name.
     * @param clueDescription The clue description.
     * @return True if newly solved, false if already solved or invalid.
     */
    public boolean markClueSolved(@NotNull Player player, @NotNull String treasureName, @NotNull String clueDescription) {
        var treasure = treasures.get(treasureName);
        if (treasure == null || treasure.getClues().stream().noneMatch(c -> c.description().equals(clueDescription))) {
            return false;
        }
        String clueId = treasureName + ":" + clueDescription;
        Set<String> playerClues = clueProgress.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
        if (!playerClues.add(clueId)) {
            return false;
        }
        databaseManager.saveClueProgress(player.getUniqueId(), treasureName, clueDescription);
        return true;
    }

    /**
     * Marks a treasure as found by a player, awarding rewards if applicable.
     *
     * @param player The player finding the treasure.
     * @param treasureName The treasure name.
     * @return True if found successfully, false if conditions not met.
     */
    public boolean markTreasureFound(@NotNull Player player, @NotNull String treasureName) {
        var treasure = treasures.get(treasureName);
        if (treasure == null) return false;

        Set<String> playerClues = clueProgress.getOrDefault(player.getUniqueId(), Set.of());
        long solvedClues = playerClues.stream()
                .filter(clueId -> clueId.startsWith(treasureName + ":"))
                .count();
        if (solvedClues < minCluesRequired) {
            return false;
        }

        Set<String> playerTreasures = playerProgress.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
        if (!playerTreasures.add(treasureName)) {
            return false;
        }

        databaseManager.savePlayerProgress(player.getUniqueId(), treasureName);
        awardRewards(player, treasure);
        updateTeamScore(player);
        player.sendMessage(messageConfig.getMessage("treasure-found", "%s", treasureName));
        return true;
    }

    /**
     * Awards rewards to a player based on treasure rarity.
     *
     * @param player The player receiving rewards.
     * @param treasure The treasure found.
     */
    private void awardRewards(@NotNull Player player, @NotNull Treasure treasure) {
        List<ItemStack> rewards = rewardItems.getOrDefault(treasure.getRarity(), List.of());
        if (!rewards.isEmpty()) {
            ItemStack reward = rewards.get(random.nextInt(rewards.size()));
            player.getInventory().addItem(reward);
            player.sendMessage(messageConfig.getMessage("reward-received", "%s", reward.getType().name()));
        }
    }

    /**
     * Updates the score of the player's team, if any.
     *
     * @param player The player whose team score should be updated.
     */
    private void updateTeamScore(@NotNull Player player) {
        teams.values().stream()
                .filter(team -> team.getMembers().contains(player.getUniqueId()))
                .findFirst()
                .ifPresent(team -> {
                    team.incrementScore();
                    databaseManager.saveTeam(team);
                });
    }

    /**
     * Creates a new team.
     *
     * @param name The team name.
     * @param player The player creating the team.
     * @return True if created, false if name exists.
     */
    public boolean createTeam(@NotNull String name, @NotNull Player player) {
        return teams.computeIfAbsent(name, k -> {
            Team team = new Team(name, player.getUniqueId());
            databaseManager.saveTeam(team);
            return team;
        }) != null;
    }

    /**
     * Invites a player to a team.
     *
     * @param teamName The team name.
     * @param invited The player to invite.
     * @return True if invited successfully, false otherwise.
     */
    public boolean invitePlayer(@NotNull String teamName, @NotNull Player invited) {
        Team team = teams.get(teamName);
        if (team == null) return false;
        team.addMember(invited.getUniqueId());
        databaseManager.saveTeam(team);
        return true;
    }

    /**
     * Kicks a player from a team.
     *
     * @param teamName The team name.
     * @param kicked The player to kick.
     * @return True if kicked successfully, false otherwise.
     */
    public boolean kickPlayer(@NotNull String teamName, @NotNull Player kicked) {
        Team team = teams.get(teamName);
        if (team == null) return false;
        team.removeMember(kicked.getUniqueId());
        databaseManager.saveTeam(team);
        return true;
    }

    /**
     * Gets all teams.
     *
     * @return A list of all teams.
     */
    public List<Team> getTeams() {
        return new ArrayList<>(teams.values());
    }

    /**
     * Starts a treasure hunt for a player.
     *
     * @param player The player starting the hunt.
     */
    public void startHunt(@NotNull Player player) {
        Long lastUsed = commandCooldowns.getIfPresent(player.getUniqueId());
        if (lastUsed != null && System.currentTimeMillis() - lastUsed < 30_000) {
            player.sendMessage(messageConfig.getMessage("cooldown-active"));
            return;
        }
        commandCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        List<Treasure> available = getTreasures().stream()
                .filter(t -> !playerProgress.getOrDefault(player.getUniqueId(), Set.of()).contains(t.getName()))
                .limit(maxTreasuresPerHunt)
                .toList();
        if (available.isEmpty()) {
            player.sendMessage(messageConfig.getMessage("no-treasures-available"));
        } else {
            player.sendMessage(messageConfig.getMessage("hunt-started", "%d", String.valueOf(available.size())));
        }
    }

    /**
     * Starts a competition among all teams.
     */
    public void startCompetition() {
        if (competitionActive) return;
        competitionActive = true;
        teams.values().forEach(Team::resetScore);
        Bukkit.broadcastMessage(messageConfig.getMessage("competition-started"));
    }

    /**
     * Gets the leaderboard of top teams by score.
     *
     * @return A sorted list of teams.
     */
    public List<Team> getLeaderboard() {
        return teams.values().stream()
                .sorted(Comparator.comparingInt(Team::getScore).reversed())
                .limit(10)
                .toList();
    }

    /**
     * Gets the treasure find radius.
     *
     * @return The treasure find radius.
     */
    public double getTreasureFindRadius() {
        return treasureFindRadius;
    }

    /**
     * Gets the minimum number of clues required to find a treasure.
     *
     * @return The minimum clues required.
     */
    public int getMinCluesRequired() {
        return minCluesRequired;
    }

    /**
     * Gets a player's clue progress.
     *
     * @param playerId The player's UUID.
     * @return A set of clue IDs solved by the player.
     */
    public Set<String> getPlayerClueProgress(@NotNull UUID playerId) {
        return clueProgress.getOrDefault(playerId, Set.of());
    }

    /**
     * Gets a player's treasure progress.
     *
     * @param playerId The player's UUID.
     * @return A set of treasure names found by the player.
     */
    public Set<String> getPlayerTreasureProgress(@NotNull UUID playerId) {
        return playerProgress.getOrDefault(playerId, Set.of());
    }

    /**
     * Loads treasures from the database.
     */
    private void loadTreasures() {
        databaseManager.loadTreasures().forEach(t -> treasures.put(t.getName(), t));
    }

    /**
     * Loads teams from the database.
     */
    private void loadTeams() {
        databaseManager.loadTeams().forEach(t -> teams.put(t.getName(), t));
    }
}