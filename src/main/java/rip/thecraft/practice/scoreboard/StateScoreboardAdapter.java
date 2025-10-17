package rip.thecraft.practice.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import rip.thecraft.practice.Practice;
import rip.thecraft.practice.kit.Kit;
import rip.thecraft.practice.match.Match;
import rip.thecraft.practice.match.MatchManager;
import rip.thecraft.practice.player.PlayerData;
import rip.thecraft.practice.player.PlayerManager;
import rip.thecraft.practice.player.PlayerState;
import rip.thecraft.practice.queue.Queue;
import rip.thecraft.practice.queue.QueueManager;
import rip.thecraft.practice.queue.QueueType;
import rip.thecraft.practice.util.scoreboard.BoardAdapter;
import rip.thecraft.practice.util.scoreboard.BoardStyle;
import rip.thecraft.practice.util.scoreboard.RGBHelper;

import java.util.*;

/**
 * State-based scoreboard adapter that provides different scoreboards based on player state
 */
public class StateScoreboardAdapter implements BoardAdapter {

    private final Practice plugin = Practice.getInstance();
    private final PlayerManager playerManager;
    private final MatchManager matchManager;
    private final QueueManager queueManager;
    
    private int animationIndex = 0;
    private long lastUpdate = 0;
    
    // Leaderboard caching
    private final Map<UUID, Integer> leaderboardCache = new HashMap<>();
    private long lastLeaderboardUpdate = 0;
    private static final long LEADERBOARD_CACHE_DURATION = 30000; // 30 seconds

    // RGB animation frames for title
    private final String[] rgbFrames = {
        "&#9D1BFF&lT&#9706FF&lh&#9D1BFF&le&#A430FF&lC&#AA45FF&lr&#B15AFF&la&#B76FFF&lf&#BE84FF&lt",
        "&#A430FF&lT&#9D1BFF&lh&#9706FF&le&#9D1BFF&lC&#A430FF&lr&#AA45FF&la&#B15AFF&lf&#B76FFF&lt",
        "&#AA45FF&lT&#A430FF&lh&#9D1BFF&le&#9706FF&lC&#9D1BFF&lr&#A430FF&la&#AA45FF&lf&#B15AFF&lt",
        "&#B15AFF&lT&#AA45FF&lh&#A430FF&le&#9D1BFF&lC&#9706FF&lr&#9D1BFF&la&#A430FF&lf&#AA45FF&lt",
        "&#B76FFF&lT&#B15AFF&lh&#AA45FF&le&#A430FF&lC&#9D1BFF&lr&#9706FF&la&#9D1BFF&lf&#A430FF&lt",
        "&#BE84FF&lT&#B76FFF&lh&#B15AFF&le&#AA45FF&lC&#A430FF&lr&#9D1BFF&la&#9706FF&lf&#9D1BFF&lt",
        "&#C499FF&lT&#BE84FF&lh&#B76FFF&le&#B15AFF&lC&#AA45FF&lr&#A430FF&la&#9D1BFF&lf&#9706FF&lt",
        "&#BE84FF&lT&#C499FF&lh&#BE84FF&le&#B76FFF&lC&#B15AFF&lr&#AA45FF&la&#A430FF&lf&#9D1BFF&lt",
        "&#B76FFF&lT&#BE84FF&lh&#C499FF&le&#BE84FF&lC&#B76FFF&lr&#B15AFF&la&#AA45FF&lf&#A430FF&lt",
        "&#B15AFF&lT&#B76FFF&lh&#BE84FF&le&#C499FF&lC&#BE84FF&lr&#B76FFF&la&#B15AFF&lf&#AA45FF&lt",
        "&#AA45FF&lT&#B15AFF&lh&#B76FFF&le&#BE84FF&lC&#C499FF&lr&#BE84FF&la&#B76FFF&lf&#B15AFF&lt",
        "&#A430FF&lT&#AA45FF&lh&#B15AFF&le&#B76FFF&lC&#BE84FF&lr&#C499FF&la&#BE84FF&lf&#B76FFF&lt",
        "&#9D1BFF&lT&#A430FF&lh&#AA45FF&le&#B15AFF&lC&#B76FFF&lr&#BE84FF&la&#C499FF&lf&#BE84FF&lt",
        "&#9706FF&lT&#9D1BFF&lh&#A430FF&le&#AA45FF&lC&#B15AFF&lr&#B76FFF&la&#BE84FF&lf&#C499FF&lt"
    };

    public StateScoreboardAdapter() {
        this.playerManager = plugin.getPlayerManager();
        this.matchManager = plugin.getMatchManager();
        this.queueManager = plugin.getQueueManager();
    }

