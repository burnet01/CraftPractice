package rip.thecraft.practice.match;

import rip.thecraft.practice.arena.Arena;
import rip.thecraft.practice.kit.Kit;
import rip.thecraft.practice.queue.QueueType;

import java.util.UUID;

public class Match {

    private final UUID player1;
    private final UUID player2;
    private final Kit kit;
    private final QueueType type;
    private final Arena arena;
    private boolean started = false;
    private long startTime;

    public Match(UUID player1, UUID player2, Kit kit, QueueType type, Arena arena) {
        this.player1 = player1;
        this.player2 = player2;
        this.kit = kit;
        this.type = type;
        this.arena = arena;
        this.startTime = System.currentTimeMillis();
    }

    public UUID getPlayer1() {
        return player1;
    }

    public UUID getPlayer2() {
        return player2;
    }

    public Kit getKit() {
        return kit;
    }

    public QueueType getType() {
        return type;
    }

    public Arena getArena() {
        return arena;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getDuration() {
        return System.currentTimeMillis() - startTime;
    }
}
