package gg.kite.managers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a team with members and a score for treasure hunts.
 */
public class Team {
    private final String name;
    private final Set<UUID> members;
    private int score;

    /**
     * Constructs a Team with the specified name and leader.
     *
     * @param name The team name.
     * @param leader The UUID of the team leader, or null if no leader.
     */
    public Team(@NotNull String name, @Nullable UUID leader) {
        this.name = name;
        this.members = new HashSet<>();
        if (leader != null) {
            this.members.add(leader);
        }
        this.score = 0;
    }

    /**
     * Gets the team name.
     *
     * @return The team name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets a copy of the team members.
     *
     * @return A set of member UUIDs.
     */
    public Set<UUID> getMembers() {
        return new HashSet<>(members);
    }

    /**
     * Adds a member to the team.
     *
     * @param playerId The UUID of the player to add.
     */
    public void addMember(@NotNull UUID playerId) {
        members.add(playerId);
    }

    /**
     * Removes a member from the team.
     *
     * @param playerId The UUID of the player to remove.
     */
    public void removeMember(@NotNull UUID playerId) {
        members.remove(playerId);
    }

    /**
     * Gets the team's score.
     *
     * @return The current score.
     */
    public int getScore() {
        return score;
    }

    /**
     * Increments the team's score by one.
     */
    public void incrementScore() {
        score++;
    }

    /**
     * Resets the team's score to zero.
     */
    public void resetScore() {
        score = 0;
    }
}