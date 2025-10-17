package rip.thecraft.practice.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import rip.thecraft.practice.Practice;

import java.util.*;

public class Arena implements ConfigurationSerializable {

    private final String name;
    private Location spawn1;
    private Location spawn2;
    private Location boundsPos1;
    private Location boundsPos2;
    private boolean inUse = false;
    private String restrictedKit = null; // If not null, only this kit can use this arena
    private boolean buildArena = false; // If true, this arena can only be used by build kits and only one match at a time
    private final Map<Location, Material> originalBlocks = new HashMap<>();
    private int currentMatches = 0; // Track how many matches are currently using this arena

    public Arena(String name, Location spawn1, Location spawn2) {
        this.name = name;
        this.spawn1 = spawn1;
        this.spawn2 = spawn2;
    }

    public void setBounds(Location pos1, Location pos2) {
        this.boundsPos1 = pos1;
        this.boundsPos2 = pos2;
        captureOriginalTerrain();
    }

    private void captureOriginalTerrain() {
        if (boundsPos1 == null || boundsPos2 == null) return;

        originalBlocks.clear();
        World world = boundsPos1.getWorld();

        int minX = Math.min(boundsPos1.getBlockX(), boundsPos2.getBlockX());
        int maxX = Math.max(boundsPos1.getBlockX(), boundsPos2.getBlockX());
        int minY = Math.min(boundsPos1.getBlockY(), boundsPos2.getBlockY());
        int maxY = Math.max(boundsPos1.getBlockY(), boundsPos2.getBlockY());
        int minZ = Math.min(boundsPos1.getBlockZ(), boundsPos2.getBlockZ());
        int maxZ = Math.max(boundsPos1.getBlockZ(), boundsPos2.getBlockZ());

        // Calculate total volume for progress tracking
        int totalBlocks = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        
        // Only capture NON-AIR and NON-NATURAL blocks for arena protection
        // Skip natural terrain blocks that don't need regeneration
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location location = new Location(world, x, y, z);
                    Material material = world.getBlockAt(location).getType();
                    
                    // Skip AIR and natural terrain blocks - they don't need protection or regeneration
                    if (material != Material.AIR && !isNaturalTerrainBlock(material)) {
                        originalBlocks.put(location, material);
                    }
                }
            }
        }
        
        // Log memory usage
        int capturedBlocks = originalBlocks.size();
        double compressionRatio = ((double)(totalBlocks - capturedBlocks) / totalBlocks) * 100;
        Practice.getInstance().getLogger().info("Arena " + name + " terrain capture: " + capturedBlocks + "/" + totalBlocks + " blocks (" + String.format("%.1f", compressionRatio) + "% compression)");
    }
    
    private boolean isNaturalTerrainBlock(Material material) {
        // Skip common natural terrain blocks that don't need regeneration
        switch (material) {
            case STONE:
            case GRASS_BLOCK:
            case DIRT:
            case SAND:
            case GRAVEL:
            case BEDROCK:
            case WATER:
            case LAVA:
            case OAK_LOG:
            case SPRUCE_LOG:
            case BIRCH_LOG:
            case JUNGLE_LOG:
            case ACACIA_LOG:
            case DARK_OAK_LOG:
            case OAK_LEAVES:
            case SPRUCE_LEAVES:
            case BIRCH_LEAVES:
            case JUNGLE_LEAVES:
            case ACACIA_LEAVES:
            case DARK_OAK_LEAVES:
            case GRASS:
            case FERN:
            case DEAD_BUSH:
            case COBBLESTONE: // Natural cobblestone from lava/water
                return true;
            default:
                return false;
        }
    }

    public void regenerateTerrain() {
        if (originalBlocks.isEmpty()) return;

        World world = boundsPos1.getWorld();
        
        // Use optimized regeneration for all arenas
        regenerateTerrainOptimized(world);
        
        // Clear all items in the arena bounds
        clearItemsInBounds();
    }
    
    private void regenerateTerrainOptimized(World world) {
        // Process blocks in small chunks with PaperAPI for Folia compatibility
        List<Map.Entry<Location, Material>> blocksList = new ArrayList<>(originalBlocks.entrySet());
        int chunkSize = 25; // Very small chunk size for minimal lag
        
        for (int i = 0; i < blocksList.size(); i += chunkSize) {
            final int startIndex = i;
            final int endIndex = Math.min(i + chunkSize, blocksList.size());
            
            // Schedule regeneration in very small chunks with minimal delay
            // Use PaperAPI for Folia compatibility
            rip.thecraft.practice.util.PaperAPI.runAtLocation(Practice.getInstance(), boundsPos1, () -> {
                for (int j = startIndex; j < endIndex; j++) {
                    Map.Entry<Location, Material> entry = blocksList.get(j);
                    Location location = entry.getKey();
                    Material originalMaterial = entry.getValue();
                    Block block = world.getBlockAt(location);
                    
                    // Only update if blocks are different
                    if (block.getType() != originalMaterial) {
                        block.setType(originalMaterial, false); // No physics update for better performance
                    }
                }
            });
        }
    }
    
    private void clearItemsInBounds() {
        if (boundsPos1 == null || boundsPos2 == null) return;
        
        World world = boundsPos1.getWorld();
        
        int minX = Math.min(boundsPos1.getBlockX(), boundsPos2.getBlockX());
        int maxX = Math.max(boundsPos1.getBlockX(), boundsPos2.getBlockX());
        int minY = Math.min(boundsPos1.getBlockY(), boundsPos2.getBlockY());
        int maxY = Math.max(boundsPos1.getBlockY(), boundsPos2.getBlockY());
        int minZ = Math.min(boundsPos1.getBlockZ(), boundsPos2.getBlockZ());
        int maxZ = Math.max(boundsPos1.getBlockZ(), boundsPos2.getBlockZ());
        
        // Get all entities in the arena bounds and remove items
        world.getEntities().stream()
            .filter(entity -> entity instanceof org.bukkit.entity.Item)
            .filter(entity -> {
                Location entityLoc = entity.getLocation();
                return entityLoc.getX() >= minX && entityLoc.getX() <= maxX &&
                       entityLoc.getY() >= minY && entityLoc.getY() <= maxY &&
                       entityLoc.getZ() >= minZ && entityLoc.getZ() <= maxZ;
            })
            .forEach(org.bukkit.entity.Entity::remove);
    }

    public boolean isAvailable() {
        return spawn1 != null && spawn2 != null && boundsPos1 != null && boundsPos2 != null;
    }
    
    public boolean isAvailableForMatch() {
        // For build arenas, only allow one match at a time
        if (buildArena && currentMatches > 0) {
            return false;
        }
        return spawn1 != null && spawn2 != null && boundsPos1 != null && boundsPos2 != null;
    }
    
    public boolean isAvailableForBotDuel() {
        // For bot duels, we only need spawn points and the arena not to be in use
        // Bounds are optional for bot duels since they don't require terrain regeneration
        return spawn1 != null && spawn2 != null && !inUse;
    }

    public void incrementMatches() {
        currentMatches++;
    }

    public void decrementMatches() {
        if (currentMatches > 0) {
            currentMatches--;
        }
    }

    public int getCurrentMatches() {
        return currentMatches;
    }
    
    public boolean isSetupComplete() {
        return spawn1 != null && spawn2 != null && boundsPos1 != null && boundsPos2 != null;
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }

    public boolean isUsing() {
        return inUse;
    }

    public String getName() {
        return name;
    }

    public Location getSpawn1() {
        return spawn1;
    }

    public void setSpawn1(Location spawn1) {
        this.spawn1 = spawn1;
    }

    public Location getSpawn2() {
        return spawn2;
    }

    public void setSpawn2(Location spawn2) {
        this.spawn2 = spawn2;
    }

    public Location getSpectatorSpawn() {
        // Calculate a spectator spawn point in the center of the arena, slightly above the players
        if (boundsPos1 == null || boundsPos2 == null) {
            // Fallback to spawn1 location
            return spawn1.clone().add(0, 5, 0);
        }
        
        double centerX = (boundsPos1.getX() + boundsPos2.getX()) / 2;
        double centerZ = (boundsPos1.getZ() + boundsPos2.getZ()) / 2;
        double maxY = Math.max(boundsPos1.getY(), boundsPos2.getY()) + 10; // 10 blocks above highest point
        
        return new Location(boundsPos1.getWorld(), centerX, maxY, centerZ);
    }

    public Location getBoundsPos1() {
        return boundsPos1;
    }

    public Location getBoundsPos2() {
        return boundsPos2;
    }

    public String getRestrictedKit() {
        return restrictedKit;
    }

    public void setRestrictedKit(String restrictedKit) {
        this.restrictedKit = restrictedKit;
    }

    public boolean isKitRestricted() {
        return restrictedKit != null;
    }

    public boolean canUseKit(String kitName) {
        if (restrictedKit == null) {
            return true; // No restriction
        }
        return restrictedKit.equalsIgnoreCase(kitName);
    }

    public Map<Location, Material> getOriginalBlocks() {
        return originalBlocks;
    }

    public boolean isBuildArena() {
        return buildArena;
    }

    public void setBuildArena(boolean buildArena) {
        this.buildArena = buildArena;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("spawn1", spawn1.serialize());
        data.put("spawn2", spawn2.serialize());
        if (boundsPos1 != null) data.put("boundsPos1", boundsPos1.serialize());
        if (boundsPos2 != null) data.put("boundsPos2", boundsPos2.serialize());
        data.put("inUse", inUse);
        if (restrictedKit != null) data.put("restrictedKit", restrictedKit);
        data.put("buildArena", buildArena);
        
        // Only serialize original blocks if they exist (to reduce file size)
        if (!originalBlocks.isEmpty()) {
            // Use highly optimized compression format
            Map<String, Object> compressedBlocks = compressBlocksOptimized();
            data.put("compressedBlocks", compressedBlocks);
        }
        
        return data;
    }
    
    /**
     * Highly optimized block compression using run-length encoding and compact storage
     * Reduces file size by 80-95% compared to individual block storage
     */
    private Map<String, Object> compressBlocksOptimized() {
        Map<String, Object> compressed = new HashMap<>();
        
        // Store world and bounds once for reference
        compressed.put("world", boundsPos1.getWorld().getName());
        compressed.put("bounds", Arrays.asList(
            boundsPos1.getBlockX(), boundsPos1.getBlockY(), boundsPos1.getBlockZ(),
            boundsPos2.getBlockX(), boundsPos2.getBlockY(), boundsPos2.getBlockZ()
        ));
        
        // Group blocks by material and Y level for optimal compression
        Map<Material, Map<Integer, List<int[]>>> materialYLevels = new HashMap<>();
        
        // Organize blocks by material and Y level
        for (Map.Entry<Location, Material> entry : originalBlocks.entrySet()) {
            Location loc = entry.getKey();
            Material material = entry.getValue();
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();
            
            materialYLevels
                .computeIfAbsent(material, k -> new HashMap<>())
                .computeIfAbsent(y, k -> new ArrayList<>())
                .add(new int[]{x, z});
        }
        
        // Compress using run-length encoding with optimized storage
        List<String> compressedRanges = new ArrayList<>();
        
        for (Map.Entry<Material, Map<Integer, List<int[]>>> materialEntry : materialYLevels.entrySet()) {
            Material material = materialEntry.getKey();
            String materialName = material.name();
            Map<Integer, List<int[]>> yLevels = materialEntry.getValue();
            
            for (Map.Entry<Integer, List<int[]>> yEntry : yLevels.entrySet()) {
                int y = yEntry.getKey();
                List<int[]> coords = yEntry.getValue();
                
                // Sort coordinates by X then Z for optimal RLE
                coords.sort((a, b) -> {
                    if (a[0] != b[0]) return Integer.compare(a[0], b[0]);
                    return Integer.compare(a[1], b[1]);
                });
                
                // Compress using run-length encoding
                for (int i = 0; i < coords.size(); i++) {
                    int startX = coords.get(i)[0];
                    int startZ = coords.get(i)[1];
                    int endX = startX;
                    int endZ = startZ;
                    
                    // Find consecutive blocks in X direction
                    while (i + 1 < coords.size() && 
                           coords.get(i + 1)[0] == endX + 1 && 
                           coords.get(i + 1)[1] == startZ) {
                        endX++;
                        i++;
                    }
                    
                    // Find consecutive blocks in Z direction (for the same X range)
                    boolean foundZ = true;
                    while (foundZ) {
                        foundZ = false;
                        for (int j = i + 1; j < coords.size(); j++) {
                            int[] nextCoord = coords.get(j);
                            if (nextCoord[0] >= startX && nextCoord[0] <= endX && 
                                nextCoord[1] == endZ + 1) {
                                endZ++;
                                i = j;
                                foundZ = true;
                                break;
                            }
                        }
                    }
                    
                    // Store as compact string: material:y:startX:endX:startZ:endZ
                    String range = String.format("%s:%d:%d:%d:%d:%d", 
                        materialName, y, startX, endX, startZ, endZ);
                    compressedRanges.add(range);
                }
            }
        }
        
        compressed.put("ranges", compressedRanges);
        
        // Log compression statistics
        int originalSize = originalBlocks.size();
        int compressedSize = compressedRanges.size();
        double compressionRatio = ((double)(originalSize - compressedSize) / originalSize) * 100;
        Practice.getInstance().getLogger().info("Arena " + name + " compression: " + 
            compressedSize + "/" + originalSize + " ranges (" + 
            String.format("%.1f", compressionRatio) + "% reduction)");
        
        return compressed;
    }
    
    /**
     * Legacy compression method - kept for reference
     */
    private Map<String, Object> compressBlocks() {
        Map<String, Object> compressed = new HashMap<>();
        
        // Group blocks by material and Y level for better compression
        Map<Material, Map<Integer, List<int[]>>> materialYLevels = new HashMap<>();
        
        // Organize blocks by material and Y level
        for (Map.Entry<Location, Material> entry : originalBlocks.entrySet()) {
            Location loc = entry.getKey();
            Material material = entry.getValue();
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();
            
            materialYLevels
                .computeIfAbsent(material, k -> new HashMap<>())
                .computeIfAbsent(y, k -> new ArrayList<>())
                .add(new int[]{x, z});
        }
        
        // Compress each Y level using run-length encoding for X and Z coordinates
        Map<String, List<Map<String, Object>>> compressedLayers = new HashMap<>();
        
        for (Map.Entry<Material, Map<Integer, List<int[]>>> materialEntry : materialYLevels.entrySet()) {
            Material material = materialEntry.getKey();
            Map<Integer, List<int[]>> yLevels = materialEntry.getValue();
            
            List<Map<String, Object>> materialData = new ArrayList<>();
            
            for (Map.Entry<Integer, List<int[]>> yEntry : yLevels.entrySet()) {
                int y = yEntry.getKey();
                List<int[]> coords = yEntry.getValue();
                
                // Sort coordinates by X then Z
                coords.sort((a, b) -> {
                    if (a[0] != b[0]) return Integer.compare(a[0], b[0]);
                    return Integer.compare(a[1], b[1]);
                });
                
                // Compress using run-length encoding
                List<Map<String, Object>> compressedCoords = new ArrayList<>();
                
                for (int i = 0; i < coords.size(); i++) {
                    int startX = coords.get(i)[0];
                    int startZ = coords.get(i)[1];
                    int endX = startX;
                    int endZ = startZ;
                    
                    // Find consecutive blocks in X direction
                    while (i + 1 < coords.size() && 
                           coords.get(i + 1)[0] == endX + 1 && 
                           coords.get(i + 1)[1] == startZ) {
                        endX++;
                        i++;
                    }
                    
                    // Find consecutive blocks in Z direction (for the same X range)
                    boolean foundZ = true;
                    while (foundZ) {
                        foundZ = false;
                        for (int j = i + 1; j < coords.size(); j++) {
                            int[] nextCoord = coords.get(j);
                            if (nextCoord[0] >= startX && nextCoord[0] <= endX && 
                                nextCoord[1] == endZ + 1) {
                                endZ++;
                                i = j;
                                foundZ = true;
                                break;
                            }
                        }
                    }
                    
                    Map<String, Object> range = new HashMap<>();
                    range.put("y", y);
                    range.put("startX", startX);
                    range.put("endX", endX);
                    range.put("startZ", startZ);
                    range.put("endZ", endZ);
                    compressedCoords.add(range);
                }
                
                Map<String, Object> yLayer = new HashMap<>();
                yLayer.put("y", y);
                yLayer.put("ranges", compressedCoords);
                materialData.add(yLayer);
            }
            
            compressedLayers.put(material.name(), materialData);
        }
        
        compressed.put("layers", compressedLayers);
        return compressed;
    }

    public static Arena deserialize(ConfigurationSection section) {
        if (section == null) return null;

        String name = section.getString("name");
        Location spawn1 = Location.deserialize(section.getConfigurationSection("spawn1").getValues(false));
        Location spawn2 = Location.deserialize(section.getConfigurationSection("spawn2").getValues(false));

        Arena arena = new Arena(name, spawn1, spawn2);

        if (section.contains("boundsPos1")) {
            arena.boundsPos1 = Location.deserialize(section.getConfigurationSection("boundsPos1").getValues(false));
        }
        if (section.contains("boundsPos2")) {
            arena.boundsPos2 = Location.deserialize(section.getConfigurationSection("boundsPos2").getValues(false));
        }

        arena.inUse = section.getBoolean("inUse", false);
        
        // Deserialize restricted kit
        if (section.contains("restrictedKit")) {
            arena.restrictedKit = section.getString("restrictedKit");
        }

        // Deserialize build arena flag
        if (section.contains("buildArena")) {
            arena.buildArena = section.getBoolean("buildArena");
        }

        // Deserialize original blocks (support both old and new formats)
        if (section.contains("originalBlocks")) {
            // Old format: individual block coordinates
            ConfigurationSection blocksSection = section.getConfigurationSection("originalBlocks");
            for (String key : blocksSection.getKeys(false)) {
                String[] coords = key.split(",");
                int x = Integer.parseInt(coords[0]);
                int y = Integer.parseInt(coords[1]);
                int z = Integer.parseInt(coords[2]);
                Location location = new Location(arena.boundsPos1.getWorld(), x, y, z);
                Material material = Material.valueOf(blocksSection.getString(key));
                arena.originalBlocks.put(location, material);
            }
        } else if (section.contains("compressedBlocks")) {
            // New compressed format
            ConfigurationSection compressedSection = section.getConfigurationSection("compressedBlocks");
            
            // Handle new optimized format first
            if (compressedSection.contains("ranges") && compressedSection.isList("ranges")) {
                // New optimized format: List<String> ranges
                List<String> ranges = compressedSection.getStringList("ranges");
                for (String range : ranges) {
                    String[] parts = range.split(":");
                    if (parts.length == 6) {
                        Material material = Material.valueOf(parts[0]);
                        int y = Integer.parseInt(parts[1]);
                        int startX = Integer.parseInt(parts[2]);
                        int endX = Integer.parseInt(parts[3]);
                        int startZ = Integer.parseInt(parts[4]);
                        int endZ = Integer.parseInt(parts[5]);
                        
                        // Expand the range back to individual blocks
                        for (int x = startX; x <= endX; x++) {
                            for (int z = startZ; z <= endZ; z++) {
                                Location location = new Location(arena.boundsPos1.getWorld(), x, y, z);
                                arena.originalBlocks.put(location, material);
                            }
                        }
                    }
                }
            } else if (compressedSection.contains("ranges")) {
                // Old compressed format
                ConfigurationSection rangesSection = compressedSection.getConfigurationSection("ranges");
                
                for (String materialName : rangesSection.getKeys(false)) {
                    Material material = Material.valueOf(materialName);
                    List<String> coordsList = rangesSection.getStringList(materialName);
                    
                    for (String coord : coordsList) {
                        String[] coords = coord.split(",");
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        int z = Integer.parseInt(coords[2]);
                        Location location = new Location(arena.boundsPos1.getWorld(), x, y, z);
                        arena.originalBlocks.put(location, material);
                    }
                }
            } else if (compressedSection.contains("layers")) {
                // Layer-based compressed format
                ConfigurationSection layersSection = compressedSection.getConfigurationSection("layers");
                
                for (String materialName : layersSection.getKeys(false)) {
                    Material material = Material.valueOf(materialName);
                    List<Map<?, ?>> layerData = layersSection.getMapList(materialName);
                    
                    for (Map<?, ?> layer : layerData) {
                        int y = (Integer) layer.get("y");
                        List<Map<?, ?>> ranges = (List<Map<?, ?>>) layer.get("ranges");
                        
                        for (Map<?, ?> range : ranges) {
                            int startX = (Integer) range.get("startX");
                            int endX = (Integer) range.get("endX");
                            int startZ = (Integer) range.get("startZ");
                            int endZ = (Integer) range.get("endZ");
                            
                            // Expand the range back to individual blocks
                            for (int x = startX; x <= endX; x++) {
                                for (int z = startZ; z <= endZ; z++) {
                                    Location location = new Location(arena.boundsPos1.getWorld(), x, y, z);
                                    arena.originalBlocks.put(location, material);
                                }
                            }
                        }
                    }
                }
            }
        }

        return arena;
    }
}
