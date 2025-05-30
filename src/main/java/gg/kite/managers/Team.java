package gg.kite.managers;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a team with members and a score.
 */
public class Team {
    private final String name;
    private final Set<UUID> members;
    private int score;

    public Team(String name, UUID leader) {
        this.name = name;
        this.members = new HashSet<>();
        if (leader != null) {
            this.members.add(leader);
        }
        this.score = 0;
    }

    public String getName() {
        return name;
    }

    public Set<UUID> getMembers() {
        return new HashSet<>(members);
    }

    public void addMember(UUID playerId) {
        members.add(playerId);
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    public int getScore() {
        return score;
    }

    public void incrementScore() {
        score++;
    }

    public void resetScore() {
        score = 0;
    }
}