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
import rip.thecraft.practice.queue.QueueManager;
import rip.thecraft.practice.queue.QueueType;
import rip.thecraft.practice.util.scoreboard.BoardAdapter;
import rip.thecraft.practice.util.scoreboard.BoardStyle;
import rip.thecraft.practice.util.scoreboard.RGBHelper;

import java.util.*;

/**
 * Configurable scoreboard adapter that uses scoreboard.yml for customization
 */
public class ConfigurableScoreboardAdapter implements BoardAdapter {

    private final Practice plugin = Practice.getInstance();
    private final PlayerManager playerManager;
    private final MatchManager matchManager;
    private final QueueManager queueManager;
    private final ScoreboardConfig scoreboardConfig;
    
    // Animation state tracking
    private int titleAnimationIndex = 0;
    private long lastTitleUpdate = 0;
    private final Map<UUID, Map<Integer, Integer>> lineAnimationStates = new HashMap<>();
    
    // Leaderboard caching
    private final Map<UUID, Integer> leaderboardCache = new HashMap<>();
    private long lastLeaderboardUpdate = 0;
    private static final long LEADERBOARD_CACHE_DURATION = 30000; // 30 seconds

    public ConfigurableScoreboardAdapter(ScoreboardConfig scoreboardConfig) {
        this.playerManager = plugin.getPlayerManager();
        this.matchManager = plugin.getMatchManager();
        this.queueManager = plugin.getQueueManager();
        this.scoreboardConfig = scoreboardConfig;
    }

    @Override
    public String getTitle(Player player) {
        PlayerData playerData = playerManager.getPlayerData(player);
        if (playerData == null) {
            return "&6&lCraftPractice";
        }
        
        PlayerState state = playerData.getState();
        ScoreboardConfig.ScoreboardTemplate template = getTemplateForState(state);
        
        if (template == null) {
            return "&6&lCraftPractice";
        }
        
        if (template.hasAnimatedTitle()) {
            // Update animation based on configured speed
            long currentTime = System.currentTimeMillis();
            long animationInterval = (1000L / template.getTitleAnimationSpeed());
            
            if (currentTime - lastTitleUpdate >= animationInterval) {
                titleAnimationIndex = (titleAnimationIndex + 1) % template.getAnimatedTitleFrames().size();
                lastTitleUpdate = currentTime;
            }
            
            return RGBHelper.translate(template.getTitleFrame(titleAnimationIndex));
        } else {
            return RGBHelper.translate(template.getStaticTitle());
        }
    }

    @Override
    public List<String> getLines(Player player) {
        PlayerData playerData = playerManager.getPlayerData(player);
        if (playerData == null) {
            return getDefaultLines();
        }
        
        PlayerState state = playerData.getState();
        ScoreboardConfig.ScoreboardTemplate template = getTemplateForState(state);
        
        if (template == null) {
            return getDefaultLines();
        }
        
        List<String> processedLines = new ArrayList<>();
        List<String> rawLines = template.getLines();
        
        // Process each line with placeholders and animations
        for (int i = 0; i < rawLines.size(); i++) {
            String line = rawLines.get(i);
            String processedLine = replacePlaceholders(line, player, playerData, state);
            
            // Apply line animations if configured
            processedLine = applyLineAnimation(player.getUniqueId(), i, processedLine, template);
            
            processedLines.add(processedLine);
        }
        
        return processedLines;
    }

    @Override
    public BoardStyle getBoardStyle(Player player) {
        return BoardStyle.MODERN;
    }
    
    private ScoreboardConfig.ScoreboardTemplate getTemplateForState(PlayerState state) {
        String stateName = state.name().toLowerCase();
        ScoreboardConfig.ScoreboardTemplate template = scoreboardConfig.getTemplate(stateName);
        
        if (template == null) {
            // Fallback to default template
            template = scoreboardConfig.getTemplate("default");
        }
        
        return template;
    }
    
