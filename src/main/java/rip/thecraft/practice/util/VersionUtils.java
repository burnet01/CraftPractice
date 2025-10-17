package rip.thecraft.practice.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Utility class for handling version compatibility between Minecraft versions
 * Supports 1.8.8 through latest versions
 */
public class VersionUtils {

    private static final String SERVER_VERSION;
    private static final int MAJOR_VERSION;
    private static final int MINOR_VERSION;
    
    static {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        SERVER_VERSION = packageName.substring(packageName.lastIndexOf('.') + 1);
        
        String[] versionParts = SERVER_VERSION.split("_");
        
        // Parse major version (e.g., 1 from "v1_8_R3")
        MAJOR_VERSION = parseVersionNumber(versionParts[1]);
        
        // Parse minor version (e.g., 8 from "v1_8_R3")
        // Handle revision numbers (R1, R2, R3) by extracting the number before 'R'
        if (versionParts.length > 2) {
            MINOR_VERSION = parseVersionNumber(versionParts[2]);
        } else {
            MINOR_VERSION = 0;
        }
    }
    
    /**
     * Parse version numbers that might contain letters (like R1, R2, R3)
     */
    private static int parseVersionNumber(String versionStr) {
        if (versionStr == null || versionStr.isEmpty()) {
            return 0;
        }
        
        // Remove any non-digit characters from the beginning
        String cleaned = versionStr.replaceAll("^[^0-9]*", "");
        
        if (cleaned.isEmpty()) {
            return 0;
        }
        
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            // If parsing fails, try to extract just the digits
            String digits = cleaned.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                return Integer.parseInt(digits);
            }
            return 0;
        }
    }
    
    /**
     * Check if the server is running 1.9 or higher
     */
    public static boolean isOneNineOrHigher() {
        return MAJOR_VERSION > 1 || (MAJOR_VERSION == 1 && MINOR_VERSION >= 9);
    }
    
    /**
     * Check if the server is running 1.13 or higher
     */
    public static boolean isOneThirteenOrHigher() {
        return MAJOR_VERSION > 1 || (MAJOR_VERSION == 1 && MINOR_VERSION >= 13);
    }
    
    /**
     * Check if the server is running 1.14 or higher
     */
    public static boolean isOneFourteenOrHigher() {
        return MAJOR_VERSION > 1 || (MAJOR_VERSION == 1 && MINOR_VERSION >= 14);
    }
    
    /**
     * Get the item in player's main hand (compatible with 1.8)
     */
    public static ItemStack getItemInMainHand(Player player) {
        if (isOneNineOrHigher()) {
            return player.getInventory().getItemInMainHand();
        } else {
            // 1.8 compatibility - main hand is held item slot
            return player.getInventory().getItem(player.getInventory().getHeldItemSlot());
        }
    }
    
    /**
     * Get the item in player's off hand (compatible with 1.8)
     */
    public static ItemStack getItemInOffHand(Player player) {
        if (isOneNineOrHigher()) {
            return player.getInventory().getItemInOffHand();
        } else {
            // 1.8 doesn't have off hand, return air
            return new ItemStack(Material.AIR);
        }
    }
    
    /**
     * Get material by name with version compatibility
     */
    public static Material getMaterial(String materialName) {
        try {
            Material material = Material.matchMaterial(materialName);
            if (material != null) {
                return material;
            }
            // Try legacy names for 1.8 compatibility
            return getLegacyMaterial(materialName);
        } catch (Exception e) {
            // Fallback to legacy names
            return getLegacyMaterial(materialName);
        }
    }
    
    /**
     * Get legacy material names for 1.8 compatibility
     */
    private static Material getLegacyMaterial(String materialName) {
        Material result = Material.AIR;
        
        switch (materialName) {
            // Wool colors
            case "RED_WOOL":
                result = isOneThirteenOrHigher() ? Material.matchMaterial("RED_WOOL") : Material.matchMaterial("WOOL");
                break;
            case "GREEN_WOOL":
                result = isOneThirteenOrHigher() ? Material.matchMaterial("GREEN_WOOL") : Material.matchMaterial("WOOL");
                break;
            case "BLUE_WOOL":
                result = isOneThirteenOrHigher() ? Material.matchMaterial("BLUE_WOOL") : Material.matchMaterial("WOOL");
                break;
            case "YELLOW_WOOL":
                result = isOneThirteenOrHigher() ? Material.matchMaterial("YELLOW_WOOL") : Material.matchMaterial("WOOL");
                break;
                
            // Dye colors (1.14+)
            case "RED_DYE":
                result = isOneFourteenOrHigher() ? Material.matchMaterial("RED_DYE") : Material.matchMaterial("INK_SACK");
                break;
            case "GREEN_DYE":
                result = isOneFourteenOrHigher() ? Material.matchMaterial("GREEN_DYE") : Material.matchMaterial("INK_SACK");
                break;
            case "BLUE_DYE":
                result = isOneFourteenOrHigher() ? Material.matchMaterial("BLUE_DYE") : Material.matchMaterial("INK_SACK");
                break;
            case "YELLOW_DYE":
                result = isOneFourteenOrHigher() ? Material.matchMaterial("YELLOW_DYE") : Material.matchMaterial("INK_SACK");
                break;
                
            // Other 1.13+ materials
            case "CLOCK":
                result = isOneThirteenOrHigher() ? Material.matchMaterial("CLOCK") : Material.matchMaterial("WATCH");
                break;
            case "BARRIER":
                result = isOneThirteenOrHigher() ? Material.matchMaterial("BARRIER") : Material.matchMaterial("STAINED_GLASS_PANE");
                break;
            case "OAK_SIGN":
                result = isOneThirteenOrHigher() ? Material.matchMaterial("OAK_SIGN") : Material.matchMaterial("SIGN");
                break;
            case "PAPER":
                result = Material.matchMaterial("PAPER"); // Same name
                break;
        }
        
        // Fallback to AIR if null
        return result != null ? result : Material.AIR;
    }
    
    /**
     * Create an item stack with proper data for 1.8 compatibility
     */
    public static ItemStack createItem(Material material, String displayName, String... lore) {
        ItemStack item = new ItemStack(material);
        
        // Handle legacy data values for 1.8
        if (!isOneThirteenOrHigher()) {
            switch (material.name()) {
                case "WOOL":
                    // Set wool color based on the modern material name we're trying to create
                    if (displayName.contains("Red") || displayName.contains("Ranked")) {
                        item.setDurability((short) 14); // Red wool
                    } else if (displayName.contains("Green") || displayName.contains("Unranked")) {
                        item.setDurability((short) 13); // Green wool
                    }
                    break;
                case "INK_SACK":
                    // Set dye color
                    if (displayName.contains("Red")) {
                        item.setDurability((short) 1); // Red dye
                    } else if (displayName.contains("Green")) {
                        item.setDurability((short) 2); // Green dye
                    } else if (displayName.contains("Blue")) {
                        item.setDurability((short) 4); // Blue dye
                    } else if (displayName.contains("Yellow")) {
                        item.setDurability((short) 11); // Yellow dye
                    }
                    break;
                case "STAINED_GLASS_PANE":
                    // Barrier item for 1.8
                    item.setDurability((short) 14); // Red stained glass pane
                    break;
            }
        }
        
        return item;
    }
    
    /**
     * Get server version string
     */
    public static String getServerVersion() {
        return SERVER_VERSION;
    }
    
    /**
     * Get major version
     */
    public static int getMajorVersion() {
        return MAJOR_VERSION;
    }
    
    /**
     * Get minor version
     */
    public static int getMinorVersion() {
        return MINOR_VERSION;
    }
}
