package rip.thecraft.practice.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rip.thecraft.practice.Practice;
import rip.thecraft.practice.util.MessageManager;

/**
 * Command executor for the settings command
 * Opens the settings GUI for players
 */
public class SettingsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageManager.getInstance().sendPlayerOnly(sender);
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
            MessageManager.getInstance().sendMessage(player, "settings.subcommand.unknown");
        }

        return true;
    }

    private void sendHelp(Player player) {
        MessageManager.getInstance().sendMessage(player, "settings.help.header");
        MessageManager.getInstance().sendMessage(player, "settings.help.open");
        MessageManager.getInstance().sendMessage(player, "settings.help.help");
        MessageManager.getInstance().sendMessage(player, "settings.help.book");
    }
}
