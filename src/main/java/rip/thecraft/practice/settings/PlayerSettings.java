package rip.thecraft.practice.settings;

/**
 * Represents a player's settings for the Practice plugin
 */
public class PlayerSettings {
    
    private WorldTimeOption worldTime = WorldTimeOption.DAY; // Default: Day time
    private boolean scoreboardEnabled = true; // Default: Scoreboard enabled
    private boolean messagesEnabled = true; // Default: Messages enabled

    public PlayerSettings() {
        // Default constructor with default values
    }

    // Getters and Setters
    public WorldTimeOption getWorldTime() {
        return worldTime;
    }

    public void setWorldTime(WorldTimeOption worldTime) {
        this.worldTime = worldTime;
    }

    public boolean isScoreboardEnabled() {
        return scoreboardEnabled;
    }

    public void setScoreboardEnabled(boolean scoreboardEnabled) {
        this.scoreboardEnabled = scoreboardEnabled;
    }

    public boolean isMessagesEnabled() {
        return messagesEnabled;
    }

    public void setMessagesEnabled(boolean messagesEnabled) {
        this.messagesEnabled = messagesEnabled;
    }

    @Override
    public String toString() {
        return "PlayerSettings{" +
                "worldTime=" + worldTime +
                ", scoreboardEnabled=" + scoreboardEnabled +
                ", messagesEnabled=" + messagesEnabled +
                '}';
    }
}
