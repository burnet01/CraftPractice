package rip.thecraft.practice.util.scoreboard;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optimized Folia-compatible scoreboard implementation
 * Minimizes Bukkit API calls and uses efficient data structures
 */
@Getter
public class FoliaBoard {

    private final List<FoliaBoardEntry> entries;
    private final Set<String> usedStrings;
    private Scoreboard scoreboard;
    private Objective objective;
    private final UUID id;
    private final FoliaBoardManager boardManager;
    
    // Performance optimizations
    public FoliaBoard(Player player, FoliaBoardManager boardManager) {
        this.id = player.getUniqueId();
        this.boardManager = boardManager;
        this.entries = new ArrayList<>();
        this.usedStrings = ConcurrentHashMap.newKeySet();
        
        setUp(player);
    }

    private void setUp(Player player) {
        // Create new scoreboard with optimized approach
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.objective = this.scoreboard.registerNewObjective("PracticeBoard", "dummy");
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        // Set initial title
        String title = this.boardManager.getAdapter().getTitle(player);
        if (title != null) {
            this.objective.setDisplayName(this.boardManager.getCachedTranslation(title));
        }
        
        player.setScoreboard(this.scoreboard);
    }

    /**
     * Generate unique scoreboard entry string with minimal overhead
     * Uses the same logic as the original Board for compatibility
     */
    public String getUniqueString() {
        String string = getRandomColorCode();
        while (this.usedStrings.contains(string)) string = string + getRandomColorCode();
        if (string.length() > 16) {
            return getUniqueString();
        }

        this.usedStrings.add(string);
        return string;
    }

    private String getRandomColorCode() {
        // Use the same logic as the original Board
        Random random = new Random();
        return colors().get(random.nextInt(colors().size() - 1)).toString();
    }

    private static List<ChatColor> colors() {
        List<ChatColor> chatColors = new ArrayList<>();
        Arrays.stream(ChatColor.values()).filter(ChatColor::isColor).forEach(chatColors::add);
        return chatColors;
    }

    public FoliaBoardEntry getEntryAtPosition(int position) {
        if (position < 0 || position >= entries.size()) return null;
        return entries.get(position);
    }

    /**
     * Update scoreboard with new lines - optimized for minimal API calls
     */
    public void update(Player player, List<String> newLines) {
        if (newLines == null || newLines.isEmpty()) {
            clear();
            return;
        }

        // Pre-translate all lines at once
        List<String> translatedLines = new ArrayList<>(newLines.size());
        for (String line : newLines) {
            String translated = boardManager.getCachedTranslation(line);
            translatedLines.add(translated);
        }

        BoardStyle style = this.boardManager.getAdapter().getBoardStyle(player);
        boolean descending = style.isDescending();

        if (!descending) {
            Collections.reverse(translatedLines);
        }

        // Remove excess entries efficiently
        int currentSize = entries.size();
        int targetSize = translatedLines.size();
        
        if (currentSize > targetSize) {
            // Remove from end (more efficient)
            for (int i = currentSize - 1; i >= targetSize; i--) {
                FoliaBoardEntry entry = entries.get(i);
                if (entry != null) {
                    entry.remove();
                }
            }
            // Trim list
            if (currentSize > targetSize) {
                entries.subList(targetSize, currentSize).clear();
            }
        }

        // Ensure we have exactly the right number of entries
        while (entries.size() < targetSize) {
            entries.add(null);
        }

        int score = style.getStart();
        for (int i = 0; i < translatedLines.size(); i++) {
            FoliaBoardEntry entry = entries.get(i);
            String line = translatedLines.get(i);

            if (entry == null) {
                entry = new FoliaBoardEntry(this, line);
                entries.set(i, entry);
            }
            entry.update(line, score);
            if (descending) {
                score--;
            } else {
                score++;
            }
        }
    }

    /**
     * Clear scoreboard efficiently
     */
    public void clear() {
        // Remove all entries
        for (FoliaBoardEntry entry : entries) {
            entry.remove();
        }
        entries.clear();
        usedStrings.clear();
        
        // Clear all scores from objective
        if (objective != null) {
            for (String entry : scoreboard.getEntries()) {
                scoreboard.resetScores(entry);
            }
        }
    }
}
