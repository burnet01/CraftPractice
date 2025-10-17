package rip.thecraft.practice.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rip.thecraft.practice.Practice;
import rip.thecraft.practice.hitdelay.HitDelayProfile;
import rip.thecraft.practice.kit.Kit;
import rip.thecraft.practice.match.Match;

public class HitDelayCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                createProfile(sender, args);
                break;
            case "list":
                listProfiles(sender);
                break;
            case "info":
                showProfileInfo(sender, args);
                break;
            case "enable":
                setProfileEnabled(sender, args, true);
                break;
            case "disable":
                setProfileEnabled(sender, args, false);
                break;
            case "update":
                updateProfile(sender, args);
                break;
            case "delete":
                deleteProfile(sender, args);
                break;
            default:
                // Try to set kit hit delay profile
                setKitHitDelay(sender, args);
                break;
        }

        return true;
    }

    private void createProfile(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /hd create <profile> <delay>");
            sender.sendMessage(ChatColor.GRAY + "Example: /hd create classic 500");
            sender.sendMessage(ChatColor.GRAY + "Delay is in milliseconds (1000ms = 1 second)");
            return;
        }

        String name = args[1];
        
        try {
            long delay = Long.parseLong(args[2]);
            
            if (delay < 0) {
                sender.sendMessage(ChatColor.RED + "Delay cannot be negative!");
                return;
            }

            HitDelayProfile profile = new HitDelayProfile(name, delay);
            Practice.getInstance().getHitDelayManager().addProfile(profile);
            sender.sendMessage(ChatColor.GREEN + "Hit delay profile '" + name + "' created successfully with " + delay + "ms delay!");
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid delay format! Please use a valid number.");
        }
    }

    private void setKitHitDelay(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /hd <kitname> <profile>");
            sender.sendMessage(ChatColor.GRAY + "Example: /hd nodebuff classic");
            return;
        }

        String kitName = args[0];
        String profileName = args[1];

        // Check if kit exists
        Kit kit = Practice.getInstance().getKitManager().getKit(kitName);
        if (kit == null) {
            sender.sendMessage(ChatColor.RED + "Kit '" + kitName + "' not found!");
            return;
        }

        // Check if profile exists
        HitDelayProfile profile = Practice.getInstance().getHitDelayManager().getProfile(profileName);
        if (profile == null) {
            sender.sendMessage(ChatColor.RED + "Hit delay profile '" + profileName + "' not found!");
            sender.sendMessage(ChatColor.GRAY + "Use '/hd list' to see available profiles.");
            return;
        }

        // Set the hit delay profile
        kit.setHitDelayProfile(profileName);
        Practice.getInstance().getKitManager().saveKits();
        
        // Reset hit delay for any players currently using this kit in matches
        var matchManager = Practice.getInstance().getMatchManager();
        var hitDelayHandler = Practice.getInstance().getHitDelayHandler();
        
        for (Player onlinePlayer : Practice.getInstance().getServer().getOnlinePlayers()) {
            if (matchManager.isPlayerInMatch(onlinePlayer)) {
                Match match = matchManager.getPlayerMatch(onlinePlayer.getUniqueId());
                if (match != null && kitName.equals(match.getKit().getName())) {
                    // Player is in a match with this kit, reset their hit delay
                    hitDelayHandler.resetPlayer(onlinePlayer);
                }
            }
        }
        
        sender.sendMessage(ChatColor.GREEN + "Kit '" + kitName + "' now uses hit delay profile '" + profileName + "' (" + profile.getHitDelay() + "ms)");
    }

    private void listProfiles(CommandSender sender) {
        var profiles = Practice.getInstance().getHitDelayManager().getProfiles();
        if (profiles.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No hit delay profiles configured.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Hit Delay Profiles ===");
        for (var entry : profiles.entrySet()) {
            var profile = entry.getValue();
            String status = profile.isEnabled() ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED";
            sender.sendMessage(ChatColor.YELLOW + "- " + entry.getKey() + " " + status + ChatColor.WHITE + " (" + profile.getHitDelay() + "ms)");
        }
    }

    private void showProfileInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /hd info <profile>");
            return;
        }

        String name = args[1];
        var profile = Practice.getInstance().getHitDelayManager().getProfile(name);
        if (profile == null) {
            sender.sendMessage(ChatColor.RED + "Hit delay profile not found!");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Hit Delay Profile: " + profile.getName() + " ===");
        sender.sendMessage(ChatColor.YELLOW + "Delay: " + ChatColor.WHITE + profile.getHitDelay() + "ms");
        sender.sendMessage(ChatColor.YELLOW + "Enabled: " + (profile.isEnabled() ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
        
        // Show which kits use this profile
        var kitManager = Practice.getInstance().getKitManager();
        var kitNames = kitManager.getKitNames();
        int usageCount = 0;
        
        for (String kitName : kitNames) {
            Kit kit = kitManager.getKit(kitName);
            if (kit != null && name.equals(kit.getHitDelayProfile())) {
                if (usageCount == 0) {
                    sender.sendMessage(ChatColor.YELLOW + "Used by kits:");
                }
                sender.sendMessage(ChatColor.WHITE + "  - " + kit.getName());
                usageCount++;
            }
        }
        
        if (usageCount == 0) {
            sender.sendMessage(ChatColor.GRAY + "Not currently used by any kits.");
        }
    }

    private void setProfileEnabled(CommandSender sender, String[] args, boolean enabled) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /hd " + (enabled ? "enable" : "disable") + " <name>");
            return;
        }

        String name = args[1];
        var profile = Practice.getInstance().getHitDelayManager().getProfile(name);
        if (profile == null) {
            sender.sendMessage(ChatColor.RED + "Hit delay profile not found!");
            return;
        }

        profile.setEnabled(enabled);
        Practice.getInstance().getHitDelayManager().saveProfiles();
        sender.sendMessage(ChatColor.GREEN + "Hit delay profile '" + name + "' " + (enabled ? "enabled" : "disabled") + "!");
    }

    private void updateProfile(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /hd update <profile> <delay>");
            sender.sendMessage(ChatColor.GRAY + "Example: /hd update classic 500");
            return;
        }

        String name = args[1];
        
        try {
            long delay = Long.parseLong(args[2]);
            
            if (delay < 0) {
                sender.sendMessage(ChatColor.RED + "Delay cannot be negative!");
                return;
            }

            var profile = Practice.getInstance().getHitDelayManager().getProfile(name);
            if (profile == null) {
                sender.sendMessage(ChatColor.RED + "Hit delay profile not found!");
                return;
            }

            profile.setHitDelay(delay);
            Practice.getInstance().getHitDelayManager().saveProfiles();
            sender.sendMessage(ChatColor.GREEN + "Hit delay profile '" + name + "' updated to " + delay + "ms!");
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid delay format! Please use a valid number.");
        }
    }

    private void deleteProfile(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /hd delete <name>");
            return;
        }

        String name = args[1];
        
        // Check if any kits are using this profile
        var kitManager = Practice.getInstance().getKitManager();
        var kitNames = kitManager.getKitNames();
        for (String kitName : kitNames) {
            Kit kit = kitManager.getKit(kitName);
            if (kit != null && name.equals(kit.getHitDelayProfile())) {
                sender.sendMessage(ChatColor.RED + "Cannot delete profile '" + name + "' - it is being used by kit '" + kit.getName() + "'");
                sender.sendMessage(ChatColor.GRAY + "Change the kit's hit delay profile first using '/hd <kitname> <newprofile>'");
                return;
            }
        }

        Practice.getInstance().getHitDelayManager().removeProfile(name);
        sender.sendMessage(ChatColor.GREEN + "Hit delay profile '" + name + "' deleted successfully!");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Hit Delay Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/hd" + ChatColor.WHITE + " - Show this help menu");
        sender.sendMessage(ChatColor.YELLOW + "/hd create <profile> <delay>" + ChatColor.WHITE + " - Create hit delay profile");
        sender.sendMessage(ChatColor.YELLOW + "/hd <kitname> <profile>" + ChatColor.WHITE + " - Set kit's hit delay profile");
        sender.sendMessage(ChatColor.YELLOW + "/hd list" + ChatColor.WHITE + " - List all hit delay profiles");
        sender.sendMessage(ChatColor.YELLOW + "/hd info <profile>" + ChatColor.WHITE + " - Show profile details");
        sender.sendMessage(ChatColor.YELLOW + "/hd enable <profile>" + ChatColor.WHITE + " - Enable profile");
        sender.sendMessage(ChatColor.YELLOW + "/hd disable <profile>" + ChatColor.WHITE + " - Disable profile");
        sender.sendMessage(ChatColor.YELLOW + "/hd update <profile> <delay>" + ChatColor.WHITE + " - Update profile delay");
        sender.sendMessage(ChatColor.YELLOW + "/hd delete <profile>" + ChatColor.WHITE + " - Delete profile");
        sender.sendMessage(ChatColor.GRAY + "Delay is in milliseconds (500ms = classic 1.7.10 PVP timing)");
    }
}
