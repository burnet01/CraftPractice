package rip.thecraft.practice.scoreboard;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import rip.thecraft.practice.Practice;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration manager for scoreboard settings
 */
@Getter
public class ScoreboardConfig {

    private final Practice plugin;
    private File configFile;
    private FileConfiguration config;
    
    // Global settings
    private int updateInterval;
    private int animationSpeed;
    private boolean animationsEnabled;
    
    // Scoreboard templates
    private Map<String, ScoreboardTemplate> templates;
    
    public ScoreboardConfig(Practice plugin) {
        this.plugin = plugin;
        this.templates = new HashMap<>();
        loadConfig();
    }
    
    public void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "scoreboard.yml");
        
        // Create default config if it doesn't exist
        if (!configFile.exists()) {
            plugin.saveResource("scoreboard.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Load global settings
        updateInterval = config.getInt("global.update-interval", 4);
        animationSpeed = config.getInt("global.animation-speed", 10);
        animationsEnabled = config.getBoolean("global.animations-enabled", true);
        
        // Load scoreboard templates
        loadTemplates();
    }
    
    private void loadTemplates() {
        templates.clear();
        
        ConfigurationSection scoreboardsSection = config.getConfigurationSection("scoreboards");
        if (scoreboardsSection == null) {
            plugin.getLogger().warning("No scoreboard templates found in scoreboard.yml");
            return;
        }
        
        for (String state : scoreboardsSection.getKeys(false)) {
            ConfigurationSection stateSection = scoreboardsSection.getConfigurationSection(state);
            if (stateSection != null) {
                ScoreboardTemplate template = new ScoreboardTemplate(state);
                
                // Load title configuration
                ConfigurationSection titleSection = stateSection.getConfigurationSection("title");
                if (titleSection != null) {
                    template.setTitleType(titleSection.getString("type", "static"));
                    template.setStaticTitle(titleSection.getString("static", "&6&lCraftPractice"));
                    
                    // Load animated title frames
                    ConfigurationSection animatedSection = titleSection.getConfigurationSection("animated");
                    if (animatedSection != null) {
                        template.setAnimatedTitleFrames(animatedSection.getStringList("frames"));
                        template.setTitleAnimationSpeed(animatedSection.getInt("speed", 2));
                    }
                }
                
                // Load lines
                template.setLines(stateSection.getStringList("lines"));
                
                // Load line animations (optional)
                ConfigurationSection animationsSection = stateSection.getConfigurationSection("animations");
                if (animationsSection != null) {
                    Map<String, LineAnimation> lineAnimations = new HashMap<>();
                    for (String lineKey : animationsSection.getKeys(false)) {
                        ConfigurationSection animationSection = animationsSection.getConfigurationSection(lineKey);
                        if (animationSection != null) {
                            LineAnimation animation = new LineAnimation(
                                animationSection.getString("type", "pulse"),
                                animationSection.getStringList("frames"),
                                animationSection.getInt("speed", 10)
                            );
                            lineAnimations.put(lineKey, animation);
                        }
                    }
                    template.setLineAnimations(lineAnimations);
                }
                
                templates.put(state.toLowerCase(), template);
            }
        }
    }
    
    public ScoreboardTemplate getTemplate(String state) {
        return templates.get(state.toLowerCase());
    }
    
    public void reloadConfig() {
        loadConfig();
    }
    
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save scoreboard.yml: " + e.getMessage());
        }
    }
    
    /**
     * Represents a scoreboard template for a specific player state
     */
    @Getter
    public static class ScoreboardTemplate {
        private final String state;
        private String titleType = "static";
        private String staticTitle = "&6&lCraftPractice";
        private List<String> animatedTitleFrames = new ArrayList<>();
        private int titleAnimationSpeed = 2;
        private List<String> lines = new ArrayList<>();
        private Map<String, LineAnimation> lineAnimations = new HashMap<>();
        
        public ScoreboardTemplate(String state) {
            this.state = state;
        }
        
        public void setTitleType(String titleType) {
            this.titleType = titleType != null ? titleType : "static";
        }
        
        public void setStaticTitle(String staticTitle) {
            this.staticTitle = staticTitle != null ? staticTitle : "&6&lCraftPractice";
        }
        
        public void setAnimatedTitleFrames(List<String> animatedTitleFrames) {
            this.animatedTitleFrames = animatedTitleFrames != null ? animatedTitleFrames : new ArrayList<>();
        }
        
        public void setTitleAnimationSpeed(int titleAnimationSpeed) {
            this.titleAnimationSpeed = Math.max(1, titleAnimationSpeed);
        }
        
        public void setLines(List<String> lines) {
            this.lines = lines != null ? lines : new ArrayList<>();
        }
        
        public void setLineAnimations(Map<String, LineAnimation> lineAnimations) {
            this.lineAnimations = lineAnimations != null ? lineAnimations : new HashMap<>();
        }
        
        public boolean hasAnimatedTitle() {
            return "animated".equals(titleType) && !animatedTitleFrames.isEmpty();
        }
        
        public String getTitleFrame(int index) {
            if (animatedTitleFrames.isEmpty()) {
                return staticTitle;
            }
            return animatedTitleFrames.get(index % animatedTitleFrames.size());
        }
    }
    
    /**
     * Represents animation for a specific line
     */
    @Getter
    public static class LineAnimation {
        private final String type;
        private final List<String> frames;
        private final int speed;
        
        public LineAnimation(String type, List<String> frames, int speed) {
            this.type = type != null ? type : "pulse";
            this.frames = frames != null ? frames : new ArrayList<>();
            this.speed = Math.max(1, speed);
        }
        
        public String getFrame(int index) {
            if (frames.isEmpty()) {
                return "";
            }
            return frames.get(index % frames.size());
        }
    }
}
