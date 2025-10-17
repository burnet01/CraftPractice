package rip.thecraft.practice.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rip.thecraft.practice.Practice;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BuildCommand implements CommandExecutor {

    // Track players who have build bypass enabled
    private final Set<UUID> buildBypassPlayers = new HashSet<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("practice.build.bypass")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            // Toggle build bypass mode
            toggleBuildBypass(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "on":
                enableBuildBypass(player);
                break;
            case "off":
                disableBuildBypass(player);
                break;
            case "status":
                showBuildStatus(player);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /build [on|off|status]");
                break;
        }

        return true;
    }

    private void toggleBuildBypass(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (buildBypassPlayers.contains(playerId)) {
            disableBuildBypass(player);
        } else {
            enableBuildBypass(player);
        }
    }

    private void enableBuildBypass(Player player) {
        UUID playerId = player.getUniqueId();
        buildBypassPlayers.add(playerId);
        player.sendMessage(ChatColor.GREEN + "Build bypass enabled! You can now break and place any blocks in arenas.");
        player.sendMessage(ChatColor.YELLOW + "Use /build off to disable this mode.");
    }

    private void disableBuildBypass(Player player) {
        UUID playerId = player.getUniqueId();
        buildBypassPlayers.remove(playerId);
        player.sendMessage(ChatColor.RED + "Build bypass disabled! Normal arena restrictions apply.");
    }

    private void showBuildStatus(Player player) {
        UUID playerId = player.getUniqueId();
        if (buildBypassPlayers.contains(playerId)) {
            player.sendMessage(ChatColor.GREEN + "Build bypass is currently ENABLED.");
        } else {
            player.sendMessage(ChatColor.RED + "Build bypass is currently DISABLED.");
        }
    }

    /**
     * Check if a player has build bypass enabled
     */
    public boolean hasBuildBypass(Player player) {
        return buildBypassPlayers.contains(player.getUniqueId());
    }

    /**
     * Clear build bypass for a player (useful when they log out)
     */
    public void clearBuildBypass(Player player) {
        buildBypassPlayers.remove(player.getUniqueId());
    }
}
