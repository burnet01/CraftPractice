package rip.thecraft.practice.queue;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import rip.thecraft.practice.Practice;
import rip.thecraft.practice.arena.Arena;
import rip.thecraft.practice.kit.Kit;
import rip.thecraft.practice.match.Match;
import rip.thecraft.practice.match.MatchManager;
import rip.thecraft.practice.player.PlayerData;
import rip.thecraft.practice.player.PlayerState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QueueManager {

    private final JavaPlugin plugin;
    private MatchManager matchManager;
    
    private final Map<QueueType, Map<String, Queue>> queues = new ConcurrentHashMap<>();
    private final Map<UUID, QueueEntry> playerQueues = new ConcurrentHashMap<>();

    public QueueManager(JavaPlugin plugin) {
        this.plugin = plugin;
        
        // Initialize queue types
        for (QueueType type : QueueType.values()) {
            queues.put(type, new ConcurrentHashMap<>());
        }
    }
    
    public void setMatchManager(MatchManager matchManager) {
        this.matchManager = matchManager;
    }

    public boolean joinQueue(Player player, String kitName, QueueType type) {
        if (playerQueues.containsKey(player.getUniqueId())) {
            if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
                player.sendMessage("§cYou are already in a queue!");
            }
            return false; // Already in queue
        }

        Kit kit = Practice.getInstance().getKitManager().getKit(kitName);
        if (kit == null) {
            if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
                player.sendMessage("§cKit '" + kitName + "' not found!");
            }
            return false;
        }

        // Check if kit is enabled
        if (!kit.isEnabled()) {
            if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
                player.sendMessage("§cThis kit is currently disabled!");
            }
            return false;
        }

        Queue queue = queues.get(type).computeIfAbsent(kitName.toLowerCase(), k -> new Queue(kit, type));
        QueueEntry entry = new QueueEntry(player.getUniqueId(), kit, type);
        
        playerQueues.put(player.getUniqueId(), entry);
        queue.addPlayer(player.getUniqueId());

        // Update player state to QUEUE
        PlayerData playerData = Practice.getInstance().getPlayerManager().getPlayerData(player);
        if (playerData != null) {
            playerData.setState(PlayerState.QUEUE);
        }

        // Update scoreboard
        rip.thecraft.practice.scoreboard.ScoreboardIntegration.updateQueueScoreboard(player);

        // Update player items
        Practice.getInstance().getItemManager().updatePlayerItems(player);

        // Send join message if messages are enabled
        if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
            player.sendMessage("§aYou have joined the " + type.name().toLowerCase() + " queue for " + kitName + "!");
        }

        // Start matchmaking
        startMatchmaking(queue);
        
        return true;
    }

    public boolean leaveQueue(Player player) {
        QueueEntry entry = playerQueues.remove(player.getUniqueId());
        if (entry == null) {
            return false;
        }

        Queue queue = queues.get(entry.getType()).get(entry.getKit().getName().toLowerCase());
        if (queue != null) {
            queue.removePlayer(player.getUniqueId());
        }

        // Update player state to LOBBY
        PlayerData playerData = Practice.getInstance().getPlayerManager().getPlayerData(player);
        if (playerData != null) {
            playerData.setState(PlayerState.LOBBY);
        }

        // Update scoreboard
        rip.thecraft.practice.scoreboard.ScoreboardIntegration.updateLobbyScoreboard(player);
        
        // Update player items
        Practice.getInstance().getItemManager().updatePlayerItems(player);
        
        // Send leave message if messages are enabled
        if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
            player.sendMessage("§aLeft the queue!");
        }
        
        return true;
    }

    public QueueEntry getPlayerQueue(UUID playerId) {
        return playerQueues.get(playerId);
    }

    private void startMatchmaking(Queue queue) {
        if (queue.getPlayers().size() < 2) {
            return;
        }

        // Simple matchmaking - find first two players
        List<UUID> players = new ArrayList<>(queue.getPlayers());
        if (players.size() >= 2) {
            UUID player1 = players.get(0);
            UUID player2 = players.get(1);

            // Remove from queue
            playerQueues.remove(player1);
            playerQueues.remove(player2);
            queue.removePlayer(player1);
            queue.removePlayer(player2);

            // Start match
            Player p1 = Bukkit.getPlayer(player1);
            Player p2 = Bukkit.getPlayer(player2);
            
            if (p1 != null && p2 != null && matchManager != null) {
                matchManager.startMatch(p1, p2, queue.getKit(), queue.getType());
            }
        }
    }

    public void updateELO(UUID winner, UUID loser, QueueType type, String kitName) {
        if (type != QueueType.RANKED) return;

        PlayerData winnerData = Practice.getInstance().getPlayerManager().getPlayerData(winner);
        PlayerData loserData = Practice.getInstance().getPlayerManager().getPlayerData(loser);

        if (winnerData != null && loserData != null) {
            // Get kit-specific ELO
            int winnerELO = winnerData.getKitStats(kitName).getElo();
            int loserELO = loserData.getKitStats(kitName).getElo();

            // Simple ELO calculation
            double expectedWinner = 1.0 / (1.0 + Math.pow(10, (loserELO - winnerELO) / 400.0));
            double expectedLoser = 1.0 - expectedWinner;

            int kFactor = 32;
            int winnerChange = (int) (kFactor * (1 - expectedWinner));
            int loserChange = (int) (kFactor * (0 - expectedLoser));

            // Update kit-specific stats using partial MongoDB updates
            Practice.getInstance().getPlayerManager().updateKitStats(winner, kitName, winnerChange, true);
            Practice.getInstance().getPlayerManager().updateKitStats(loser, kitName, loserChange, false);
        }
    }

    // Legacy method for compatibility
    public void updateELO(UUID winner, UUID loser, QueueType type) {
        // This method is deprecated - use the one with kitName instead
        // For now, use a default kit name if not specified
        updateELO(winner, loser, type, "global");
    }

    public Map<QueueType, Map<String, Queue>> getQueues() {
        return queues;
    }

    public void shutdown() {
        if (matchManager != null) {
            matchManager.shutdown();
        }
    }

    // Scoreboard integration methods
    public int getTotalQueuePlayers() {
        return playerQueues.size();
    }

    public Kit getPlayerQueueKit(UUID playerId) {
        QueueEntry entry = playerQueues.get(playerId);
        return entry != null ? entry.getKit() : null;
    }

    public QueueType getPlayerQueueType(UUID playerId) {
        QueueEntry entry = playerQueues.get(playerId);
        return entry != null ? entry.getType() : null;
    }

    public int getPlayerQueuePosition(UUID playerId) {
        QueueEntry entry = playerQueues.get(playerId);
        if (entry == null) return -1;

        Queue queue = queues.get(entry.getType()).get(entry.getKit().getName().toLowerCase());
        if (queue == null) return -1;

        List<UUID> players = new ArrayList<>(queue.getPlayers());
        return players.indexOf(playerId) + 1; // 1-based position
    }

    public int getPlayerQueueSize(UUID playerId) {
        QueueEntry entry = playerQueues.get(playerId);
        if (entry == null) return 0;

        Queue queue = queues.get(entry.getType()).get(entry.getKit().getName().toLowerCase());
        return queue != null ? queue.getPlayers().size() : 0;
    }

    public long getPlayerQueueWaitTime(UUID playerId) {
        QueueEntry entry = playerQueues.get(playerId);
        return entry != null ? entry.getQueueTime() : 0;
    }
}
