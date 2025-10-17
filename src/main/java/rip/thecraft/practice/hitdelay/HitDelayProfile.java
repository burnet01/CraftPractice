package rip.thecraft.practice.hitdelay;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;

public class HitDelayProfile implements ConfigurationSerializable {

    private final String name;
    private long hitDelay; // Delay in milliseconds between hits
    private boolean enabled;

    public HitDelayProfile(String name) {
        this.name = name;
        this.hitDelay = 500L; // Default 1.7.10 PVP timing (500ms)
        this.enabled = true;
    }

    public HitDelayProfile(String name, long hitDelay) {
        this.name = name;
        this.hitDelay = hitDelay;
        this.enabled = true;
    }

    public String getName() {
        return name;
    }

    public long getHitDelay() {
        return hitDelay;
    }

    public void setHitDelay(long hitDelay) {
        this.hitDelay = hitDelay;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("hitDelay", hitDelay);
        data.put("enabled", enabled);
        return data;
    }

    public static HitDelayProfile deserialize(Map<String, Object> data) {
        String name = (String) data.get("name");
        long hitDelay = ((Number) data.getOrDefault("hitDelay", 500L)).longValue();
        
        HitDelayProfile profile = new HitDelayProfile(name, hitDelay);
        profile.setEnabled((boolean) data.getOrDefault("enabled", true));
        return profile;
    }

    @Override
    public String toString() {
        return "HitDelayProfile{" +
                "name='" + name + '\'' +
                ", hitDelay=" + hitDelay +
                ", enabled=" + enabled +
                '}';
    }
}
