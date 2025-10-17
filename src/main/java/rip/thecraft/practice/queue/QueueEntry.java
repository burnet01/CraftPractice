package rip.thecraft.practice.queue;

import rip.thecraft.practice.kit.Kit;

import java.util.UUID;

public class QueueEntry {
    
    private final UUID playerId;
    private final Kit kit;
    private final QueueType type;
    private final long joinTime;
    
    public QueueEntry(UUID playerId, Kit kit, QueueType type) {
        this.playerId = playerId;
        this.kit = kit;
        this.type = type;
        this.joinTime = System.currentTimeMillis();
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public Kit getKit() {
        return kit;
    }
    
    public QueueType getType() {
        return type;
    }
    
    public long getJoinTime() {
        return joinTime;
    }
    
    public long getQueueTime() {
        return System.currentTimeMillis() - joinTime;
    }
}