    private String replacePlaceholders(String line, Player player, PlayerData playerData, PlayerState state) {
        if (line == null) return "";
        
        String result = line;
        
        // Player placeholders
        result = result.replace("{player_name}", player.getName());
        result = result.replace("{player_elo}", String.valueOf(playerData.getElo()));
        
        // Leaderboard position
        int leaderboardPosition = getLeaderboardPosition(player);
        if (leaderboardPosition > 0) {
            result = result.replace("{leaderboard_position}", String.valueOf(leaderboardPosition));
        } else {
            result = result.replace("{leaderboard_position}", "N/A");
        }
        
        // Server placeholders
        result = result.replace("{online_players}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        result = result.replace("{queue_players}", String.valueOf(getTotalQueuePlayers()));
        result = result.replace("{match_players}", String.valueOf(getTotalMatchPlayers()));
        
        // State-specific placeholders
        switch (state) {
            case QUEUE:
                result = replaceQueuePlaceholders(result, player);
                break;
            case MATCH:
                result = replaceMatchPlaceholders(result, player, playerData);
                break;
            case SPECTATING:
                result = replaceSpectatingPlaceholders(result, player, playerData);
                break;
        }
        
        return result;
    }
    
    private String replaceQueuePlaceholders(String line, Player player) {
        String result = line;
        
        Kit kit = queueManager.getPlayerQueueKit(player.getUniqueId());
        QueueType type = queueManager.getPlayerQueueType(player.getUniqueId());
        
        if (kit != null) {
            result = result.replace("{kit_name}", kit.getName());
        } else {
            result = result.replace("{kit_name}", "Unknown");
        }
        
        if (type != null) {
            result = result.replace("{queue_type}", type == QueueType.RANKED ? "Ranked" : "Unranked");
        } else {
            result = result.replace("{queue_type}", "Unknown");
        }
        
        int position = queueManager.getPlayerQueuePosition(player.getUniqueId());
        result = result.replace("{queue_position}", String.valueOf(position > 0 ? position : 1));
        
        int size = queueManager.getPlayerQueueSize(player.getUniqueId());
        result = result.replace("{queue_size}", String.valueOf(size));
        
        long waitTime = queueManager.getPlayerQueueWaitTime(player.getUniqueId());
        result = result.replace("{wait_time}", formatWaitTime(waitTime));
        
        return result;
    }
    
    private String replaceMatchPlaceholders(String line, Player player, PlayerData playerData) {
        String result = line;
        
        Match match = matchManager.getPlayerMatch(player.getUniqueId());
        if (match == null) return result;
        
        result = result.replace("{kit_name}", match.getKit().getName());
        result = result.replace("{match_type}", match.getType() == QueueType.RANKED ? "Ranked" : "Unranked");
        result = result.replace("{duration}", formatDuration(match.getDuration()));
        
        // Opponent information
        UUID opponentId = getOpponent(player.getUniqueId(), match);
        if (opponentId != null) {
            Player opponent = Bukkit.getPlayer(opponentId);
            if (opponent != null) {
                PlayerData opponentData = playerManager.getPlayerData(opponent);
                result = result.replace("{opponent_name}", opponent.getName());
                result = result.replace("{opponent_elo}", String.valueOf(opponentData.getElo()));
            }
        }
        
        return result;
    }
    
    private String replaceSpectatingPlaceholders(String line, Player player, PlayerData playerData) {
        String result = line;
        
        Match match = playerData.getSpectatingMatch();
        if (match == null) return result;
        
        result = result.replace("{kit_name}", match.getKit().getName());
        result = result.replace("{match_type}", match.getType() == QueueType.RANKED ? "Ranked" : "Unranked");
        result = result.replace("{duration}", formatDuration(match.getDuration()));
        
        // Player information
        Player player1 = Bukkit.getPlayer(match.getPlayer1());
        Player player2 = Bukkit.getPlayer(match.getPlayer2());
        
        if (player1 != null && player2 != null) {
            PlayerData player1Data = playerManager.getPlayerData(player1);
            PlayerData player2Data = playerManager.getPlayerData(player2);
            
            result = result.replace("{player1_name}", player1.getName());
            result = result.replace("{player2_name}", player2.getName());
            result = result.replace("{player1_elo}", String.valueOf(player1Data.getElo()));
            result = result.replace("{player2_elo}", String.valueOf(player2Data.getElo()));
        }
        
        return result;
    }
    
    private String applyLineAnimation(UUID playerId, int lineIndex, String line, ScoreboardConfig.ScoreboardTemplate template) {
        if (!scoreboardConfig.isAnimationsEnabled()) {
            return line;
        }
        
        Map<String, ScoreboardConfig.LineAnimation> animations = template.getLineAnimations();
        if (animations.isEmpty()) {
            return line;
        }
        
        // Find if this line has an animation
        for (Map.Entry<String, ScoreboardConfig.LineAnimation> entry : animations.entrySet()) {
            String animationKey = entry.getKey();
            ScoreboardConfig.LineAnimation animation = entry.getValue();
            
            // Check if this line contains the animation key (simple matching for now)
            if (line.contains(animationKey)) {
                // Get or create animation state for this player and line
                Map<Integer, Integer> playerAnimations = lineAnimationStates.computeIfAbsent(playerId, k -> new HashMap<>());
                int animationState = playerAnimations.getOrDefault(lineIndex, 0);
                
                // Update animation state based on speed
                long currentTime = System.currentTimeMillis();
                long animationInterval = (1000L / animation.getSpeed());
                
                // Simple animation state tracking - we'll update this more sophisticatedly later
                if (currentTime % animationInterval == 0) {
                    animationState = (animationState + 1) % animation.getFrames().size();
                    playerAnimations.put(lineIndex, animationState);
                }
                
                // Replace the line with animated frame
                return animation.getFrame(animationState);
            }
        }
        
        return line;
    }
    
    private List<String> getDefaultLines() {
        List<String> lines = new ArrayList<>();
        lines.add("&7&m----------------------");
        lines.add("&fWelcome to Practice");
        lines.add("");
        lines.add("&fOnline: &b" + Bukkit.getOnlinePlayers().size());
        lines.add("&7&m----------------------");
        return lines;
    }
    
    // Helper methods
    private int getTotalQueuePlayers() {
        return queueManager.getTotalQueuePlayers();
    }

    private int getTotalMatchPlayers() {
        return matchManager.getTotalMatchPlayers();
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

    private String formatWaitTime(long waitTime) {
        double totalSeconds = waitTime / 1000.0;
        
        if (totalSeconds < 60) {
            return String.format("%.1fs", totalSeconds);
        } else {
            int minutes = (int) (totalSeconds / 60);
            int seconds = (int) (totalSeconds % 60);
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    private int getLeaderboardPosition(Player player) {
        PlayerData playerData = playerManager.getPlayerData(player);
        if (playerData == null) return -1;
        
        int totalMatches = playerData.getWins() + playerData.getLosses();
        if (totalMatches == 0) {
            return -1;
        }
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        if (leaderboardCache.containsKey(playerId) && 
            (currentTime - lastLeaderboardUpdate) < LEADERBOARD_CACHE_DURATION) {
            return leaderboardCache.get(playerId);
        }
        
        try {
            var database = Practice.getInstance().getDatabase();
            if (database == null) return -1;
            
            var playersCollection = database.getCollection("players");
            var filter = new org.bson.Document("globalElo", new org.bson.Document("$gt", playerData.getElo()));
            long playersAbove = playersCollection.countDocuments(filter);
            
            int position = (int) playersAbove + 1;
            position = Math.max(1, position);
            
            leaderboardCache.put(playerId, position);
            lastLeaderboardUpdate = currentTime;
            
            return position;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get leaderboard position for " + player.getName() + ": " + e.getMessage());
            return -1;
        }
    }
}
