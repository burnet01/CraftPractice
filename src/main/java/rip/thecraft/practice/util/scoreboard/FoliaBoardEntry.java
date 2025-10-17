package rip.thecraft.practice.util.scoreboard;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Optimized scoreboard entry with minimal API calls
 * Reduces overhead by tracking state and only updating when necessary
 */
@Getter @Setter
public class FoliaBoardEntry {

    private final FoliaBoard board;
    private String text;
    private final String string;
    private Team team;
    
    // State tracking for minimal updates
    private String lastPrefix = "";
    private String lastSuffix = "";
    private int lastPosition = -1;
    private boolean initialized = false;

    public FoliaBoardEntry(FoliaBoard board, String text) {
        this.board = board;
        this.text = ""; // Initialize with empty string
        this.string = board.getUniqueString();
        setUp();
        // Force initial text update
        this.text = text;
        updateDisplayText();
    }

    /**
     * Set up team and entry with minimal API calls
     */
    public void setUp() {
        Scoreboard scoreboard = this.board.getScoreboard();
        if (scoreboard == null) return;

        String teamName = this.string;
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }

        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        
        if (!team.getEntries().contains(this.string)) {
            team.addEntry(this.string);
        }
        
        // Note: We don't add to board entries here - that's handled by the board
        this.team = team;
        this.initialized = true;
        

    }

    /**
     * Update entry with new text and position
     * Only makes API calls when values actually change
     */
    public void update(String newText, int position) {
        if (!this.initialized) {
            setUp();
        }
        
        boolean textChanged = !this.text.equals(newText);
        boolean positionChanged = this.lastPosition != position;
        

        
        // Always update position to ensure correct ordering
        if (positionChanged) {
            updatePosition(position);
        }
        
        // Update text if changed
        if (textChanged) {
            this.text = newText;
            updateDisplayText();
        }
    }

    /**
     * Legacy send method for compatibility
     */
    public void send(int position) {
        update(this.text, position);
    }

    /**
     * Update display text with optimized prefix/suffix splitting
     * Uses the same logic as the original BoardEntry for compatibility
     */
    private void updateDisplayText() {
        if (this.team == null) return;

        String prefix;
        String suffix = "";

        if (this.text.length() > 16) {
            prefix = this.text.substring(0, 16);

            // Handle color code boundaries carefully - same logic as original
            if (prefix.charAt(15) == '§') {
                prefix = prefix.substring(0, 15);
                suffix = this.text.substring(15);
            } else if (prefix.charAt(14) == '§') {
                prefix = prefix.substring(0, 14);
                suffix = this.text.substring(14);
            } else if (ChatColor.getLastColors(prefix).equalsIgnoreCase(ChatColor.getLastColors(this.string))) {
                suffix = this.text.substring(16);
            } else {
                suffix = ChatColor.getLastColors(prefix) + this.text.substring(16);
            }

            if (suffix.length() > 16) {
                suffix = suffix.substring(0, 16);
            }
        } else {
            prefix = this.text;
            suffix = "";
        }

        // Only update if values changed
        if (!prefix.equals(lastPrefix) || !suffix.equals(lastSuffix)) {
            this.team.setPrefix(prefix);
            this.team.setSuffix(suffix);
            this.lastPrefix = prefix;
            this.lastSuffix = suffix;
            

        }
    }

    /**
     * Update score position with minimal API calls
     */
    private void updatePosition(int position) {
        if (this.board.getObjective() == null) return;
        
        // Always update the score position to ensure correct ordering
        Score score = this.board.getObjective().getScore(this.string);
        score.setScore(position);
        this.lastPosition = position;
    }

    /**
     * Remove entry from scoreboard
     */
    public void remove() {
        if (this.board.getScoreboard() != null) {
            this.board.getScoreboard().resetScores(this.string);
        }
        if (this.team != null) {
            this.team.unregister();
        }
        this.board.getUsedStrings().remove(this.string);
        this.lastPrefix = "";
        this.lastSuffix = "";
        this.lastPosition = -1;
        this.initialized = false;
    }
}
