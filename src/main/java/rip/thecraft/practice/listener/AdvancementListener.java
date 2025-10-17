package rip.thecraft.practice.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

/**
 * Listener to handle advancement events and prevent advancement messages
 */
public class AdvancementListener implements Listener {

    @EventHandler
    public void onPlayerAdvancement(PlayerAdvancementDoneEvent event) {
        // Cancel the advancement message entirely
        event.message(null);
    }
}