    @Override
    public String getTitle(Player player) {
        // Update animation every 500ms for better performance (reduced from 150ms)
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdate >= 500) {
            animationIndex = (animationIndex + 1) % rgbFrames.length;
            lastUpdate = currentTime;
        }

        return RGBHelper.translate(rgbFrames[animationIndex]);
    }

    @Override
    public List<String> getLines(Player player) {
        PlayerData playerData = playerManager.getPlayerData(player);
        PlayerState state = playerData.getState();

        switch (state) {
            case LOBBY:
                return getLobbyScoreboard(player, playerData);
            case QUEUE:
                return getQueueScoreboard(player, playerData);
            case MATCH:
                return getMatchScoreboard(player, playerData);
            case SPECTATING:
                return getSpectatingScoreboard(player, playerData);
            default:
                return getDefaultScoreboard(player, playerData);
        }
    }

    @Override
    public BoardStyle getBoardStyle(Player player) {
        return BoardStyle.MODERN;
    }

    private List<String> getLobbyScoreboard(Player player, PlayerData playerData) {
        List<String> lines = new ArrayList<>();
        lines.add("&7&m----------------------");
        
        // Show leaderboard position instead of ELO
        int leaderboardPosition = getLeaderboardPosition(player);
        if (leaderboardPosition > 0) {
            lines.add("&fLeaderboard: &6#" + leaderboardPosition);
        } else if (leaderboardPosition == -1) {
            lines.add("&fLeaderboard: &7#N/A");
        }
        
        lines.add("");
        lines.add("&fOnline: &b" + Bukkit.getOnlinePlayers().size());
        lines.add("&fIn Queue: &b" + getTotalQueuePlayers());
        lines.add("&fIn Match: &b" + getTotalMatchPlayers());
        lines.add("");
        lines.add("&7&m----------------------");
        return lines;
    }

    private List<String> getQueueScoreboard(Player player, PlayerData playerData) {
        List<String> lines = new ArrayList<>();
        lines.add("&7&m----------------------");
        
        // Get queue info
        Kit kit = queueManager.getPlayerQueueKit(player.getUniqueId());
        QueueType type = queueManager.getPlayerQueueType(player.getUniqueId());
        
        if (kit != null && type != null) {
            lines.add("&fKit: &e" + kit.getName());
            lines.add("&fType: &a" + (type == QueueType.RANKED ? "Ranked" : "Unranked"));
        }
        
        // Show wait time instead of position
        long waitTime = getQueueWaitTime(player);
        lines.add("&fWait: &b" + formatWaitTime(waitTime));
        
        lines.add("");
        lines.add("&eWaiting for opponent...");
        lines.add("&7&m----------------------");
        return lines;
    }

    private List<String> getMatchScoreboard(Player player, PlayerData playerData) {
        List<String> lines = new ArrayList<>();
        Match match = matchManager.getPlayerMatch(player.getUniqueId());
        
        if (match == null) {
            return getDefaultScoreboard(player, playerData);
        }

        lines.add("&7&m----------------------");
        lines.add("&fKit: &e" + match.getKit().getName());
        lines.add("&fType: &a" + (match.getType() == QueueType.RANKED ? "Ranked" : "Unranked"));
        lines.add("&fDuration: &b" + formatDuration(match.getDuration()));
        lines.add("");
        
        // Get opponent info
        UUID opponentId = getOpponent(player.getUniqueId(), match);
        if (opponentId != null) {
            Player opponent = Bukkit.getPlayer(opponentId);
            if (opponent != null) {
                PlayerData opponentData = playerManager.getPlayerData(opponent);
                lines.add("&fOpponent: &c" + opponent.getName());
                
                // Only show ELO for ranked matches
                if (match.getType() == QueueType.RANKED) {
                    lines.add("&fOpponent ELO: &c" + opponentData.getElo());
                    lines.add("&fYour ELO: &e" + playerData.getElo());
                }
            }
        }
        
        lines.add("&7&m----------------------");
        return lines;
    }

    private List<String> getSpectatingScoreboard(Player player, PlayerData playerData) {
        List<String> lines = new ArrayList<>();
        
        // Get the match being spectated
        Match match = playerData.getSpectatingMatch();
        if (match == null) {
            return getDefaultScoreboard(player, playerData);
        }

        lines.add("&7&m----------------------");
        lines.add("&fSpectating Match");
        lines.add("");
        
        // Show match information similar to what players see
        lines.add("&fKit: &e" + match.getKit().getName());
        lines.add("&fType: &a" + (match.getType() == QueueType.RANKED ? "Ranked" : "Unranked"));
        lines.add("&fDuration: &b" + formatDuration(match.getDuration()));
        lines.add("");
        
        // Show player information
        Player player1 = Bukkit.getPlayer(match.getPlayer1());
        Player player2 = Bukkit.getPlayer(match.getPlayer2());
        
        if (player1 != null && player2 != null) {
            PlayerData player1Data = playerManager.getPlayerData(player1);
            PlayerData player2Data = playerManager.getPlayerData(player2);
            
            // Only show ELO for ranked matches
            if (match.getType() == QueueType.RANKED) {
                lines.add("&f" + player1.getName() + ": &c" + player1Data.getElo());
                lines.add("&f" + player2.getName() + ": &c" + player2Data.getElo());
            } else {
                // For unranked matches, just show player names
                lines.add("&f" + player1.getName() + " vs " + player2.getName());
            }
        }
        
        lines.add("");
        lines.add("&eUse &a/spec &eto leave");
        lines.add("&7&m----------------------");
        return lines;
    }

    private List<String> getDefaultScoreboard(Player player, PlayerData playerData) {
        List<String> lines = new ArrayList<>();
        lines.add("&7&m----------------------");
        lines.add("&fWelcome to Practice");
        lines.add("");
        lines.add("&fOnline: &b" + Bukkit.getOnlinePlayers().size());
        
        // Show leaderboard position instead of ELO
        int leaderboardPosition = getLeaderboardPosition(player);
        if (leaderboardPosition > 0) {
            lines.add("&fLeaderboard: &6#" + leaderboardPosition);
        } else if (leaderboardPosition == -1) {
            lines.add("&fLeaderboard: &7#N/A");
        }
        
        lines.add("");
        lines.add("&7&m----------------------");
        return lines;
    }

    // Helper methods - now using the actual methods we added
    private int getTotalQueuePlayers() {
        return queueManager.getTotalQueuePlayers();
    }

    private int getTotalMatchPlayers() {
        return matchManager.getTotalMatchPlayers();
    }

    private String getQueueInfo(Player player) {
        Kit kit = queueManager.getPlayerQueueKit(player.getUniqueId());
        QueueType type = queueManager.getPlayerQueueType(player.getUniqueId());
        if (kit != null && type != null) {
            return kit.getName() + " " + type.name();
        }
        return "Unknown";
    }

    private int getQueuePosition(Player player) {
        int position = queueManager.getPlayerQueuePosition(player.getUniqueId());
        return position > 0 ? position : 1;
    }

    private int getQueueSize(Player player) {
        return queueManager.getPlayerQueueSize(player.getUniqueId());
    }

    private UUID getOpponent(UUID playerId, Match match) {
        if (match.getPlayer1().equals(playerId)) {
            return match.getPlayer2();
        } else if (match.getPlayer2().equals(playerId)) {
            return match.getPlayer1();
        }
        return null;
    }

    private String formatDuration(long duration) {
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // New helper methods for queue wait time and leaderboard
    private long getQueueWaitTime(Player player) {
        return queueManager.getPlayerQueueWaitTime(player.getUniqueId());
    }

    private String formatWaitTime(long waitTime) {
        double totalSeconds = waitTime / 1000.0;
        
        if (totalSeconds < 60) {
            // For wait times less than 60 seconds, show as seconds with one decimal place
            return String.format("%.1fs", totalSeconds);
        } else {
            // For wait times 60 seconds or more, convert to minutes:seconds format
            int minutes = (int) (totalSeconds / 60);
            int seconds = (int) (totalSeconds % 60);
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    private int getLeaderboardPosition(Player player) {
        PlayerData playerData = playerManager.getPlayerData(player);
        if (playerData == null) return -1;
        
        // Check if player has played any ranked matches (wins + losses > 0)
        int totalMatches = playerData.getWins() + playerData.getLosses();
        if (totalMatches == 0) {
            return -1; // No matches played, return -1 to indicate N/A
        }
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check cache first
        if (leaderboardCache.containsKey(playerId) && 
            (currentTime - lastLeaderboardUpdate) < LEADERBOARD_CACHE_DURATION) {
            return leaderboardCache.get(playerId);
        }
        
        // Cache expired or not found, query database
        try {
            var database = Practice.getInstance().getDatabase();
            if (database == null) return -1;
            
            var playersCollection = database.getCollection("players");
            
            // Count players with higher global ELO than this player
            var filter = new org.bson.Document("globalElo", new org.bson.Document("$gt", playerData.getElo()));
            long playersAbove = playersCollection.countDocuments(filter);
            
            // Position is players above + 1
            int position = (int) playersAbove + 1;
            
            // Ensure position is at least 1
            position = Math.max(1, position);
            
            // Update cache
            leaderboardCache.put(playerId, position);
            lastLeaderboardUpdate = currentTime;
            
            return position;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get leaderboard position for " + player.getName() + ": " + e.getMessage());
            return -1;
        }
    }
}
