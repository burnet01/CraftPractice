package rip.thecraft.practice.util;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Elb1to
 * Project: SoupPvP
 * Date: 5/6/2021 @ 1:24 PM
 * Optimized for 500+ players
 */
public class ColorHelper {

    public final static String MENU_BAR = ChatColor.GRAY.toString() + ChatColor.STRIKETHROUGH.toString() + "------------------------";
    public final static String CHAT_BAR = ChatColor.GRAY.toString() + ChatColor.STRIKETHROUGH.toString() + "-----------------------------------------------------";

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final ConcurrentHashMap<String, String> TRANSLATION_CACHE = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 2000;
    
    // Pre-compiled common hex color mappings
    private static final ConcurrentHashMap<String, String> HEX_COLOR_MAP = new ConcurrentHashMap<>();
    
    static {
        // Pre-populate common hex color mappings
        HEX_COLOR_MAP.put("9D1BFF", "&5");
        HEX_COLOR_MAP.put("9706FF", "&5");
        HEX_COLOR_MAP.put("A430FF", "&5");
        HEX_COLOR_MAP.put("AA45FF", "&d");
        HEX_COLOR_MAP.put("B15AFF", "&d");
        HEX_COLOR_MAP.put("B76FFF", "&d");
        HEX_COLOR_MAP.put("BE84FF", "&d");
        HEX_COLOR_MAP.put("C499FF", "&d");
    }

	public static String translate(String message) {
        if (message == null) return null;
        
        // Check cache first
        String cached = TRANSLATION_CACHE.get(message);
        if (cached != null) {
            return cached;
        }

        // Convert RGB hex format to Minecraft color codes
        String translated = convertHexColors(message);

        // Convert standard color codes
        translated = ChatColor.translateAlternateColorCodes('&', translated);
        
        // Cache the result
        cacheTranslation(message, translated);
        
        return translated;
	}

    private static String convertHexColors(String message) {
        // Early exit if no hex patterns
        if (!message.contains("&#")) {
            return message;
        }
        
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            // Check pre-compiled map first, then fallback
            String minecraftColor = HEX_COLOR_MAP.getOrDefault(hex.toUpperCase(), "&f");
            matcher.appendReplacement(result, minecraftColor);
		}

        matcher.appendTail(result);
        return result.toString();
	}

    private static String getNearestMinecraftColor(String hex) {
        // Map RGB colors to closest Minecraft 1.12.1 color codes
        switch (hex.toUpperCase()) {
            case "9D1BFF": return "&5"; // Dark Purple
            case "9706FF": return "&5"; // Dark Purple
            case "A430FF": return "&5"; // Dark Purple
            case "AA45FF": return "&d"; // Light Purple
            case "B15AFF": return "&d"; // Light Purple
            case "B76FFF": return "&d"; // Light Purple
            case "BE84FF": return "&d"; // Light Purple
            case "C499FF": return "&d"; // Light Purple
            default: return "&f"; // Default to white if no match
        }
    }

    public static List<String> translate(List<String> lines) {
		List<String> strings = new ArrayList<>();
		for (String line : lines) {
            strings.add(translate(line));
			}

		return strings;
	}

    public static List<String> translate(String[] lines) {
        List<String> strings = new ArrayList<>();
        for (String line : lines) {
            if (line != null) {
                strings.add(translate(line));
}
        }

        return strings;
    }
    
    // Cache management
    private static void cacheTranslation(String original, String translated) {
        if (TRANSLATION_CACHE.size() >= MAX_CACHE_SIZE) {
            // Remove some entries to make space (simple FIFO)
            synchronized (TRANSLATION_CACHE) {
                if (TRANSLATION_CACHE.size() >= MAX_CACHE_SIZE) {
                    // Remove oldest 100 entries
                    TRANSLATION_CACHE.keySet().stream()
                        .limit(100)
                        .forEach(TRANSLATION_CACHE::remove);
                }
            }
        }
        TRANSLATION_CACHE.put(original, translated);
    }
    
    // Clear cache (useful for debugging or memory management)
    public static void clearCache() {
        TRANSLATION_CACHE.clear();
    }
}
