package rip.thecraft.practice.kit;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import rip.thecraft.practice.Practice;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KitManager {

    private final JavaPlugin plugin;
    private final Map<String, Kit> kits = new ConcurrentHashMap<>();
    private final File kitFile;
    private final FileConfiguration kitConfig;

    public KitManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.kitFile = new File(plugin.getDataFolder(), "kits.yml");
        this.kitConfig = YamlConfiguration.loadConfiguration(kitFile);
        loadKits();
    }

    public void loadKits() {
        kits.clear();
        if (!kitFile.exists()) {
            saveKits();
            createDefaultKits();
            return;
        }

        for (String name : kitConfig.getKeys(false)) {
            Kit kit = Kit.deserialize(kitConfig.getConfigurationSection(name));
            if (kit != null) {
                kits.put(name.toLowerCase(), kit);
            }
        }
        plugin.getLogger().info("Loaded " + kits.size() + " kits");
    }

    private void createDefaultKits() {
        // Create some default kits
        Kit noDebuff = createDefaultNoDebuffKit();
        Kit buildUHC = createDefaultBuildUHCKit();
        Kit sumo = createDefaultSumoKit();

        kits.put("nodebuff", noDebuff);
        kits.put("builduhc", buildUHC);
        kits.put("sumo", sumo);

        saveKits();
    }

    private Kit createDefaultNoDebuffKit() {
        Kit kit = new Kit("NoDebuff");
        kit.setDisplayName("&bNo Debuff");
        kit.setDescription(Arrays.asList("&7Classic No Debuff kit", "&7with speed and strength"));
        
        // Setup inventory
        ItemStack[] contents = new ItemStack[36];
        contents[0] = new ItemStack(Material.DIAMOND_SWORD);
        contents[1] = new ItemStack(Material.ENDER_PEARL, 16);
        
        // Armor
        kit.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        kit.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        kit.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        kit.setBoots(new ItemStack(Material.DIAMOND_BOOTS));
        
        // Hotbar
        for (int i = 2; i < 9; i++) {
            contents[i] = new ItemStack(Material.POTION, 1, (short) 16421); // Speed II
        }
        
        // Inventory
        for (int i = 9; i < 36; i++) {
            contents[i] = new ItemStack(Material.POTION, 1, (short) 16421); // Speed II
        }
        
        kit.setContents(contents);
        return kit;
    }

    private Kit createDefaultBuildUHCKit() {
        Kit kit = new Kit("BuildUHC");
        kit.setDisplayName("&6Build UHC");
        kit.setDescription(Arrays.asList("&7Build UHC with blocks", "&7and healing items"));
        
        ItemStack[] contents = new ItemStack[36];
        contents[0] = new ItemStack(Material.DIAMOND_SWORD);
        contents[1] = new ItemStack(Material.DIAMOND_AXE);
        contents[2] = new ItemStack(Material.GOLDEN_APPLE, 8);
        contents[3] = new ItemStack(Material.COBBLESTONE, 64);
        contents[4] = new ItemStack(Material.WATER_BUCKET);
        contents[5] = new ItemStack(Material.LAVA_BUCKET);
        
        kit.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        kit.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        kit.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        kit.setBoots(new ItemStack(Material.DIAMOND_BOOTS));
        
        kit.setContents(contents);
        return kit;
    }

    private Kit createDefaultSumoKit() {
        Kit kit = new Kit("Sumo");
        kit.setDisplayName("&cSumo");
        kit.setDescription(Arrays.asList("&7Sumo - no items, just fists!"));
        
        // Sumo has no items
        kit.setContents(new ItemStack[36]);
        return kit;
    }

    public void saveKits() {
        try {
            for (Map.Entry<String, Kit> entry : kits.entrySet()) {
                kitConfig.set(entry.getKey(), entry.getValue().serialize());
            }
            kitConfig.save(kitFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save kits: " + e.getMessage());
        }
    }

    public boolean createKit(String name) {
        if (kits.containsKey(name.toLowerCase())) {
            return false;
        }

        Kit kit = new Kit(name);
        kits.put(name.toLowerCase(), kit);
        saveKits();
        return true;
    }

    public boolean createKitFromInventory(String name, Player player) {
        if (kits.containsKey(name.toLowerCase())) {
            return false;
        }

        Kit kit = new Kit(name);
        
        // Copy inventory contents
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            contents[i] = inventory.getItem(i) != null ? inventory.getItem(i).clone() : null;
        }
        kit.setContents(contents);
        
        // Copy armor
        kit.setHelmet(inventory.getHelmet() != null ? inventory.getHelmet().clone() : null);
        kit.setChestplate(inventory.getChestplate() != null ? inventory.getChestplate().clone() : null);
        kit.setLeggings(inventory.getLeggings() != null ? inventory.getLeggings().clone() : null);
        kit.setBoots(inventory.getBoots() != null ? inventory.getBoots().clone() : null);
        
        kits.put(name.toLowerCase(), kit);
        saveKits();
        return true;
    }

    public boolean updateKitInventory(String name, Player player) {
        Kit kit = getKit(name);
        if (kit == null) {
            return false;
        }

        // Copy inventory contents
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            contents[i] = inventory.getItem(i) != null ? inventory.getItem(i).clone() : null;
        }
        kit.setContents(contents);
        
        // Copy armor
        kit.setHelmet(inventory.getHelmet() != null ? inventory.getHelmet().clone() : null);
        kit.setChestplate(inventory.getChestplate() != null ? inventory.getChestplate().clone() : null);
        kit.setLeggings(inventory.getLeggings() != null ? inventory.getLeggings().clone() : null);
        kit.setBoots(inventory.getBoots() != null ? inventory.getBoots().clone() : null);
        
        saveKits();
        return true;
    }

    public boolean setKitIcon(String name, ItemStack icon) {
        Kit kit = getKit(name);
        if (kit == null) {
            return false;
        }

        kit.setIcon(icon != null ? icon.clone() : null);
        saveKits();
        return true;
    }

    public boolean setKitDisplayName(String name, String displayName) {
        Kit kit = getKit(name);
        if (kit == null) {
            return false;
        }

        kit.setDisplayName(displayName);
        saveKits();
        return true;
    }

    public boolean setKitEnabled(String name, boolean enabled) {
        Kit kit = getKit(name);
        if (kit == null) {
            return false;
        }

        kit.setEnabled(enabled);
        saveKits();
        return true;
    }

    public boolean toggleSumoMode(String name) {
        Kit kit = getKit(name);
        if (kit == null) {
            return false;
        }

        kit.setSumoMode(!kit.isSumoMode());
        saveKits();
        return true;
    }

    public boolean toggleBoxingMode(String name) {
        Kit kit = getKit(name);
        if (kit == null) {
            return false;
        }

        kit.setBoxingMode(!kit.isBoxingMode());
        saveKits();
        return true;
    }

    public boolean toggleBuildMode(String name) {
        Kit kit = getKit(name);
        if (kit == null) {
            return false;
        }

        kit.setBuildMode(!kit.isBuildMode());
        saveKits();
        return true;
    }

    public boolean deleteKit(String name) {
        if (kits.remove(name.toLowerCase()) != null) {
            kitConfig.set(name.toLowerCase(), null);
            saveKits();
            return true;
        }
        return false;
    }

    public Kit getKit(String name) {
        return kits.get(name.toLowerCase());
    }

    public Set<String> getKitNames() {
        return kits.keySet();
    }

    public void applyKit(Player player, Kit kit) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();

        // Apply armor
        if (kit.getHelmet() != null) inventory.setHelmet(kit.getHelmet());
        if (kit.getChestplate() != null) inventory.setChestplate(kit.getChestplate());
        if (kit.getLeggings() != null) inventory.setLeggings(kit.getLeggings());
        if (kit.getBoots() != null) inventory.setBoots(kit.getBoots());

        // Apply inventory
        ItemStack[] contents = kit.getContents();
        if (contents != null) {
            for (int i = 0; i < contents.length && i < 36; i++) {
                if (contents[i] != null) {
                    inventory.setItem(i, contents[i]);
                }
            }
        }

        // Apply speed effect for boxing kits
        if (kit.isBoxingMode()) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SPEED, 
                Integer.MAX_VALUE, // Infinite duration
                1 // Speed II (level 2)
            ));
        }

        player.updateInventory();
    }

    public Inventory createKitSelectionGUI() {
        Inventory gui = Bukkit.createInventory(null, 54, "Kit Selection");
        
        int slot = 0;
        for (Kit kit : kits.values()) {
            if (slot >= 54) break;
            if (kit.isEnabled()) {
                gui.setItem(slot, kit.createDisplayItem());
                slot++;
            }
        }
        
        // Debug: Log available kits
        // Debug: Available kits in GUI - commented out to reduce console spam
        // plugin.getLogger().info("Available kits in GUI: " + kits.keySet());
        
        return gui;
    }

    public void shutdown() {
        saveKits();
    }
}
