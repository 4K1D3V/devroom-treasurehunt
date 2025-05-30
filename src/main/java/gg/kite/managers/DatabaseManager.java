package gg.kite.managers;

import com.google.inject.Inject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import gg.kite.TreasureHunt;
import org.bson.Document;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages MongoDB database operations for the TreasureHunt plugin.
 */
public class DatabaseManager {
    private final MongoCollection<Document> treasuresCollection;
    private final MongoCollection<Document> cluesCollection;
    private final MongoCollection<Document> playerProgressCollection;
    private final MongoCollection<Document> clueProgressCollection;
    private final MongoCollection<Document> teamsCollection;
    private final TreasureHunt plugin;

    /**
     * Constructs a DatabaseManager with the specified MongoDB client and plugin.
     *
     * @param plugin The plugin instance.
     * @param mongoClient The MongoDB client for database operations.
     */
    @Inject
    public DatabaseManager(@NotNull TreasureHunt plugin, @NotNull MongoClient mongoClient) {
        this.plugin = plugin;
        MongoDatabase database = mongoClient.getDatabase("treasurehunt");
        treasuresCollection = database.getCollection("treasures");
        cluesCollection = database.getCollection("clues");
        playerProgressCollection = database.getCollection("player_progress");
        clueProgressCollection = database.getCollection("clue_progress");
        teamsCollection = database.getCollection("teams");
        createIndexes();
        plugin.getLogger().info("MongoDB collections initialized with indexes.");
    }

    /**
     * Creates indexes for efficient database queries.
     */
    private void createIndexes() {
        treasuresCollection.createIndex(Indexes.ascending("name"));
        cluesCollection.createIndex(Indexes.ascending("treasure_name", "description"));
        playerProgressCollection.createIndex(Indexes.ascending("player_uuid", "treasure_name"));
        clueProgressCollection.createIndex(Indexes.ascending("player_uuid", "treasure_name", "clue_description"));
        teamsCollection.createIndex(Indexes.ascending("name"));
    }

    /**
     * Saves a treasure to the database.
     *
     * @param treasure The treasure to save.
     */
    public void saveTreasure(@NotNull Treasure treasure) {
        var doc = new Document("name", treasure.getName())
                .append("world", treasure.getLocation().getWorld().getName())
                .append("x", treasure.getLocation().getX())
                .append("y", treasure.getLocation().getY())
                .append("z", treasure.getLocation().getZ())
                .append("rarity", treasure.getRarity());
        treasuresCollection.replaceOne(Filters.eq("name", treasure.getName()), doc,
                new com.mongodb.client.model.ReplaceOptions().upsert(true));
    }

    /**
     * Deletes a treasure and its associated data from the database.
     *
     * @param name The name of the treasure to delete.
     */
    public void deleteTreasure(@NotNull String name) {
        treasuresCollection.deleteOne(Filters.eq("name", name));
        cluesCollection.deleteMany(Filters.eq("treasure_name", name));
        playerProgressCollection.deleteMany(Filters.eq("treasure_name", name));
        clueProgressCollection.deleteMany(Filters.eq("treasure_name", name));
    }

