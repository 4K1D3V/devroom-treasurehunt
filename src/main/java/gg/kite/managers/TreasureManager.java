package gg.kite.managers;

import gg.kite.config.MessageConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages treasures, clues, teams, and game state for the Treasure Hunt plugin.
 */
public class TreasureManager {
    private final DatabaseManager databaseManager;
    private final MessageConfig messageConfig;
    private final Map<String, Treasure> treasures;
    private final Map<String, Team> teams;
    private final Map<UUID, Set<String>> playerProgress;
    private final Map<UUID, Set<String>> clueProgress;
    private boolean competitionActive;
    private final Random random;
    private final double maxClueDistance;
    private final String clueDifficulty;
    private final int minCluesRequired;
    private final double treasureFindRadius;
    private final int maxTreasuresPerHunt;

    /**
     * Constructs a TreasureManager with dependencies and configuration.
     *
     * @param databaseManager The database manager.
     * @param config The plugin configuration.
     * @param messageConfig The message configuration.
     */
    public TreasureManager(DatabaseManager databaseManager, @NotNull FileConfiguration config, MessageConfig messageConfig) {
        this.databaseManager = databaseManager;
        this.messageConfig = messageConfig;
        this.treasures = new HashMap<>();
        this.teams = new HashMap<>();
        this.playerProgress = new HashMap<>();
        this.clueProgress = new HashMap<>();
        this.competitionActive = false;
        this.random = new Random();
        this.maxClueDistance = config.getDouble("treasure.max-clue-distance", 100.0);
        this.clueDifficulty = config.getString("treasure.clue-difficulty", "medium").toLowerCase();
        this.minCluesRequired = config.getInt("treasure.min-clues-required", 1);
        this.treasureFindRadius = config.getDouble("treasure.find-radius", 2.0);
        this.maxTreasuresPerHunt = config.getInt("treasure.max-treasures-per-hunt", 5);
        // Validate config values
        if (treasureFindRadius <= 0) {
            throw new IllegalArgumentException("treasure.find-radius must be positive");
        }
        if (maxClueDistance <= 0) {
            throw new IllegalArgumentException("treasure.max-clue-distance must be positive");
        }
        if (minCluesRequired < 0) {
            throw new IllegalArgumentException("treasure.min-clues-required cannot be negative");
        }
        if (maxTreasuresPerHunt <= 0) {
            throw new IllegalArgumentException("treasure.max-treasures-per-hunt must be positive");
        }
        loadTreasures();
        loadTeams();
    }

    /**
     * Loads treasures from the database.
     */
    private void loadTreasures() {
        treasures.putAll(databaseManager.loadTreasures().stream()
                .collect(Collectors.toMap(Treasure::getName, t -> t)));
    }

    /**
     * Loads teams from the database.
     */
    private void loadTeams() {
        teams.putAll(databaseManager.loadTeams().stream()
                .collect(Collectors.toMap(Team::getName, t -> t)));
    }

    /**
     * Creates a new treasure.
     *
     * @param name The treasure name.
     * @param location The treasure location.
     * @param rarity The treasure rarity (1-10).
     * @return True if created, false if name exists.
     */
    public boolean createTreasure(String name, Location location, int rarity) {
        if (treasures.containsKey(name)) {
            return false;
        }
        var treasure = new Treasure(name, location, rarity);
        treasures.put(name, treasure);
        databaseManager.saveTreasure(treasure);
        return true;
    }

    /**
     * Deletes a treasure.
     *
     * @param name The treasure name.
     * @return True if deleted, false if not found.
     */
    public boolean deleteTreasure(String name) {
        if (!treasures.containsKey(name)) {
            return false;
        }
        treasures.remove(name);
        databaseManager.deleteTreasure(name);
        return true;
    }

    /**
     * Gets all treasures.
     *
     * @return List of treasures.
     */
    public List<Treasure> getTreasures() {
        return new ArrayList<>(treasures.values());
    }

