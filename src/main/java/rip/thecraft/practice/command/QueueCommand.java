package rip.thecraft.practice.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rip.thecraft.practice.Practice;
import rip.thecraft.practice.queue.QueueType;
import rip.thecraft.practice.util.MessageManager;

import java.util.HashMap;
import java.util.Map;

public class QueueCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageManager.getInstance().sendPlayerOnly(sender);
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
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("usage", "/queue join <kit> <unranked|ranked>");
            MessageManager.getInstance().sendMessage(player, "queue.join.invalid-usage", placeholders);
            return;
        }

        String kitName = args[1];
        String queueType = args[2].toUpperCase();

        QueueType type;
        try {
            type = QueueType.valueOf(queueType);
        } catch (IllegalArgumentException e) {
            MessageManager.getInstance().sendMessage(player, "queue.join.invalid-type");
            return;
        }

        if (Practice.getInstance().getQueueManager().joinQueue(player, kitName, type)) {
            // Message is handled inside joinQueue method with settings check
        } else {
            if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
                MessageManager.getInstance().sendMessage(player, "queue.join.failed");
            }
        }
    }

    private void leaveQueue(Player player) {
        if (Practice.getInstance().getQueueManager().leaveQueue(player)) {
            // Message is handled inside leaveQueue method with settings check
        } else {
            if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
                MessageManager.getInstance().sendMessage(player, "queue.leave.not-in-queue");
            }
        }
    }

    private void showStatus(Player player) {
        var queueEntry = Practice.getInstance().getQueueManager().getPlayerQueue(player.getUniqueId());
        if (queueEntry == null) {
            if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
                MessageManager.getInstance().sendMessage(player, "queue.status.not-in-queue");
            }
            return;
        }

        if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
            MessageManager.getInstance().sendMessage(player, "queue.status.header");
            
            Map<String, String> kitPlaceholders = new HashMap<>();
            kitPlaceholders.put("kit", queueEntry.getKit().getName());
            MessageManager.getInstance().sendMessage(player, "queue.status.kit", kitPlaceholders);
            
            Map<String, String> typePlaceholders = new HashMap<>();
            typePlaceholders.put("type", queueEntry.getType().name().toLowerCase());
            MessageManager.getInstance().sendMessage(player, "queue.status.type", typePlaceholders);
            
            // Show queue size
            var queues = Practice.getInstance().getQueueManager().getQueues();
            var queue = queues.get(queueEntry.getType()).get(queueEntry.getKit().getName().toLowerCase());
            if (queue != null) {
                Map<String, String> sizePlaceholders = new HashMap<>();
                sizePlaceholders.put("size", String.valueOf(queue.getSize()));
                MessageManager.getInstance().sendMessage(player, "queue.status.players", sizePlaceholders);
            }
        }
    }

    private void sendHelp(Player player) {
        if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
            MessageManager.getInstance().sendMessage(player, "queue.help.header");
            MessageManager.getInstance().sendMessage(player, "queue.help.join");
            MessageManager.getInstance().sendMessage(player, "queue.help.leave");
            MessageManager.getInstance().sendMessage(player, "queue.help.status");
        }
    }
}
