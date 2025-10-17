package rip.thecraft.practice.tournament;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import rip.thecraft.practice.match.Match;
import rip.thecraft.practice.player.PlayerData;
import rip.thecraft.practice.player.PlayerState;

import java.util.UUID;

@Getter
@Setter
public class TournamentMatch {

    private final Tournament tournament;
    private final UUID player1;
    private final UUID player2;
    private final int round;
    private final long startTime;
    
    private Match match;
    private boolean started;
    private boolean ended;
    
    public TournamentMatch(Tournament tournament, UUID player1, UUID player2, int round) {
        this.tournament = tournament;
        this.player1 = player1;
        this.player2 = player2;
        this.round = round;
        this.startTime = System.currentTimeMillis();
        this.started = false;
        this.ended = false;
    }
    
    public void startMatch() {
        Player p1 = Bukkit.getPlayer(player1);
        Player p2 = Bukkit.getPlayer(player2);
        
        if (p1 == null || p2 == null) {
            // One or both players are offline
            if (p1 != null) {
                p1.sendMessage(ChatColor.RED + "Your opponent is not online! You win by default.");
                tournament.onMatchComplete(this, player1);
            } else if (p2 != null) {
                p2.sendMessage(ChatColor.RED + "Your opponent is not online! You win by default.");
                tournament.onMatchComplete(this, player2);
            }
            return;
        }
        
        // Use the existing queue system to start the match
        // This ensures proper match handling, visibility, and combat mechanics
        rip.thecraft.practice.Practice.getInstance().getMatchManager().startMatch(
            p1, p2, tournament.getKit(), rip.thecraft.practice.queue.QueueType.UNRANKED
        );
        
        // Store the match reference for tracking
        this.match = rip.thecraft.practice.Practice.getInstance().getMatchManager().getPlayerMatch(player1);
        
        this.started = true;
        
        // Send match start messages
        p1.sendMessage(ChatColor.GOLD + "=== TOURNAMENT MATCH ===");
        p1.sendMessage(ChatColor.YELLOW + "Round: " + ChatColor.WHITE + round);
        p1.sendMessage(ChatColor.YELLOW + "Opponent: " + ChatColor.WHITE + p2.getName());
        p1.sendMessage(ChatColor.GOLD + "========================");
        
        p2.sendMessage(ChatColor.GOLD + "=== TOURNAMENT MATCH ===");
        p2.sendMessage(ChatColor.YELLOW + "Round: " + ChatColor.WHITE + round);
        p2.sendMessage(ChatColor.YELLOW + "Opponent: " + ChatColor.WHITE + p1.getName());
        p2.sendMessage(ChatColor.GOLD + "========================");
    }
    
    public void onMatchEnd(UUID winner) {
        if (ended) return;
        
        this.ended = true;
        
        // Notify tournament
        tournament.onMatchComplete(this, winner);
    }
    
    public void forceEnd() {
        if (ended) return;
        
        this.ended = true;
        
        // Force end the match using the existing match manager
        if (match != null) {
            rip.thecraft.practice.Practice.getInstance().getMatchManager().endMatch(match, null);
        }
    }
    
    public boolean containsPlayer(UUID playerId) {
        return player1.equals(playerId) || player2.equals(playerId);
    }
    
    public UUID getOpponent(UUID playerId) {
        if (player1.equals(playerId)) return player2;
        if (player2.equals(playerId)) return player1;
        return null;
    }
    
    public boolean isPlayerInMatch(UUID playerId) {
        return containsPlayer(playerId) && started && !ended;
    }
}
