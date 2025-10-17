package rip.thecraft.practice.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rip.thecraft.practice.Practice;

import java.util.HashMap;
import java.util.Map;

public class PracticeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Practice.getInstance().getMessageManager().sendPlayerOnly(sender);
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                sendHelp(player);
                break;
            case "stats":
                showStats(player);
                break;
            case "setspawn":
                setSpawn(player);
                break;
            case "debug":
                debugMatches(player);
                break;
            case "spawn":
                teleportToSpawn(player);
                break;
            case "queue":
                player.openInventory(Practice.getInstance().getKitManager().createKitSelectionGUI());
                break;
            default:
                Practice.getInstance().getMessageManager().sendMessage(player, "invalid-usage", 
                    Map.of("usage", "/practice help"));
                break;
        }

        return true;
    }

    private void sendHelp(Player player) {
        if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
            Practice.getInstance().getMessageManager().sendMessage(player, "practice.help.header");
            Practice.getInstance().getMessageManager().sendMessage(player, "practice.help.commands.practice");
            Practice.getInstance().getMessageManager().sendMessage(player, "practice.help.commands.stats");
            Practice.getInstance().getMessageManager().sendMessage(player, "practice.help.commands.setspawn");
            Practice.getInstance().getMessageManager().sendMessage(player, "practice.help.commands.spawn");
            Practice.getInstance().getMessageManager().sendMessage(player, "practice.help.commands.arena");
            Practice.getInstance().getMessageManager().sendMessage(player, "practice.help.commands.kit");
            Practice.getInstance().getMessageManager().sendMessage(player, "practice.help.commands.queue");
        }
    }

    private void showStats(Player player) {
        if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
            var playerData = Practice.getInstance().getPlayerManager().getPlayerData(player);
            
            Map<String, String> globalPlaceholders = new HashMap<>();
            globalPlaceholders.put("elo", String.valueOf(playerData.getElo()));
            globalPlaceholders.put("wins", String.valueOf(playerData.getWins()));
            globalPlaceholders.put("losses", String.valueOf(playerData.getLosses()));
            globalPlaceholders.put("winrate", String.format("%.2f%%", playerData.getWinRate()));
            
            Practice.getInstance().getMessageManager().sendMessage(player, "practice.stats.header");
            Practice.getInstance().getMessageManager().sendMessage(player, "practice.stats.global.elo", globalPlaceholders);
            Practice.getInstance().getMessageManager().sendMessage(player, "practice.stats.global.wins", globalPlaceholders);
            Practice.getInstance().getMessageManager().sendMessage(player, "practice.stats.global.losses", globalPlaceholders);
            Practice.getInstance().getMessageManager().sendMessage(player, "practice.stats.global.winrate", globalPlaceholders);
            
            // Show per-kit stats
            if (!playerData.getKitStats().isEmpty()) {
                Practice.getInstance().getMessageManager().sendMessage(player, "practice.stats.kits.header");
                for (var entry : playerData.getKitStats().entrySet()) {
                    String kitName = entry.getKey();
                    var kitStats = entry.getValue();
                    double kitWinRate = kitStats.getWinRate();
                    
                    Map<String, String> kitPlaceholders = new HashMap<>();
                    kitPlaceholders.put("kit", kitName);
                    kitPlaceholders.put("elo", String.valueOf(kitStats.getElo()));
                    kitPlaceholders.put("wins", String.valueOf(kitStats.getWins()));
                    kitPlaceholders.put("losses", String.valueOf(kitStats.getLosses()));
                    kitPlaceholders.put("winrate", String.format("%.2f%%", kitWinRate));
                    
                    Practice.getInstance().getMessageManager().sendMessage(player, "practice.stats.kits.entry", kitPlaceholders);
                }
            }
        }
    }

    private void setSpawn(Player player) {
        if (!player.hasPermission("practice.admin")) {
            Practice.getInstance().getMessageManager().sendNoPermission(player);
            return;
        }

        // Save spawn location to config
        var config = Practice.getInstance().getConfig();
        config.set("spawn.world", player.getWorld().getName());
        config.set("spawn.x", player.getLocation().getX());
        config.set("spawn.y", player.getLocation().getY());
        config.set("spawn.z", player.getLocation().getZ());
        config.set("spawn.yaw", player.getLocation().getYaw());
        config.set("spawn.pitch", player.getLocation().getPitch());
        Practice.getInstance().saveConfig();

        Practice.getInstance().getMessageManager().sendMessage(player, "practice.setspawn.success");
    }

    private void debugMatches(Player player) {
        if (!player.hasPermission("practice.admin")) {
            Practice.getInstance().getMessageManager().sendNoPermission(player);
            return;
        }
        
        var matchManager = Practice.getInstance().getMatchManager();
        if (matchManager != null) {
            matchManager.debugActiveMatches();
            Practice.getInstance().getMessageManager().sendMessage(player, "practice.debug.enabled");
        } else {
            Practice.getInstance().getMessageManager().sendMessage(player, "practice.debug.disabled");
        }
    }

    private void teleportToSpawn(Player player) {
        // Execute the spawn command
        player.performCommand("spawn");
    }
}
