package rip.thecraft.practice.hitdelay;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import rip.thecraft.practice.Practice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HitDelayHandler {

    private final Map<UUID, Long> lastHitTimes = new HashMap<>();
    private final HitDelayManager hitDelayManager;

    public HitDelayHandler(HitDelayManager hitDelayManager) {
        this.hitDelayManager = hitDelayManager;
    }

    /**
     * Check if a player can hit based on their kit's hit delay profile
     * 
     * @param player The player attempting to hit
     * @param hitDelayProfile The hit delay profile from their kit
     * @return true if the player can hit, false if they're still in cooldown
     */
    public boolean canHit(Player player, String hitDelayProfile) {
        if (hitDelayProfile == null || hitDelayProfile.isEmpty()) {
            return true; // No hit delay profile, allow all hits
        }

        HitDelayProfile profile = hitDelayManager.getProfile(hitDelayProfile);
        if (profile == null || !profile.isEnabled()) {
            return true; // Profile not found or disabled, allow all hits
        }

        long currentTime = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        
        if (!lastHitTimes.containsKey(playerId)) {
            // First hit, always allow
            lastHitTimes.put(playerId, currentTime);
            return true;
        }

        long lastHitTime = lastHitTimes.get(playerId);
        long timeSinceLastHit = currentTime - lastHitTime;
        long requiredDelay = profile.getHitDelay();

        if (timeSinceLastHit >= requiredDelay) {
            // Enough time has passed, allow hit and update timestamp
            lastHitTimes.put(playerId, currentTime);
            return true;
        }

        // Still in cooldown
        return false;
    }

    /**
     * Force update a player's last hit time (useful for when they switch kits)
     * 
     * @param player The player to update
     */
    public void updateLastHitTime(Player player) {
        lastHitTimes.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Reset a player's hit delay data (useful when they switch kits mid-match)
     * This clears their cooldown and allows them to immediately use the new hit delay
     * 
     * @param player The player to reset
     */
    public void resetPlayer(Player player) {
        lastHitTimes.remove(player.getUniqueId());
    }

    /**
     * Clear a player's hit delay data (useful when they leave a match)
     * 
     * @param player The player to clear
     */
    public void clearPlayer(Player player) {
        lastHitTimes.remove(player.getUniqueId());
    }

    /**
     * Clear all hit delay data (useful on server restart)
     */
    public void clearAll() {
        lastHitTimes.clear();
    }

    /**
     * Get the remaining cooldown time for a player
     * 
     * @param player The player to check
     * @param hitDelayProfile The hit delay profile
     * @return Remaining cooldown in milliseconds, 0 if no cooldown
     */
    public long getRemainingCooldown(Player player, String hitDelayProfile) {
        if (hitDelayProfile == null || hitDelayProfile.isEmpty()) {
            return 0;
        }

        HitDelayProfile profile = hitDelayManager.getProfile(hitDelayProfile);
        if (profile == null || !profile.isEnabled()) {
            return 0;
        }

        UUID playerId = player.getUniqueId();
        if (!lastHitTimes.containsKey(playerId)) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        long lastHitTime = lastHitTimes.get(playerId);
        long timeSinceLastHit = currentTime - lastHitTime;
        long requiredDelay = profile.getHitDelay();

        return Math.max(0, requiredDelay - timeSinceLastHit);
    }

    /**
     * Get the HitDelayManager instance
     * 
     * @return The HitDelayManager instance
     */
    public HitDelayManager getHitDelayManager() {
        return hitDelayManager;
    }
}
