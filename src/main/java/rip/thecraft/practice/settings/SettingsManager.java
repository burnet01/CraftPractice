package rip.thecraft.practice.settings;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import rip.thecraft.practice.Practice;

import java.util.*;

/**
 * Manages player settings for the Practice plugin
 * Provides GUI-based settings management with in-memory storage
 */
public class SettingsManager implements Listener {

    private final Practice plugin;
    private final Map<UUID, PlayerSettings> playerSettings = new HashMap<>();
    
    public SettingsManager(Practice plugin) {
        this.plugin = plugin;
    }

    /**
     * Gets or creates settings for a player
     */
    public PlayerSettings getPlayerSettings(Player player) {
        return playerSettings.computeIfAbsent(player.getUniqueId(), k -> new PlayerSettings());
    }

    /**
     * Creates the settings GUI for a player
     */
    public Inventory createSettingsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "Practice Settings");
        PlayerSettings settings = getPlayerSettings(player);

        // World Time Cycle (Slot 11)
        ItemStack worldTimeItem = createWorldTimeItem(settings.getWorldTime());
        gui.setItem(11, worldTimeItem);

        // Scoreboard Toggle (Slot 13)
        ItemStack scoreboardItem = createToggleItem(
            Material.OAK_SIGN,
            "&bScoreboard",
            settings.isScoreboardEnabled(),
            "&7Toggle scoreboard display",
            "&7Saves resources when disabled",
            "&7Current: " + (settings.isScoreboardEnabled() ? "&aEnabled" : "&cDisabled")
        );
        gui.setItem(13, scoreboardItem);

        // Toggle Messages (Slot 15)
        ItemStack messagesItem = createToggleItem(
            Material.PAPER,
            "&eToggle Messages",
            settings.isMessagesEnabled(),
            "&7Toggle practice-related messages",
            "&7Includes queue, match, and kit messages",
            "&7Current: " + (settings.isMessagesEnabled() ? "&aEnabled" : "&cDisabled")
        );
        gui.setItem(15, messagesItem);

        // Close button (Slot 22)
        ItemStack closeItem = createItem(
            Material.BARRIER,
            "&cClose",
            "&7Click to close this menu"
        );
        gui.setItem(22, closeItem);

        return gui;
    }

    /**
     * Creates a toggle item with on/off state
     */
    private ItemStack createToggleItem(Material material, String name, boolean enabled, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String status = enabled ? "&aENABLED" : "&cDISABLED";
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name + " " + status));
            
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            loreList.add("");
            loreList.add(ChatColor.YELLOW + "Click to toggle!");
            
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * Creates a world time cycle item
     */
    private ItemStack createWorldTimeItem(WorldTimeOption currentTime) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6World Time"));
            
            List<String> loreList = new ArrayList<>();
            loreList.add(ChatColor.GRAY + "Cycle through world time options");
            loreList.add("");
            loreList.add(ChatColor.YELLOW + "Current: " + currentTime.getDisplayName());
            loreList.add("");
            loreList.add(ChatColor.GRAY + "Available options:");
            for (WorldTimeOption option : WorldTimeOption.values()) {
                String prefix = (option == currentTime) ? "&a→ " : "&7- ";
                loreList.add(ChatColor.translateAlternateColorCodes('&', prefix + option.getDisplayName()));
            }
            loreList.add("");
            loreList.add(ChatColor.YELLOW + "Click to cycle!");
            
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * Creates a simple item with name and lore
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    loreList.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(loreList);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * Handles settings GUI clicks
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if this is the settings GUI
        if (!event.getView().getTitle().equals("Practice Settings")) return;
        
        // Only handle clicks in the top inventory (not player inventory)
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;
        
        String displayName = clickedItem.getItemMeta().getDisplayName();
        if (displayName == null) return;
        
        PlayerSettings settings = getPlayerSettings(player);
        
        // Handle close button
        if (displayName.contains("Close")) {
            player.closeInventory();
            return;
        }
        
        // Handle world time cycle
        if (displayName.contains("World Time")) {
            WorldTimeOption nextTime = settings.getWorldTime().next();
            settings.setWorldTime(nextTime);
            applyWorldTime(player, nextTime);
            player.sendMessage(ChatColor.YELLOW + "World Time set to: " + ChatColor.GREEN + nextTime.getDisplayName());
        } else if (displayName.contains("Scoreboard")) {
            settings.setScoreboardEnabled(!settings.isScoreboardEnabled());
            applyScoreboard(player, settings.isScoreboardEnabled());
            player.sendMessage(ChatColor.YELLOW + "Scoreboard: " + 
                (settings.isScoreboardEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
        } else if (displayName.contains("Toggle Messages")) {
            settings.setMessagesEnabled(!settings.isMessagesEnabled());
            player.sendMessage(ChatColor.YELLOW + "Practice Messages: " + 
                (settings.isMessagesEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
        }
        
        // Update GUI - no need to save since it's in-memory
        player.openInventory(createSettingsGUI(player));
    }

    /**
     * Applies world time setting to player (per-player time)
     */
    private void applyWorldTime(Player player, WorldTimeOption timeOption) {
        player.setPlayerTime(timeOption.getTimeTicks(), false);
    }

    /**
     * Applies scoreboard setting to player
     */
    private void applyScoreboard(Player player, boolean enabled) {
        if (enabled) {
            // Show scoreboard
            Practice.getInstance().getScoreboardService().updatePlayerScoreboard(player);
        } else {
            // Hide scoreboard
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
    }

    /**
     * Handles player join to apply their settings
     */
    public void applyPlayerSettings(Player player) {
        PlayerSettings settings = getPlayerSettings(player);
        
        // Apply world time setting
        applyWorldTime(player, settings.getWorldTime());
        
        // Apply scoreboard setting
        applyScoreboard(player, settings.isScoreboardEnabled());
    }

    /**
     * Opens the settings GUI for a player
     */
    public void openSettingsGUI(Player player) {
        player.openInventory(createSettingsGUI(player));
    }

    /**
     * Checks if a player has messages enabled
     */
    public boolean hasMessagesEnabled(Player player) {
        return getPlayerSettings(player).isMessagesEnabled();
    }

    /**
     * Checks if a player has sounds enabled
     * Note: Sound effects have been removed
     */
    public boolean hasSoundsEnabled(Player player) {
        return false; // Sound effects are no longer supported
    }

    /**
     * Shuts down the settings manager
     */
    public void shutdown() {
        // Clear in-memory settings - no file I/O needed
        playerSettings.clear();
    }
}
