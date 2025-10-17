package rip.thecraft.practice.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import rip.thecraft.practice.Practice;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {

    private static MessageManager instance;
    
    private final Practice plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;
    private final Map<String, String> messageCache = new HashMap<>();

    public MessageManager(Practice plugin) {
        this.plugin = plugin;
        instance = this;
        loadMessages();
    }

    public static MessageManager getInstance() {
        return instance;
    }

    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        messageCache.clear();
        
        // Cache all messages for faster access
        for (String key : messagesConfig.getKeys(true)) {
            if (messagesConfig.isString(key)) {
                messageCache.put(key, messagesConfig.getString(key));
            }
        }
        
        plugin.getLogger().info("Loaded " + messageCache.size() + " messages from messages.yml");
    }

    public void saveMessages() {
        if (messagesConfig != null && messagesFile != null) {
            try {
                messagesConfig.save(messagesFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save messages.yml: " + e.getMessage());
            }
        }
    }

    public String getMessage(String key) {
        String message = messageCache.get(key);
        if (message == null) {
            plugin.getLogger().warning("Message key not found: " + key);
            return "&cMessage not found: " + key;
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String message = getMessage(key);
        
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        
        return message;
    }

    public void sendMessage(CommandSender sender, String key) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
                sender.sendMessage(getMessage(key));
            }
        } else {
            sender.sendMessage(getMessage(key));
        }
    }

    public void sendMessage(CommandSender sender, String key, Map<String, String> placeholders) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
                sender.sendMessage(getMessage(key, placeholders));
            }
        } else {
            sender.sendMessage(getMessage(key, placeholders));
        }
    }

    public void sendNoPermission(CommandSender sender) {
        sendMessage(sender, "no-permission");
    }

    public void sendPlayerOnly(CommandSender sender) {
        sendMessage(sender, "player-only");
    }

    public void sendPlayerNotFound(CommandSender sender, String playerName) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", playerName);
        sendMessage(sender, "player-not-found", placeholders);
    }

    public void sendInvalidUsage(CommandSender sender, String usage) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("usage", usage);
        sendMessage(sender, "invalid-usage", placeholders);
    }

    public void reload() {
        loadMessages();
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public void setMessage(String key, String value) {
        messagesConfig.set(key, value);
        messageCache.put(key, value);
        saveMessages();
    }
}
