package rip.thecraft.practice.command;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rip.thecraft.practice.Practice;
import rip.thecraft.practice.util.MessageManager;

import java.util.HashMap;
import java.util.Map;

public class KitCommand implements CommandExecutor {

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
            case "create":
                createKit(player, args);
                break;
            case "delete":
                deleteKit(player, args);
                break;
            case "icon":
                setKitIcon(player, args);
                break;
            case "display":
                setKitDisplayName(player, args);
                break;
            case "inventory":
                updateKitInventory(player, args);
                break;
            case "enable":
                enableKit(player, args);
                break;
            case "sumo":
                toggleSumoMode(player, args);
                break;
            case "boxing":
                toggleBoxingMode(player, args);
                break;
            case "build":
                toggleBuildMode(player, args);
                break;
            case "current":
                showCurrentKit(player, args);
                break;
            case "inv":
                updateKitInventory(player, args); // Alias for inventory
                break;
            case "list":
                listKits(player);
                break;
            case "kb":
            case "knockback":
                setKitKnockback(player, args);
                break;
            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void createKit(Player player, String[] args) {
        if (args.length < 2) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("usage", "/kit create <name>");
            MessageManager.getInstance().sendInvalidUsage(player, "/kit create <name>");
            return;
        }

        String name = args[1];
        if (Practice.getInstance().getKitManager().createKitFromInventory(name, player)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("kit", name);
            MessageManager.getInstance().sendMessage(player, "kit.create.success", placeholders);
        } else {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("kit", name);
            MessageManager.getInstance().sendMessage(player, "kit.create.error.exists", placeholders);
        }
    }



    private void deleteKit(Player player, String[] args) {
        if (args.length < 2) {
            MessageManager.getInstance().sendInvalidUsage(player, "/kit delete <name>");
            return;
        }

        String name = args[1];
        if (Practice.getInstance().getKitManager().deleteKit(name)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("kit", name);
            MessageManager.getInstance().sendMessage(player, "kit.delete.success", placeholders);
        } else {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("kit", name);
            MessageManager.getInstance().sendMessage(player, "kit.error.not-found", placeholders);
        }
    }

    private void listKits(Player player) {
        var kits = Practice.getInstance().getKitManager().getKitNames();
        if (kits.isEmpty()) {
            MessageManager.getInstance().sendMessage(player, "kit.list.empty");
            return;
        }

        MessageManager.getInstance().sendMessage(player, "kit.list.header");
        for (String name : kits) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("kit", name);
            MessageManager.getInstance().sendMessage(player, "kit.list.item", placeholders);
        }
    }

    private void setKitIcon(Player player, String[] args) {
        if (args.length < 2) {
            MessageManager.getInstance().sendInvalidUsage(player, "/kit icon <name>");
            return;
        }

        String name = args[1];
        ItemStack itemInHand = player.getInventory().getItemInHand();
        
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            MessageManager.getInstance().sendMessage(player, "kit.icon.error.no-item");
            return;
        }

        if (Practice.getInstance().getKitManager().setKitIcon(name, itemInHand)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("kit", name);
            MessageManager.getInstance().sendMessage(player, "kit.icon.success", placeholders);
        } else {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("kit", name);
            MessageManager.getInstance().sendMessage(player, "kit.error.not-found", placeholders);
        }
    }

    private void setKitDisplayName(Player player, String[] args) {
        if (args.length < 3) {
            MessageManager.getInstance().sendInvalidUsage(player, "/kit display <name> <displayname>");
            return;
        }

        String name = args[1];
        StringBuilder displayName = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            displayName.append(args[i]).append(" ");
        }

        String displayNameStr = displayName.toString().trim();
        if (Practice.getInstance().getKitManager().setKitDisplayName(name, displayNameStr)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("kit", name);
            placeholders.put("displayname", displayNameStr);
            MessageManager.getInstance().sendMessage(player, "kit.display.success", placeholders);
        } else {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("kit", name);
            MessageManager.getInstance().sendMessage(player, "kit.error.not-found", placeholders);
        }
    }

    private void updateKitInventory(Player player, String[] args) {
        if (args.length < 2) {
            MessageManager.getInstance().sendInvalidUsage(player, "/kit inventory <name>");
            return;
        }

        String name = args[1];
        if (Practice.getInstance().getKitManager().updateKitInventory(name, player)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("kit", name);
            MessageManager.getInstance().sendMessage(player, "kit.inventory.success", placeholders);
        } else {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("kit", name);
            MessageManager.getInstance().sendMessage(player, "kit.error.not-found", placeholders);
        }
    }

    private void enableKit(Player player, String[] args) {
        if (args.length < 2) {
            MessageManager.getInstance().sendInvalidUsage(player, "/kit enable <name>");
            return;
        }

        String name = args[1];
        if (Practice.getInstance().getKitManager().setKitEnabled(name, true)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("kit", name);
            MessageManager.getInstance().sendMessage(player, "kit.enable.success", placeholders);
        } else {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("kit", name);
            MessageManager.getInstance().sendMessage(player, "kit.error.not-found", placeholders);
        }
    }

    private void toggleSumoMode(Player player, String[] args) {
        if (args.length < 2) {
            MessageManager.getInstance().sendInvalidUsage(player, "/kit sumo <name>");
            return;
        }

        String name = args[1];
        if (Practice.getInstance().getKitManager().toggleSumoMode(name)) {
            var kit = Practice.getInstance().getKitManager().getKit(name);
            if (kit != null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("kit", name);
                placeholders.put("state", kit.isSumoMode() ? "enabled" : "disabled");
                MessageManager.getInstance().sendMessage(player, "kit.sumo.toggle", placeholders);
            }
        } else {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("kit", name);
            MessageManager.getInstance().sendMessage(player, "kit.error.not-found", placeholders);
        }
    }

    private void toggleBoxingMode(Player player, String[] args) {
        if (args.length < 2) {
            MessageManager.getInstance().sendInvalidUsage(player, "/kit boxing <name>");
            return;
        }

        String name = args[1];
        if (Practice.getInstance().getKitManager().toggleBoxingMode(name)) {
            var kit = Practice.getInstance().getKitManager().getKit(name);
            if (kit != null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("kit", name);
                placeholders.put("state", kit.isBoxingMode() ? "enabled" : "disabled");
                MessageManager.getInstance().sendMessage(player, "kit.boxing.toggle", placeholders);
            }
        } else {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("kit", name);
            MessageManager.getInstance().sendMessage(player, "kit.error.not-found", placeholders);
        }
    }

    private void showCurrentKit(Player player, String[] args) {
        if (args.length < 2) {
            MessageManager.getInstance().sendInvalidUsage(player, "/kit current <name>");
            return;
        }

        String name = args[1];
        var kit = Practice.getInstance().getKitManager().getKit(name);
        if (kit == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("kit", name);
            MessageManager.getInstance().sendMessage(player, "kit.error.not-found", placeholders);
            return;
        }

        // Apply the kit to the player
        Practice.getInstance().getKitManager().applyKit(player, kit);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("kit", name);
        MessageManager.getInstance().sendMessage(player, "kit.current.success", placeholders);
    }

    private void toggleBuildMode(Player player, String[] args) {
        if (args.length < 2) {
            MessageManager.getInstance().sendInvalidUsage(player, "/kit build <name>");
            return;
        }

        String name = args[1];
        if (Practice.getInstance().getKitManager().toggleBuildMode(name)) {
            var kit = Practice.getInstance().getKitManager().getKit(name);
            if (kit != null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("kit", name);
                placeholders.put("state", kit.isBuildMode() ? "enabled" : "disabled");
                MessageManager.getInstance().sendMessage(player, "kit.build.toggle", placeholders);
            }
        } else {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("kit", name);
            MessageManager.getInstance().sendMessage(player, "kit.error.not-found", placeholders);
        }
    }

    private void setKitKnockback(Player player, String[] args) {
        if (args.length < 3) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("usage1", "/kit kb <kitname> <knockbackprofilename>");
            placeholders.put("usage2", "/kit knockback <kitname> <knockbackprofilename>");
            MessageManager.getInstance().sendMessage(player, "kit.kb.error.usage", placeholders);
            return;
        }

        String kitName = args[1];
        String profileName = args[2];

        var kit = Practice.getInstance().getKitManager().getKit(kitName);
        if (kit == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("kit", kitName);
            MessageManager.getInstance().sendMessage(player, "kit.error.not-found", placeholders);
            return;
        }

        var profile = Practice.getInstance().getKnockbackManager().getProfile(profileName);
        if (profile == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("profile", profileName);
            MessageManager.getInstance().sendMessage(player, "kit.kb.error.profile-not-found", placeholders);
            MessageManager.getInstance().sendMessage(player, "kit.kb.info.list-profiles");
            return;
        }

        if (!profile.isEnabled()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("profile", profileName);
            MessageManager.getInstance().sendMessage(player, "kit.kb.error.profile-disabled", placeholders);
            return;
        }

        kit.setKnockbackProfile(profileName);
        Practice.getInstance().getKitManager().saveKits();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("kit", kitName);
        placeholders.put("profile", profileName);
        MessageManager.getInstance().sendMessage(player, "kit.kb.success", placeholders);
    }

    private void sendHelp(Player player) {
        MessageManager.getInstance().sendMessage(player, "kit.help.header");
        MessageManager.getInstance().sendMessage(player, "kit.help.create");
        MessageManager.getInstance().sendMessage(player, "kit.help.delete");
        MessageManager.getInstance().sendMessage(player, "kit.help.icon");
        MessageManager.getInstance().sendMessage(player, "kit.help.display");
        MessageManager.getInstance().sendMessage(player, "kit.help.inventory");
        MessageManager.getInstance().sendMessage(player, "kit.help.inv");
        MessageManager.getInstance().sendMessage(player, "kit.help.current");
        MessageManager.getInstance().sendMessage(player, "kit.help.enable");
        MessageManager.getInstance().sendMessage(player, "kit.help.sumo");
        MessageManager.getInstance().sendMessage(player, "kit.help.boxing");
        MessageManager.getInstance().sendMessage(player, "kit.help.build");
        MessageManager.getInstance().sendMessage(player, "kit.help.kb");
        MessageManager.getInstance().sendMessage(player, "kit.help.knockback");
        MessageManager.getInstance().sendMessage(player, "kit.help.list");
    }
}
