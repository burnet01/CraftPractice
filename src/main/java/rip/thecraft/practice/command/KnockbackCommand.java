package rip.thecraft.practice.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rip.thecraft.practice.Practice;

public class KnockbackCommand implements CommandExecutor {

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
            case "delete":
                deleteProfile(sender, args);
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
            case "friction":
                setFriction(sender, args);
                break;
            case "sprint":
                setSprintToggle(sender, args);
                break;
            case "sprintreset":
                setSprintResetTicks(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void createProfile(CommandSender sender, String[] args) {
        if (args.length < 7) {
            sender.sendMessage(ChatColor.RED + "Usage: /kb create <name> <horizontal> <vertical> <verticalLimit> <extraHorizontal> <extraVertical>");
            sender.sendMessage(ChatColor.GRAY + "Example: /kb create light 0.2 0.2 0.3 0.3 0.05");
            return;
        }

        String name = args[1];
        
        try {
            double horizontal = Double.parseDouble(args[2]);
            double vertical = Double.parseDouble(args[3]);
            double verticalLimit = Double.parseDouble(args[4]);
            double extraHorizontal = Double.parseDouble(args[5]);
            double extraVertical = Double.parseDouble(args[6]);
            
            boolean netheriteKnockbackResistance = false;
            if (args.length > 7) {
                netheriteKnockbackResistance = Boolean.parseBoolean(args[7]);
            }

            if (Practice.getInstance().getKnockbackManager().createProfile(name, horizontal, vertical, verticalLimit, 
                                                                          extraHorizontal, extraVertical, netheriteKnockbackResistance)) {
                sender.sendMessage(ChatColor.GREEN + "Knockback profile '" + name + "' created successfully!");
            } else {
                sender.sendMessage(ChatColor.RED + "A knockback profile with that name already exists!");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format! Please use valid decimal numbers.");
        }
    }

    private void deleteProfile(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /kb delete <name>");
            return;
        }

        String name = args[1];
        if (Practice.getInstance().getKnockbackManager().deleteProfile(name)) {
            sender.sendMessage(ChatColor.GREEN + "Knockback profile '" + name + "' deleted successfully!");
        } else {
            sender.sendMessage(ChatColor.RED + "Knockback profile not found!");
        }
    }

    private void listProfiles(CommandSender sender) {
        var profiles = Practice.getInstance().getKnockbackManager().getProfiles();
        if (profiles.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No knockback profiles configured.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Knockback Profiles ===");
        for (var entry : profiles.entrySet()) {
            var profile = entry.getValue();
            String status = profile.isEnabled() ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED";
            sender.sendMessage(ChatColor.YELLOW + "- " + entry.getKey() + " " + status);
        }
    }

    private void showProfileInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /kb info <name>");
            return;
        }

        String name = args[1];
        var profile = Practice.getInstance().getKnockbackManager().getProfile(name);
        if (profile == null) {
            sender.sendMessage(ChatColor.RED + "Knockback profile not found!");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Knockback Profile: " + profile.getName() + " ===");
        sender.sendMessage(ChatColor.YELLOW + "Horizontal: " + ChatColor.WHITE + profile.getHorizontal());
        sender.sendMessage(ChatColor.YELLOW + "Vertical: " + ChatColor.WHITE + profile.getVertical());
        sender.sendMessage(ChatColor.YELLOW + "Vertical Limit: " + ChatColor.WHITE + profile.getVerticalLimit());
        sender.sendMessage(ChatColor.YELLOW + "Extra Horizontal: " + ChatColor.WHITE + profile.getExtraHorizontal());
        sender.sendMessage(ChatColor.YELLOW + "Extra Vertical: " + ChatColor.WHITE + profile.getExtraVertical());
        sender.sendMessage(ChatColor.YELLOW + "Extra Vertical Limit: " + ChatColor.WHITE + profile.getExtraVerticalLimit());
        sender.sendMessage(ChatColor.YELLOW + "Netherite Resistance: " + ChatColor.WHITE + profile.isNetheriteKnockbackResistance());
        sender.sendMessage(ChatColor.YELLOW + "Vertical Friction Reduction: " + ChatColor.WHITE + profile.isVerticalFrictionReduction());
        sender.sendMessage(ChatColor.YELLOW + "Horizontal Friction: " + ChatColor.WHITE + profile.getHorizontalFriction());
        sender.sendMessage(ChatColor.YELLOW + "Vertical Friction: " + ChatColor.WHITE + profile.getVerticalFriction());
        sender.sendMessage(ChatColor.YELLOW + "Disable Sprint Toggle: " + ChatColor.WHITE + profile.isDisableSprintToggle());
        sender.sendMessage(ChatColor.YELLOW + "Sprint Reset Ticks: " + ChatColor.WHITE + profile.getSprintResetTicks());
        sender.sendMessage(ChatColor.YELLOW + "Enabled: " + (profile.isEnabled() ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
    }

    private void setProfileEnabled(CommandSender sender, String[] args, boolean enabled) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /kb " + (enabled ? "enable" : "disable") + " <name>");
            return;
        }

        String name = args[1];
        if (Practice.getInstance().getKnockbackManager().setProfileEnabled(name, enabled)) {
            sender.sendMessage(ChatColor.GREEN + "Knockback profile '" + name + "' " + (enabled ? "enabled" : "disabled") + "!");
        } else {
            sender.sendMessage(ChatColor.RED + "Knockback profile not found!");
        }
    }

    private void updateProfile(CommandSender sender, String[] args) {
        if (args.length < 7) {
            sender.sendMessage(ChatColor.RED + "Usage: /kb update <name> <horizontal> <vertical> <verticalLimit> <extraHorizontal> <extraVertical>");
            sender.sendMessage(ChatColor.GRAY + "Example: /kb update light 0.25 0.25 0.35 0.35 0.08");
            return;
        }

        String name = args[1];
        
        try {
            double horizontal = Double.parseDouble(args[2]);
            double vertical = Double.parseDouble(args[3]);
            double verticalLimit = Double.parseDouble(args[4]);
            double extraHorizontal = Double.parseDouble(args[5]);
            double extraVertical = Double.parseDouble(args[6]);
            
            boolean netheriteKnockbackResistance = false;
            if (args.length > 7) {
                netheriteKnockbackResistance = Boolean.parseBoolean(args[7]);
            }

            if (Practice.getInstance().getKnockbackManager().updateProfile(name, horizontal, vertical, verticalLimit, 
                                                                         extraHorizontal, extraVertical, netheriteKnockbackResistance)) {
                sender.sendMessage(ChatColor.GREEN + "Knockback profile '" + name + "' updated successfully!");
            } else {
                sender.sendMessage(ChatColor.RED + "Knockback profile not found!");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format! Please use valid decimal numbers.");
        }
    }

    private void setFriction(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(ChatColor.RED + "Usage: /kb friction <name> <verticalFrictionReduction> <horizontalFriction> <verticalFriction>");
            sender.sendMessage(ChatColor.GRAY + "Example: /kb friction minehq true 1.5 1.0");
            return;
        }

        String name = args[1];
        
        try {
            boolean verticalFrictionReduction = Boolean.parseBoolean(args[2]);
            double horizontalFriction = Double.parseDouble(args[3]);
            double verticalFriction = Double.parseDouble(args[4]);

            var profile = Practice.getInstance().getKnockbackManager().getProfile(name);
            if (profile == null) {
                sender.sendMessage(ChatColor.RED + "Knockback profile not found!");
                return;
            }

            profile.setVerticalFrictionReduction(verticalFrictionReduction);
            profile.setHorizontalFriction(horizontalFriction);
            profile.setVerticalFriction(verticalFriction);
            Practice.getInstance().getKnockbackManager().saveProfiles();
            
            sender.sendMessage(ChatColor.GREEN + "Friction settings updated for profile '" + name + "'!");
            sender.sendMessage(ChatColor.YELLOW + "Vertical Friction Reduction: " + ChatColor.WHITE + verticalFrictionReduction);
            sender.sendMessage(ChatColor.YELLOW + "Horizontal Friction: " + ChatColor.WHITE + horizontalFriction);
            sender.sendMessage(ChatColor.YELLOW + "Vertical Friction: " + ChatColor.WHITE + verticalFriction);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format! Please use valid decimal numbers.");
        }
    }

    private void setSprintToggle(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /kb sprint <name> <disableSprintToggle>");
            sender.sendMessage(ChatColor.GRAY + "Example: /kb sprint minehq true");
            return;
        }

        String name = args[1];
        
        try {
            boolean disableSprintToggle = Boolean.parseBoolean(args[2]);

            var profile = Practice.getInstance().getKnockbackManager().getProfile(name);
            if (profile == null) {
                sender.sendMessage(ChatColor.RED + "Knockback profile not found!");
                return;
            }

            profile.setDisableSprintToggle(disableSprintToggle);
            Practice.getInstance().getKnockbackManager().saveProfiles();
            
            sender.sendMessage(ChatColor.GREEN + "Sprint toggle setting updated for profile '" + name + "'!");
            sender.sendMessage(ChatColor.YELLOW + "Disable Sprint Toggle: " + ChatColor.WHITE + disableSprintToggle);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid boolean format! Please use 'true' or 'false'.");
        }
    }

