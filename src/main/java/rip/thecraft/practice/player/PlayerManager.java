package rip.thecraft.practice.player;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import rip.thecraft.practice.Practice;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {

    private final JavaPlugin plugin;
    private final Map<UUID, PlayerData> playerData = new ConcurrentHashMap<>();
    private final MongoCollection<Document> playersCollection;

    public PlayerManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playersCollection = Practice.getInstance().getDatabase().getCollection("players");
    }

    public PlayerData getPlayerData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, this::loadPlayerData);
    }

    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    private PlayerData loadPlayerData(UUID playerId) {
        Document document = playersCollection.find(Filters.eq("_id", playerId.toString())).first();
        if (document != null) {
            return PlayerData.deserialize(document);
        }
        
        // Create new player data - don't save to MongoDB until first match result
        return new PlayerData(playerId);
    }

    public void savePlayerData(PlayerData data) {
        Document document = data.serialize();
        playersCollection.replaceOne(
            Filters.eq("_id", data.getPlayerId().toString()),
            document,
            new ReplaceOptions().upsert(true)
        );
    }

    public void updateKitStats(UUID playerId, String kitName, int eloChange, boolean won) {
        // Update in-memory data first
        PlayerData playerData = getPlayerData(playerId);
        playerData.updateKitStats(kitName, eloChange, won);
        
        // Then save the entire player data to MongoDB
        // This avoids MongoDB update conflicts and ensures data consistency
        savePlayerData(playerData);
    }

    public void saveAllPlayerData() {
        for (PlayerData data : playerData.values()) {
            savePlayerData(data);
        }
    }

    public void removePlayerData(UUID playerId) {
        playerData.remove(playerId);
    }
}
