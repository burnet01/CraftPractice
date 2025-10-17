package rip.thecraft.practice.knockback;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KnockbackManager {

    private final JavaPlugin plugin;
    private final Map<String, KnockbackProfile> profiles = new ConcurrentHashMap<>();
    private final File knockbackFile;
    private final FileConfiguration knockbackConfig;

    public KnockbackManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.knockbackFile = new File(plugin.getDataFolder(), "knockback.yml");
        this.knockbackConfig = YamlConfiguration.loadConfiguration(knockbackFile);
        loadProfiles();
    }

    public void loadProfiles() {
        profiles.clear();
        if (!knockbackFile.exists()) {
            saveProfiles();
            createDefaultProfiles();
            return;
        }

        ConfigurationSection profilesSection = knockbackConfig.getConfigurationSection("profiles");
        if (profilesSection != null) {
            for (String name : profilesSection.getKeys(false)) {
                ConfigurationSection profileSection = profilesSection.getConfigurationSection(name);
                if (profileSection != null) {
                    KnockbackProfile profile = KnockbackProfile.deserialize(profileSection.getValues(false));
                    if (profile != null) {
                        profiles.put(name.toLowerCase(), profile);
                    }
                }
            }
        }
        plugin.getLogger().info("Loaded " + profiles.size() + " knockback profiles");
    }

    private void createDefaultProfiles() {
        // Create default knockback profiles
        KnockbackProfile defaultProfile = new KnockbackProfile("default");
        defaultProfile.setHorizontal(0.4D);
        defaultProfile.setVertical(0.4D);
        defaultProfile.setVerticalLimit(0.4D);
        defaultProfile.setExtraHorizontal(0.5D);
        defaultProfile.setExtraVertical(0.1D);
        defaultProfile.setNetheriteKnockbackResistance(false);
        defaultProfile.setVerticalFrictionReduction(false);
        defaultProfile.setHorizontalFriction(2.0D);
        defaultProfile.setVerticalFriction(2.0D);

        KnockbackProfile lightProfile = new KnockbackProfile("light");
        lightProfile.setHorizontal(0.35D);
        lightProfile.setVertical(0.35D);
        lightProfile.setVerticalLimit(0.4D);
        lightProfile.setExtraHorizontal(0.5D);
        lightProfile.setExtraVertical(0.1D);
        lightProfile.setVerticalFrictionReduction(false);
        lightProfile.setHorizontalFriction(2.0D);
        lightProfile.setVerticalFriction(2.0D);

        KnockbackProfile heavyProfile = new KnockbackProfile("heavy");
        heavyProfile.setHorizontal(0.6D);
        heavyProfile.setVertical(0.6D);
        heavyProfile.setVerticalLimit(0.5D);
        heavyProfile.setExtraHorizontal(0.7D);
        heavyProfile.setExtraVertical(0.15D);
        heavyProfile.setVerticalFrictionReduction(false);
        heavyProfile.setHorizontalFriction(2.0D);
        heavyProfile.setVerticalFriction(2.0D);

        // Smooth knockback profile for modern PvP
        KnockbackProfile smoothProfile = new KnockbackProfile("smooth");
        smoothProfile.setHorizontal(0.35D);
        smoothProfile.setVertical(0.35D);
        smoothProfile.setVerticalLimit(0.4D);
        smoothProfile.setExtraHorizontal(0.4D);
        smoothProfile.setExtraVertical(0.08D);
        smoothProfile.setVerticalFrictionReduction(true);
        smoothProfile.setHorizontalFriction(1.2D);
        smoothProfile.setVerticalFriction(1.0D);
        smoothProfile.setDisableSprintToggle(true);
        smoothProfile.setSprintResetTicks(3);

        profiles.put("default", defaultProfile);
        profiles.put("light", lightProfile);
        profiles.put("heavy", heavyProfile);
        profiles.put("smooth", smoothProfile);

        saveProfiles();
    }

    public void saveProfiles() {
        try {
            ConfigurationSection profilesSection = knockbackConfig.createSection("profiles");
            for (Map.Entry<String, KnockbackProfile> entry : profiles.entrySet()) {
                profilesSection.set(entry.getKey(), entry.getValue().serialize());
            }
            knockbackConfig.save(knockbackFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save knockback profiles: " + e.getMessage());
        }
    }

    public boolean createProfile(String name, double horizontal, double vertical, double verticalLimit, 
                                double extraHorizontal, double extraVertical, boolean netheriteKnockbackResistance) {
        return createProfile(name, horizontal, vertical, verticalLimit, extraHorizontal, extraVertical, 
                           netheriteKnockbackResistance, false, 2.0D, 2.0D);
    }

    public boolean createProfile(String name, double horizontal, double vertical, double verticalLimit, 
                                double extraHorizontal, double extraVertical, boolean netheriteKnockbackResistance,
                                boolean verticalFrictionReduction, double horizontalFriction, double verticalFriction) {
        if (profiles.containsKey(name.toLowerCase())) {
            return false;
        }

        KnockbackProfile profile = new KnockbackProfile(name, horizontal, vertical, verticalLimit, 
                                                       extraHorizontal, extraVertical, netheriteKnockbackResistance);
        profile.setVerticalFrictionReduction(verticalFrictionReduction);
        profile.setHorizontalFriction(horizontalFriction);
        profile.setVerticalFriction(verticalFriction);
        profiles.put(name.toLowerCase(), profile);
        saveProfiles();
        return true;
    }

    public boolean deleteProfile(String name) {
        if (profiles.remove(name.toLowerCase()) != null) {
            saveProfiles();
            return true;
        }
        return false;
    }

    public KnockbackProfile getProfile(String name) {
        return profiles.get(name.toLowerCase());
    }

    public KnockbackProfile getDefaultProfile() {
        return profiles.getOrDefault("default", new KnockbackProfile("default"));
    }

    public Map<String, KnockbackProfile> getProfiles() {
        return new HashMap<>(profiles);
    }

    public boolean updateProfile(String name, double horizontal, double vertical, double verticalLimit, 
                                double extraHorizontal, double extraVertical, boolean netheriteKnockbackResistance) {
        return updateProfile(name, horizontal, vertical, verticalLimit, extraHorizontal, extraVertical, 
                           netheriteKnockbackResistance, false, 2.0D, 2.0D);
    }

    public boolean updateProfile(String name, double horizontal, double vertical, double verticalLimit, 
                                double extraHorizontal, double extraVertical, boolean netheriteKnockbackResistance,
                                boolean verticalFrictionReduction, double horizontalFriction, double verticalFriction) {
        KnockbackProfile profile = getProfile(name);
        if (profile == null) {
            return false;
        }

        profile.setHorizontal(horizontal);
        profile.setVertical(vertical);
        profile.setVerticalLimit(verticalLimit);
        profile.setExtraHorizontal(extraHorizontal);
        profile.setExtraVertical(extraVertical);
        profile.setNetheriteKnockbackResistance(netheriteKnockbackResistance);
        profile.setVerticalFrictionReduction(verticalFrictionReduction);
        profile.setHorizontalFriction(horizontalFriction);
        profile.setVerticalFriction(verticalFriction);
        
        saveProfiles();
        return true;
    }

    public boolean setProfileEnabled(String name, boolean enabled) {
        KnockbackProfile profile = getProfile(name);
        if (profile == null) {
            return false;
        }

        profile.setEnabled(enabled);
        saveProfiles();
        return true;
    }

    public void shutdown() {
        saveProfiles();
    }
}
