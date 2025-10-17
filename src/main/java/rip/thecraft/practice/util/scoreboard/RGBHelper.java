package rip.thecraft.practice.util.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RGB color support for Minecraft 1.20.1 with 1.12.1 API compatibility
 */
public class RGBHelper {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static boolean supportsRGB = false;
    private static Method fromHexMethod = null;

    static {
        // Check if server supports RGB (1.16+)
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            String[] packageParts = packageName.split("\\.");
            if (packageParts.length >= 4) {
                String version = packageParts[3];
                String[] versionParts = version.split("_");
                if (versionParts.length >= 2) {
                    int majorVersion = Integer.parseInt(versionParts[1]);
                    supportsRGB = majorVersion >= 16;

                    if (supportsRGB) {
                        // Try to get the ChatColor.of method for RGB support
                        Class<?> chatColorClass = Class.forName("org.bukkit.ChatColor");
                        fromHexMethod = chatColorClass.getMethod("of", String.class);
                    }
                }
            }
        } catch (Exception e) {
            supportsRGB = false;
        }
    }

    public static String translate(String message) {
        if (message == null) return null;

        if (supportsRGB && fromHexMethod != null) {
            // Use RGB support if available
            return translateHexColors(message);
        } else {
            // Fallback to standard color codes
            return ChatColor.translateAlternateColorCodes('&', message);
        }
    }

    private static String translateHexColors(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            try {
                // Use reflection to call ChatColor.of for RGB support
                Object color = fromHexMethod.invoke(null, "#" + hex);
                matcher.appendReplacement(result, color.toString());
            } catch (Exception e) {
                // Fallback to nearest color if RGB fails
                String fallback = getNearestColor(hex);
                matcher.appendReplacement(result, fallback);
            }
        }

        matcher.appendTail(result);
        return ChatColor.translateAlternateColorCodes('&', result.toString());
    }

    private static String getNearestColor(String hex) {
        // Map hex colors to closest Minecraft color codes
        switch (hex.toUpperCase()) {
            case "9D1BFF":
            case "9706FF":
            case "A430FF":
                return "&5"; // Dark Purple
            case "AA45FF":
            case "B15AFF":
            case "B76FFF":
            case "BE84FF":
            case "C499FF":
                return "&d"; // Light Purple
            default:
                return "&f"; // Default to white
        }
    }

    public static boolean supportsRGB() {
        return supportsRGB;
    }
}