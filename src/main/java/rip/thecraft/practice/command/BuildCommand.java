package rip.thecraft.practice.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rip.thecraft.practice.Practice;
import rip.thecraft.practice.util.MessageManager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BuildCommand implements CommandExecutor {

    // Track players who have build bypass enabled
    private final Set<UUID> buildBypassPlayers = new HashSet<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageManager.getInstance().sendPlayerOnly(sender);
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("practice.build.bypass")) {
            MessageManager.getInstance().sendNoPermission(player);
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
                MessageManager.getInstance().sendMessage(player, "build.subcommand.unknown");
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
        MessageManager.getInstance().sendMessage(player, "build.enabled");
        MessageManager.getInstance().sendMessage(player, "build.disable.hint");
    }

    private void disableBuildBypass(Player player) {
        UUID playerId = player.getUniqueId();
        buildBypassPlayers.remove(playerId);
        MessageManager.getInstance().sendMessage(player, "build.disabled");
    }

    private void showBuildStatus(Player player) {
        UUID playerId = player.getUniqueId();
        if (buildBypassPlayers.contains(playerId)) {
            MessageManager.getInstance().sendMessage(player, "build.status.enabled");
        } else {
            MessageManager.getInstance().sendMessage(player, "build.status.disabled");
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
