package rip.thecraft.practice.settings;

/**
 * Represents the available world time options for players
 */
public enum WorldTimeOption {
    DAY("Day", 6000L),      // Noon time
    SUNSET("Sunset", 13000L), // Sunset time
    NIGHT("Night", 18000L);  // Midnight time

    private final String displayName;
    private final long timeTicks;

    WorldTimeOption(String displayName, long timeTicks) {
        this.displayName = displayName;
        this.timeTicks = timeTicks;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getTimeTicks() {
        return timeTicks;
    }

    /**
     * Gets the next time option in the cycle
     */
    public WorldTimeOption next() {
        return switch (this) {
            case DAY -> SUNSET;
            case SUNSET -> NIGHT;
            case NIGHT -> DAY;
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
