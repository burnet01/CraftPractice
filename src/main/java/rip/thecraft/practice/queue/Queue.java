package rip.thecraft.practice.queue;

import rip.thecraft.practice.kit.Kit;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Queue {

    private final Kit kit;
    private final QueueType type;
    private final Set<UUID> players = new HashSet<>();

    public Queue(Kit kit, QueueType type) {
        this.kit = kit;
        this.type = type;
    }

    public Kit getKit() {
        return kit;
    }

    public QueueType getType() {
        return type;
    }

    public Set<UUID> getPlayers() {
        return players;
    }

    public void addPlayer(UUID playerId) {
        players.add(playerId);
    }

    public void removePlayer(UUID playerId) {
        players.remove(playerId);
    }

    public int getSize() {
        return players.size();
    }
}
