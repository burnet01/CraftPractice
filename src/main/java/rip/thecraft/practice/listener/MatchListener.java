package rip.thecraft.practice.listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import rip.thecraft.practice.Practice;
import rip.thecraft.practice.hitdelay.HitDelayHandler;
import rip.thecraft.practice.hitdelay.HitDelayProfile;
import rip.thecraft.practice.kit.Kit;
import rip.thecraft.practice.match.Match;
import rip.thecraft.practice.match.MatchManager;
import rip.thecraft.practice.player.PlayerData;
import rip.thecraft.practice.player.PlayerState;
import rip.thecraft.practice.scoreboard.ScoreboardIntegration;
import rip.thecraft.practice.tournament.Tournament;
import rip.thecraft.practice.tournament.TournamentMatch;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MatchListener implements Listener {

    private final MatchManager matchManager;
    private final Map<UUID, Integer> playerHitCounts = new HashMap<>();

    public MatchListener() {
        this.matchManager = Practice.getInstance().getMatchManager();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Match match = matchManager.getPlayerMatch(player.getUniqueId());
        
        if (match == null) return;
        
        Kit kit = match.getKit();
        if (kit == null || !kit.isSumoMode()) return;

        // Check if player is touching water, ice, or lava
        Location location = player.getLocation();
        
        // Check all blocks around the player (including the block they're standing on)
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Material blockType = location.clone().add(x, y, z).getBlock().getType();
                    
                    if (isLiquidOrIce(blockType)) {
                        // Instant death for sumo mode - bypass all damage systems
                        // Use Bukkit's scheduler to ensure the death happens after all events
                        Bukkit.getScheduler().runTask(Practice.getInstance(), () -> {
                            if (player.isOnline() && matchManager.isPlayerInMatch(player.getUniqueId())) {
                                // Determine winner
                                UUID winner = match.getPlayer1().equals(player.getUniqueId()) ? match.getPlayer2() : match.getPlayer1();
                                
                                // Update stats BEFORE ending the match
                                var winnerData = Practice.getInstance().getPlayerManager().getPlayerData(winner);
                                var loserData = Practice.getInstance().getPlayerManager().getPlayerData(player.getUniqueId());
                                
                                winnerData.setWins(winnerData.getWins() + 1);
                                loserData.setLosses(loserData.getLosses() + 1);
                                
                                // Save stats to MongoDB
                                Practice.getInstance().getPlayerManager().savePlayerData(winnerData);
                                Practice.getInstance().getPlayerManager().savePlayerData(loserData);
                                
                                // Kill the player directly
                                player.setHealth(0);
                                player.sendMessage(ChatColor.RED + "You died by touching water/lava/ice in sumo mode!");
                                
                                // Force end the match
                                matchManager.endMatch(match, winner);
                                
                                // Handle tournament match completion
                                handleTournamentMatchCompletion(match, winner);
                                
                                // Update scoreboards for both players
                                ScoreboardIntegration.updateMatchScoreboard(player);
                                Player winnerPlayer = Bukkit.getPlayer(winner);
                                if (winnerPlayer != null) {
                                    ScoreboardIntegration.updateMatchScoreboard(winnerPlayer);
                                }
                            }
                        });
                        return; // Only trigger once per move event
                    }
                }
            }
        }
    }

    private boolean isLiquidOrIce(Material material) {
        return material == Material.WATER || 
               material == Material.LAVA || 
               material == Material.ICE || 
               material == Material.PACKED_ICE ||
               material == Material.FROSTED_ICE ||
               material == Material.BLUE_ICE ||
               material.name().contains("WATER") || 
               material.name().contains("LAVA") ||
               material.name().contains("ICE");
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        
        Match match = matchManager.getPlayerMatch(player.getUniqueId());
        
        if (match == null) return;
        
        Kit kit = match.getKit();
        if (kit == null) return;

        // Handle boxing mode - prevent health damage but allow knockback
        if (kit.isBoxingMode()) {
            // Only cancel the damage, but allow the knockback to happen
            event.setDamage(0);
        }
        
        // Handle sumo mode - prevent health damage but allow knockback
        if (kit.isSumoMode() && event.getCause() != EntityDamageEvent.DamageCause.VOID) {
            // Set damage to 0 but don't cancel the event - this allows knockback to still happen
            event.setDamage(0);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) return;
        
        Player damager = (Player) event.getDamager();
        Player target = (Player) event.getEntity();
        
        Match damagerMatch = matchManager.getPlayerMatch(damager.getUniqueId());
        Match targetMatch = matchManager.getPlayerMatch(target.getUniqueId());
        
        // Ensure both players are in the same match
        if (damagerMatch == null || targetMatch == null || !damagerMatch.equals(targetMatch)) return;
        
        Kit kit = damagerMatch.getKit();
        if (kit == null) return;

        // Handle hit delay - KEEP YOUR EXISTING SYSTEM
        HitDelayHandler hitDelayHandler = Practice.getInstance().getHitDelayHandler();
        if (hitDelayHandler != null) {
            String hitDelayProfile = kit.getHitDelayProfile();
            
            if (!hitDelayHandler.canHit(damager, hitDelayProfile)) {
                // Player is still in hit delay cooldown, cancel the damage
                event.setCancelled(true);
                return;
            } else {
                // Hit is allowed, reset the target's noDamageTicks to match our hit delay
                // This prevents the default Minecraft invulnerability period from interfering
                HitDelayProfile profile = hitDelayHandler.getHitDelayManager().getProfile(hitDelayProfile);
                if (profile != null && profile.isEnabled()) {
                    long hitDelay = profile.getHitDelay();
                    // Convert milliseconds to ticks (1 tick = 50ms)
                    int noDamageTicks = (int) Math.max(0L, hitDelay / 50);
                    
                    // Reset the target's noDamageTicks to our custom value
                    // Use Bukkit scheduler to ensure this happens after the event
                    Bukkit.getScheduler().runTask(Practice.getInstance(), () -> {
                        target.setNoDamageTicks(noDamageTicks);
                    });
                }
            }
        }

        // IMPORTANT: MODIFY DAMAGE SYSTEM FOR 1.8 COMBAT
        // Instead of reducing damage for spam clicking, we ensure full damage on every allowed hit
        
        // Calculate 1.8-style damage (full damage regardless of timing)
        // This overrides Minecraft's 1.9+ damage reduction for fast attacks
        double weaponDamage = getWeaponDamage(damager.getItemInHand());
        double strengthMultiplier = getStrengthMultiplier(damager);
        double finalDamage = weaponDamage * strengthMultiplier;
        
        // Apply 1.8-style critical hits (falling while hitting)
        if (damager.getFallDistance() > 0.0F && 
            !damager.isOnGround() && 
            !damager.isInsideVehicle() &&
            damager.getVelocity().getY() < -0.08) {
            finalDamage *= 1.5; // 1.8 crit multiplier
        }
        
        // Set the final damage - this ensures consistent damage on every allowed hit
        event.setDamage(finalDamage);

        // Handle boxing mode
        if (kit.isBoxingMode()) {
            // For boxing mode, we still want to track hits but prevent actual damage
            // The damage will be handled by the EntityDamageEvent handler
            
            // Track hits
            int hits = playerHitCounts.getOrDefault(damager.getUniqueId(), 0) + 1;
            playerHitCounts.put(damager.getUniqueId(), hits);
            
            // Check for win condition
            if (hits >= 100) {
                // Update stats BEFORE ending the match
                var winnerData = Practice.getInstance().getPlayerManager().getPlayerData(damager.getUniqueId());
                var loserData = Practice.getInstance().getPlayerManager().getPlayerData(target.getUniqueId());
                
                winnerData.setWins(winnerData.getWins() + 1);
                loserData.setLosses(loserData.getLosses() + 1);
                
                // Save stats to MongoDB
                Practice.getInstance().getPlayerManager().savePlayerData(winnerData);
                Practice.getInstance().getPlayerManager().savePlayerData(loserData);
                
                // Immediately end the match before any damage can be dealt
                matchManager.endMatch(damagerMatch, damager, target);
                
                // Handle tournament match completion
                handleTournamentMatchCompletion(damagerMatch, damager.getUniqueId());
                playerHitCounts.remove(damager.getUniqueId());
                playerHitCounts.remove(target.getUniqueId());
                
                // Update scoreboards for both players
                ScoreboardIntegration.updateMatchScoreboard(damager);
                ScoreboardIntegration.updateMatchScoreboard(target);
                
                // Broadcast win message
                Practice.getInstance().getServer().broadcastMessage(
                    ChatColor.GOLD + damager.getName() + " won a boxing match against " + target.getName() + "!"
                );
                
                // Prevent any further damage processing for this hit
                event.setCancelled(true);
            }
        }
    }
    
    /**
     * Get weapon damage based on 1.8 mechanics
     */
    private double getWeaponDamage(org.bukkit.inventory.ItemStack item) {
        if (item == null) return 1.0; // Fist damage
        
        switch (item.getType()) {
            case DIAMOND_SWORD:
                return 7.0;
            case IRON_SWORD:
                return 6.0;
            case STONE_SWORD:
                return 5.0;
            case WOODEN_SWORD:
                return 4.0;
            case DIAMOND_AXE:
                return 6.0;
            case IRON_AXE:
                return 5.0;
            case STONE_AXE:
                return 4.0;
            case WOODEN_AXE:
                return 3.0;
            default:
                return 1.0; // Fist damage for other items
        }
    }
    
    /**
     * Get strength multiplier based on potion effects
     */
    private double getStrengthMultiplier(Player player) {
        for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(org.bukkit.potion.PotionEffectType.INCREASE_DAMAGE)) {
                return 1.3 * (effect.getAmplifier() + 1); // Strength I = 1.3x, Strength II = 1.6x
            }
        }
        return 1.0; // No strength effect
    }

    public void clearHitCounts(UUID playerId) {
        playerHitCounts.remove(playerId);
    }

    public void clearAllHitCounts() {
        playerHitCounts.clear();
    }

    /**
     * Handle tournament match completion when a regular match ends
     */
    private void handleTournamentMatchCompletion(Match match, UUID winner) {
        // Check if this match is part of a tournament
        for (Tournament tournament : Practice.getInstance().getTournamentManager().getActiveTournaments()) {
            for (TournamentMatch tournamentMatch : tournament.getActiveMatches()) {
                if (tournamentMatch.getMatch() != null && tournamentMatch.getMatch().equals(match)) {
                    tournamentMatch.onMatchEnd(winner);
                    
                    // Remove all spectators from this tournament match
                    removeTournamentSpectators(tournamentMatch);
                    return;
                }
            }
        }
    }

    /**
     * Remove all spectators from a tournament match when it ends
     */
    private void removeTournamentSpectators(TournamentMatch tournamentMatch) {
        for (Player spectator : Bukkit.getOnlinePlayers()) {
            PlayerData spectatorData = Practice.getInstance().getPlayerManager().getPlayerData(spectator);
            if (spectatorData != null && spectatorData.getState() == PlayerState.SPECTATING) {
                Match spectatingMatch = spectatorData.getSpectatingMatch();
                if (spectatingMatch != null && spectatingMatch.equals(tournamentMatch.getMatch())) {
                    // Use the spectate command's leave method to properly handle the spectator
                    // This will teleport them back to spawn and give them lobby items
                    Practice.getInstance().getCommand("spec").execute(spectator, "spec", new String[0]);
                }
            }
        }
    }

    /**
     * Handle tournament match completion when a player dies in a match
     */
    @EventHandler
    public void onPlayerDeathInMatch(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player killer = player.getKiller();

        var matchManager = Practice.getInstance().getMatchManager();
        if (matchManager == null) return;

        Match match = matchManager.getPlayerMatch(player.getUniqueId());
        if (match != null && match.isStarted()) {
            // Check if this is a tournament match
            for (Tournament tournament : Practice.getInstance().getTournamentManager().getActiveTournaments()) {
                for (TournamentMatch tournamentMatch : tournament.getActiveMatches()) {
                    if (tournamentMatch.getMatch() != null && tournamentMatch.getMatch().equals(match)) {
                        // This is a tournament match - determine winner
                        UUID winner = killer != null ? killer.getUniqueId() : 
                            (match.getPlayer1().equals(player.getUniqueId()) ? match.getPlayer2() : match.getPlayer1());
                        
                        // Handle tournament match completion
                        tournamentMatch.onMatchEnd(winner);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        Match match = matchManager.getPlayerMatch(player.getUniqueId());
        
        if (match == null) return;
        
        Kit kit = match.getKit();
        if (kit == null) return;

        // Prevent hunger loss in sumo and boxing modes
        if (kit.isSumoMode() || kit.isBoxingMode()) {
            event.setCancelled(true);
            player.setFoodLevel(20); // Keep food level maxed
        }
    }
}
