package rip.thecraft.practice.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rip.thecraft.practice.Practice;

public class PracticeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Open kit selection GUI
            player.openInventory(Practice.getInstance().getKitManager().createKitSelectionGUI());
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
            default:
                if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
                    player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /practice help for help.");
                }
                break;
        }

        return true;
    }

    private void sendHelp(Player player) {
        if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
            player.sendMessage(ChatColor.GOLD + "=== Practice Plugin Commands ===");
            player.sendMessage(ChatColor.YELLOW + "/practice" + ChatColor.WHITE + " - Open kit selection");
            player.sendMessage(ChatColor.YELLOW + "/practice stats" + ChatColor.WHITE + " - View your stats");
            player.sendMessage(ChatColor.YELLOW + "/practice setspawn" + ChatColor.WHITE + " - Set practice spawn");
            player.sendMessage(ChatColor.YELLOW + "/practice spawn" + ChatColor.WHITE + " - Teleport to spawn");
            player.sendMessage(ChatColor.YELLOW + "/arena" + ChatColor.WHITE + " - Arena management");
            player.sendMessage(ChatColor.YELLOW + "/kit" + ChatColor.WHITE + " - Kit management");
            player.sendMessage(ChatColor.YELLOW + "/queue" + ChatColor.WHITE + " - Queue management");
        }
    }

    private void showStats(Player player) {
        if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
            var playerData = Practice.getInstance().getPlayerManager().getPlayerData(player);
            player.sendMessage(ChatColor.GOLD + "=== Your Stats ===");
            player.sendMessage(ChatColor.YELLOW + "Global ELO: " + ChatColor.WHITE + playerData.getElo());
            player.sendMessage(ChatColor.YELLOW + "Global Wins: " + ChatColor.WHITE + playerData.getWins());
            player.sendMessage(ChatColor.YELLOW + "Global Losses: " + ChatColor.WHITE + playerData.getLosses());
            player.sendMessage(ChatColor.YELLOW + "Global Win Rate: " + ChatColor.WHITE + String.format("%.2f%%", playerData.getWinRate()));
            
            // Show per-kit stats
            if (!playerData.getKitStats().isEmpty()) {
                player.sendMessage(ChatColor.GOLD + "=== Per-Kit Stats ===");
                for (var entry : playerData.getKitStats().entrySet()) {
                    String kitName = entry.getKey();
                    var kitStats = entry.getValue();
                    double kitWinRate = kitStats.getWinRate();
                    player.sendMessage(ChatColor.YELLOW + kitName + ": " + ChatColor.WHITE + 
                        kitStats.getElo() + " ELO, " + kitStats.getWins() + "W/" + kitStats.getLosses() + "L (" + 
                        String.format("%.2f%%", kitWinRate) + ")");
                }
            }
        }
    }

    private void setSpawn(Player player) {
        if (!player.hasPermission("practice.admin")) {
            if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
                player.sendMessage(ChatColor.RED + "You don't have permission to set the practice spawn!");
            }
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

        if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
            player.sendMessage(ChatColor.GREEN + "Practice spawn set to your current location!");
        }
    }

    private void debugMatches(Player player) {
        if (!player.hasPermission("practice.admin")) {
            if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            }
            return;
        }
        
        var matchManager = Practice.getInstance().getMatchManager();
        if (matchManager != null) {
            matchManager.debugActiveMatches();
            if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
                player.sendMessage(ChatColor.GREEN + "Debug info logged to console!");
            }
        } else {
            if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
                player.sendMessage(ChatColor.RED + "MatchManager is null!");
            }
        }
    }

    private void teleportToSpawn(Player player) {
        // Execute the spawn command
        player.performCommand("spawn");
    }
}
