package rip.thecraft.practice.scoreboard;

import lombok.Getter;
import org.bukkit.entity.Player;
import rip.thecraft.practice.Practice;
import rip.thecraft.practice.player.PlayerData;
import rip.thecraft.practice.player.PlayerState;
import rip.thecraft.practice.util.scoreboard.BoardAdapter;
import rip.thecraft.practice.util.scoreboard.BoardManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Centralized service for managing scoreboards with optimized API calls
 * Provides clean integration points for different game states
 */
@Getter
public class ScoreboardService {

    private final Practice plugin;
    private final StateScoreboardManager stateManager;
    private final rip.thecraft.practice.util.scoreboard.FoliaBoardManager boardManager;
    
    // Cache for player state transitions to avoid unnecessary updates
    private final Map<UUID, PlayerState> lastPlayerState = new HashMap<>();
    
    // Performance tracking
    private long lastUpdateTime = 0;
    private int updateCount = 0;

    public ScoreboardService(Practice plugin) {
        this.plugin = plugin;
        this.stateManager = new StateScoreboardManager(plugin);
        
        // Initialize with state-based adapter for dynamic switching
        BoardAdapter adapter = new StateScoreboardAdapter();
        
        // Use 4 tick update interval for better performance (5 updates per second)
        // For Folia compatibility, use the new optimized FoliaBoardManager
        this.boardManager = new rip.thecraft.practice.util.scoreboard.FoliaBoardManager(adapter, 4);
    }

    /**
     * Update a player's scoreboard based on their current state
     * Optimized to only update when state changes or content differs
     */
    public void updatePlayerScoreboard(Player player) {
        // Check if player has scoreboard enabled
        if (plugin.getSettingsManager() != null && 
            !plugin.getSettingsManager().getPlayerSettings(player).isScoreboardEnabled()) {
            // Hide scoreboard if disabled - remove from board manager to prevent recreation
            boardManager.clearCache(player.getUniqueId());
            boardManager.getBoardMap().remove(player.getUniqueId());
            player.setScoreboard(plugin.getServer().getScoreboardManager().getNewScoreboard());
            return;
        }

        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
        if (playerData == null) return;

        PlayerState currentState = playerData.getState();
        PlayerState lastState = lastPlayerState.get(player.getUniqueId());

        // Only force update if state changed
        boolean forceUpdate = lastState != currentState;
        
        if (forceUpdate) {
            // Clear cache on state change to ensure fresh content
            boardManager.clearCache(player.getUniqueId());
            lastPlayerState.put(player.getUniqueId(), currentState);
        }
        
        // Force immediate scoreboard update
        boardManager.sendScoreboard();
        

    }

    /**
     * Force update a player's scoreboard immediately
     * Use sparingly for important state changes
     */
    public void forceUpdatePlayerScoreboard(Player player) {
        boardManager.clearCache(player.getUniqueId());
        updatePlayerScoreboard(player);
    }

    /**
     * Update all online players' scoreboards
     * Optimized for bulk updates
     */
    public void updateAllScoreboards() {
        plugin.getServer().getOnlinePlayers().forEach(this::updatePlayerScoreboard);
    }

    /**
     * Set a custom scoreboard adapter for a player
     * Useful for temporary scoreboards (e.g., events, special modes)
     */
    public void setCustomScoreboard(Player player, BoardAdapter adapter) {
        // This would require modifying BoardManager to support per-player adapters
        // For now, we'll use the state-based approach
        updatePlayerScoreboard(player);
    }

    /**
     * Reset a player to use the default state-based scoreboard
     */
    public void resetToDefaultScoreboard(Player player) {
        boardManager.clearCache(player.getUniqueId());
        lastPlayerState.remove(player.getUniqueId());
        updatePlayerScoreboard(player);
    }

    /**
     * Handle player join - initialize their scoreboard
     */
    public void handlePlayerJoin(Player player) {
        // Check if player has scoreboard enabled
        if (plugin.getSettingsManager() != null && 
            !plugin.getSettingsManager().getPlayerSettings(player).isScoreboardEnabled()) {
            // Hide scoreboard if disabled - remove from board manager to prevent recreation
            boardManager.clearCache(player.getUniqueId());
            boardManager.getBoardMap().remove(player.getUniqueId());
            player.setScoreboard(plugin.getServer().getScoreboardManager().getNewScoreboard());
            return;
        }

        // Initialize with current state
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
        if (playerData != null) {
            lastPlayerState.put(player.getUniqueId(), playerData.getState());
        }
        updatePlayerScoreboard(player);
    }

    /**
     * Handle player quit - clean up resources
     */
    public void handlePlayerQuit(Player player) {
        lastPlayerState.remove(player.getUniqueId());
        boardManager.clearCache(player.getUniqueId());
    }

    /**
     * Get performance metrics
     */
    public String getPerformanceMetrics() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastUpdate = currentTime - lastUpdateTime;
        
        return String.format(
            "Scoreboard Service - Updates: %d, Last update: %dms ago",
            updateCount, timeSinceLastUpdate
        );
    }

    /**
     * Shutdown the scoreboard service
     */
    public void shutdown() {
        // Clean up any resources if needed
        lastPlayerState.clear();
    }
}
