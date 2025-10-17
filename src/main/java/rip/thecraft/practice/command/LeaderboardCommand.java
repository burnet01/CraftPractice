package rip.thecraft.practice.command;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rip.thecraft.practice.Practice;
import rip.thecraft.practice.kit.Kit;
import rip.thecraft.practice.player.PlayerData;
import rip.thecraft.practice.util.MessageManager;
import java.util.HashMap;
import java.util.Map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class LeaderboardCommand implements CommandExecutor {

    private static final int STARTING_ELO = 1000;
    private static final int LEADERBOARD_SIZE = 10;
    private static final int KIT_LEADERBOARD_SIZE = 10;
    private static final int TOP_KITS_SIZE = 5;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageManager.getInstance().sendPlayerOnly(sender);
            return true;
        }

        Player player = (Player) sender;
        
        if (args.length == 0) {
            showKitELOLeaderboard(player);
            return true;
        }

        // Check if it's a kit name
        String kitName = args[0].toLowerCase();
        if (Practice.getInstance().getKitManager().getKit(kitName) != null) {
            showKitSpecificLeaderboard(player, kitName);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "elo":
                showELOLeaderboard(player);
                break;
            case "wins":
                showWinsLeaderboard(player);
                break;
            default:
                MessageManager.getInstance().sendMessage(player, "leaderboard.unknown-type");
                break;
        }

        return true;
    }

    private void showELOLeaderboard(Player player) {
        List<LeaderboardEntry> entries = getLeaderboard("elo");
        
        Map<String, String> headerPlaceholders = new HashMap<>();
        headerPlaceholders.put("size", String.valueOf(LEADERBOARD_SIZE));
        MessageManager.getInstance().sendMessage(player, "leaderboard.elo.header", headerPlaceholders);
        
        if (entries.isEmpty()) {
            Map<String, String> emptyPlaceholders = new HashMap<>();
            emptyPlaceholders.put("elo", String.valueOf(STARTING_ELO));
            MessageManager.getInstance().sendMessage(player, "leaderboard.elo.empty", emptyPlaceholders);
            return;
        }
        
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntry entry = entries.get(i);
            Map<String, String> entryPlaceholders = new HashMap<>();
            entryPlaceholders.put("position", String.valueOf(i + 1));
            entryPlaceholders.put("player", entry.getPlayerName());
            entryPlaceholders.put("elo", String.valueOf(entry.getValue()));
            MessageManager.getInstance().sendMessage(player, "leaderboard.elo.entry", entryPlaceholders);
        }
    }

    private void showWinsLeaderboard(Player player) {
        List<LeaderboardEntry> entries = getLeaderboard("wins");
        
        Map<String, String> headerPlaceholders = new HashMap<>();
        headerPlaceholders.put("size", String.valueOf(LEADERBOARD_SIZE));
        MessageManager.getInstance().sendMessage(player, "leaderboard.wins.header", headerPlaceholders);
        
        if (entries.isEmpty()) {
            MessageManager.getInstance().sendMessage(player, "leaderboard.wins.empty");
            return;
        }
        
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntry entry = entries.get(i);
            Map<String, String> entryPlaceholders = new HashMap<>();
            entryPlaceholders.put("position", String.valueOf(i + 1));
            entryPlaceholders.put("player", entry.getPlayerName());
            entryPlaceholders.put("wins", String.valueOf(entry.getValue()));
            MessageManager.getInstance().sendMessage(player, "leaderboard.wins.entry", entryPlaceholders);
        }
    }

    private List<LeaderboardEntry> getLeaderboard(String field) {
        MongoCollection<Document> playersCollection = Practice.getInstance().getDatabase().getCollection("players");
        
        // Build filter to exclude players with ELO <= starting ELO for ELO leaderboard
        Document filter = new Document();
        if (field.equals("elo")) {
            filter.append("elo", new Document("$gt", STARTING_ELO));
        }
        
        FindIterable<Document> results = playersCollection
            .find(filter)
            .sort(new Document(field, -1))
            .limit(LEADERBOARD_SIZE);
        
        List<LeaderboardEntry> entries = new ArrayList<>();
        
        for (Document document : results) {
            String playerId = document.getString("_id");
            String playerName = getPlayerName(UUID.fromString(playerId));
            int value = document.getInteger(field, 0);
            
            entries.add(new LeaderboardEntry(playerName, value));
        }
        
        return entries;
    }

    private String getPlayerName(UUID playerId) {
        // Try to get online player first
        Player player = Practice.getInstance().getServer().getPlayer(playerId);
        if (player != null) {
            return player.getName();
        }
        
        // Query player name from database for offline players
        var playerData = Practice.getInstance().getPlayerManager().getPlayerData(playerId);
        if (playerData != null) {
            String playerName = playerData.getPlayerName();
            
            // Return player name if found, otherwise return formatted UUID
            if (playerName != null && !playerName.isEmpty()) {
                return playerName;
            }
        }
        
        // Return a more user-friendly UUID format (first 8 characters)
        return playerId.toString().substring(0, 8) + "...";
    }

    private void showKitELOLeaderboard(Player player) {
        MongoCollection<Document> playersCollection = Practice.getInstance().getDatabase().getCollection("players");
        
        // Get all players with kit stats
        FindIterable<Document> results = playersCollection.find();
        
        List<KitLeaderboardEntry> allEntries = new ArrayList<>();
        
        for (Document document : results) {
            String playerId = document.getString("_id");
            String playerName = getPlayerName(UUID.fromString(playerId));
            
            // Get kit stats
            if (document.containsKey("kitStats")) {
                Document kitStats = (Document) document.get("kitStats");
                for (String kitName : kitStats.keySet()) {
                    Document kitStat = (Document) kitStats.get(kitName);
                    int elo = kitStat.getInteger("elo", 1000);
                    
                    if (elo > STARTING_ELO) {
                        allEntries.add(new KitLeaderboardEntry(playerName, kitName, elo));
                    }
                }
            }
        }
        
        // Sort by ELO descending
        allEntries.sort((a, b) -> Integer.compare(b.getElo(), a.getElo()));
        
        Map<String, String> headerPlaceholders = new HashMap<>();
        headerPlaceholders.put("size", String.valueOf(TOP_KITS_SIZE));
        MessageManager.getInstance().sendMessage(player, "leaderboard.kits.header", headerPlaceholders);
        
        if (allEntries.isEmpty()) {
            Map<String, String> emptyPlaceholders = new HashMap<>();
            emptyPlaceholders.put("elo", String.valueOf(STARTING_ELO));
            MessageManager.getInstance().sendMessage(player, "leaderboard.kits.empty", emptyPlaceholders);
            return;
        }
        
        int count = 0;
        for (int i = 0; i < allEntries.size() && count < TOP_KITS_SIZE; i++) {
            KitLeaderboardEntry entry = allEntries.get(i);
            Map<String, String> entryPlaceholders = new HashMap<>();
            entryPlaceholders.put("position", String.valueOf(count + 1));
            entryPlaceholders.put("player", entry.getPlayerName());
            entryPlaceholders.put("elo", String.valueOf(entry.getElo()));
            entryPlaceholders.put("kit", entry.getKitName());
            MessageManager.getInstance().sendMessage(player, "leaderboard.kits.entry", entryPlaceholders);
            count++;
        }
    }

    private void showKitSpecificLeaderboard(Player player, String kitName) {
        MongoCollection<Document> playersCollection = Practice.getInstance().getDatabase().getCollection("players");
        
        // Build aggregation pipeline to get top players for specific kit
        List<Document> pipeline = Arrays.asList(
            new Document("$match", new Document("kitStats." + kitName + ".elo", new Document("$gt", STARTING_ELO))),
            new Document("$project", new Document()
                .append("playerName", 1)
                .append("elo", "$kitStats." + kitName + ".elo")
                .append("wins", "$kitStats." + kitName + ".wins")
                .append("losses", "$kitStats." + kitName + ".losses")
            ),
            new Document("$sort", new Document("elo", -1)),
            new Document("$limit", KIT_LEADERBOARD_SIZE)
        );
        
        List<KitLeaderboardEntry> entries = new ArrayList<>();
        
        for (Document document : playersCollection.aggregate(pipeline)) {
            String playerName = getPlayerName(UUID.fromString(document.getString("_id")));
            int elo = document.getInteger("elo", 1000);
            entries.add(new KitLeaderboardEntry(playerName, kitName, elo));
        }
        
        Kit kit = Practice.getInstance().getKitManager().getKit(kitName);
        String displayName = kit != null ? kit.getDisplayName() : kitName;
        
        Map<String, String> headerPlaceholders = new HashMap<>();
        headerPlaceholders.put("size", String.valueOf(KIT_LEADERBOARD_SIZE));
        headerPlaceholders.put("kit", displayName);
        MessageManager.getInstance().sendMessage(player, "leaderboard.kit.header", headerPlaceholders);
        
        if (entries.isEmpty()) {
            Map<String, String> emptyPlaceholders = new HashMap<>();
            emptyPlaceholders.put("elo", String.valueOf(STARTING_ELO));
            emptyPlaceholders.put("kit", displayName);
            MessageManager.getInstance().sendMessage(player, "leaderboard.kit.empty", emptyPlaceholders);
            return;
        }
        
        for (int i = 0; i < entries.size(); i++) {
            KitLeaderboardEntry entry = entries.get(i);
            Map<String, String> entryPlaceholders = new HashMap<>();
            entryPlaceholders.put("position", String.valueOf(i + 1));
            entryPlaceholders.put("player", entry.getPlayerName());
            entryPlaceholders.put("elo", String.valueOf(entry.getElo()));
            MessageManager.getInstance().sendMessage(player, "leaderboard.kit.entry", entryPlaceholders);
        }
    }

    private static class LeaderboardEntry {
        private final String playerName;
        private final int value;

        public LeaderboardEntry(String playerName, int value) {
            this.playerName = playerName;
            this.value = value;
        }

        public String getPlayerName() {
            return playerName;
        }

        public int getValue() {
            return value;
        }
    }

    private static class KitLeaderboardEntry {
        private final String playerName;
        private final String kitName;
        private final int elo;

        public KitLeaderboardEntry(String playerName, String kitName, int elo) {
            this.playerName = playerName;
            this.kitName = kitName;
            this.elo = elo;
        }

        public String getPlayerName() {
            return playerName;
        }

        public String getKitName() {
            return kitName;
        }

        public int getElo() {
            return elo;
        }
    }
}
