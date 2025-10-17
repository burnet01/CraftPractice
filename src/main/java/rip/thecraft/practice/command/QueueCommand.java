package rip.thecraft.practice.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rip.thecraft.practice.Practice;
import rip.thecraft.practice.queue.QueueType;

public class QueueCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join":
                joinQueue(player, args);
                break;
            case "leave":
                leaveQueue(player);
                break;
            case "status":
                showStatus(player);
                break;
            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void joinQueue(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /queue join <kit> <unranked|ranked>");
            return;
        }

        String kitName = args[1];
        String queueType = args[2].toUpperCase();

        QueueType type;
        try {
            type = QueueType.valueOf(queueType);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid queue type! Use 'unranked' or 'ranked'.");
            return;
        }

        if (Practice.getInstance().getQueueManager().joinQueue(player, kitName, type)) {
            // Message is handled inside joinQueue method with settings check
        } else {
            if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
                player.sendMessage(ChatColor.RED + "Failed to join queue! You might already be in a queue or the kit doesn't exist.");
            }
        }
    }

    private void leaveQueue(Player player) {
        if (Practice.getInstance().getQueueManager().leaveQueue(player)) {
            // Message is handled inside leaveQueue method with settings check
        } else {
            if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
                player.sendMessage(ChatColor.RED + "You are not in a queue!");
            }
        }
    }

    private void showStatus(Player player) {
        var queueEntry = Practice.getInstance().getQueueManager().getPlayerQueue(player.getUniqueId());
        if (queueEntry == null) {
            if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
                player.sendMessage(ChatColor.YELLOW + "You are not in a queue.");
            }
            return;
        }

        if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
            player.sendMessage(ChatColor.GOLD + "=== Queue Status ===");
            player.sendMessage(ChatColor.YELLOW + "Kit: " + ChatColor.WHITE + queueEntry.getKit().getName());
            player.sendMessage(ChatColor.YELLOW + "Type: " + ChatColor.WHITE + queueEntry.getType().name().toLowerCase());
            
            // Show queue size
            var queues = Practice.getInstance().getQueueManager().getQueues();
            var queue = queues.get(queueEntry.getType()).get(queueEntry.getKit().getName().toLowerCase());
            if (queue != null) {
                player.sendMessage(ChatColor.YELLOW + "Players in queue: " + ChatColor.WHITE + queue.getSize());
            }
        }
    }

    private void sendHelp(Player player) {
        if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
            player.sendMessage(ChatColor.GOLD + "=== Queue Commands ===");
            player.sendMessage(ChatColor.YELLOW + "/queue join <kit> <unranked|ranked>" + ChatColor.WHITE + " - Join a queue");
            player.sendMessage(ChatColor.YELLOW + "/queue leave" + ChatColor.WHITE + " - Leave current queue");
            player.sendMessage(ChatColor.YELLOW + "/queue status" + ChatColor.WHITE + " - Show queue status");
        }
    }
}
