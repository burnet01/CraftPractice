package rip.thecraft.practice.util.scoreboard;

import org.bukkit.entity.Player;

import java.util.List;

/**
 * Created by: ThatKawaiiSam
 * Project: Assemble
 */
public interface BoardAdapter {

	String getTitle(Player player);

	List<String> getLines(Player player);

	BoardStyle getBoardStyle(Player player);
}

