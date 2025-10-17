package rip.thecraft.practice.scoreboard;

import org.bukkit.entity.Player;
import rip.thecraft.practice.Practice;

/**
 * Utility class for easy scoreboard integration from other plugin components
 * Provides simple API methods for common scoreboard operations
 */
public class ScoreboardIntegration {

    private static Practice getPlugin() {
        return Practice.getInstance();
    }

    /**
     * Update a player's scoreboard (optimized - only updates if needed)
     * Use this for general state changes
     */
    public static void updateScoreboard(Player player) {
        // Check if player has scoreboard enabled
        if (getPlugin().getSettingsManager() != null && 
            !getPlugin().getSettingsManager().getPlayerSettings(player).isScoreboardEnabled()) {
            return; // Skip if scoreboard is disabled
        }
        getPlugin().getScoreboardService().updatePlayerScoreboard(player);
    }

    /**
     * Force immediate update of a player's scoreboard
     * Use this for important state changes that need immediate feedback
     */
    public static void forceUpdateScoreboard(Player player) {
        // Check if player has scoreboard enabled
        if (getPlugin().getSettingsManager() != null && 
            !getPlugin().getSettingsManager().getPlayerSettings(player).isScoreboardEnabled()) {
            return; // Skip if scoreboard is disabled
        }
        getPlugin().getScoreboardService().forceUpdatePlayerScoreboard(player);
    }

    /**
     * Update all online players' scoreboards
     * Use this for global events or server-wide changes
     */
    public static void updateAllScoreboards() {
        getPlugin().getScoreboardService().updateAllScoreboards();
    }

    /**
     * Handle player join - call this when a player joins
     */
    public static void handlePlayerJoin(Player player) {
        // Check if player has scoreboard enabled
        if (getPlugin().getSettingsManager() != null && 
            !getPlugin().getSettingsManager().getPlayerSettings(player).isScoreboardEnabled()) {
            return; // Skip if scoreboard is disabled
        }
        getPlugin().getScoreboardService().handlePlayerJoin(player);
    }

    /**
     * Handle player quit - call this when a player leaves
     */
    public static void handlePlayerQuit(Player player) {
        getPlugin().getScoreboardService().handlePlayerQuit(player);
    }

    /**
     * Reset a player to default scoreboard
     * Use this when exiting special modes or events
     */
    public static void resetToDefaultScoreboard(Player player) {
        getPlugin().getScoreboardService().resetToDefaultScoreboard(player);
    }

    /**
     * Get performance metrics for debugging
     */
    public static String getPerformanceMetrics() {
        return getPlugin().getScoreboardService().getPerformanceMetrics();
    }

    /**
     * Quick update for match-related scoreboard changes
     * Automatically handles the optimization
     */
    public static void updateMatchScoreboard(Player player) {
        forceUpdateScoreboard(player); // Force update for match changes
    }

    /**
     * Quick update for queue-related scoreboard changes
     * Automatically handles the optimization
     */
    public static void updateQueueScoreboard(Player player) {
        forceUpdateScoreboard(player); // Force update for queue changes
    }

    /**
     * Quick update for lobby-related scoreboard changes
     * Uses optimized update
     */
    public static void updateLobbyScoreboard(Player player) {
        updateScoreboard(player); // Regular update for lobby
    }
}
