package rip.thecraft.practice.listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import rip.thecraft.practice.Practice;
import rip.thecraft.practice.match.Match;
import rip.thecraft.practice.player.PlayerState;
import rip.thecraft.practice.util.VersionUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ArenaListener implements Listener {

    // Track blocks that need regeneration for each match
    private final Map<String, Set<Block>> brokenBlocks = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Set<Block>> placedBlocks = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Track which player placed which blocks for visibility management
    private final Map<String, Map<Block, Player>> blockOwners = new java.util.concurrent.ConcurrentHashMap<>();

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        var matchManager = Practice.getInstance().getMatchManager();
        if (matchManager == null) return;
        
        // Check if player has build bypass enabled
        var buildCommand = Practice.getInstance().getBuildCommand();
        if (buildCommand != null && buildCommand.hasBuildBypass(player)) {
            // Allow breaking any blocks for players with build bypass
            return;
        }
        
        Match match = matchManager.getPlayerMatch(player.getUniqueId());
        
        // Check if block is in any arena bounds
        var arenaManager = Practice.getInstance().getArenaManager();
        if (arenaManager == null) return;
        
        for (var arenaName : arenaManager.getArenaNames()) {
            var arena = arenaManager.getArena(arenaName);
            if (arena != null && arena.getBoundsPos1() != null && arena.getBoundsPos2() != null) {
                if (isLocationInBounds(event.getBlock().getLocation(), arena.getBoundsPos1(), arena.getBoundsPos2())) {
                    // Block is in arena bounds - check if we should allow breaking
                    if (match != null && match.isStarted()) {
                        var kit = match.getKit();
                        if (kit != null && kit.isBuildMode()) {
                            // Check if this is a player-placed block (not part of original arena structure)
                            if (isPlayerPlacedBlock(match, event.getBlock())) {
                                // Allow breaking player-placed blocks during matches with build mode
                                // Track the broken block for regeneration
                                trackBrokenBlock(match, event.getBlock());
                                return;
                            } else {
                                // Prevent breaking arena structure blocks and natural terrain
                                event.setCancelled(true);
                                player.sendMessage("§cYou cannot break arena blocks!");
                                return;
                            }
                        } else {
                            // Prevent block breaking in non-build mode kits
                            event.setCancelled(true);
                            player.sendMessage("§cYou cannot break blocks in this kit!");
                            return;
                        }
                    } else {
                        // Prevent breaking arena blocks outside of matches
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        var matchManager = Practice.getInstance().getMatchManager();
        if (matchManager == null) return;
        
        // Check if player has build bypass enabled
        var buildCommand = Practice.getInstance().getBuildCommand();
        if (buildCommand != null && buildCommand.hasBuildBypass(player)) {
            // Allow placing any blocks for players with build bypass
            return;
        }
        
        Match match = matchManager.getPlayerMatch(player.getUniqueId());
        
        // Check if block is in any arena bounds
        var arenaManager = Practice.getInstance().getArenaManager();
        if (arenaManager == null) return;
        
        for (var arenaName : arenaManager.getArenaNames()) {
            var arena = arenaManager.getArena(arenaName);
            if (arena != null && arena.getBoundsPos1() != null && arena.getBoundsPos2() != null) {
                if (isLocationInBounds(event.getBlock().getLocation(), arena.getBoundsPos1(), arena.getBoundsPos2())) {
                    // Block is in arena bounds - check if we should allow placing
                    if (match != null && match.isStarted()) {
                        var kit = match.getKit();
                        if (kit != null && kit.isBuildMode()) {
                            // Allow block placing during matches with build mode
                            // Track the placed block for regeneration
                            trackPlacedBlock(match, event.getBlock());
                            
                            // Hide the placed block from the opponent
                            hidePlacedBlockFromOpponent(match, player, event.getBlock());
                            return;
                        } else {
                            // Prevent block placing in non-build mode kits
                            event.setCancelled(true);
                            player.sendMessage("§cYou cannot place blocks in this kit!");
                            return;
                        }
                    } else {
                        // Prevent placing arena blocks outside of matches
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        var matchManager = Practice.getInstance().getMatchManager();
        if (matchManager == null) return;
        
        // Bot system has been removed
        
        Match match = matchManager.getPlayerMatch(player.getUniqueId());
        
        // Only allow damage in active matches
        if (match != null && match.isStarted()) {
            // Allow ALL damage during matches
            return;
        }
        
        // Prevent ALL damage outside of matches (including environmental damage)
        event.setCancelled(true);
        
        // Immediately reset player state if they somehow took damage
        if (player.getHealth() < 20.0) {
            player.setHealth(20.0);
        }
        
        // Reset fire ticks if player is on fire
        if (player.getFireTicks() > 0) {
            player.setFireTicks(0);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();
        var matchManager = Practice.getInstance().getMatchManager();
        if (matchManager == null) return;
        
        // Bot system has been removed
        
        Match playerMatch = matchManager.getPlayerMatch(player.getUniqueId());
        Match damagerMatch = matchManager.getPlayerMatch(damager.getUniqueId());
        
        // Only allow PvP damage when both players are in the same active match
        if (playerMatch != null && damagerMatch != null && 
            playerMatch.equals(damagerMatch) && 
            playerMatch.isStarted()) {
            // Allow PvP damage during matches
            return;
        }
        
        // Prevent PvP damage outside of matches
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        var matchManager = Practice.getInstance().getMatchManager();
        if (matchManager == null) return;
        
        Match match = matchManager.getPlayerMatch(player.getUniqueId());
        
        // Prevent movement before match starts only for sumo
        if (match != null && !match.isStarted()) {
            // Check if this is a sumo match
            if (match.getKit() != null && match.getKit().getName().equalsIgnoreCase("sumo")) {
                // For sumo, prevent all movement during countdown
                if (event.getFrom().getX() != event.getTo().getX() || 
                    event.getFrom().getZ() != event.getTo().getZ()) {
                    event.setTo(event.getFrom());
                }
            }
            // For all other gamemodes, allow movement during countdown
        }
        
        // Check if player is spectating and prevent them from leaving arena bounds
        var playerData = Practice.getInstance().getPlayerManager().getPlayerData(player);
        if (playerData != null && playerData.getState() == PlayerState.SPECTATING) {
            Match spectatingMatch = playerData.getSpectatingMatch();
            if (spectatingMatch != null) {
                var arena = spectatingMatch.getArena();
                if (arena != null && arena.getBoundsPos1() != null && arena.getBoundsPos2() != null) {
                    // Check if player is trying to leave arena bounds
                    if (!isLocationInBounds(event.getTo(), arena.getBoundsPos1(), arena.getBoundsPos2())) {
                        // Teleport player back to center of arena
                        Location center = arena.getSpectatorSpawn();
                        event.setTo(center);
                        player.sendMessage(ChatColor.RED + "You cannot leave the arena bounds while spectating.");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        event.setCancelled(true);
    }
    
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        var matchManager = Practice.getInstance().getMatchManager();
        if (matchManager == null) return;
        
        Match match = matchManager.getPlayerMatch(player.getUniqueId());
        
        // Only allow hunger changes in active matches
        if (match != null && match.isStarted()) {
            // Allow hunger changes during matches
            return;
        }
        
        // Prevent hunger changes outside of matches
        event.setCancelled(true);
        
        // Reset food level to full
        player.setFoodLevel(20);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if this is the kit selection GUI
        if (event.getView().getTitle().equals("Kit Selection")) {
            // Prevent taking items from the GUI
            event.setCancelled(true);
            
            // Handle kit selection
            if (event.getCurrentItem() != null && event.getCurrentItem().hasItemMeta()) {
                String displayName = event.getCurrentItem().getItemMeta().getDisplayName();
                
                // Extract kit name from display name (remove color codes)
                String kitName = displayName.replace("§", "&");
                kitName = kitName.replaceAll("&[0-9a-f]", "").trim();
                
                // Debug: Log selected kit - commented out to reduce console spam
                // Practice.getInstance().getLogger().info("Player " + player.getName() + " selected kit - Display: '" + displayName + "', Extracted: '" + kitName + "'");
                
                // Try to find the actual kit name by matching display names
                String actualKitName = findKitNameByDisplayName(displayName);
                if (actualKitName != null) {
                    kitName = actualKitName;
                    // Debug: Found actual kit name - commented out to reduce console spam
                    // Practice.getInstance().getLogger().info("Found actual kit name: " + actualKitName);
                }
                
                // Open queue selection for this kit
                openQueueSelection(player, kitName);
            }
        }
        // Check if this is the queue selection GUI
        else if (event.getView().getTitle().startsWith("Queue Selection:")) {
            // Prevent taking items from the GUI
            event.setCancelled(true);
            
            // Handle queue selection
            if (event.getCurrentItem() != null && event.getCurrentItem().hasItemMeta()) {
                String queueType = event.getCurrentItem().getItemMeta().getDisplayName().replace("§", "&");
                String kitName = event.getView().getTitle().replace("Queue Selection: ", "").trim();
                
                // Extract queue type from display name
                queueType = queueType.replaceAll("&[0-9a-f]", "").trim();
                
                // Join the queue
                joinQueue(player, kitName, queueType);
            }
        }
    }
    
    private void openQueueSelection(Player player, String kitName) {
        // Create queue selection GUI
        org.bukkit.inventory.Inventory gui = org.bukkit.Bukkit.createInventory(null, 27, "Queue Selection: " + kitName);
        
        // Add queue type options
        org.bukkit.inventory.ItemStack unrankedItem = createQueueItem("§aUnranked", "§7Play for fun, no ELO changes", VersionUtils.getMaterial("GREEN_WOOL"));
        org.bukkit.inventory.ItemStack rankedItem = createQueueItem("§cRanked", "§7Play for ELO and ranking", VersionUtils.getMaterial("RED_WOOL"));
        
        gui.setItem(11, unrankedItem);
        gui.setItem(15, rankedItem);
        
        player.openInventory(gui);
    }
    
    private org.bukkit.inventory.ItemStack createQueueItem(String name, String description, org.bukkit.Material material) {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(java.util.Arrays.asList(description.split("\n")));
        item.setItemMeta(meta);
        return item;
    }
    
    private String findKitNameByDisplayName(String displayName) {
        // Try to match the display name with actual kit names
        for (rip.thecraft.practice.kit.Kit kit : Practice.getInstance().getKitManager().getKitNames().stream()
                .map(name -> Practice.getInstance().getKitManager().getKit(name))
                .filter(kit -> kit != null)
                .collect(java.util.stream.Collectors.toList())) {
            
            // Check if the kit's display name matches
            String kitDisplayName = org.bukkit.ChatColor.translateAlternateColorCodes('&', kit.getDisplayName());
            if (kitDisplayName.equals(displayName)) {
                return kit.getName();
            }
        }
        return null;
    }
    
    private void joinQueue(Player player, String kitName, String queueType) {
        // Debug: Log the kit name being used - commented out to reduce console spam
        // Practice.getInstance().getLogger().info("Attempting to join queue - Player: " + player.getName() + 
        //    ", Kit: " + kitName + ", QueueType: " + queueType);
        
        // Convert queue type string to QueueType enum
        rip.thecraft.practice.queue.QueueType type;
        if (queueType.equalsIgnoreCase("ranked")) {
            type = rip.thecraft.practice.queue.QueueType.RANKED;
        } else {
            type = rip.thecraft.practice.queue.QueueType.UNRANKED;
        }
        
        // Join the queue - QueueManager will handle the message
        Practice.getInstance().getQueueManager().joinQueue(player, kitName, type);
        player.closeInventory();
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        // Check if item spawned in any arena bounds
        var arenaManager = Practice.getInstance().getArenaManager();
        if (arenaManager == null) return;
        
        for (var arenaName : arenaManager.getArenaNames()) {
            var arena = arenaManager.getArena(arenaName);
            if (arena != null && arena.getBoundsPos1() != null && arena.getBoundsPos2() != null) {
                if (isLocationInBounds(event.getLocation(), arena.getBoundsPos1(), arena.getBoundsPos2())) {
                    // Remove item instantly instead of setting pickup delay
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onPlayerPickupItem(org.bukkit.event.player.PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        var playerData = Practice.getInstance().getPlayerManager().getPlayerData(player);
        
        // Prevent spectators from picking up items
        if (playerData != null && playerData.getState() == PlayerState.SPECTATING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        Player player = event.getPlayer();
        var playerData = Practice.getInstance().getPlayerManager().getPlayerData(player);
        
        // Prevent spectators from using items that could give them advantages
        if (playerData != null && playerData.getState() == PlayerState.SPECTATING) {
            if (event.getItem() != null) {
                org.bukkit.Material itemType = event.getItem().getType();
                
                // Prevent using potions, food, or other consumables
                if (itemType == org.bukkit.Material.POTION ||
                    itemType == org.bukkit.Material.GOLDEN_APPLE ||
                    itemType == org.bukkit.Material.ENCHANTED_GOLDEN_APPLE ||
                    itemType == org.bukkit.Material.COOKED_BEEF ||
                    itemType == org.bukkit.Material.BREAD ||
                    itemType.name().contains("POTION") ||
                    itemType.isEdible()) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You cannot use items while spectating.");
                }
            }
        }
    }

    @EventHandler
    public void onPotionSplash(org.bukkit.event.entity.PotionSplashEvent event) {
        // Prevent spectators from being affected by potions
        for (org.bukkit.entity.LivingEntity entity : event.getAffectedEntities()) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                var playerData = Practice.getInstance().getPlayerManager().getPlayerData(player);
                
                if (playerData != null && playerData.getState() == PlayerState.SPECTATING) {
                    event.setIntensity(player, 0); // Remove effect from spectator
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(org.bukkit.event.player.PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        var playerData = Practice.getInstance().getPlayerManager().getPlayerData(player);
        
        // Prevent spectators from dropping items
        if (playerData != null && playerData.getState() == PlayerState.SPECTATING) {
            event.setCancelled(true);
        }
    }

    // ================================
    // SNOW COLLECTION PREVENTION
    // ================================

    @EventHandler
    public void onBlockForm(BlockFormEvent event) {
        // Prevent snow layers from forming in arenas
        var arenaManager = Practice.getInstance().getArenaManager();
        if (arenaManager == null) return;
        
        for (var arenaName : arenaManager.getArenaNames()) {
            var arena = arenaManager.getArena(arenaName);
            if (arena != null && arena.getBoundsPos1() != null && arena.getBoundsPos2() != null) {
                if (isLocationInBounds(event.getBlock().getLocation(), arena.getBoundsPos1(), arena.getBoundsPos2())) {
                    // Cancel snow formation in arena bounds
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        // Prevent snow weather in arena worlds
        if (event.getWorld().getName().equals("arenas")) {
            // Always keep clear weather in arena world
            if (event.toWeatherState()) {
                event.setCancelled(true);
                event.getWorld().setStorm(false);
                event.getWorld().setThundering(false);
                event.getWorld().setWeatherDuration(0);
            }
        }
    }

    // ================================
    // FIRE SPREAD PREVENTION
    // ================================

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        // Prevent fire from starting in arenas
        var arenaManager = Practice.getInstance().getArenaManager();
        if (arenaManager == null) return;
        
        for (var arenaName : arenaManager.getArenaNames()) {
            var arena = arenaManager.getArena(arenaName);
            if (arena != null && arena.getBoundsPos1() != null && arena.getBoundsPos2() != null) {
                if (isLocationInBounds(event.getBlock().getLocation(), arena.getBoundsPos1(), arena.getBoundsPos2())) {
                    // Cancel fire ignition in arena bounds
                    // This prevents flint & steel, lava, and other fire sources from creating fire
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        // Prevent fire from spreading in arenas
        var arenaManager = Practice.getInstance().getArenaManager();
        if (arenaManager == null) return;
        
        for (var arenaName : arenaManager.getArenaNames()) {
            var arena = arenaManager.getArena(arenaName);
            if (arena != null && arena.getBoundsPos1() != null && arena.getBoundsPos2() != null) {
                if (isLocationInBounds(event.getBlock().getLocation(), arena.getBoundsPos1(), arena.getBoundsPos2())) {
                    // Cancel fire spread in arena bounds
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }

    private boolean isLocationInBounds(Location location, Location pos1, Location pos2) {
        if (!location.getWorld().equals(pos1.getWorld())) return false;
        
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());
        
        return location.getX() >= minX && location.getX() <= maxX &&
               location.getY() >= minY && location.getY() <= maxY &&
               location.getZ() >= minZ && location.getZ() <= maxZ;
    }

    // Track a broken block for regeneration
    private void trackBrokenBlock(Match match, Block block) {
        String matchKey = getMatchKey(match);
        brokenBlocks.computeIfAbsent(matchKey, k -> new HashSet<>()).add(block);
    }

    // Track a placed block for regeneration
    private void trackPlacedBlock(Match match, Block block) {
        String matchKey = getMatchKey(match);
        placedBlocks.computeIfAbsent(matchKey, k -> new HashSet<>()).add(block);
    }

    // Regenerate arena after match ends
    public void regenerateArena(Match match) {
        String matchKey = getMatchKey(match);
        
        // Only regenerate if this was a build mode kit
        var kit = match.getKit();
        if (kit == null || !kit.isBuildMode()) {
            return;
        }

        // Get the arena
        var arena = match.getArena();
        if (arena == null) return;

        // Clean up all player-placed blocks and environmental changes
        Set<Block> broken = brokenBlocks.remove(matchKey);
        Set<Block> placed = placedBlocks.remove(matchKey);
        
        // Clean up water, lava, obsidian, and other environmental blocks
        cleanEnvironmentalBlocks(arena);
        
        // Show all hidden blocks before regeneration
        showHiddenBlocksToOpponent(match);

        // Trigger arena regeneration
        Practice.getInstance().getArenaManager().regenerateArena(arena);
    }
    
    // Clean up environmental blocks like water, lava, obsidian, etc.
    private void cleanEnvironmentalBlocks(rip.thecraft.practice.arena.Arena arena) {
        if (arena.getBoundsPos1() == null || arena.getBoundsPos2() == null) return;
        
        org.bukkit.World world = arena.getBoundsPos1().getWorld();
        
        int minX = Math.min(arena.getBoundsPos1().getBlockX(), arena.getBoundsPos2().getBlockX());
        int maxX = Math.max(arena.getBoundsPos1().getBlockX(), arena.getBoundsPos2().getBlockX());
        int minY = Math.min(arena.getBoundsPos1().getBlockY(), arena.getBoundsPos2().getBlockY());
        int maxY = Math.max(arena.getBoundsPos1().getBlockY(), arena.getBoundsPos2().getBlockY());
        int minZ = Math.min(arena.getBoundsPos1().getBlockZ(), arena.getBoundsPos2().getBlockZ());
        int maxZ = Math.max(arena.getBoundsPos1().getBlockZ(), arena.getBoundsPos2().getBlockZ());
        
        // Clean up water, lava, obsidian, and other environmental blocks
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    org.bukkit.block.Block block = world.getBlockAt(x, y, z);
                    org.bukkit.Material type = block.getType();
                    
                    // Remove water, lava, obsidian, and other environmental blocks
                    if (type == org.bukkit.Material.WATER || 
                        type == org.bukkit.Material.LAVA ||
                        type == org.bukkit.Material.OBSIDIAN ||
                        type == org.bukkit.Material.COBBLESTONE ||
                        type == org.bukkit.Material.STONE ||
                        type == org.bukkit.Material.DIRT ||
                        type == org.bukkit.Material.SAND ||
                        type == org.bukkit.Material.GRAVEL) {
                        // Only remove if it's not part of the original arena structure
                        // For now, we'll remove all environmental blocks and let the arena regeneration handle it
                        block.setType(org.bukkit.Material.AIR);
                    }
                }
            }
        }
    }

    // Check if a block is part of the arena structure (original terrain)
    private boolean isArenaStructureBlock(rip.thecraft.practice.arena.Arena arena, Block block) {
        // Check if the arena has original blocks stored
        if (arena.getOriginalBlocks() == null || arena.getOriginalBlocks().isEmpty()) {
            return false;
        }
        
        // Check if this block's location is in the original blocks map
        Location blockLocation = block.getLocation();
        for (Map.Entry<Location, org.bukkit.Material> entry : arena.getOriginalBlocks().entrySet()) {
            Location originalLocation = entry.getKey();
            if (originalLocation.getBlockX() == blockLocation.getBlockX() &&
                originalLocation.getBlockY() == blockLocation.getBlockY() &&
                originalLocation.getBlockZ() == blockLocation.getBlockZ()) {
                // This block is part of the original arena structure
                return true;
            }
        }
        
        return false;
    }

    // Check if a block was placed by a player during the match
    private boolean isPlayerPlacedBlock(Match match, Block block) {
        String matchKey = getMatchKey(match);
        Set<Block> placedBlocksForMatch = placedBlocks.get(matchKey);
        
        if (placedBlocksForMatch == null || placedBlocksForMatch.isEmpty()) {
            return false;
        }
        
        // Check if this block is in the placed blocks set
        for (Block placedBlock : placedBlocksForMatch) {
            if (placedBlock.getLocation().equals(block.getLocation())) {
                return true;
            }
        }
        
        return false;
    }

    // Generate a unique key for the match
    private String getMatchKey(Match match) {
        return match.getPlayer1().toString() + "_" + match.getPlayer2().toString();
    }

    /**
     * Hides a placed block from the opponent in build mode
     * NOTE: With build arenas, we no longer need to hide blocks since each build arena
     * is only used by one match at a time, so there are no other matches to hide blocks from
     */
    private void hidePlacedBlockFromOpponent(Match match, Player placingPlayer, Block block) {
        // With build arenas, we don't need to hide blocks from opponents
        // because build arenas are only used by one match at a time
        // This eliminates the need for Packet Events ghost blocks
    }
    
    /**
     * Hides blocks from all other players in the same arena
     * NOTE: With build arenas, this is no longer needed
     */
    private void hideBlockFromOtherPlayersInArena(Match match, Player placingPlayer, Block block) {
        // With build arenas, we don't need to hide blocks from other players
        // because build arenas are only used by one match at a time
    }
    
    /**
     * Hides all blocks placed by other matches from players in this match
     * This is called when a match starts to ensure players don't see blocks from other matches
     * NOTE: With build arenas, this is no longer needed
     */
    public void hideBlocksFromOtherMatches(Match match) {
        // With build arenas, we don't need to hide blocks from other matches
        // because build arenas are only used by one match at a time
    }
    
    /**
     * Hides blocks placed in another match from players in this match
     * NOTE: With build arenas, this is no longer needed
     */
    private void hidePlacedBlocksFromOtherMatch(Match targetMatch, Match sourceMatch) {
        // With build arenas, we don't need to hide blocks from other matches
        // because build arenas are only used by one match at a time
    }
    
    /**
     * Shows all hidden blocks to the opponent when match ends
     * NOTE: With build arenas, this is no longer needed since we don't hide blocks
     */
    private void showHiddenBlocksToOpponent(Match match) {
        // With build arenas, we don't need to show hidden blocks
        // because we never hide them in the first place
    }
}
