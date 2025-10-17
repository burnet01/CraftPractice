package rip.thecraft.practice.player;

import org.bson.Document;
import rip.thecraft.practice.match.Match;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {

    private final UUID playerId;
    private String playerName;
    private int globalElo = 1000; // Legacy global ELO for compatibility
    private int globalWins = 0;
    private int globalLosses = 0;
    private PlayerState state = PlayerState.LOBBY;
    private Match spectatingMatch;
    private rip.thecraft.practice.tournament.Tournament spectatingTournament;
    private final Map<String, KitStats> kitStats = new java.util.concurrent.ConcurrentHashMap<>();

    public PlayerData(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    // Legacy methods for compatibility
    public int getElo() {
        return globalElo;
    }

    public void setElo(int elo) {
        this.globalElo = elo;
    }

    public int getWins() {
        return globalWins;
    }

    public void setWins(int wins) {
        this.globalWins = wins;
    }

    public int getLosses() {
        return globalLosses;
    }

    public void setLosses(int losses) {
        this.globalLosses = losses;
    }

    public PlayerState getState() {
        return state;
    }

    public void setState(PlayerState state) {
        this.state = state;
    }

    public Match getSpectatingMatch() {
        return spectatingMatch;
    }

    public void setSpectatingMatch(Match spectatingMatch) {
        this.spectatingMatch = spectatingMatch;
    }

    public rip.thecraft.practice.tournament.Tournament getSpectatingTournament() {
        return spectatingTournament;
    }

    public void setSpectatingTournament(rip.thecraft.practice.tournament.Tournament spectatingTournament) {
        this.spectatingTournament = spectatingTournament;
    }

    public double getWinRate() {
        int total = globalWins + globalLosses;
        return total == 0 ? 0 : (double) globalWins / total * 100;
    }

    // New methods for per-kit statistics
    public KitStats getKitStats(String kitName) {
        return kitStats.computeIfAbsent(kitName.toLowerCase(), k -> new KitStats());
    }

    public Map<String, KitStats> getKitStats() {
        return kitStats;
    }

    public void updateKitStats(String kitName, int eloChange, boolean won) {
        KitStats stats = getKitStats(kitName);
        stats.setElo(stats.getElo() + eloChange);
        if (won) {
            stats.setWins(stats.getWins() + 1);
            globalWins++;
        } else {
            stats.setLosses(stats.getLosses() + 1);
            globalLosses++;
        }
        
        // Update global ELO as well (weighted average of all kit ELOs)
        updateGlobalElo();
    }
    
    private void updateGlobalElo() {
        if (kitStats.isEmpty()) {
            globalElo = 1000; // Default if no kits
            return;
        }
        
        // Calculate weighted average based on number of games played
        int totalGames = 0;
        int weightedEloSum = 0;
        
        for (KitStats stats : kitStats.values()) {
            int games = stats.getWins() + stats.getLosses();
            if (games > 0) {
                totalGames += games;
                weightedEloSum += stats.getElo() * games;
            }
        }
        
        if (totalGames > 0) {
            globalElo = weightedEloSum / totalGames;
        } else {
            globalElo = 1000; // Default if no games played
        }
        
        // Debug logging to verify calculation
        System.out.println("[DEBUG] Global ELO updated: " + globalElo + " (totalGames: " + totalGames + ", weightedSum: " + weightedEloSum + ")");
    }

    public Document serialize() {
        Document document = new Document();
        document.put("_id", playerId.toString());
        document.put("playerName", playerName);
        document.put("globalElo", globalElo);
        document.put("globalWins", globalWins);
        document.put("globalLosses", globalLosses);
        
        // Serialize kit stats
        Document kitStatsDocument = new Document();
        for (Map.Entry<String, KitStats> entry : kitStats.entrySet()) {
            kitStatsDocument.put(entry.getKey(), entry.getValue().serialize());
        }
        document.put("kitStats", kitStatsDocument);
        
        return document;
    }

    public static PlayerData deserialize(Document document) {
        UUID playerId = UUID.fromString(document.getString("_id"));
        PlayerData data = new PlayerData(playerId);
        data.setPlayerName(document.getString("playerName"));
        
        // Handle legacy data migration
        if (document.containsKey("elo")) {
            data.setElo(document.getInteger("elo", 1000));
        } else {
            data.setElo(document.getInteger("globalElo", 1000));
        }
        
        if (document.containsKey("wins")) {
            data.setWins(document.getInteger("wins", 0));
        } else {
            data.setWins(document.getInteger("globalWins", 0));
        }
        
        if (document.containsKey("losses")) {
            data.setLosses(document.getInteger("losses", 0));
        } else {
            data.setLosses(document.getInteger("globalLosses", 0));
        }
        
        // Deserialize kit stats
        if (document.containsKey("kitStats")) {
            Document kitStatsDocument = (Document) document.get("kitStats");
            for (String kitName : kitStatsDocument.keySet()) {
                Document kitStatDoc = (Document) kitStatsDocument.get(kitName);
                data.kitStats.put(kitName, KitStats.deserialize(kitStatDoc));
            }
        }
        
        return data;
    }
}
