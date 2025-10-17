package rip.thecraft.practice.tournament;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import rip.thecraft.practice.kit.Kit;
import rip.thecraft.practice.match.Match;
import rip.thecraft.practice.player.PlayerData;
import rip.thecraft.practice.player.PlayerState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class Tournament {

    private final String id;
    private final Kit kit;
    private final UUID host;
    private final long startTime;
    
    private TournamentState state;
    private int currentRound;
    private int maxPlayers;
    private int requiredPlayers;
    private List<String> prizeCommands;
    
    private final Set<UUID> participants;
    private final Set<UUID> eliminated;
    private final Map<UUID, Integer> playerWins;
    private final List<TournamentMatch> activeMatches;
    private final Map<Integer, List<TournamentMatch>> bracket;
    
    public Tournament(Kit kit, UUID host, int maxPlayers, int requiredPlayers, List<String> prizeCommands) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.kit = kit;
        this.host = host;
        this.startTime = System.currentTimeMillis();
        this.state = TournamentState.WAITING;
        this.currentRound = 0;
        this.maxPlayers = maxPlayers;
        this.requiredPlayers = requiredPlayers;
        this.prizeCommands = prizeCommands != null ? prizeCommands : new ArrayList<>();
        
        this.participants = ConcurrentHashMap.newKeySet();
        this.eliminated = ConcurrentHashMap.newKeySet();
        this.playerWins = new ConcurrentHashMap<>();
        this.activeMatches = new ArrayList<>();
        this.bracket = new ConcurrentHashMap<>();
    }
    
    public boolean addParticipant(Player player) {
        if (participants.size() >= maxPlayers) {
            player.sendMessage(ChatColor.RED + "This tournament is full!");
            return false;
        }
        
        if (state != TournamentState.WAITING) {
            player.sendMessage(ChatColor.RED + "This tournament has already started!");
            return false;
        }
        
        if (participants.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are already in this tournament!");
            return false;
        }
        
        participants.add(player.getUniqueId());
        playerWins.put(player.getUniqueId(), 0);
        
        // Update player state
        PlayerData playerData = rip.thecraft.practice.Practice.getInstance().getPlayerManager().getPlayerData(player);
        if (playerData != null) {
            playerData.setState(PlayerState.TOURNAMENT);
        }
        
        broadcast(ChatColor.GREEN + player.getName() + " has joined the tournament! (" + participants.size() + "/" + maxPlayers + ")");
        
        // Check if we can start
        if (participants.size() >= requiredPlayers && state == TournamentState.WAITING) {
            startTournament();
        }
        
        return true;
    }
    
    public void removeParticipant(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (participants.remove(playerId)) {
            playerWins.remove(playerId);
            eliminated.add(playerId);
            
            // Remove from any active matches
            activeMatches.removeIf(match -> match.containsPlayer(playerId));
            
            // Update player state
            PlayerData playerData = rip.thecraft.practice.Practice.getInstance().getPlayerManager().getPlayerData(player);
            if (playerData != null) {
                playerData.setState(PlayerState.LOBBY);
            }
            
            broadcast(ChatColor.RED + player.getName() + " has left the tournament!");
        }
    }
    
    public void startTournament() {
        if (state != TournamentState.WAITING) {
            return;
        }
        
        if (participants.size() < requiredPlayers) {
            broadcast(ChatColor.RED + "Not enough players to start the tournament! (" + participants.size() + "/" + requiredPlayers + ")");
            return;
        }
        
        state = TournamentState.RUNNING;
        currentRound = 1;
        
        broadcast(ChatColor.GOLD + "=== TOURNAMENT STARTED ===");
        broadcast(ChatColor.YELLOW + "Kit: " + ChatColor.WHITE + kit.getDisplayName());
        broadcast(ChatColor.YELLOW + "Players: " + ChatColor.WHITE + participants.size());
        broadcast(ChatColor.YELLOW + "Format: " + ChatColor.WHITE + "Single Elimination");
        broadcast(ChatColor.GOLD + "==========================");
        
        // Create first round bracket
        createBracket();
        startRound();
    }
    
    private void createBracket() {
        bracket.clear();
        
        List<UUID> remainingPlayers = getRemainingPlayers();
        Collections.shuffle(remainingPlayers);
        
        List<TournamentMatch> roundMatches = new ArrayList<>();
        
        // Create matches for current round
        for (int i = 0; i < remainingPlayers.size(); i += 2) {
            if (i + 1 < remainingPlayers.size()) {
                UUID player1 = remainingPlayers.get(i);
                UUID player2 = remainingPlayers.get(i + 1);
                
                TournamentMatch match = new TournamentMatch(this, player1, player2, currentRound);
                roundMatches.add(match);
            } else {
                // Odd number of players - give bye to last player
                UUID player = remainingPlayers.get(i);
                eliminated.add(player); // This player gets a bye to next round
                
                Player bukkitPlayer = Bukkit.getPlayer(player);
                if (bukkitPlayer != null) {
                    bukkitPlayer.sendMessage(ChatColor.GREEN + "You received a bye this round!");
                }
            }
        }
        
        bracket.put(currentRound, roundMatches);
    }
    
    private void startRound() {
        List<TournamentMatch> roundMatches = bracket.get(currentRound);
        if (roundMatches == null || roundMatches.isEmpty()) {
            // No matches this round - check if tournament is over
            checkTournamentEnd();
            return;
        }
        
        broadcast(ChatColor.GOLD + "=== ROUND " + currentRound + " ===");
        
        for (TournamentMatch match : roundMatches) {
            if (!eliminated.contains(match.getPlayer1()) && !eliminated.contains(match.getPlayer2())) {
                activeMatches.add(match);
                match.startMatch();
            } else {
                // Debug: Log why match isn't starting
                System.out.println("[TOURNAMENT DEBUG] Match not started - Player1 eliminated: " + eliminated.contains(match.getPlayer1()) + 
                    ", Player2 eliminated: " + eliminated.contains(match.getPlayer2()));
            }
        }
        
        // Debug: Log round start info
        System.out.println("[TOURNAMENT DEBUG] Starting round " + currentRound + 
            " | Active matches: " + activeMatches.size() + 
            " | Remaining players: " + getRemainingPlayers().size());
    }
    
    public void onMatchComplete(TournamentMatch match, UUID winner) {
        activeMatches.remove(match);
        
        UUID loser = winner.equals(match.getPlayer1()) ? match.getPlayer2() : match.getPlayer1();
        eliminated.add(loser);
        
        // Update wins
        playerWins.put(winner, playerWins.getOrDefault(winner, 0) + 1);
        
        // Send messages
        Player winnerPlayer = Bukkit.getPlayer(winner);
        Player loserPlayer = Bukkit.getPlayer(loser);
        
        if (winnerPlayer != null) {
            winnerPlayer.sendMessage(ChatColor.GREEN + "You won your match! Moving to next round...");
            // Update player state back to TOURNAMENT (from MATCH)
            PlayerData winnerData = rip.thecraft.practice.Practice.getInstance().getPlayerManager().getPlayerData(winnerPlayer);
            if (winnerData != null) {
                winnerData.setState(PlayerState.TOURNAMENT);
            }
        }
        if (loserPlayer != null) {
            loserPlayer.sendMessage(ChatColor.RED + "You lost your match and are eliminated from the tournament.");
            // Update player state back to LOBBY
            PlayerData loserData = rip.thecraft.practice.Practice.getInstance().getPlayerManager().getPlayerData(loserPlayer);
            if (loserData != null) {
                loserData.setState(PlayerState.LOBBY);
            }
        }
        
        broadcast(ChatColor.YELLOW + Bukkit.getOfflinePlayer(winner).getName() + " defeated " + 
                 Bukkit.getOfflinePlayer(loser).getName() + " in Round " + currentRound);
        
        // Debug: Log match completion
        System.out.println("[TOURNAMENT DEBUG] Match completed - Winner: " + winner + 
            " | Loser: " + loser + 
            " | Active matches remaining: " + activeMatches.size() + 
            " | Remaining players: " + getRemainingPlayers().size());
        
        // Check if round is complete
        if (activeMatches.isEmpty()) {
            startNextRound();
        }
    }
    
    private void startNextRound() {
        // Check if tournament is over
        List<UUID> remainingPlayers = getRemainingPlayers();
        if (remainingPlayers.size() <= 1) {
            endTournament(remainingPlayers.isEmpty() ? null : remainingPlayers.get(0));
            return;
        }
        
        currentRound++;
        
        // Give 10-second warning
        broadcast(ChatColor.YELLOW + "Next round starting in 10 seconds...");
        
        Bukkit.getScheduler().runTaskLater(rip.thecraft.practice.Practice.getInstance(), () -> {
            if (state == TournamentState.RUNNING) {
                createBracket();
                startRound();
            }
        }, 200L); // 10 seconds (20 ticks * 10)
    }
    
    private List<UUID> getRemainingPlayers() {
        List<UUID> remaining = new ArrayList<>();
        for (UUID participant : participants) {
            if (!eliminated.contains(participant)) {
                remaining.add(participant);
            }
        }
        return remaining;
    }
    
    private void checkTournamentEnd() {
        List<UUID> remainingPlayers = getRemainingPlayers();
        if (remainingPlayers.size() == 1) {
            endTournament(remainingPlayers.get(0));
        } else if (remainingPlayers.isEmpty()) {
            endTournament(null);
        }
    }
    
    public void endTournament(UUID winner) {
        state = TournamentState.ENDED;
        
        // End all active matches
        for (TournamentMatch match : new ArrayList<>(activeMatches)) {
            match.forceEnd();
        }
        activeMatches.clear();
        
        // Reset player states
        for (UUID participant : participants) {
            Player player = Bukkit.getPlayer(participant);
            if (player != null) {
                PlayerData playerData = rip.thecraft.practice.Practice.getInstance().getPlayerManager().getPlayerData(player);
                if (playerData != null) {
                    playerData.setState(PlayerState.LOBBY);
                }
            }
        }
        
        // Announce winner and distribute prizes
        if (winner != null) {
            Player winnerPlayer = Bukkit.getPlayer(winner);
            String winnerName = winnerPlayer != null ? winnerPlayer.getName() : Bukkit.getOfflinePlayer(winner).getName();
            
            broadcast(ChatColor.GOLD + "=== TOURNAMENT FINISHED ===");
            broadcast(ChatColor.GREEN + "Winner: " + ChatColor.WHITE + winnerName);
            broadcast(ChatColor.YELLOW + "Congratulations!");
            broadcast(ChatColor.GOLD + "==========================");
            
            // Execute prize commands
            executePrizeCommands(winner);
        } else {
            broadcast(ChatColor.RED + "Tournament ended with no winner!");
        }
        
        // Clean up
        rip.thecraft.practice.Practice.getInstance().getTournamentManager().removeTournament(this);
    }
    
    private void executePrizeCommands(UUID winner) {
        if (prizeCommands.isEmpty()) return;
        
        Player winnerPlayer = Bukkit.getPlayer(winner);
        if (winnerPlayer == null) return;
        
        for (String command : prizeCommands) {
            String formattedCommand = command.replace("%p%", winnerPlayer.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCommand);
            
            // Debug: Log the command execution
            System.out.println("[TOURNAMENT DEBUG] Executing prize command: " + formattedCommand);
        }
    }
    
    public void broadcast(String message) {
        for (UUID participant : participants) {
            Player player = Bukkit.getPlayer(participant);
            if (player != null) {
                player.sendMessage(message);
            }
        }
        
        // Also send to tournament spectators
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData playerData = rip.thecraft.practice.Practice.getInstance().getPlayerManager().getPlayerData(player);
            if (playerData != null && playerData.getState() == PlayerState.SPECTATING && 
                playerData.getSpectatingTournament() == this) {
                player.sendMessage(message);
            }
        }
    }
    
    public boolean containsPlayer(UUID playerId) {
        return participants.contains(playerId);
    }
    
    public boolean isPlayerEliminated(UUID playerId) {
        return eliminated.contains(playerId);
    }
    
    public int getRemainingPlayerCount() {
        return participants.size() - eliminated.size();
    }
    
    public List<TournamentMatch> getActiveMatches() {
        return Collections.unmodifiableList(activeMatches);
    }
    
    public enum TournamentState {
        WAITING,
        RUNNING,
        ENDED
    }
}
