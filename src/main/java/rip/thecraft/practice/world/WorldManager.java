package rip.thecraft.practice.world;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.plugin.java.JavaPlugin;
import rip.thecraft.practice.Practice;

import java.io.File;

public class WorldManager {

    private final JavaPlugin plugin;
    private World arenaWorld;
    private static final String ARENA_WORLD_NAME = "arenas";

    public WorldManager(JavaPlugin plugin) {
        this.plugin = plugin;
        initializeArenaWorld();
    }

    private void initializeArenaWorld() {
        try {
            // Check if the arena world already exists
            arenaWorld = Bukkit.getWorld(ARENA_WORLD_NAME);
            
            if (arenaWorld == null) {
                // Create a new void world
                WorldCreator worldCreator = new WorldCreator(ARENA_WORLD_NAME);
                worldCreator.type(WorldType.FLAT);
                worldCreator.generatorSettings("2;0;1;"); // Void world settings
                worldCreator.generateStructures(false);
                
                arenaWorld = worldCreator.createWorld();
                
                if (arenaWorld != null) {
                    // Configure world settings for optimal arena usage
                    configureWorldSettings(arenaWorld);
                    plugin.getLogger().info("Created arena world: " + ARENA_WORLD_NAME);
                } else {
                    plugin.getLogger().severe("Failed to create arena world: " + ARENA_WORLD_NAME);
                }
            } else {
                plugin.getLogger().info("Loaded existing arena world: " + ARENA_WORLD_NAME);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize arena world: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void configureWorldSettings(World world) {
        // Disable weather
        world.setStorm(false);
        world.setThundering(false);
        world.setWeatherDuration(0);
        
        // Disable time changes
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setTime(6000); // Noon time
        
        // Disable mob spawning
        world.setGameRuleValue("doMobSpawning", "false");
        
        // Disable block updates that could cause lag
        world.setGameRuleValue("randomTickSpeed", "0");
        
        // Disable fire spread
        world.setGameRuleValue("doFireTick", "false");
        
        // Disable mob griefing
        world.setGameRuleValue("mobGriefing", "false");
        
        // Keep inventory for easier testing
        world.setGameRuleValue("keepInventory", "true");
        
        // Disable natural regeneration to prevent healing during matches
        world.setGameRuleValue("naturalRegeneration", "false");
        
        plugin.getLogger().info("Configured arena world settings");
    }

    public World getArenaWorld() {
        return arenaWorld;
    }

    public boolean teleportToArenaWorld(org.bukkit.entity.Player player) {
        if (arenaWorld == null) {
            player.sendMessage(org.bukkit.ChatColor.RED + "Arena world is not available!");
            return false;
        }
        
        // Find a safe spawn location in the void world
        org.bukkit.Location spawnLocation = findSafeSpawnLocation();
        
        // Use the safe teleportation system from MatchManager
        Practice.getInstance().getMatchManager().teleportPlayerSafely(player, spawnLocation);
        player.sendMessage(org.bukkit.ChatColor.GREEN + "Teleported to arena world!");
        
        return true;
    }

    private org.bukkit.Location findSafeSpawnLocation() {
        // Start at world spawn and find a safe location
        org.bukkit.Location spawn = arenaWorld.getSpawnLocation();
        
        // Ensure we're at a safe Y level (above the void)
        if (spawn.getY() < 64) {
            spawn.setY(64);
        }
        
        // Check if the block below is safe
        org.bukkit.Location blockBelow = spawn.clone().subtract(0, 1, 0);
        if (blockBelow.getBlock().getType() == org.bukkit.Material.AIR) {
            // Place a platform for safety
            createSpawnPlatform(spawn);
        }
        
        return spawn;
    }

    private void createSpawnPlatform(org.bukkit.Location center) {
        // Create a small platform around the spawn point
        int radius = 3;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                org.bukkit.Location blockLoc = center.clone().add(x, -1, z);
                blockLoc.getBlock().setType(org.bukkit.Material.STONE);
            }
        }
    }

    public void shutdown() {
        // Clean up if needed
        if (arenaWorld != null) {
            // Save the world
            arenaWorld.save();
            plugin.getLogger().info("Saved arena world: " + ARENA_WORLD_NAME);
        }
    }
}
