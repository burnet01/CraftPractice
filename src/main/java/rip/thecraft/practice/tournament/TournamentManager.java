package rip.thecraft.practice.tournament;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import rip.thecraft.practice.kit.Kit;
import rip.thecraft.practice.kit.KitManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class TournamentManager {

    private final Map<String, Tournament> activeTournaments;
    private final Map<String, Tournament> tournamentsByKit;
    
    public TournamentManager() {
        this.activeTournaments = new ConcurrentHashMap<>();
        this.tournamentsByKit = new ConcurrentHashMap<>();
    }
    
    public Tournament createTournament(Kit kit, Player host, int maxPlayers, int requiredPlayers, List<String> prizeCommands) {
        // Check if there's already a tournament for this kit
        if (tournamentsByKit.containsKey(kit.getName().toLowerCase())) {
            host.sendMessage(ChatColor.RED + "There is already an active tournament for " + kit.getDisplayName() + "!");
            return null;
        }
        
        // Check if kit is a build kit
        if (kit.isBuildMode()) {
            host.sendMessage(ChatColor.RED + "Build kits cannot be used for tournaments!");
            return null;
        }
        
        Tournament tournament = new Tournament(kit, host.getUniqueId(), maxPlayers, requiredPlayers, prizeCommands);
        activeTournaments.put(tournament.getId(), tournament);
        tournamentsByKit.put(kit.getName().toLowerCase(), tournament);
        
        return tournament;
    }
    
    public void removeTournament(Tournament tournament) {
        activeTournaments.remove(tournament.getId());
        tournamentsByKit.remove(tournament.getKit().getName().toLowerCase());
    }
    
    public Tournament getTournament(String id) {
        return activeTournaments.get(id);
    }
    
    public Tournament getTournamentByKit(String kitName) {
        return tournamentsByKit.get(kitName.toLowerCase());
    }
    
    public boolean hasActiveTournament(String kitName) {
        return tournamentsByKit.containsKey(kitName.toLowerCase());
    }
    
    public boolean joinTournament(Player player, String kitName) {
        Tournament tournament = getTournamentByKit(kitName);
        if (tournament == null) {
            player.sendMessage(ChatColor.RED + "No active tournament found for " + kitName + "!");
            return false;
        }
        
        return tournament.addParticipant(player);
    }
    
    public void endTournament(String kitName) {
        Tournament tournament = getTournamentByKit(kitName);
        if (tournament != null) {
            tournament.endTournament(null);
        }
    }
    
    public void endAllTournaments() {
        for (Tournament tournament : new ArrayList<>(activeTournaments.values())) {
            tournament.endTournament(null);
        }
        activeTournaments.clear();
        tournamentsByKit.clear();
    }
    
    public List<Tournament> getActiveTournaments() {
        return new ArrayList<>(activeTournaments.values());
    }
    
    public Tournament getPlayerTournament(UUID playerId) {
        for (Tournament tournament : activeTournaments.values()) {
            if (tournament.containsPlayer(playerId)) {
                return tournament;
            }
        }
        return null;
    }
    
    public boolean isPlayerInTournament(UUID playerId) {
        return getPlayerTournament(playerId) != null;
    }
    
    public void handlePlayerQuit(Player player) {
        Tournament tournament = getPlayerTournament(player.getUniqueId());
        if (tournament != null) {
            tournament.removeParticipant(player);
        }
    }
    
    public void shutdown() {
        endAllTournaments();
    }
}
