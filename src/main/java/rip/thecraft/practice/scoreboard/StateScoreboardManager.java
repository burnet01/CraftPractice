package rip.thecraft.practice.scoreboard;

import lombok.Getter;
import org.bukkit.entity.Player;
import rip.thecraft.practice.Practice;
import rip.thecraft.practice.player.PlayerData;
import rip.thecraft.practice.player.PlayerState;
import rip.thecraft.practice.util.scoreboard.BoardAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages dynamic scoreboard switching based on player state
 * Provides optimized state transitions and content caching
 */
@Getter
public class StateScoreboardManager {

    private final Practice plugin;
    private final StateScoreboardAdapter stateAdapter;
    
    // Cache for frequently accessed data to reduce lookups
    private final Map<UUID, Long> lastStateChange = new HashMap<>();
    private final Map<UUID, PlayerState> currentStates = new HashMap<>();

    public StateScoreboardManager(Practice plugin) {
        this.plugin = plugin;
        this.stateAdapter = new StateScoreboardAdapter();
    }

    /**
     * Get the appropriate scoreboard adapter for a player's current state
     */
    public BoardAdapter getAdapterForPlayer(Player player) {
        // Always use the state-based adapter for dynamic content
        return stateAdapter;
    }

    /**
     * Check if a player's state has changed since last check
     * Used to optimize scoreboard updates
     */
    public boolean hasStateChanged(Player player) {
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
        if (playerData == null) return false;

        PlayerState currentState = playerData.getState();
        PlayerState lastState = currentStates.get(player.getUniqueId());

        boolean changed = lastState != currentState;
        
        if (changed) {
            currentStates.put(player.getUniqueId(), currentState);
            lastStateChange.put(player.getUniqueId(), System.currentTimeMillis());
        }

        return changed;
    }

    /**
     * Get the time since last state change for a player
     * Useful for animations or transition effects
     */
    public long getTimeSinceStateChange(Player player) {
        Long lastChange = lastStateChange.get(player.getUniqueId());
        return lastChange != null ? System.currentTimeMillis() - lastChange : 0;
    }

    /**
     * Force a state change for a player
     * Useful for manual state transitions
     */
    public void forceStateChange(Player player, PlayerState newState) {
        currentStates.put(player.getUniqueId(), newState);
        lastStateChange.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Get current state for a player
     */
    public PlayerState getCurrentState(Player player) {
        return currentStates.getOrDefault(player.getUniqueId(), PlayerState.LOBBY);
    }

    /**
     * Handle player join - initialize state tracking
     */
    public void handlePlayerJoin(Player player) {
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
        if (playerData != null) {
            PlayerState initialState = playerData.getState();
            currentStates.put(player.getUniqueId(), initialState);
            lastStateChange.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    /**
     * Handle player quit - clean up state tracking
     */
    public void handlePlayerQuit(Player player) {
        currentStates.remove(player.getUniqueId());
        lastStateChange.remove(player.getUniqueId());
    }

    /**
     * Check if a player is in a state that requires frequent updates
     * (e.g., match states with timers)
     */
    public boolean requiresFrequentUpdates(Player player) {
        PlayerState state = getCurrentState(player);
        return state == PlayerState.MATCH || state == PlayerState.QUEUE;
    }

    /**
     * Get recommended update interval for a player based on their state
     * Returns ticks between updates
     */
    public int getRecommendedUpdateInterval(Player player) {
        if (requiresFrequentUpdates(player)) {
            return 2; // More frequent updates for match/queue states
        }
        return 4; // Less frequent updates for lobby/spectating states
    }

    /**
     * Shutdown the state manager
     */
    public void shutdown() {
        currentStates.clear();
        lastStateChange.clear();
    }
}