    private void setSprintResetTicks(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /kb sprintreset <name> <sprintResetTicks>");
            sender.sendMessage(ChatColor.GRAY + "Example: /kb sprintreset minehq 5");
            return;
        }

        String name = args[1];
        
        try {
            int sprintResetTicks = Integer.parseInt(args[2]);

            var profile = Practice.getInstance().getKnockbackManager().getProfile(name);
            if (profile == null) {
                sender.sendMessage(ChatColor.RED + "Knockback profile not found!");
                return;
            }

            profile.setSprintResetTicks(sprintResetTicks);
            Practice.getInstance().getKnockbackManager().saveProfiles();
            
            sender.sendMessage(ChatColor.GREEN + "Sprint reset ticks updated for profile '" + name + "'!");
            sender.sendMessage(ChatColor.YELLOW + "Sprint Reset Ticks: " + ChatColor.WHITE + sprintResetTicks);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format! Please use a valid integer.");
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Knockback Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/kb create <name> <h> <v> <vl> <eh> <ev> [netherite]" + ChatColor.WHITE + " - Create knockback profile");
        sender.sendMessage(ChatColor.YELLOW + "/kb delete <name>" + ChatColor.WHITE + " - Delete knockback profile");
        sender.sendMessage(ChatColor.YELLOW + "/kb update <name> <h> <v> <vl> <eh> <ev> [netherite]" + ChatColor.WHITE + " - Update knockback profile");
        sender.sendMessage(ChatColor.YELLOW + "/kb list" + ChatColor.WHITE + " - List all knockback profiles");
        sender.sendMessage(ChatColor.YELLOW + "/kb info <name>" + ChatColor.WHITE + " - Show profile details");
        sender.sendMessage(ChatColor.YELLOW + "/kb enable <name>" + ChatColor.WHITE + " - Enable profile");
        sender.sendMessage(ChatColor.YELLOW + "/kb disable <name>" + ChatColor.WHITE + " - Disable profile");
        sender.sendMessage(ChatColor.YELLOW + "/kb friction <name> <vfr> <hf> <vf>" + ChatColor.WHITE + " - Set friction settings");
        sender.sendMessage(ChatColor.YELLOW + "/kb sprint <name> <dst>" + ChatColor.WHITE + " - Set sprint toggle setting");
        sender.sendMessage(ChatColor.YELLOW + "/kb sprintreset <name> <srt>" + ChatColor.WHITE + " - Set sprint reset ticks");
        sender.sendMessage(ChatColor.GRAY + "Parameters: h=horizontal, v=vertical, vl=verticalLimit, eh=extraHorizontal, ev=extraVertical");
        sender.sendMessage(ChatColor.GRAY + "Friction: vfr=verticalFrictionReduction, hf=horizontalFriction, vf=verticalFriction");
        sender.sendMessage(ChatColor.GRAY + "Sprint: dst=disableSprintToggle, srt=sprintResetTicks");
    }
}