    /**
     * Loads all treasures from the database.
     *
     * @return A list of loaded treasures.
     */
    public List<Treasure> loadTreasures() {
        var treasures = new ArrayList<Treasure>();
        for (var doc : treasuresCollection.find()) {
            String name = doc.getString("name");
            String worldName = doc.getString("world");
            World world = org.bukkit.Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("World not found for treasure: " + name);
                continue;
            }
            var location = new Location(world, doc.getDouble("x"), doc.getDouble("y"), doc.getDouble("z"));
            int rarity = doc.getInteger("rarity", 1);
            treasures.add(new Treasure(name, location, rarity));
        }
        treasures.forEach(this::loadClues);
        return treasures;
    }

    /**
     * Loads clues for a specific treasure from the database.
     *
     * @param treasure The treasure to load clues for.
     */
    private void loadClues(@NotNull Treasure treasure) {
        for (var doc : cluesCollection.find(Filters.eq("treasure_name", treasure.getName()))) {
            String description = doc.getString("description");
            String worldName = doc.getString("world");
            World world = org.bukkit.Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("World not found for clue in treasure: " + treasure.getName());
                continue;
            }
            var location = new Location(world, doc.getDouble("x"), doc.getDouble("y"), doc.getDouble("z"));
            String difficulty = doc.getString("difficulty");
            treasure.addClue(new Clue(description, location, difficulty));
        }
    }

    /**
     * Saves a clue to the database.
     *
     * @param treasureName The name of the associated treasure.
     * @param clue The clue to save.
     */
    public void saveClue(@NotNull String treasureName, @NotNull Clue clue) {
        var doc = new Document("treasure_name", treasureName)
                .append("description", clue.description())
                .append("world", clue.location().getWorld().getName())
                .append("x", clue.location().getX())
                .append("y", clue.location().getY())
                .append("z", clue.location().getZ())
                .append("difficulty", clue.difficulty())
                .append("created_at", clue.createdAt());
        cluesCollection.replaceOne(
                Filters.and(Filters.eq("treasure_name", treasureName), Filters.eq("description", clue.description())),
                doc,
                new com.mongodb.client.model.ReplaceOptions().upsert(true)
        );
    }

    /**
     * Deletes all clues for a specified treasure.
     *
     * @param treasureName The name of the treasure.
     */
    public void deleteClues(@NotNull String treasureName) {
        cluesCollection.deleteMany(Filters.eq("treasure_name", treasureName));
    }

    /**
     * Saves a player's progress for a found treasure.
     *
     * @param playerId The UUID of the player.
     * @param treasureName The name of the treasure.
     */
    public void savePlayerProgress(@NotNull UUID playerId, @NotNull String treasureName) {
        var doc = new Document("player_uuid", playerId.toString())
                .append("treasure_name", treasureName);
        playerProgressCollection.replaceOne(
                Filters.and(Filters.eq("player_uuid", playerId.toString()), Filters.eq("treasure_name", treasureName)),
                doc,
                new com.mongodb.client.model.ReplaceOptions().upsert(true)
        );
    }

    /**
     * Saves a player's clue progress.
     *
     * @param playerId The UUID of the player.
     * @param treasureName The name of the treasure.
     * @param clueDescription The description of the clue.
     */
    public void saveClueProgress(@NotNull UUID playerId, @NotNull String treasureName, @NotNull String clueDescription) {
        var doc = new Document("player_uuid", playerId.toString())
                .append("treasure_name", treasureName)
                .append("clue_description", clueDescription);
        clueProgressCollection.replaceOne(
                Filters.and(
                        Filters.eq("player_uuid", playerId.toString()),
                        Filters.eq("treasure_name", treasureName),
                        Filters.eq("clue_description", clueDescription)
                ),
                doc,
                new com.mongodb.client.model.ReplaceOptions().upsert(true)
        );
    }

    /**
     * Saves a team to the database.
     *
     * @param team The team to save.
     */
    public void saveTeam(@NotNull Team team) {
        var doc = new Document("name", team.getName())
                .append("members", team.getMembers().stream().map(UUID::toString).toList())
                .append("score", team.getScore());
        teamsCollection.replaceOne(Filters.eq("name", team.getName()), doc,
                new com.mongodb.client.model.ReplaceOptions().upsert(true));
    }

    /**
     * Deletes a team from the database.
     *
     * @param name The name of the team to delete.
     */
    public void deleteTeam(@NotNull String name) {
        teamsCollection.deleteOne(Filters.eq("name", name));
    }

    /**
     * Loads all teams from the database.
     *
     * @return A list of loaded teams.
     */
    public List<Team> loadTeams() {
        var teams = new ArrayList<Team>();
        for (var doc : teamsCollection.find()) {
            String name = doc.getString("name");
            List<String> memberStrings = doc.getList("members", String.class, List.of());
            Set<UUID> members = memberStrings.stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toSet());
            int score = doc.getInteger("score", 0);
            Team team = new Team(name, members.stream().findFirst().orElse(null));
            members.forEach(team::addMember);
            for (int i = 0; i < score; i++) team.incrementScore();
            teams.add(team);
        }
        return teams;
    }
}