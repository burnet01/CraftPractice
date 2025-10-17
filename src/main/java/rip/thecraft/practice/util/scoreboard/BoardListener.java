package rip.thecraft.practice.util.scoreboard;

import lombok.AllArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Created by: ThatKawaiiSam
 * Project: Assemble
 */
@AllArgsConstructor
public class BoardListener implements Listener {

	private Object board;

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		// Handle player join - boards are created on-demand in the manager
		// No need to create board here, it will be created when needed
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		// Remove the board and clear cache when player quits
		if (board instanceof BoardManager) {
			((BoardManager) board).getBoardMap().remove(event.getPlayer().getUniqueId());
			((BoardManager) board).clearCache(event.getPlayer().getUniqueId());
		} else if (board instanceof FoliaBoardManager) {
			((FoliaBoardManager) board).removePlayer(event.getPlayer().getUniqueId());
		}
	}
}

