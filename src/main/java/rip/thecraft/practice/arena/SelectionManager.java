package rip.thecraft.practice.arena;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import rip.thecraft.practice.Practice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionManager implements Listener {
    
    private static SelectionManager instance;
    
    public static SelectionManager getInstance() {
        return instance;
    }
    
    // Private constructor to enforce singleton
    public SelectionManager() {
        // Initialize maps
        pos1Selections = new HashMap<>();
        pos2Selections = new HashMap<>();
    }
    
    // Initialize the singleton when the plugin loads
    public static void initialize() {
        if (instance == null) {
            instance = new SelectionManager();
        }
    }
    


    private final Map<UUID, Location> pos1Selections;
    private final Map<UUID, Location> pos2Selections;

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is holding selection tool
        if (!isSelectionTool(player)) return;
        
        // Check if it's a right-click or left-click
        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        // Check if it's the main hand
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        event.setCancelled(true);
        
        Location clickedLocation = event.getClickedBlock().getLocation();
        
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Set position 1
            pos1Selections.put(player.getUniqueId(), clickedLocation);
            player.sendMessage("§aPosition 1 set to: " + formatLocation(clickedLocation));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Set position 2
            pos2Selections.put(player.getUniqueId(), clickedLocation);
            player.sendMessage("§aPosition 2 set to: " + formatLocation(clickedLocation));
        }
        
        // Check if both positions are set
        if (hasBothPositions(player)) {
            player.sendMessage("§6Both positions set! Use /arena setbounds <name> to apply the selection.");
        }
    }

    public boolean hasBothPositions(Player player) {
        return pos1Selections.containsKey(player.getUniqueId()) && pos2Selections.containsKey(player.getUniqueId());
    }

    public Location getPos1(Player player) {
        return pos1Selections.get(player.getUniqueId());
    }

    public Location getPos2(Player player) {
        return pos2Selections.get(player.getUniqueId());
    }

    public void clearSelection(Player player) {
        pos1Selections.remove(player.getUniqueId());
        pos2Selections.remove(player.getUniqueId());
    }

    private boolean isSelectionTool(Player player) {
        // Check if player has permission and is holding a stick (selection tool)
        return player.hasPermission("practice.admin") && 
               player.getInventory().getItemInMainHand().getType() == org.bukkit.Material.STICK;
    }
    
    public void giveSelectionTool(Player player) {
        // Give player a stick as selection tool
        org.bukkit.inventory.ItemStack selectionTool = new org.bukkit.inventory.ItemStack(org.bukkit.Material.STICK);
        org.bukkit.inventory.meta.ItemMeta meta = selectionTool.getItemMeta();
        meta.setDisplayName("§6Arena Selection Tool");
        meta.setLore(java.util.Arrays.asList(
            "§7Left-click: Set position 1",
            "§7Right-click: Set position 2",
            "§7Then use /arena setbounds <name>"
        ));
        selectionTool.setItemMeta(meta);
        player.getInventory().addItem(selectionTool);
        player.sendMessage("§aSelection tool added to your inventory!");
    }

    private String formatLocation(Location location) {
        return String.format("X: %d, Y: %d, Z: %d", 
            location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
