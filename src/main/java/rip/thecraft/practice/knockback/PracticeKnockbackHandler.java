package rip.thecraft.practice.knockback;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;
import rip.thecraft.practice.Practice;
import rip.thecraft.practice.kit.Kit;
import rip.thecraft.practice.match.Match;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PracticeKnockbackHandler implements Listener {

    private final HashMap<Player, Vector> playerKnockbackHashMap = new HashMap<>();
    private final Map<Player, Long> playerSprintResetMap = new HashMap<>();
    private final Set<Player> playersWithResetActions = new HashSet<>();
    private final Map<Player, Long> lastAttackTimeMap = new HashMap<>();
    private boolean versionHasNetherite = true;
    private boolean hasShields = false;

    public PracticeKnockbackHandler() {
        // Detect server version for compatibility
        if (Bukkit.getVersion().contains("1.7") || Bukkit.getVersion().contains("1.8") ||
                Bukkit.getVersion().contains("1.9") || Bukkit.getVersion().contains("1.10") ||
                Bukkit.getVersion().contains("1.11") || Bukkit.getVersion().contains("1.12") ||
                Bukkit.getVersion().contains("1.13") || Bukkit.getVersion().contains("1.14") ||
                Bukkit.getVersion().contains("1.15")) {
            versionHasNetherite = false;
        }

        if (Bukkit.getVersion().contains("1.8") || Bukkit.getVersion().contains("1.7")) {
            hasShields = false;
        } else {
            hasShields = true;
        }

        // Clear knockback hashmap every 10 ticks to prevent memory leaks while maintaining consistency
        Bukkit.getScheduler().runTaskTimer(Practice.getInstance(), playerKnockbackHashMap::clear, 10, 10);
        
        // Clean up sprint reset map every 20 ticks (1 second) to prevent memory leaks
        Bukkit.getScheduler().runTaskTimer(Practice.getInstance(), () -> {
            long currentTime = System.currentTimeMillis();
            playerSprintResetMap.entrySet().removeIf(entry -> {
                // Remove entries older than 10 seconds to prevent memory leaks
                return currentTime - entry.getValue() > 10000;
            });
        }, 20, 20);
        
        // Clean up reset actions set every 20 ticks
        Bukkit.getScheduler().runTaskTimer(Practice.getInstance(), playersWithResetActions::clear, 20, 20);
        
        // Clean up last attack time map every 20 ticks
        Bukkit.getScheduler().runTaskTimer(Practice.getInstance(), () -> {
            long currentTime = System.currentTimeMillis();
            lastAttackTimeMap.entrySet().removeIf(entry -> {
                // Remove entries older than 15 seconds to prevent memory leaks
                return currentTime - entry.getValue() > 15000;
            });
        }, 20, 20);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerVelocityEvent(PlayerVelocityEvent event) {
        if (!playerKnockbackHashMap.containsKey(event.getPlayer())) return;
        event.setVelocity(playerKnockbackHashMap.get(event.getPlayer()));
        playerKnockbackHashMap.remove(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageEntity(EntityDamageByEntityEvent event) {
        // Check if sword PvP, not PvE or EvE
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player && 
            !event.isCancelled() && event.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK)) {
            
            if (hasShields && event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) != 0) {
                return;
            }

            if (!(event.getEntity() instanceof Player)) return;
            Player victim = (Player) event.getEntity();

            // Bot system has been removed - skip bot match checks

            // Get the match to find the kit being used
            Match match = Practice.getInstance().getMatchManager().getPlayerMatch(victim.getUniqueId());
            if (match == null) return;

            Kit kit = match.getKit();
            if (kit == null) return;

            // Get the knockback profile for this kit
            String profileName = kit.getKnockbackProfile();
            KnockbackProfile profile = Practice.getInstance().getKnockbackManager().getProfile(profileName);
            if (profile == null || !profile.isEnabled()) {
                profile = Practice.getInstance().getKnockbackManager().getDefaultProfile();
            }

            // Disable netherite kb, the knockback resistance attribute makes the velocity event not be called
            // Also it makes players sometimes just not take any knockback, and reduces knockback
            // This affects both PvP and PvE, so put it above the PvP check
            // We technically don't have to check the version but bad server jars might break if we do
            if (versionHasNetherite && !profile.isNetheriteKnockbackResistance())
                for (AttributeModifier modifier : victim.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).getModifiers())
                    victim.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).removeModifier(modifier);

            if (!(event.getDamager() instanceof Player)) return;
            if (!event.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK)) return;
            if (event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) != 0) return;

            Player attacker = (Player) event.getDamager();

            // Figure out base knockback direction
            double d0 = attacker.getLocation().getX() - victim.getLocation().getX();
            double d1;

            for (d1 = attacker.getLocation().getZ() - victim.getLocation().getZ();
                 d0 * d0 + d1 * d1 < 1.0E-4D; d1 = (Math.random() - Math.random()) * 0.01D)
                d0 = (Math.random() - Math.random()) * 0.01D;

            double magnitude = Math.sqrt(d0 * d0 + d1 * d1);

            // Get player knockback taken before any friction applied
            Vector playerVelocity = victim.getVelocity();

            // apply friction then add the base knockback
            // Dynamic friction calculation based on horizontal/vertical values
            double horizontalFriction = profile.getHorizontalFriction() * (1.0 + profile.getHorizontal() * 0.5);
            double verticalFriction = profile.getVerticalFriction() * (1.0 + profile.getVertical() * 0.5);
            
            playerVelocity.setX((playerVelocity.getX() / horizontalFriction) - (d0 / magnitude * profile.getHorizontal()));
            
            // Apply vertical friction reduction if enabled
            if (profile.isVerticalFrictionReduction()) {
                playerVelocity.setY(playerVelocity.getY() + profile.getVertical());
            } else {
                playerVelocity.setY((playerVelocity.getY() / verticalFriction) + profile.getVertical());
            }
            
            playerVelocity.setZ((playerVelocity.getZ() / horizontalFriction) - (d1 / magnitude * profile.getHorizontal()));

            // Calculate bonus knockback for sprinting or knockback enchantment levels
            int i = attacker.getItemInHand().getEnchantmentLevel(Enchantment.KNOCKBACK);
            
            // Handle sprint reset ticks for bonus knockback
            if (!profile.isDisableSprintToggle() && profile.getSprintResetTicks() > 0) {
                long currentTime = System.currentTimeMillis();
                Long lastSprintTime = playerSprintResetMap.get(attacker);
                
                // Check if player performed a reset action (W-tap, S-tap, block hit, or hasn't attacked recently)
                boolean hasResetAction = playersWithResetActions.contains(attacker);
                
                // Check if player hasn't attacked in 7 seconds (7000ms)
                Long lastAttackTime = lastAttackTimeMap.get(attacker);
                boolean hasRecentAttack = lastAttackTime != null && (currentTime - lastAttackTime) <= 7000;
                
                if (attacker.isSprinting() && !hasResetAction && hasRecentAttack) {
                    // Player is currently sprinting, hasn't performed reset action, and has attacked recently - update timestamp
                    playerSprintResetMap.put(attacker, currentTime);
                    ++i;
                } else if (lastSprintTime != null && !hasResetAction && hasRecentAttack) {
                    // Player was sprinting recently, hasn't performed reset action, and has attacked recently - check if within reset window
                    long timeSinceLastSprint = currentTime - lastSprintTime;
                    long resetWindowTicks = profile.getSprintResetTicks() * 50L; // Convert ticks to milliseconds
                    
                    if (timeSinceLastSprint <= resetWindowTicks) {
                        // Still within sprint reset window, no reset action performed, and has attacked recently
                        ++i;
                    }
                }
            } else if (!profile.isDisableSprintToggle() && attacker.isSprinting()) {
                // Default behavior - sprinting gives bonus knockback
                ++i;
            }

            if (playerVelocity.getY() > profile.getVerticalLimit())
                playerVelocity.setY(profile.getVerticalLimit());

            // Apply bonus knockback
            if (i > 0) {
                playerVelocity.add(new Vector((-Math.sin(attacker.getLocation().getYaw() * 3.1415927F / 180.0F) *
                        (float) i * profile.getExtraHorizontal()), profile.getExtraVertical(),
                        Math.cos(attacker.getLocation().getYaw() * 3.1415927F / 180.0F) *
                                (float) i * profile.getExtraHorizontal()));
                
                // Apply extra vertical limit for bonus knockback
                if (playerVelocity.getY() > profile.getExtraVerticalLimit())
                    playerVelocity.setY(profile.getExtraVerticalLimit());
            }

            // Allow netherite to affect the horizontal knockback
            if (profile.isNetheriteKnockbackResistance()) {
                double resistance = 1 - victim.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).getValue();
                playerVelocity.multiply(new Vector(resistance, 1, resistance));
            }

            // Update last attack time for both attacker and victim
            long currentTime = System.currentTimeMillis();
            lastAttackTimeMap.put(attacker, currentTime);
            lastAttackTimeMap.put(victim, currentTime);
            
            // Knockback is sent immediately in 1.8+, there is no reason to send packets manually
            playerKnockbackHashMap.put(victim, playerVelocity);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Detect block hits (right-click with sword) - this is a reset action
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR || 
            event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            
            // Check if player is holding a sword (block hitting)
            if (event.getItem() != null && 
                event.getItem().getType().toString().contains("SWORD")) {
                playersWithResetActions.add(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        // Detect S-tapping (sneaking) - this is a reset action
        if (event.isSneaking()) {
            playersWithResetActions.add(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerVelocity(PlayerVelocityEvent event) {
        // Detect W-tapping (sudden velocity changes) - this is a reset action
        // W-tapping typically involves stopping and starting movement quickly
        // We can detect this by looking for rapid velocity changes
        Player player = event.getPlayer();
        Vector velocity = event.getVelocity();
        
        // Check if this is a significant velocity change (likely W-tap)
        if (velocity.length() > 0.1) {
            // Add a small delay to ensure this is processed after the knockback
            Bukkit.getScheduler().runTaskLater(Practice.getInstance(), () -> {
                playersWithResetActions.add(player);
            }, 1L);
        }
    }
}