    /**
     * Creates a clue for a treasure.
     *
     * @param treasureName The treasure name.
     * @param description The clue description.
     * @param location The clue location.
     * @return True if created, false if invalid.
     */
    public boolean createClue(String treasureName, String description, Location location) {
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
     * Validates clue location against treasure.
     *
     * @param treasure The treasure.
     * @param location The clue location.
     * @return True if valid.
     */
    private boolean isValidClueLocation(Treasure treasure, Location location) {
        return location.getWorld().equals(treasure.getLocation().getWorld()) &&
                location.distance(treasure.getLocation()) <= maxClueDistance;
    }

    /**
     * Deletes all clues for a treasure.
     *
     * @param treasureName The treasure name.
     * @return True if deleted, false if not found.
     */
    public boolean deleteClues(String treasureName) {
        var treasure = treasures.get(treasureName);
        if (treasure == null) {
            return false;
        }
        treasure.clearClues();
        databaseManager.deleteClues(treasureName);
        return true;
    }

    /**
     * Gets clues for a treasure.
     *
     * @param treasureName The treasure name.
     * @return List of clues.
     */
    public List<Clue> getClues(String treasureName) {
        var treasure = treasures.get(treasureName);
        return treasure != null ? treasure.getClues() : List.of();
    }

    /**
     * Marks a clue as solved by a player.
     *
     * @param player The player.
     * @param treasureName The treasure name.
     * @param clueDescription The clue description.
     * @return True if the clue was newly solved, false if already solved or invalid.
     */
    public boolean markClueSolved(Player player, String treasureName, String clueDescription) {
        var treasure = treasures.get(treasureName);
        if (treasure == null || treasure.getClues().stream().noneMatch(c -> c.description().equals(clueDescription))) {
            return false;
        }
        String clueId = treasureName + ":" + clueDescription;
        Set<String> playerClues = clueProgress.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        if (playerClues.contains(clueId)) {
            return false; // Clue already solved
        }
        playerClues.add(clueId);
        databaseManager.saveClueProgress(player.getUniqueId(), treasureName, clueDescription);
        return true;
    }

    /**
     * Creates a new team.
     *
     * @param name The team name.
     * @param leader The team leader.
     * @return True if created, false if name exists.
     */
    public boolean createTeam(String name, Player leader) {
        if (teams.containsKey(name)) {
            return false;
        }
        var team = new Team(name, leader.getUniqueId());
        teams.put(name, team);
        databaseManager.saveTeam(team);
        return true;
    }

    /**
     * Invites a player to a team.
     *
     * @param teamName The team name.
     * @param invited The invited player.
     * @return True if invited, false if team not found or player in another team.
     */
    public boolean invitePlayer(String teamName, Player invited) {
        var team = teams.get(teamName);
        if (team == null) {
            return false;
        }
        // Check if player already in another team
        if (getPlayerTeam(invited.getUniqueId()) != null) {
            return false;
        }
        team.addMember(invited.getUniqueId());
        databaseManager.saveTeam(team);
        return true;
    }

    /**
     * Kicks a player from a team.
     *
     * @param teamName The team name.
     * @param kicked The kicked player.
     * @return True if kicked, false if team not found.
     */
    public boolean kickPlayer(String teamName, Player kicked) {
        var team = teams.get(teamName);
        if (team == null) {
            return false;
        }
        team.removeMember(kicked.getUniqueId());
        databaseManager.saveTeam(team);
        if (team.getMembers().isEmpty()) {
            teams.remove(teamName);
            databaseManager.deleteTeam(teamName);
        }
        return true;
    }

    /**
     * Gets all teams.
     *
     * @return List of teams.
     */
    public List<Team> getTeams() {
        return new ArrayList<>(teams.values());
    }

    /**
     * Starts a treasure hunt for a player.
     *
     * @param player The player.
     */
    public void startHunt(Player player) {
        if (treasures.isEmpty()) {
            player.sendMessage(messageConfig.getMessage("no-treasures"));
            return;
        }
        var availableTreasures = getAvailableTreasures(player.getUniqueId());
        if (availableTreasures.isEmpty()) {
            player.sendMessage(messageConfig.getMessage("all-treasures-found"));
            return;
        }
        var treasure = selectTreasure(availableTreasures);
        var clues = getDifficultyFilteredClues(treasure);
        if (clues.isEmpty()) {
            player.sendMessage(messageConfig.getMessage("no-clues"));
            return;
        }
        var clue = clues.get(random.nextInt(clues.size()));
        player.sendMessage(messageConfig.getMessage("hunt-started") + clue.description());
    }

    /**
     * Selects a treasure based on rarity weights.
     *
     * @param treasures The available treasures.
     * @return The selected treasure.
     */
    private Treasure selectTreasure(@NotNull List<Treasure> treasures) {
        int totalRarity = treasures.stream().mapToInt(Treasure::getRarity).sum();
        int randomValue = random.nextInt(totalRarity);
        int current = 0;
        for (var treasure : treasures) {
            current += treasure.getRarity();
            if (randomValue < current) {
                return treasure;
            }
        }
        return treasures.getFirst(); // Fallback
    }

    /**
     * Gets treasures not yet found by the player.
     *
     * @param playerId The player's UUID.
     * @return List of available treasures.
     */
    private List<Treasure> getAvailableTreasures(UUID playerId) {
        return treasures.values().stream()
                .filter(t -> !playerProgress.getOrDefault(playerId, Set.of()).contains(t.getName()))
                .toList();
    }

    /**
     * Filters clues by configured difficulty.
     *
     * @param treasure The treasure.
     * @return List of matching clues.
     */
    private List<Clue> getDifficultyFilteredClues(Treasure treasure) {
        var clues = treasure.getClues();
        var filtered = clues.stream()
                .filter(c -> c.difficulty().equalsIgnoreCase(clueDifficulty))
                .toList();
        return filtered.isEmpty() ? clues : filtered;
    }

    /**
     * Starts a team competition.
     */
    public void startCompetition() {
        if (competitionActive) {
            return;
        }
        competitionActive = true;
        teams.values().forEach(team -> {
            team.resetScore();
            databaseManager.saveTeam(team);
        });
        broadcastToTeams(messageConfig.getMessage("competition-start"));
    }

    /**
     * Marks a treasure as found by a player.
     *
     * @param player The player.
     * @param treasureName The treasure name.
     * @return True if marked, false if invalid.
     */
    public boolean markTreasureFound(Player player, String treasureName) {
        var treasure = treasures.get(treasureName);
        if (treasure == null || !hasEnoughCluesSolved(player.getUniqueId(), treasureName)) {
            return false;
        }
        playerProgress.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(treasureName);
        databaseManager.savePlayerProgress(player.getUniqueId(), treasureName);
        updateTeamScore(player.getUniqueId());
        player.sendMessage(messageConfig.getMessage("treasure-found", "%s", treasureName));
        return true;
    }

    /**
     * Checks if a player has solved enough clues for a treasure.
     *
     * @param playerId The player's UUID.
     * @param treasureName The treasure name.
     * @return True if enough clues solved.
     */
    private boolean hasEnoughCluesSolved(UUID playerId, String treasureName) {
        long solvedCount = clueProgress.getOrDefault(playerId, Set.of()).stream()
                .filter(cp -> cp.startsWith(treasureName + ":"))
                .count();
        return solvedCount >= minCluesRequired;
    }

    /**
     * Updates team score and checks for competition winner.
     *
     * @param playerId The player's UUID.
     */
    private void updateTeamScore(UUID playerId) {
        if (!competitionActive) {
            return;
        }
        var team = getPlayerTeam(playerId);
        if (team != null) {
            team.incrementScore();
            databaseManager.saveTeam(team);
            checkCompetitionStatus();
        }
    }

    /**
     * Gets the team a player belongs to.
     *
     * @param playerId The player's UUID.
     * @return The team, or null if not found.
     */
    private Team getPlayerTeam(UUID playerId) {
        return teams.values().stream()
                .filter(team -> team.getMembers().contains(playerId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks if a team has won the competition.
     */
    private void checkCompetitionStatus() {
        if (treasures.isEmpty()) {
            return;
        }
        var winner = teams.values().stream()
                .max(Comparator.comparingInt(Team::getScore))
                .orElse(null);
        if (winner != null && winner.getScore() >= treasures.size()) {
            competitionActive = false;
            broadcastToTeams(messageConfig.getMessage("competition-end", "%s", winner.getName()));
        }
    }

    /**
     * Broadcasts a message to all team members.
     *
     * @param message The message to broadcast.
     */
    private void broadcastToTeams(String message) {
        teams.values().stream()
                .flatMap(team -> team.getMembers().stream())
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .forEach(p -> p.sendMessage(message));
    }

    /**
     * Gets the treasure find radius.
     *
     * @return The radius in blocks.
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
}