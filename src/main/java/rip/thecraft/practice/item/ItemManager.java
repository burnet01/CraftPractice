package rip.thecraft.practice.item;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import rip.thecraft.practice.Practice;
import rip.thecraft.practice.player.PlayerData;
import rip.thecraft.practice.player.PlayerState;
import rip.thecraft.practice.queue.QueueType;
import rip.thecraft.practice.util.VersionUtils;

import java.util.Arrays;

/**
 * Manages spawn items for the Practice plugin
 * Provides optimized item handling for better usability
 */
public class ItemManager implements Listener {

    private final Practice plugin;
    
    public ItemManager(Practice plugin) {
        this.plugin = plugin;
    }

    /**
     * Gives spawn items to a player based on their current state
     */
    public void giveSpawnItems(Player player) {
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
        if (playerData == null) return;

        // Use PaperAPI for Folia compatibility - run on entity scheduler
        rip.thecraft.practice.util.PaperAPI.runForEntity(plugin, player, () -> {
            // Clear inventory first
            player.getInventory().clear();

            switch (playerData.getState()) {
                case LOBBY:
                    giveLobbyItems(player);
                    break;
                case QUEUE:
                    giveQueueItems(player);
                    break;
                case MATCH:
                    // No items for matches - kits handle this
                    break;
                case SPECTATING:
                    giveSpectatorItems(player);
                    break;
            }
        });
    }

    /**
     * Gives lobby items to a player
     */
    private void giveLobbyItems(Player player) {
        // Stone sword for practice menu (slot 0)
        ItemStack practiceSword = createItem(
            Material.STONE_SWORD,
            ChatColor.GREEN + "Practice Menu",
            ChatColor.GRAY + "Right-click to open practice menu",
            ChatColor.GRAY + "Select unranked or ranked queues"
        );
        player.getInventory().setItem(0, practiceSword);

        // Book for settings menu (slot 8)
        ItemStack settingsBook = createItem(
            Material.BOOK,
            ChatColor.BLUE + "Settings",
            ChatColor.GRAY + "Right-click to open settings",
            ChatColor.GRAY + "Configure your practice preferences"
        );
        player.getInventory().setItem(8, settingsBook);
    }

    /**
     * Gives queue items to a player
     */
    private void giveQueueItems(Player player) {
        // Red dye for leaving queue (slot 4 - middle of hotbar)
        ItemStack leaveQueueItem = createItem(
            VersionUtils.getMaterial("RED_DYE"),
            ChatColor.RED + "Leave Queue",
            ChatColor.GRAY + "Right-click to leave the queue",
            ChatColor.GRAY + "Use /queue leave"
        );
        player.getInventory().setItem(4, leaveQueueItem);

        // Optional: Add queue info item or other queue-related items
    }

    /**
     * Gives spectator items to a player
     */
    private void giveSpectatorItems(Player player) {
        // Red dye for leaving spectator mode
        ItemStack leaveSpectatorItem = createItem(
            VersionUtils.getMaterial("RED_DYE"),
            ChatColor.RED + "Leave Spectator",
            ChatColor.GRAY + "Right-click to stop spectating",
            ChatColor.GRAY + "Use /spec leave"
        );
        player.getInventory().setItem(4, leaveSpectatorItem);
    }

    /**
     * Creates a custom item with name and lore
     */
    public ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * Handles right-click interactions with custom items
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
        if (playerData == null) return;

        // Check for practice menu sword
        if (item.getType() == Material.STONE_SWORD && playerData.getState() == PlayerState.LOBBY) {
            event.setCancelled(true);
            openPracticeMenu(player);
            return;
        }

        // Check for leave queue item
        if (item.getType() == VersionUtils.getMaterial("RED_DYE")) {
            event.setCancelled(true);
            
            switch (playerData.getState()) {
                case QUEUE:
                    // Execute the actual queue leave command
                    player.performCommand("queue leave");
                    break;
                case SPECTATING:
                    // Execute the actual spectator leave command
                    player.performCommand("spec");
                    break;
                default:
                    player.sendMessage(ChatColor.RED + "This item is not available in your current state.");
                    break;
            }
        }

        // Check for settings book
        if (item.getType() == Material.BOOK && playerData.getState() == PlayerState.LOBBY) {
            event.setCancelled(true);
            openSettingsMenu(player);
        }
    }

    /**
     * Opens the practice menu for a player
     */
    private void openPracticeMenu(Player player) {
        // Execute the practice command to open the actual GUI
        player.performCommand("practice");
    }

    /**
     * Opens the settings menu for a player
     */
    private void openSettingsMenu(Player player) {
        // Execute the settings command to open the actual GUI
        player.performCommand("settings");
    }

    /**
     * Updates a player's items based on their current state
     * Call this when player state changes
     */
    public void updatePlayerItems(Player player) {
        giveSpawnItems(player);
    }

    /**
     * Clears all custom items from a player
     */
    public void clearPlayerItems(Player player) {
        // Use PaperAPI for Folia compatibility - run on entity scheduler
        rip.thecraft.practice.util.PaperAPI.runForEntity(plugin, player, () -> {
            player.getInventory().clear();
        });
    }

    /**
     * Checks if an item is a custom practice item
     */
    public boolean isPracticeItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        
        String displayName = meta.getDisplayName();
        return displayName.contains("Practice Menu") || 
               displayName.contains("Leave Queue") || 
               displayName.contains("Leave Spectator");
    }
}
