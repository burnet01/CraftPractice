package rip.thecraft.practice.kit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class Kit implements ConfigurationSerializable {

    private final String name;
    private String displayName;
    private List<String> description;
    private ItemStack[] contents;
    private ItemStack helmet;
    private ItemStack chestplate;
    private ItemStack leggings;
    private ItemStack boots;
    private ItemStack icon;
    private boolean enabled;
    private boolean sumoMode;
    private boolean boxingMode;
    private boolean buildMode; // If true, this kit allows building and needs terrain regeneration
    private String knockbackProfile; // Name of the knockback profile to use
    private String hitDelayProfile; // Name of the hit delay profile to use

    public Kit(String name) {
        this.name = name;
        this.displayName = name;
        this.description = new ArrayList<>();
        this.contents = new ItemStack[36];
        this.icon = new ItemStack(Material.DIAMOND_SWORD);
        this.enabled = true;
        this.sumoMode = false;
        this.boxingMode = false;
        this.knockbackProfile = "default"; // Default knockback profile
        this.hitDelayProfile = "default"; // Default hit delay profile
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    public ItemStack[] getContents() {
        return contents;
    }

    public void setContents(ItemStack[] contents) {
        this.contents = contents;
    }

    public ItemStack getHelmet() {
        return helmet;
    }

    public void setHelmet(ItemStack helmet) {
        this.helmet = helmet;
    }

    public ItemStack getChestplate() {
        return chestplate;
    }

    public void setChestplate(ItemStack chestplate) {
        this.chestplate = chestplate;
    }

    public ItemStack getLeggings() {
        return leggings;
    }

    public void setLeggings(ItemStack leggings) {
        this.leggings = leggings;
    }

    public ItemStack getBoots() {
        return boots;
    }

    public void setBoots(ItemStack boots) {
        this.boots = boots;
    }

    public ItemStack getIcon() {
        return icon;
    }

    public void setIcon(ItemStack icon) {
        this.icon = icon;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSumoMode() {
        return sumoMode;
    }

    public void setSumoMode(boolean sumoMode) {
        this.sumoMode = sumoMode;
    }

    public boolean isBoxingMode() {
        return boxingMode;
    }

    public void setBoxingMode(boolean boxingMode) {
        this.boxingMode = boxingMode;
    }

    public boolean isBuildMode() {
        return buildMode;
    }

    public void setBuildMode(boolean buildMode) {
        this.buildMode = buildMode;
    }

    public String getKnockbackProfile() {
        return knockbackProfile;
    }

    public void setKnockbackProfile(String knockbackProfile) {
        this.knockbackProfile = knockbackProfile;
    }

    public String getHitDelayProfile() {
        return hitDelayProfile;
    }

    public void setHitDelayProfile(String hitDelayProfile) {
        this.hitDelayProfile = hitDelayProfile;
    }

    public ItemStack createDisplayItem() {
        ItemStack displayItem = icon != null ? icon.clone() : new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = displayItem.getItemMeta();
        
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(displayItem.getType());
        }
        
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
        
        List<String> lore = new ArrayList<>();
        for (String line : description) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        
        // Add special mode indicators
        if (sumoMode) {
            lore.add(ChatColor.RED + "Sumo Mode: Instant death on water/lava/ice");
        }
        if (boxingMode) {
            lore.add(ChatColor.BLUE + "Boxing Mode: First to 5 hits wins");
        }
        if (buildMode) {
            lore.add(ChatColor.GREEN + "Build Mode: Allows block placement/breaking");
        }
        
        lore.add("");
        if (!enabled) {
            lore.add(ChatColor.RED + "DISABLED");
        } else {
            lore.add(ChatColor.GRAY + "Click to select this kit!");
        }
        
        meta.setLore(lore);
        displayItem.setItemMeta(meta);
        
        return displayItem;
    }

    public void sendKitInfo(org.bukkit.entity.Player player) {
        // Send kit information to player
        player.sendMessage(ChatColor.GREEN + "Kit: " + ChatColor.WHITE + displayName);
        if (!description.isEmpty()) {
            for (String line : description) {
                player.sendMessage(ChatColor.GRAY + line);
            }
        }
        
        if (sumoMode) {
            player.sendMessage(ChatColor.RED + "Sumo Mode: Instant death on water/lava/ice");
        }
        if (boxingMode) {
            player.sendMessage(ChatColor.BLUE + "Boxing Mode: First to 5 hits wins");
        }
        if (buildMode) {
            player.sendMessage(ChatColor.GREEN + "Build Mode: Allows block placement/breaking");
        }
    }

    public ItemStack[] getInventoryContents() {
        return contents;
    }

    public ItemStack[] getArmorContents() {
        return new ItemStack[] { boots, leggings, chestplate, helmet };
    }

    public boolean isCombo() {
        // Check if this is a combo kit (based on knockback profile or other criteria)
        return knockbackProfile != null && knockbackProfile.toLowerCase().contains("combo");
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("displayName", displayName);
        data.put("description", description);
        data.put("enabled", enabled);
        data.put("sumoMode", sumoMode);
        data.put("boxingMode", boxingMode);
        data.put("buildMode", buildMode);
        data.put("knockbackProfile", knockbackProfile);
        data.put("hitDelayProfile", hitDelayProfile);

        // Serialize icon
        if (icon != null) {
            data.put("icon", icon.serialize());
        }

        // Serialize inventory contents
        List<Map<String, Object>> contentsData = new ArrayList<>();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("slot", i);
                itemData.put("item", contents[i].serialize());
                contentsData.add(itemData);
            }
        }
        data.put("contents", contentsData);

        // Serialize armor
        if (helmet != null) data.put("helmet", helmet.serialize());
        if (chestplate != null) data.put("chestplate", chestplate.serialize());
        if (leggings != null) data.put("leggings", leggings.serialize());
        if (boots != null) data.put("boots", boots.serialize());

        return data;
    }

    public static Kit deserialize(ConfigurationSection section) {
        if (section == null) return null;

        String name = section.getString("name");
        Kit kit = new Kit(name);
        kit.setDisplayName(section.getString("displayName", name));
        kit.setDescription(section.getStringList("description"));
        kit.setEnabled(section.getBoolean("enabled", true));
        kit.setSumoMode(section.getBoolean("sumoMode", false));
        kit.setBoxingMode(section.getBoolean("boxingMode", false));
        kit.setBuildMode(section.getBoolean("buildMode", false));
        kit.setKnockbackProfile(section.getString("knockbackProfile", "default"));
        kit.setHitDelayProfile(section.getString("hitDelayProfile", "default"));

        // Deserialize icon
        if (section.contains("icon")) {
            Map<String, Object> iconMap = section.getConfigurationSection("icon").getValues(false);
            kit.icon = ItemStack.deserialize(iconMap);
        }

        // Deserialize inventory contents
        if (section.contains("contents")) {
            List<Map<?, ?>> contentsData = section.getMapList("contents");
            for (Map<?, ?> itemData : contentsData) {
                int slot = (Integer) itemData.get("slot");
                Map<String, Object> itemMap = (Map<String, Object>) itemData.get("item");
                ItemStack item = ItemStack.deserialize(itemMap);
                kit.contents[slot] = item;
            }
        }

        // Deserialize armor
        if (section.contains("helmet")) {
            kit.helmet = ItemStack.deserialize(section.getConfigurationSection("helmet").getValues(false));
        }
        if (section.contains("chestplate")) {
            kit.chestplate = ItemStack.deserialize(section.getConfigurationSection("chestplate").getValues(false));
        }
        if (section.contains("leggings")) {
            kit.leggings = ItemStack.deserialize(section.getConfigurationSection("leggings").getValues(false));
        }
        if (section.contains("boots")) {
            kit.boots = ItemStack.deserialize(section.getConfigurationSection("boots").getValues(false));
        }

        return kit;
    }
}
