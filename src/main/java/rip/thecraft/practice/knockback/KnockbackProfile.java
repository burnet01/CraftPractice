package rip.thecraft.practice.knockback;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;

public class KnockbackProfile implements ConfigurationSerializable {

    private final String name;
    private double horizontal;
    private double vertical;
    private double verticalLimit;
    private double extraHorizontal;
    private double extraVertical;
    private boolean netheriteKnockbackResistance;
    private boolean enabled;
    private boolean verticalFrictionReduction;
    private double horizontalFriction;
    private double verticalFriction;
    private double extraVerticalLimit;
    private boolean disableSprintToggle;
    private int sprintResetTicks;

    public KnockbackProfile(String name) {
        this.name = name;
        this.horizontal = 0.4D;
        this.vertical = 0.4D;
        this.verticalLimit = 0.4D;
        this.extraHorizontal = 0.5D;
        this.extraVertical = 0.1D;
        this.netheriteKnockbackResistance = false;
        this.enabled = true;
        this.verticalFrictionReduction = false;
        this.horizontalFriction = 2.0D;
        this.verticalFriction = 2.0D;
        this.extraVerticalLimit = 0.4D;
        this.disableSprintToggle = false;
        this.sprintResetTicks = 0;
    }

    public KnockbackProfile(String name, double horizontal, double vertical, double verticalLimit, 
                           double extraHorizontal, double extraVertical, boolean netheriteKnockbackResistance) {
        this.name = name;
        this.horizontal = horizontal;
        this.vertical = vertical;
        this.verticalLimit = verticalLimit;
        this.extraHorizontal = extraHorizontal;
        this.extraVertical = extraVertical;
        this.netheriteKnockbackResistance = netheriteKnockbackResistance;
        this.enabled = true;
        this.verticalFrictionReduction = false;
        this.horizontalFriction = 2.0D;
        this.verticalFriction = 2.0D;
        this.extraVerticalLimit = 0.4D;
        this.disableSprintToggle = false;
        this.sprintResetTicks = 0;
    }

    public String getName() {
        return name;
    }

    public double getHorizontal() {
        return horizontal;
    }

    public void setHorizontal(double horizontal) {
        this.horizontal = horizontal;
    }

    public double getVertical() {
        return vertical;
    }

    public void setVertical(double vertical) {
        this.vertical = vertical;
    }

    public double getVerticalLimit() {
        return verticalLimit;
    }

    public void setVerticalLimit(double verticalLimit) {
        this.verticalLimit = verticalLimit;
    }

    public double getExtraHorizontal() {
        return extraHorizontal;
    }

    public void setExtraHorizontal(double extraHorizontal) {
        this.extraHorizontal = extraHorizontal;
    }

    public double getExtraVertical() {
        return extraVertical;
    }

    public void setExtraVertical(double extraVertical) {
        this.extraVertical = extraVertical;
    }

    public boolean isNetheriteKnockbackResistance() {
        return netheriteKnockbackResistance;
    }

    public void setNetheriteKnockbackResistance(boolean netheriteKnockbackResistance) {
        this.netheriteKnockbackResistance = netheriteKnockbackResistance;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isVerticalFrictionReduction() {
        return verticalFrictionReduction;
    }

    public void setVerticalFrictionReduction(boolean verticalFrictionReduction) {
        this.verticalFrictionReduction = verticalFrictionReduction;
    }

    public double getHorizontalFriction() {
        return horizontalFriction;
    }

    public void setHorizontalFriction(double horizontalFriction) {
        this.horizontalFriction = horizontalFriction;
    }

    public double getVerticalFriction() {
        return verticalFriction;
    }

    public void setVerticalFriction(double verticalFriction) {
        this.verticalFriction = verticalFriction;
    }

    public double getExtraVerticalLimit() {
        return extraVerticalLimit;
    }

    public void setExtraVerticalLimit(double extraVerticalLimit) {
        this.extraVerticalLimit = extraVerticalLimit;
    }

    public boolean isDisableSprintToggle() {
        return disableSprintToggle;
    }

    public void setDisableSprintToggle(boolean disableSprintToggle) {
        this.disableSprintToggle = disableSprintToggle;
    }

    public int getSprintResetTicks() {
        return sprintResetTicks;
    }

    public void setSprintResetTicks(int sprintResetTicks) {
        this.sprintResetTicks = sprintResetTicks;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("horizontal", horizontal);
        data.put("vertical", vertical);
        data.put("verticalLimit", verticalLimit);
        data.put("extraHorizontal", extraHorizontal);
        data.put("extraVertical", extraVertical);
        data.put("netheriteKnockbackResistance", netheriteKnockbackResistance);
        data.put("enabled", enabled);
        data.put("verticalFrictionReduction", verticalFrictionReduction);
        data.put("horizontalFriction", horizontalFriction);
        data.put("verticalFriction", verticalFriction);
        data.put("extraVerticalLimit", extraVerticalLimit);
        data.put("disableSprintToggle", disableSprintToggle);
        data.put("sprintResetTicks", sprintResetTicks);
        return data;
    }

    public static KnockbackProfile deserialize(Map<String, Object> data) {
        String name = (String) data.get("name");
        double horizontal = (double) data.getOrDefault("horizontal", 0.4D);
        double vertical = (double) data.getOrDefault("vertical", 0.4D);
        double verticalLimit = (double) data.getOrDefault("verticalLimit", 0.4D);
        double extraHorizontal = (double) data.getOrDefault("extraHorizontal", 0.5D);
        double extraVertical = (double) data.getOrDefault("extraVertical", 0.1D);
        boolean netheriteKnockbackResistance = (boolean) data.getOrDefault("netheriteKnockbackResistance", false);
        
        KnockbackProfile profile = new KnockbackProfile(name, horizontal, vertical, verticalLimit, 
                                                       extraHorizontal, extraVertical, netheriteKnockbackResistance);
        profile.setEnabled((boolean) data.getOrDefault("enabled", true));
        profile.setVerticalFrictionReduction((boolean) data.getOrDefault("verticalFrictionReduction", false));
        profile.setHorizontalFriction((double) data.getOrDefault("horizontalFriction", 2.0D));
        profile.setVerticalFriction((double) data.getOrDefault("verticalFriction", 2.0D));
        profile.setExtraVerticalLimit((double) data.getOrDefault("extraVerticalLimit", 0.4D));
        profile.setDisableSprintToggle((boolean) data.getOrDefault("disableSprintToggle", false));
        profile.setSprintResetTicks((int) data.getOrDefault("sprintResetTicks", 0));
        return profile;
    }

    @Override
    public String toString() {
        return "KnockbackProfile{" +
                "name='" + name + '\'' +
                ", horizontal=" + horizontal +
                ", vertical=" + vertical +
                ", verticalLimit=" + verticalLimit +
                ", extraHorizontal=" + extraHorizontal +
                ", extraVertical=" + extraVertical +
                ", netheriteKnockbackResistance=" + netheriteKnockbackResistance +
                ", enabled=" + enabled +
                ", verticalFrictionReduction=" + verticalFrictionReduction +
                ", horizontalFriction=" + horizontalFriction +
                ", verticalFriction=" + verticalFriction +
                ", extraVerticalLimit=" + extraVerticalLimit +
                ", disableSprintToggle=" + disableSprintToggle +
                ", sprintResetTicks=" + sprintResetTicks +
                '}';
    }
}
