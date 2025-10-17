package rip.thecraft.practice.match;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import rip.thecraft.practice.Practice;
import rip.thecraft.practice.arena.Arena;
import rip.thecraft.practice.kit.Kit;
import rip.thecraft.practice.player.PlayerData;
import rip.thecraft.practice.player.PlayerState;
import rip.thecraft.practice.queue.QueueType;
import rip.thecraft.practice.scoreboard.ScoreboardIntegration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MatchManager {

    private final JavaPlugin plugin;
    private final Map<UUID, Match> activeMatches = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToMatchId = new ConcurrentHashMap<>(); // Bidirectional mapping for faster lookups

    public MatchManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void startMatch(Player player1, Player player2, Kit kit, QueueType type) {
        // Validate players are not already in matches
        if (isPlayerInMatch(player1.getUniqueId())) {
            player1.sendMessage("§cYou are already in a match!");
            return;
        }
        if (isPlayerInMatch(player2.getUniqueId())) {
            player2.sendMessage("§cYou are already in a match!");
            return;
        }

        // Find an arena that can be used with this kit
        Arena arena = Practice.getInstance().getArenaManager().findAvailableArenaForKit(kit.getName());
        if (arena == null) {
            player1.sendMessage("§cNo available arenas for this kit!");
            player2.sendMessage("§cNo available arenas for this kit!");
            
            // Reset player states to LOBBY when no arena is found
            PlayerData player1Data = Practice.getInstance().getPlayerManager().getPlayerData(player1);
            PlayerData player2Data = Practice.getInstance().getPlayerManager().getPlayerData(player2);
            if (player1Data != null) player1Data.setState(PlayerState.LOBBY);
            if (player2Data != null) player2Data.setState(PlayerState.LOBBY);
            
            // Give lobby items back to players
            Practice.getInstance().getItemManager().giveSpawnItems(player1);
            Practice.getInstance().getItemManager().giveSpawnItems(player2);
            
            // Update scoreboards
            ScoreboardIntegration.updateLobbyScoreboard(player1);
            ScoreboardIntegration.updateLobbyScoreboard(player2);
            return;
        }

        // Mark arena as in use
        Practice.getInstance().getArenaManager().useArena(arena);
        
        Match match = new Match(player1.getUniqueId(), player2.getUniqueId(), kit, type, arena);
        UUID matchId = player1.getUniqueId(); // Use player1 UUID as match ID
        
        // Store match with bidirectional mapping
        activeMatches.put(matchId, match);
        playerToMatchId.put(player1.getUniqueId(), matchId);
        playerToMatchId.put(player2.getUniqueId(), matchId);

        // Update player states to MATCH
        PlayerData player1Data = Practice.getInstance().getPlayerManager().getPlayerData(player1);
        PlayerData player2Data = Practice.getInstance().getPlayerManager().getPlayerData(player2);
        if (player1Data != null) player1Data.setState(PlayerState.MATCH);
        if (player2Data != null) player2Data.setState(PlayerState.MATCH);
        
        // Update scoreboards for state change
        Practice.getInstance().getScoreboardService().forceUpdatePlayerScoreboard(player1);
        Practice.getInstance().getScoreboardService().forceUpdatePlayerScoreboard(player2);

        // Teleport players to arena safely
        teleportPlayerSafely(player1, arena.getSpawn1());
        teleportPlayerSafely(player2, arena.getSpawn2());

        // Hide all other players and show only opponent
        setupPlayerVisibility(match);

        // Apply kit using PaperAPI for Folia compatibility
        rip.thecraft.practice.util.PaperAPI.runForEntity(plugin, player1, () -> {
            Practice.getInstance().getKitManager().applyKit(player1, kit);
        });
        rip.thecraft.practice.util.PaperAPI.runForEntity(plugin, player2, () -> {
            Practice.getInstance().getKitManager().applyKit(player2, kit);
        });
        
        // Set up block visibility for build mode kits
        if (kit.isBuildMode()) {
            // Hide blocks from other matches in the same arena
            Practice.getInstance().getArenaListener().hideBlocksFromOtherMatches(match);
        }

        // Send match start messages
        player1.sendMessage("§aMatch started! vs " + player2.getName());
        player2.sendMessage("§aMatch started! vs " + player1.getName());

        // Start countdown
        startCountdown(match);
        
        // Start match timeout
        startMatchTimeout(match);
        
        // Debug: Log match start
        plugin.getLogger().info("Match started: " + player1.getName() + " vs " + player2.getName() + " in arena " + arena.getName());
        plugin.getLogger().info("Player1 UUID: " + player1.getUniqueId());
        plugin.getLogger().info("Player2 UUID: " + player2.getUniqueId());
        plugin.getLogger().info("Active matches size: " + activeMatches.size());
        
        // Update scoreboards for both players
        ScoreboardIntegration.updateMatchScoreboard(player1);
        ScoreboardIntegration.updateMatchScoreboard(player2);
    }

    private void startCountdown(Match match) {
        final int[] countdown = {5};
        
        // Start the countdown with proper delays
        startCountdownTick(match, countdown);
    }
    
    private void startCountdownTick(Match match, int[] countdown) {
        // Schedule next tick with 1 second delay
        rip.thecraft.practice.util.PaperAPI.runAtLocationLater(plugin, match.getArena().getSpawn1(), () -> {
            Player player1 = Bukkit.getPlayer(match.getPlayer1());
            Player player2 = Bukkit.getPlayer(match.getPlayer2());

            if (player1 == null || player2 == null) {
                endMatch(match, null);
                return;
            }

            if (countdown[0] > 0) {
                player1.sendMessage("§eMatch starting in " + countdown[0] + "...");
                player2.sendMessage("§eMatch starting in " + countdown[0] + "...");
                countdown[0]--;
                
                // Schedule next countdown tick
                startCountdownTick(match, countdown);
            } else {
                player1.sendMessage("§aMatch started!");
                player2.sendMessage("§aMatch started!");
                match.setStarted(true);
            }
        }, 20L); // 1 second delay
    }

    public void endMatch(Match match, UUID winner) {
        Player player1 = Bukkit.getPlayer(match.getPlayer1());
        Player player2 = Bukkit.getPlayer(match.getPlayer2());

        // Handle tournament match completion first
        handleTournamentMatchCompletion(match, winner);

        // Update ELO if ranked
        if (match.getType() == QueueType.RANKED && winner != null) {
            UUID loser = winner.equals(match.getPlayer1()) ? match.getPlayer2() : match.getPlayer1();
            Practice.getInstance().getQueueManager().updateELO(winner, loser, QueueType.RANKED, match.getKit().getName());
        }

        // Save player data to MongoDB after match results (ranked or unranked)
        if (winner != null) {
            UUID loser = winner.equals(match.getPlayer1()) ? match.getPlayer2() : match.getPlayer1();
            
            var winnerData = Practice.getInstance().getPlayerManager().getPlayerData(winner);
            var loserData = Practice.getInstance().getPlayerManager().getPlayerData(loser);
            
            // Save both players to MongoDB
            Practice.getInstance().getPlayerManager().savePlayerData(winnerData);
            Practice.getInstance().getPlayerManager().savePlayerData(loserData);
        }

        // Send end messages
        if (player1 != null) {
            if (winner == null) {
                player1.sendMessage("§cMatch ended!");
            } else if (winner.equals(player1.getUniqueId())) {
                player1.sendMessage("§aYou won the match!");
            } else {
                player1.sendMessage("§cYou lost the match!");
            }
        }

        if (player2 != null) {
            if (winner == null) {
                player2.sendMessage("§cMatch ended!");
            } else if (winner.equals(player2.getUniqueId())) {
                player2.sendMessage("§aYou won the match!");
            } else {
                player2.sendMessage("§cYou lost the match!");
            }
        }

        // Remove from active matches and player mappings
        UUID matchId = playerToMatchId.get(match.getPlayer1());
        if (matchId != null) {
            activeMatches.remove(matchId);
        }
        playerToMatchId.remove(match.getPlayer1());
        playerToMatchId.remove(match.getPlayer2());

        // Release arena
        Practice.getInstance().getArenaManager().releaseArena(match.getArena());

        // Restore player visibility for all players
        restorePlayerVisibility(match);
        
        // Remove all spectators from this match
        removeSpectatorsFromMatch(match);

        // Teleport players to practice spawn and restore their state
        if (player1 != null) {
            restorePlayerToLobby(player1);
        }
        if (player2 != null) {
            restorePlayerToLobby(player2);
        }

        // Clear hit counts for boxing mode
        Practice.getInstance().getMatchListener().clearHitCounts(match.getPlayer1());
        Practice.getInstance().getMatchListener().clearHitCounts(match.getPlayer2());
        
        // Regenerate arena if needed (for build mode kits)
        Practice.getInstance().getArenaListener().regenerateArena(match);
        
        // Debug: Log match end
        plugin.getLogger().info("Match ended: " + (player1 != null ? player1.getName() : "Player1") + " vs " + (player2 != null ? player2.getName() : "Player2") + " - Winner: " + (winner != null ? winner : "None"));
        
        // Update scoreboards for both players
        if (player1 != null) {
            ScoreboardIntegration.updateMatchScoreboard(player1);
        }
        if (player2 != null) {
            ScoreboardIntegration.updateMatchScoreboard(player2);
        }
    }

    public void endMatch(Match match, Player winner, Player loser) {
        endMatch(match, winner != null ? winner.getUniqueId() : null);
    }

    public Match getPlayerMatch(UUID playerId) {
        UUID matchId = playerToMatchId.get(playerId);
        return matchId != null ? activeMatches.get(matchId) : null;
    }
    
    public boolean isPlayerInMatch(UUID playerId) {
        return playerToMatchId.containsKey(playerId);
    }
    
    public boolean isPlayerInMatch(Player player) {
        return isPlayerInMatch(player.getUniqueId());
    }
    
    // Debug method to check active matches
    public void debugActiveMatches() {
        plugin.getLogger().info("=== ACTIVE MATCHES DEBUG ===");
        plugin.getLogger().info("Total active matches: " + activeMatches.size());
        plugin.getLogger().info("Total players in matches: " + playerToMatchId.size());
        
        for (Map.Entry<UUID, Match> entry : activeMatches.entrySet()) {
            Match match = entry.getValue();
            Player player1 = Bukkit.getPlayer(match.getPlayer1());
            Player player2 = Bukkit.getPlayer(match.getPlayer2());
            
            plugin.getLogger().info("Match ID: " + entry.getKey());
            plugin.getLogger().info("  Player1: " + (player1 != null ? player1.getName() : match.getPlayer1()) + 
                " | Player2: " + (player2 != null ? player2.getName() : match.getPlayer2()) +
                " | Started: " + match.isStarted() + 
                " | Arena: " + match.getArena().getName());
        }
        
        plugin.getLogger().info("Player to Match Mappings:");
        for (Map.Entry<UUID, UUID> entry : playerToMatchId.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            plugin.getLogger().info("  Player: " + (player != null ? player.getName() : entry.getKey()) + 
                " -> Match ID: " + entry.getValue());
        }
        plugin.getLogger().info("============================");
    }

    public void shutdown() {
        // End all active matches
        for (Match match : new HashSet<>(activeMatches.values())) {
            endMatch(match, null);
        }
        activeMatches.clear();
        playerToMatchId.clear();
    }

    public void setCurrentFight(Player player, Match match) {
        if (match == null) {
            playerToMatchId.remove(player.getUniqueId());
        } else {
            playerToMatchId.put(player.getUniqueId(), player.getUniqueId()); // Use player UUID as match ID
            activeMatches.put(player.getUniqueId(), match);
        }
    }

    public Location getPracticeSpawn() {
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

    public void clearPlayer(Player player, boolean clearInventory, boolean resetStats) {
        // Use PaperAPI for Folia compatibility - run on entity scheduler
        rip.thecraft.practice.util.PaperAPI.runForEntity(plugin, player, () -> {
            if (clearInventory) {
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);
            }
            
            if (resetStats) {
                player.setHealth(20.0);
                player.setFoodLevel(20);
                player.setSaturation(5.0f);
                player.setFireTicks(0);
                player.setFallDistance(0);
                player.setRemainingAir(player.getMaximumAir());
                
                // Clear potion effects
                for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
                    player.removePotionEffect(effect.getType());
                }
            }
        });
    }

    public void teleportToPracticeSpawn(Player player) {
        var config = Practice.getInstance().getConfig();
        Location spawnLocation;
        
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
                spawnLocation = new Location(world, x, y, z, yaw, pitch);
            } else {
                // Fallback to world spawn
                spawnLocation = Practice.getInstance().getServer().getWorlds().get(0).getSpawnLocation();
            }
        } else {
            // Fallback to world spawn
            spawnLocation = Practice.getInstance().getServer().getWorlds().get(0).getSpawnLocation();
        }
        
        // Use the safe teleportation system
        teleportPlayerSafely(player, spawnLocation);
    }

    private void startMatchTimeout(Match match) {
        // Auto-end match after 10 minutes to prevent arenas being stuck
        // Use PaperAPI for Folia compatibility with proper delay
        rip.thecraft.practice.util.PaperAPI.runAtLocationLater(plugin, match.getArena().getSpawn1(), () -> {
            if (isPlayerInMatch(match.getPlayer1())) {
                // Match is still active after timeout
                Player player1 = Bukkit.getPlayer(match.getPlayer1());
                Player player2 = Bukkit.getPlayer(match.getPlayer2());
                
                if (player1 != null) player1.sendMessage("§cMatch timeout - match ended due to inactivity");
                if (player2 != null) player2.sendMessage("§cMatch timeout - match ended due to inactivity");
                
                endMatch(match, null);
            }
        }, 20L * 60 * 10); // 10 minutes
    }
    
    public int getActiveMatchCount() {
        return activeMatches.size();
    }
    
    public int getPlayerInMatchCount() {
        return playerToMatchId.size();
    }
    
    public Collection<Match> getAllActiveMatches() {
        return Collections.unmodifiableCollection(activeMatches.values());
    }

    // Scoreboard integration method
    public int getTotalMatchPlayers() {
        return playerToMatchId.size();
    }

    public void teleportPlayerSafely(Player player, Location location) {
        if (player == null || location == null) return;
        
        // The PROVEN solution for "moved too quickly" errors:
        // 1. Reset ALL movement properties
        // 2. Use a small delay before teleport
        // 3. Use async teleport for Folia compatibility
        
        // Reset movement properties
        player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        player.setFallDistance(0);
        player.setFireTicks(0);
        
        // Clear movement effects
        for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        
        // Schedule teleport for next tick - this is the key fix
        // Use PaperAPI for Folia compatibility
        rip.thecraft.practice.util.PaperAPI.runForEntity(plugin, player, () -> {
            if (player.isOnline()) {
                try {
                    // Use async teleport for Folia compatibility
                    rip.thecraft.practice.util.PaperAPI.teleportAsync(player, location);
                } catch (Exception e) {
                    plugin.getLogger().warning("Teleport failed for " + player.getName() + ": " + e.getMessage());
                }
            }
        });
    }

    /**
     * Sets up player visibility so players in a match can only see their opponent
     */
    private void setupPlayerVisibility(Match match) {
        Player player1 = Bukkit.getPlayer(match.getPlayer1());
        Player player2 = Bukkit.getPlayer(match.getPlayer2());
        
        if (player1 == null || player2 == null) return;
        
        // Hide all players from player1 except player2
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(player2)) {
                player1.hidePlayer(plugin, onlinePlayer);
            }
        }
        
        // Hide all players from player2 except player1
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(player1)) {
                player2.hidePlayer(plugin, onlinePlayer);
            }
        }
        
        // Show opponents to each other
        player1.showPlayer(plugin, player2);
        player2.showPlayer(plugin, player1);
        
        // Hide all entities from players in matches (to hide pearls, arrows, etc.)
        hideEntitiesFromPlayers(match);
    }

    /**
     * Hides entities from players in matches to prevent seeing pearls, arrows, etc.
     */
    private void hideEntitiesFromPlayers(Match match) {
        Player player1 = Bukkit.getPlayer(match.getPlayer1());
        Player player2 = Bukkit.getPlayer(match.getPlayer2());
        
        if (player1 == null || player2 == null) return;
        
        // Get the arena world
        World arenaWorld = match.getArena().getSpawn1().getWorld();
        
        // Hide all entities from both players except themselves and their opponent
        for (org.bukkit.entity.Entity entity : arenaWorld.getEntities()) {
            if (entity instanceof Player) {
                // Skip players - they're handled by player visibility
                continue;
            }
            
            // Hide entity from player1 if it's not from player2
            if (!isEntityFromPlayer(entity, player2)) {
                player1.hideEntity(plugin, entity);
            }
            
            // Hide entity from player2 if it's not from player1
            if (!isEntityFromPlayer(entity, player1)) {
                player2.hideEntity(plugin, entity);
            }
        }
    }

    /**
     * Checks if an entity was spawned/created by a specific player
     */
    private boolean isEntityFromPlayer(org.bukkit.entity.Entity entity, Player player) {
        // Check for projectiles (arrows, snowballs, eggs, etc.)
        if (entity instanceof org.bukkit.entity.Projectile) {
            org.bukkit.entity.Projectile projectile = (org.bukkit.entity.Projectile) entity;
            return projectile.getShooter() instanceof Player && ((Player) projectile.getShooter()).equals(player);
        }
        
        // Check for thrown items (ender pearls, etc.)
        if (entity instanceof org.bukkit.entity.ThrownPotion || 
            entity instanceof org.bukkit.entity.EnderPearl ||
            entity instanceof org.bukkit.entity.ThrownExpBottle) {
            org.bukkit.entity.Projectile projectile = (org.bukkit.entity.Projectile) entity;
            return projectile.getShooter() instanceof Player && ((Player) projectile.getShooter()).equals(player);
        }
        
        // Check for dropped items
        if (entity instanceof org.bukkit.entity.Item) {
            // For items, we can't easily track the source player, so hide all items
            return false;
        }
        
        // For other entities, hide them by default
        return false;
    }

    /**
     * Restores player visibility for all players after match ends
     */
    private void restorePlayerVisibility(Match match) {
        Player player1 = Bukkit.getPlayer(match.getPlayer1());
        Player player2 = Bukkit.getPlayer(match.getPlayer2());
        
        // Show all players to player1
        if (player1 != null) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                player1.showPlayer(plugin, onlinePlayer);
            }
        }
        
        // Show all players to player2
        if (player2 != null) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                player2.showPlayer(plugin, onlinePlayer);
            }
        }
        
        // Show player1 and player2 to all other players
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (player1 != null && !onlinePlayer.equals(player1)) {
                onlinePlayer.showPlayer(plugin, player1);
            }
            if (player2 != null && !onlinePlayer.equals(player2)) {
                onlinePlayer.showPlayer(plugin, player2);
            }
        }
        
        // Show all entities to both players
        restoreEntityVisibility(match);
    }

    /**
     * Restores entity visibility for players after match ends
     */
    private void restoreEntityVisibility(Match match) {
        Player player1 = Bukkit.getPlayer(match.getPlayer1());
        Player player2 = Bukkit.getPlayer(match.getPlayer2());
        
        if (player1 == null || player2 == null) return;
        
        // Get the arena world
        World arenaWorld = match.getArena().getSpawn1().getWorld();
        
        // Show all entities to both players
        for (org.bukkit.entity.Entity entity : arenaWorld.getEntities()) {
            if (!(entity instanceof Player)) {
                player1.showEntity(plugin, entity);
                player2.showEntity(plugin, entity);
            }
        }
    }

    /**
     * Handle tournament match completion when a regular match ends
     */
    private void handleTournamentMatchCompletion(Match match, UUID winner) {
        // Check if this match is part of a tournament
        for (rip.thecraft.practice.tournament.Tournament tournament : Practice.getInstance().getTournamentManager().getActiveTournaments()) {
            for (rip.thecraft.practice.tournament.TournamentMatch tournamentMatch : tournament.getActiveMatches()) {
                if (tournamentMatch.getMatch() != null && tournamentMatch.getMatch().equals(match)) {
                    tournamentMatch.onMatchEnd(winner);
                    return;
                }
            }
        }
    }

    /**
     * Restores a player to lobby state after match ends
     * This method ensures thread safety by using PaperAPI for Folia compatibility
     */
    private void restorePlayerToLobby(Player player) {
        if (player == null || !player.isOnline()) return;
        
        // Use PaperAPI for Folia compatibility - run on entity scheduler
        rip.thecraft.practice.util.PaperAPI.runForEntity(plugin, player, () -> {
            if (player.isOnline()) {
                // Update player state
                PlayerData playerData = Practice.getInstance().getPlayerManager().getPlayerData(player);
                if (playerData != null) {
                    playerData.setState(PlayerState.LOBBY);
                }
                
                // Clear inventory and reset stats
                player.getInventory().clear();
                player.setHealth(20.0);
                player.setFoodLevel(20);
                player.setSaturation(5.0f);
                player.setFireTicks(0);
                player.setFallDistance(0);
                
                // Clear potion effects
                for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
                    player.removePotionEffect(effect.getType());
                }
                
                // Teleport to spawn
                teleportToPracticeSpawn(player);
                
                // Give lobby items
                Practice.getInstance().getItemManager().giveSpawnItems(player);
                
                // Update scoreboard
                Practice.getInstance().getScoreboardService().updatePlayerScoreboard(player);
            }
        });
    }

    /**
     * Removes all spectators from a match when it ends
     */
    private void removeSpectatorsFromMatch(Match match) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            PlayerData playerData = Practice.getInstance().getPlayerManager().getPlayerData(onlinePlayer);
            if (playerData != null && playerData.getState() == PlayerState.SPECTATING) {
                Match spectatingMatch = playerData.getSpectatingMatch();
                if (spectatingMatch != null && spectatingMatch.equals(match)) {
                    // Remove spectator from this match
                    playerData.setState(PlayerState.LOBBY);
                    playerData.setSpectatingMatch(null);
                    
                    // Reset flight
                    onlinePlayer.setAllowFlight(false);
                    onlinePlayer.setFlying(false);
                    
                    // Restore visibility
                    for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
                        if (!otherPlayer.equals(onlinePlayer)) {
                            otherPlayer.showPlayer(plugin, onlinePlayer);
                        }
                    }
                    for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
                        onlinePlayer.showPlayer(plugin, otherPlayer);
                    }
                    
                    // Teleport back to spawn
                    teleportToPracticeSpawn(onlinePlayer);
                    
                    // Use PaperAPI for Folia compatibility - run on entity scheduler
                    rip.thecraft.practice.util.PaperAPI.runForEntity(plugin, onlinePlayer, () -> {
                        // Clear inventory and reset stats
                        onlinePlayer.getInventory().clear();
                        onlinePlayer.setHealth(20.0);
                        onlinePlayer.setFoodLevel(20);
                        
                        // Give lobby items
                        Practice.getInstance().getItemManager().giveSpawnItems(onlinePlayer);
                    });
                    
                    // Send message
                    onlinePlayer.sendMessage(ChatColor.YELLOW + "The match you were spectating has ended.");
                    
                    // Update scoreboard
                    Practice.getInstance().getScoreboardService().updatePlayerScoreboard(onlinePlayer);
                }
            }
        }
        
        // Also remove tournament spectators
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            PlayerData playerData = Practice.getInstance().getPlayerManager().getPlayerData(onlinePlayer);
            if (playerData != null && playerData.getState() == PlayerState.SPECTATING) {
                rip.thecraft.practice.tournament.Tournament spectatingTournament = playerData.getSpectatingTournament();
                if (spectatingTournament != null) {
                    // Check if this match is part of the tournament they're spectating
                    for (rip.thecraft.practice.tournament.TournamentMatch tournamentMatch : spectatingTournament.getActiveMatches()) {
                        if (tournamentMatch.getMatch() != null && tournamentMatch.getMatch().equals(match)) {
                            // Remove spectator from tournament
                            playerData.setState(PlayerState.LOBBY);
                            playerData.setSpectatingTournament(null);
                            
                            // Reset flight
                            onlinePlayer.setAllowFlight(false);
                            onlinePlayer.setFlying(false);
                            
                            // Restore visibility
                            for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
                                if (!otherPlayer.equals(onlinePlayer)) {
                                    otherPlayer.showPlayer(plugin, onlinePlayer);
                                }
                            }
                            for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
                                onlinePlayer.showPlayer(plugin, otherPlayer);
                            }
                            
                            // Teleport back to spawn
                            teleportToPracticeSpawn(onlinePlayer);
                            
                            // Use PaperAPI for Folia compatibility - run on entity scheduler
                            rip.thecraft.practice.util.PaperAPI.runForEntity(plugin, onlinePlayer, () -> {
                                // Clear inventory and reset stats
                                onlinePlayer.getInventory().clear();
                                onlinePlayer.setHealth(20.0);
                                onlinePlayer.setFoodLevel(20);
                                
                                // Give lobby items
                                Practice.getInstance().getItemManager().giveSpawnItems(onlinePlayer);
                            });
                            
                            // Send message
                            onlinePlayer.sendMessage(ChatColor.YELLOW + "The tournament match you were spectating has ended.");
                            
                            // Update scoreboard
                            Practice.getInstance().getScoreboardService().updatePlayerScoreboard(onlinePlayer);
                            break;
                        }
                    }
                }
            }
        }
    }
}
