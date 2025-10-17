package rip.thecraft.practice.util.scoreboard;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Created by: ThatKawaiiSam
 * Project: Assemble
 */
@Getter @Setter
public class BoardEntry {

    private Board board;
    private String text;
    private String string;
    private Team team;
    private String lastPrefix = "";
    private String lastSuffix = "";
    private int lastPosition = -1;

    public BoardEntry(Board board, String text) {
        this.board = board;
        this.text = text;
        this.string = board.getUniqueString();
        setUp();
    }

    public void setUp() {
        Scoreboard scoreboard = this.board.getScoreboard();
        if (scoreboard != null) {
            String name = this.string;
            if (name.length() > 16) name = name.substring(0, 16);

            Team team = scoreboard.getTeam(name);
            if (team == null) team = scoreboard.registerNewTeam(name);
            if (!team.getEntries().contains(this.string)) team.addEntry(this.string);
            if (!this.board.getEntries().contains(this)) this.board.getEntries().add(this);
            this.team = team;
        }
    }

    public void send(int position) {
        if (position == lastPosition && text.equals(getCurrentDisplayText())) {
            return;
        }

        String prefix;
        String suffix = "";

        if (this.text.length() > 16) {
            prefix = this.text.substring(0, 16);

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

            if (suffix.length() > 16) suffix = suffix.substring(0, 16);
        } else {
            prefix = this.text;
            suffix = "";
        }

        if (!prefix.equals(lastPrefix) || !suffix.equals(lastSuffix)) {
            this.team.setPrefix(prefix);
            this.team.setSuffix(suffix);
            this.lastPrefix = prefix;
            this.lastSuffix = suffix;
        }

        if (position != lastPosition) {
            Score score = this.board.getObjective().getScore(this.string);
            score.setScore(position);
            this.lastPosition = position;
        }
    }

    private String getCurrentDisplayText() {
        if (this.team == null) {
            return "";
        }

        String currentPrefix = this.team.getPrefix();
        String currentSuffix = this.team.getSuffix();

        if (currentPrefix == null) {
            currentPrefix = "";
        }
        if (currentSuffix == null) {
            currentSuffix = "";
        }

        if (currentSuffix.isEmpty()) {
            return currentPrefix;
        } else {
            String cleanSuffix = currentSuffix;
            String lastColors = ChatColor.getLastColors(currentPrefix);
            if (!lastColors.isEmpty() && currentSuffix.startsWith(lastColors)) {
                cleanSuffix = currentSuffix.substring(lastColors.length());
            }
            return currentPrefix + cleanSuffix;
        }
    }

    public void quit() {
        this.board.getStrings().remove(this.string);
        this.board.getScoreboard().resetScores(this.string);
        this.lastPrefix = "";
        this.lastSuffix = "";
        this.lastPosition = -1;
    }
}
