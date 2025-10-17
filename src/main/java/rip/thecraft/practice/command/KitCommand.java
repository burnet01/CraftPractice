package rip.thecraft.practice.command;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rip.thecraft.practice.Practice;

public class KitCommand implements CommandExecutor {

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
            player.sendMessage(ChatColor.RED + "Usage: /kit create <name>");
            return;
        }

        String name = args[1];
        if (Practice.getInstance().getKitManager().createKitFromInventory(name, player)) {
            player.sendMessage(ChatColor.GREEN + "Kit '" + name + "' created from your inventory!");
        } else {
            player.sendMessage(ChatColor.RED + "A kit with that name already exists!");
        }
    }

    private void editKit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /kit edit <name>");
            return;
        }

        String name = args[1];
        var kit = Practice.getInstance().getKitManager().getKit(name);
        if (kit == null) {
            player.sendMessage(ChatColor.RED + "Kit not found!");
            return;
        }

        // For now, just apply the kit to the player for editing
        Practice.getInstance().getKitManager().applyKit(player, kit);
        player.sendMessage(ChatColor.GREEN + "Kit '" + name + "' applied to your inventory!");
        player.sendMessage(ChatColor.YELLOW + "Edit your inventory and use /kit save " + name + " to save changes.");
    }

    private void deleteKit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /kit delete <name>");
            return;
        }

        String name = args[1];
        if (Practice.getInstance().getKitManager().deleteKit(name)) {
            player.sendMessage(ChatColor.GREEN + "Kit '" + name + "' deleted!");
        } else {
            player.sendMessage(ChatColor.RED + "Kit not found!");
        }
    }

    private void listKits(Player player) {
        var kits = Practice.getInstance().getKitManager().getKitNames();
        if (kits.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No kits configured.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== Kits ===");
        for (String name : kits) {
            player.sendMessage(ChatColor.YELLOW + "- " + name);
        }
    }

    private void setKitIcon(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /kit icon <name>");
            return;
        }

        String name = args[1];
        ItemStack itemInHand = player.getInventory().getItemInHand();
        
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You must hold an item in your hand to set as the kit icon!");
            return;
        }

        if (Practice.getInstance().getKitManager().setKitIcon(name, itemInHand)) {
            player.sendMessage(ChatColor.GREEN + "Kit '" + name + "' icon set to the item in your hand!");
        } else {
            player.sendMessage(ChatColor.RED + "Kit not found!");
        }
    }

    private void setKitDisplayName(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /kit display <name> <displayname>");
            return;
        }

        String name = args[1];
        StringBuilder displayName = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            displayName.append(args[i]).append(" ");
        }

        if (Practice.getInstance().getKitManager().setKitDisplayName(name, displayName.toString().trim())) {
            player.sendMessage(ChatColor.GREEN + "Kit '" + name + "' display name set to '" + displayName.toString().trim() + "'!");
        } else {
            player.sendMessage(ChatColor.RED + "Kit not found!");
        }
    }

    private void updateKitInventory(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /kit inventory <name>");
            return;
        }

        String name = args[1];
        if (Practice.getInstance().getKitManager().updateKitInventory(name, player)) {
            player.sendMessage(ChatColor.GREEN + "Kit '" + name + "' inventory updated from your inventory!");
        } else {
            player.sendMessage(ChatColor.RED + "Kit not found!");
        }
    }

    private void enableKit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /kit enable <name>");
            return;
        }

        String name = args[1];
        if (Practice.getInstance().getKitManager().setKitEnabled(name, true)) {
            player.sendMessage(ChatColor.GREEN + "Kit '" + name + "' enabled!");
        } else {
            player.sendMessage(ChatColor.RED + "Kit not found!");
        }
    }

    private void toggleSumoMode(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /kit sumo <name>");
            return;
        }

        String name = args[1];
        if (Practice.getInstance().getKitManager().toggleSumoMode(name)) {
            var kit = Practice.getInstance().getKitManager().getKit(name);
            if (kit != null) {
                player.sendMessage(ChatColor.GREEN + "Kit '" + name + "' sumo mode " + (kit.isSumoMode() ? "enabled" : "disabled") + "!");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Kit not found!");
        }
    }

    private void toggleBoxingMode(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /kit boxing <name>");
            return;
        }

        String name = args[1];
        if (Practice.getInstance().getKitManager().toggleBoxingMode(name)) {
            var kit = Practice.getInstance().getKitManager().getKit(name);
            if (kit != null) {
                player.sendMessage(ChatColor.GREEN + "Kit '" + name + "' boxing mode " + (kit.isBoxingMode() ? "enabled" : "disabled") + "!");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Kit not found!");
        }
    }

    private void showCurrentKit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /kit current <name>");
            return;
        }

        String name = args[1];
        var kit = Practice.getInstance().getKitManager().getKit(name);
        if (kit == null) {
            player.sendMessage(ChatColor.RED + "Kit not found!");
            return;
        }

        // Apply the kit to the player
        Practice.getInstance().getKitManager().applyKit(player, kit);
        player.sendMessage(ChatColor.GREEN + "Kit '" + name + "' applied to your inventory!");
    }

    private void toggleBuildMode(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /kit build <name>");
            return;
        }

        String name = args[1];
        if (Practice.getInstance().getKitManager().toggleBuildMode(name)) {
            var kit = Practice.getInstance().getKitManager().getKit(name);
            if (kit != null) {
                player.sendMessage(ChatColor.GREEN + "Kit '" + name + "' build mode " + (kit.isBuildMode() ? "enabled" : "disabled") + "!");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Kit not found!");
        }
    }

    private void setKitKnockback(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /kit kb <kitname> <knockbackprofilename>");
            player.sendMessage(ChatColor.RED + "Usage: /kit knockback <kitname> <knockbackprofilename>");
            return;
        }

        String kitName = args[1];
        String profileName = args[2];

        var kit = Practice.getInstance().getKitManager().getKit(kitName);
        if (kit == null) {
            player.sendMessage(ChatColor.RED + "Kit not found!");
            return;
        }

        var profile = Practice.getInstance().getKnockbackManager().getProfile(profileName);
        if (profile == null) {
            player.sendMessage(ChatColor.RED + "Knockback profile not found!");
            player.sendMessage(ChatColor.YELLOW + "Use /kb list to see available profiles.");
            return;
        }

        if (!profile.isEnabled()) {
            player.sendMessage(ChatColor.RED + "That knockback profile is disabled!");
            return;
        }

        kit.setKnockbackProfile(profileName);
        Practice.getInstance().getKitManager().saveKits();
        player.sendMessage(ChatColor.GREEN + "Kit '" + kitName + "' knockback profile set to '" + profileName + "'!");
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Kit Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/kit create <name>" + ChatColor.WHITE + " - Create kit from your inventory");
        player.sendMessage(ChatColor.YELLOW + "/kit delete <name>" + ChatColor.WHITE + " - Delete kit");
        player.sendMessage(ChatColor.YELLOW + "/kit icon <name>" + ChatColor.WHITE + " - Set kit icon from held item");
        player.sendMessage(ChatColor.YELLOW + "/kit display <name> <displayname>" + ChatColor.WHITE + " - Set kit display name");
        player.sendMessage(ChatColor.YELLOW + "/kit inventory <name>" + ChatColor.WHITE + " - Update kit inventory from your inventory");
        player.sendMessage(ChatColor.YELLOW + "/kit inv <name>" + ChatColor.WHITE + " - Alias for /kit inventory");
        player.sendMessage(ChatColor.YELLOW + "/kit current <name>" + ChatColor.WHITE + " - Apply kit to your inventory");
        player.sendMessage(ChatColor.YELLOW + "/kit enable <name>" + ChatColor.WHITE + " - Enable kit for queueing");
        player.sendMessage(ChatColor.YELLOW + "/kit sumo <name>" + ChatColor.WHITE + " - Toggle sumo mode");
        player.sendMessage(ChatColor.YELLOW + "/kit boxing <name>" + ChatColor.WHITE + " - Toggle boxing mode");
        player.sendMessage(ChatColor.YELLOW + "/kit build <name>" + ChatColor.WHITE + " - Toggle build mode (allows block placement/breaking)");
        player.sendMessage(ChatColor.YELLOW + "/kit kb <name> <profile>" + ChatColor.WHITE + " - Set knockback profile for kit");
        player.sendMessage(ChatColor.YELLOW + "/kit knockback <name> <profile>" + ChatColor.WHITE + " - Alias for /kit kb");
        player.sendMessage(ChatColor.YELLOW + "/kit list" + ChatColor.WHITE + " - List all kits");
    }
}
