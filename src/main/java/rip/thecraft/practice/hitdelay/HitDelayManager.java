package rip.thecraft.practice.hitdelay;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import rip.thecraft.practice.Practice;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HitDelayManager {

    private final Map<String, HitDelayProfile> profiles = new HashMap<>();
    private final Practice plugin;

    public HitDelayManager(Practice plugin) {
        this.plugin = plugin;
        loadProfiles();
    }

    public void loadProfiles() {
        profiles.clear();
        
        File hitDelayFile = new File(plugin.getDataFolder(), "hitdelay.yml");
        if (!hitDelayFile.exists()) {
            createDefaultProfiles();
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(hitDelayFile);
        ConfigurationSection profilesSection = config.getConfigurationSection("profiles");
        if (profilesSection == null) return;

        for (String key : profilesSection.getKeys(false)) {
            ConfigurationSection profileSection = profilesSection.getConfigurationSection(key);
            if (profileSection != null) {
                Map<String, Object> data = profileSection.getValues(false);
                HitDelayProfile profile = HitDelayProfile.deserialize(data);
                profiles.put(profile.getName(), profile);
            }
        }

        plugin.getLogger().info("Loaded " + profiles.size() + " hit delay profiles");
    }

    public void saveProfiles() {
        File hitDelayFile = new File(plugin.getDataFolder(), "hitdelay.yml");
        FileConfiguration config = new YamlConfiguration();
        
        // Save all profiles
        for (HitDelayProfile profile : profiles.values()) {
            String path = "profiles." + profile.getName();
            config.set(path + ".name", profile.getName());
            config.set(path + ".hitDelay", profile.getHitDelay());
            config.set(path + ".enabled", profile.isEnabled());
        }
        
        try {
            config.save(hitDelayFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save hit delay profiles: " + e.getMessage());
        }
    }

    private void createDefaultProfiles() {
        // Create default profiles
        profiles.put("default", new HitDelayProfile("default", 500L)); // 1.7.10 PVP timing
        profiles.put("combo", new HitDelayProfile("combo", 0L)); // No delay for combo kits
        profiles.put("fast", new HitDelayProfile("fast", 250L)); // Faster hits
        profiles.put("slow", new HitDelayProfile("slow", 1000L)); // Slower hits
        
        saveProfiles();
        plugin.getLogger().info("Created default hit delay profiles");
    }

    public HitDelayProfile getProfile(String name) {
        return profiles.get(name);
    }

    public HitDelayProfile getDefaultProfile() {
        return profiles.getOrDefault("default", new HitDelayProfile("default"));
    }

    public void addProfile(HitDelayProfile profile) {
        profiles.put(profile.getName(), profile);
        saveProfiles();
    }

    public void removeProfile(String name) {
        profiles.remove(name);
        saveProfiles();
    }

    public Map<String, HitDelayProfile> getProfiles() {
        return new HashMap<>(profiles);
    }

    public boolean profileExists(String name) {
        return profiles.containsKey(name);
    }
}
