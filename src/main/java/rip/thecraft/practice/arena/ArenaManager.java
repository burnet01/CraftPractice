package rip.thecraft.practice.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.plugin.java.JavaPlugin;
import rip.thecraft.practice.Practice;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaManager {

    private final JavaPlugin plugin;
    private final Map<String, Arena> arenas = new ConcurrentHashMap<>();
    private final File arenaFile;
    private final FileConfiguration arenaConfig;

    public ArenaManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.arenaFile = new File(plugin.getDataFolder(), "arenas.yml");
        this.arenaConfig = YamlConfiguration.loadConfiguration(arenaFile);
        loadArenas();
    }

    public void loadArenas() {
        arenas.clear();
        if (!arenaFile.exists()) {
            saveArenas();
            return;
        }

        for (String name : arenaConfig.getKeys(false)) {
            Arena arena = Arena.deserialize(arenaConfig.getConfigurationSection(name));
            if (arena != null) {
                arenas.put(name.toLowerCase(), arena);
            }
        }
        plugin.getLogger().info("Loaded " + arenas.size() + " arenas");
    }

    public void saveArenas() {
        try {
            for (Map.Entry<String, Arena> entry : arenas.entrySet()) {
                arenaConfig.set(entry.getKey(), entry.getValue().serialize());
            }
            arenaConfig.save(arenaFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save arenas: " + e.getMessage());
        }
    }

    public boolean createArena(String name, Location spawn1, Location spawn2) {
        if (arenas.containsKey(name.toLowerCase())) {
            return false;
        }

        Arena arena = new Arena(name, spawn1, spawn2);
        arenas.put(name.toLowerCase(), arena);
        saveArenas();
        return true;
    }

    public boolean setArenaBounds(String name, Location pos1, Location pos2) {
        Arena arena = arenas.get(name.toLowerCase());
        if (arena == null) {
            return false;
        }

        arena.setBounds(pos1, pos2);
        saveArenas();
        return true;
    }

    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    public List<Arena> getAvailableArenas() {
        List<Arena> available = new ArrayList<>();
        for (Arena arena : arenas.values()) {
            if (arena.isAvailable()) {
                available.add(arena);
            }
        }
        return available;
    }

    public Arena findAvailableArena() {
        for (Arena arena : arenas.values()) {
            if (arena.isAvailableForMatch()) {
                return arena;
            }
        }
        return null;
    }

    public Arena findAvailableArenaForKit(String kitName) {
        // Get the kit to check if it's a build kit
        var kit = Practice.getInstance().getKitManager().getKit(kitName);
        boolean isBuildKit = kit != null && kit.isBuildMode();
        
        plugin.getLogger().info("Finding arena for kit: " + kitName + " (build kit: " + isBuildKit + ", total arenas: " + arenas.size() + ")");
        
        // First, try to find an arena that's specifically restricted to this kit
        for (Arena arena : arenas.values()) {
            if (arena.isAvailableForMatch() && arena.isKitRestricted() && arena.canUseKit(kitName)) {
                // For build kits, only use build arenas
                if (isBuildKit && !arena.isBuildArena()) {
                    continue;
                }
                // For non-build kits, don't use build arenas
                if (!isBuildKit && arena.isBuildArena()) {
                    continue;
                }
                plugin.getLogger().info("Found restricted arena: " + arena.getName() + " for kit: " + kitName);
                return arena;
            }
        }
        
        // If no restricted arena found, find any available arena
        for (Arena arena : arenas.values()) {
            if (arena.isAvailableForMatch() && !arena.isKitRestricted()) {
                // For build kits, only use build arenas
                if (isBuildKit && !arena.isBuildArena()) {
                    continue;
                }
                // For non-build kits, don't use build arenas
                if (!isBuildKit && arena.isBuildArena()) {
                    continue;
                }
                plugin.getLogger().info("Found available arena: " + arena.getName() + " for kit: " + kitName);
                return arena;
            }
        }
        
        // For bot duels specifically, use the less restrictive check
        // This allows arenas that only have spawn points (no bounds required)
        for (Arena arena : arenas.values()) {
            if (arena.isAvailableForBotDuel()) {
                // For build kits, only use build arenas
                if (isBuildKit && !arena.isBuildArena()) {
                    continue;
                }
                // For non-build kits, don't use build arenas
                if (!isBuildKit && arena.isBuildArena()) {
                    continue;
                }
                plugin.getLogger().info("Found bot duel arena: " + arena.getName() + " for kit: " + kitName);
                return arena;
            }
        }
        
        // FALLBACK: If no arena found with the normal logic, try ANY arena with spawn points
        // This is the emergency fallback for bot duels
        for (Arena arena : arenas.values()) {
            if (arena.getSpawn1() != null && arena.getSpawn2() != null && !arena.isUsing()) {
                plugin.getLogger().info("Found fallback arena: " + arena.getName() + " for kit: " + kitName);
                return arena;
            }
        }
        
        // Debug: log all arenas and their status
        plugin.getLogger().info("No suitable arena found for kit: " + kitName);
        plugin.getLogger().info("Available arenas status:");
        for (Arena arena : arenas.values()) {
            plugin.getLogger().info("  Arena: " + arena.getName() + 
                " | Spawn1: " + (arena.getSpawn1() != null) + 
                " | Spawn2: " + (arena.getSpawn2() != null) + 
                " | Bounds1: " + (arena.getBoundsPos1() != null) + 
                " | Bounds2: " + (arena.getBoundsPos2() != null) + 
                " | InUse: " + arena.isUsing() + 
                " | BuildArena: " + arena.isBuildArena() + 
                " | RestrictedKit: " + arena.getRestrictedKit() + 
                " | AvailableForMatch: " + arena.isAvailableForMatch() + 
                " | AvailableForBotDuel: " + arena.isAvailableForBotDuel());
        }
        
        return null;
    }

    public boolean setArenaRestrictedKit(String arenaName, String kitName) {
        Arena arena = arenas.get(arenaName.toLowerCase());
        if (arena == null) {
            return false;
        }

        arena.setRestrictedKit(kitName);
        saveArenas();
        return true;
    }

    public boolean removeArenaRestrictedKit(String arenaName) {
        Arena arena = arenas.get(arenaName.toLowerCase());
        if (arena == null) {
            return false;
        }

        arena.setRestrictedKit(null);
        saveArenas();
        return true;
    }

    public void releaseArena(Arena arena) {
        arena.decrementMatches();
        
        // Only regenerate terrain if no matches are using this arena
        if (arena.getCurrentMatches() == 0) {
            // Schedule terrain regeneration with a small delay to ensure match is fully ended
            // Use PaperAPI for Folia compatibility
            rip.thecraft.practice.util.PaperAPI.runAtLocation(plugin, arena.getSpawn1(), arena::regenerateTerrain);
        }
    }

    public void useArena(Arena arena) {
        arena.incrementMatches();
    }

    public void regenerateArena(Arena arena) {
        // Regenerate the arena terrain
        arena.regenerateTerrain();
        
        // Clear any remaining items
        if (arena.getBoundsPos1() != null && arena.getBoundsPos2() != null) {
            clearItemsInBounds(arena.getBoundsPos1(), arena.getBoundsPos2());
        }
    }

    public boolean deleteArena(String name) {
        if (arenas.remove(name.toLowerCase()) != null) {
            arenaConfig.set(name.toLowerCase(), null);
            saveArenas();
            return true;
        }
        return false;
    }

    public Set<String> getArenaNames() {
        return arenas.keySet();
    }

    public void shutdown() {
        saveArenas();
    }
    
    public void clearAllArenaItems() {
        // Clear items from all arenas
        for (Arena arena : arenas.values()) {
            if (arena.getBoundsPos1() != null && arena.getBoundsPos2() != null) {
                clearItemsInBounds(arena.getBoundsPos1(), arena.getBoundsPos2());
            }
        }
    }
    
    private void clearItemsInBounds(Location pos1, Location pos2) {
        if (pos1 == null || pos2 == null) return;
        
        World world = pos1.getWorld();
        
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        
        // Get all entities in the arena bounds and remove items
        world.getEntities().stream()
            .filter(entity -> entity instanceof Item)
            .filter(entity -> {
                Location entityLoc = entity.getLocation();
                return entityLoc.getX() >= minX && entityLoc.getX() <= maxX &&
                       entityLoc.getY() >= minY && entityLoc.getY() <= maxY &&
                       entityLoc.getZ() >= minZ && entityLoc.getZ() <= maxZ;
            })
            .forEach(org.bukkit.entity.Entity::remove);
    }
}
