package rip.thecraft.practice.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import rip.thecraft.practice.Practice;
import rip.thecraft.practice.match.Match;
import rip.thecraft.practice.scoreboard.ScoreboardIntegration;

import java.util.UUID;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Disable advancement messages
        player.sendMessage(""); // Clear any advancement messages
        
        // Load player data
        var playerData = Practice.getInstance().getPlayerManager().getPlayerData(player);
        
        // Update player name in database
        playerData.setPlayerName(player.getName());
        Practice.getInstance().getPlayerManager().savePlayerData(playerData);
        
        // Teleport player to practice spawn
        teleportToPracticeSpawn(player);
        
        // Use PaperAPI for Folia compatibility - run on entity scheduler
        rip.thecraft.practice.util.PaperAPI.runForEntity(Practice.getInstance(), player, () -> {
            // Clear inventory and reset health/hunger
            player.getInventory().clear();
            player.setHealth(20.0);
            player.setFoodLevel(20);
            
            // Give spawn items based on player state
            Practice.getInstance().getItemManager().giveSpawnItems(player);
            
            // Apply player settings
            Practice.getInstance().getSettingsManager().applyPlayerSettings(player);
            
            // Handle player visibility for existing matches
            handlePlayerJoinVisibility(player);
            
            // Initialize scoreboard
            ScoreboardIntegration.handlePlayerJoin(player);
            

        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Debug: Log player quit
        //Practice.getInstance().getLogger().info("Player quit: " + player.getName());
        
        // Leave queue if in one
        Practice.getInstance().getQueueManager().leaveQueue(player);
        
        // Handle tournament if in one
        Practice.getInstance().getTournamentManager().handlePlayerQuit(player);
        
        // Handle match if in one
        var matchManager = Practice.getInstance().getMatchManager();
        if (matchManager != null) {
            Match match = matchManager.getPlayerMatch(player.getUniqueId());
            if (match != null) {
                // Debug: Log match found for quitting player
              //  Practice.getInstance().getLogger().info("Found match for quitting player: " + player.getName() +
          //          " | Match started: " + match.isStarted());
                
                // End match with opponent as winner
                UUID winner = match.getPlayer1().equals(player.getUniqueId()) ? match.getPlayer2() : match.getPlayer1();
                
                // Update stats for the winner
                var winnerData = Practice.getInstance().getPlayerManager().getPlayerData(winner);
                winnerData.setWins(winnerData.getWins() + 1);
                
                // Update winner name if player is online
                Player winnerPlayer = Practice.getInstance().getServer().getPlayer(winner);
                if (winnerPlayer != null) {
                    winnerData.setPlayerName(winnerPlayer.getName());
                }
                
                // Save winner stats to MongoDB
                Practice.getInstance().getPlayerManager().savePlayerData(winnerData);
                
                // Send message to remaining player BEFORE ending match
                Player remainingPlayer = Practice.getInstance().getServer().getPlayer(winner);
                if (remainingPlayer != null) {
                    remainingPlayer.sendMessage("§aYour opponent left the match! You win!");
                    //Practice.getInstance().getLogger().info("Sent win message to: " + remainingPlayer.getName());
                } else {
                   // Practice.getInstance().getLogger().info("Winner player not found: " + winner);
                }
                
                // End the match
               // Practice.getInstance().getLogger().info("Ending match with winner: " + winner);
                matchManager.endMatch(match, winner);
            } else {
              //  Practice.getInstance().getLogger().info("No match found for quitting player: " + player.getName());
            }
        }
        
        // Clean up scoreboard
        ScoreboardIntegration.handlePlayerQuit(player);
        
        // Clean up hit delay data
        var hitDelayHandler = Practice.getInstance().getHitDelayHandler();
        if (hitDelayHandler != null) {
            hitDelayHandler.clearPlayer(player);
        }
        
        // Don't save player data on quit - only save after match results
        Practice.getInstance().getPlayerManager().removePlayerData(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player killer = player.getKiller();

        var matchManager = Practice.getInstance().getMatchManager();
        if (matchManager == null) return;

        Match match = matchManager.getPlayerMatch(player.getUniqueId());
        if (match != null && match.isStarted()) {
            // Prevent the death entirely by cancelling the event
            event.setCancelled(true);
            
            // End the match
            UUID winner = killer != null ? killer.getUniqueId() : 
                (match.getPlayer1().equals(player.getUniqueId()) ? match.getPlayer2() : match.getPlayer1());
            
            // Update stats BEFORE ending the match
            var playerData = Practice.getInstance().getPlayerManager().getPlayerData(player);
            var winnerData = Practice.getInstance().getPlayerManager().getPlayerData(winner);
            
            playerData.setLosses(playerData.getLosses() + 1);
            winnerData.setWins(winnerData.getWins() + 1);
            
            // Update player names if players are online
            playerData.setPlayerName(player.getName());
            Player winnerPlayer = Practice.getInstance().getServer().getPlayer(winner);
            if (winnerPlayer != null) {
                winnerData.setPlayerName(winnerPlayer.getName());
            }
            
            // Save stats to MongoDB
            Practice.getInstance().getPlayerManager().savePlayerData(playerData);
            Practice.getInstance().getPlayerManager().savePlayerData(winnerData);
            
            // Prevent death message
            event.setDeathMessage(null);
            
            // End the match - this will handle player state restoration
            matchManager.endMatch(match, winner);
        }
    }
    
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // Set respawn location to practice spawn
        event.setRespawnLocation(getPracticeSpawnLocation());
        
        // Use PaperAPI for Folia compatibility - run on entity scheduler
        rip.thecraft.practice.util.PaperAPI.runForEntity(Practice.getInstance(), player, () -> {
            player.getInventory().clear();
            player.setHealth(20.0);
            player.setFoodLevel(20);
            
            // Give spawn items
            Practice.getInstance().getItemManager().giveSpawnItems(player);
            
            // Apply player settings
            Practice.getInstance().getSettingsManager().applyPlayerSettings(player);
        });
    }
    
    private void teleportToPracticeSpawn(Player player) {
        Location spawnLocation = getPracticeSpawnLocation();
        Practice.getInstance().getMatchManager().teleportPlayerSafely(player, spawnLocation);
    }
    
    private Location getPracticeSpawnLocation() {
        var config = Practice.getInstance().getConfig();
        
        // Check if practice spawn is set
        if (config.contains("spawn.world")) {
            String worldName = config.getString("spawn.world");
            double x = config.getDouble("spawn.x");
            double y = config.getDouble("spawn.y");
            double z = config.getDouble("spawn.z");
            float yaw = (float) config.getDouble("spawn.yaw");
            float pitch = (float) config.getDouble("spawn.pitch");
            
            var world = Practice.getInstance().getServer().getWorld(worldName);
            if (world != null) {
                return new Location(world, x, y, z, yaw, pitch);
            }
        }
        
        // Fallback to world spawn
        return Practice.getInstance().getServer().getWorlds().get(0).getSpawnLocation();
    }

    /**
     * Handles player visibility when a player joins the server
     * If there are active matches, hide the joining player from players in matches
     * and hide players in matches from the joining player
     */
    private void handlePlayerJoinVisibility(Player joiningPlayer) {
        var matchManager = Practice.getInstance().getMatchManager();
        if (matchManager == null) return;
        
        // Hide the joining player from all players in matches
        for (Player onlinePlayer : Practice.getInstance().getServer().getOnlinePlayers()) {
            if (matchManager.isPlayerInMatch(onlinePlayer) && !onlinePlayer.equals(joiningPlayer)) {
                onlinePlayer.hidePlayer(Practice.getInstance(), joiningPlayer);
            }
        }
        
        // Hide all players in matches from the joining player
        for (Player onlinePlayer : Practice.getInstance().getServer().getOnlinePlayers()) {
            if (matchManager.isPlayerInMatch(onlinePlayer) && !onlinePlayer.equals(joiningPlayer)) {
                joiningPlayer.hidePlayer(Practice.getInstance(), onlinePlayer);
            }
        }
    }
}
