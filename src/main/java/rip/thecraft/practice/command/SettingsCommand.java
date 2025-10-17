package rip.thecraft.practice.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rip.thecraft.practice.Practice;

/**
 * Command executor for the settings command
 * Opens the settings GUI for players
 */
public class SettingsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Open settings GUI
            Practice.getInstance().getSettingsManager().openSettingsGUI(player);
            return true;
        }

        if (args[0].toLowerCase().equals("help")) {
            sendHelp(player);
        } else {
            player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /settings help for help.");
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Settings Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/settings" + ChatColor.WHITE + " - Open settings GUI");
        player.sendMessage(ChatColor.YELLOW + "/settings help" + ChatColor.WHITE + " - Show this help");
        player.sendMessage(ChatColor.GRAY + "You can also use the settings book in your inventory!");
    }
}
