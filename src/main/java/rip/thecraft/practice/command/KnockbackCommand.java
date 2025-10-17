package rip.thecraft.practice.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rip.thecraft.practice.Practice;
import rip.thecraft.practice.util.MessageManager;

import java.util.HashMap;
import java.util.Map;

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
            MessageManager.getInstance().sendInvalidUsage(sender, "/kb create <name> <horizontal> <vertical> <verticalLimit> <extraHorizontal> <extraVertical>");
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
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("profile", name);
                MessageManager.getInstance().sendMessage(sender, "knockback.profile.create.success", placeholders);
            } else {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("profile", name);
                MessageManager.getInstance().sendMessage(sender, "knockback.profile.create.exists", placeholders);
            }
        } catch (NumberFormatException e) {
            MessageManager.getInstance().sendMessage(sender, "knockback.profile.create.invalid-number");
        }
    }

    private void deleteProfile(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageManager.getInstance().sendInvalidUsage(sender, "/kb delete <name>");
            return;
        }

        String name = args[1];
        if (Practice.getInstance().getKnockbackManager().deleteProfile(name)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("profile", name);
            MessageManager.getInstance().sendMessage(sender, "knockback.profile.delete.success", placeholders);
        } else {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("profile", name);
            MessageManager.getInstance().sendMessage(sender, "knockback.profile.delete.not-found", placeholders);
        }
    }

    private void listProfiles(CommandSender sender) {
        var profiles = Practice.getInstance().getKnockbackManager().getProfiles();
        if (profiles.isEmpty()) {
            MessageManager.getInstance().sendMessage(sender, "knockback.profile.list.empty");
            return;
        }

        MessageManager.getInstance().sendMessage(sender, "knockback.profile.list.header");
        for (var entry : profiles.entrySet()) {
            var profile = entry.getValue();
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("profile", entry.getKey());
            placeholders.put("status", profile.isEnabled() ? "ENABLED" : "DISABLED");
            placeholders.put("status-color", profile.isEnabled() ? "&a" : "&c");
            MessageManager.getInstance().sendMessage(sender, "knockback.profile.list.item", placeholders);
        }
    }

    private void showProfileInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("usage", "/kb info <name>");
            MessageManager.getInstance().sendMessage(sender, "knockback.profile.info.usage", placeholders);
            return;
        }

        String name = args[1];
        var profile = Practice.getInstance().getKnockbackManager().getProfile(name);
        if (profile == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("profile", name);
            MessageManager.getInstance().sendMessage(sender, "knockback.profile.info.not-found", placeholders);
            return;
        }

        Map<String, String> headerPlaceholders = new HashMap<>();
        headerPlaceholders.put("profile", profile.getName());
        MessageManager.getInstance().sendMessage(sender, "knockback.profile.info.header", headerPlaceholders);
        
        Map<String, String> horizontalPlaceholders = new HashMap<>();
        horizontalPlaceholders.put("value", String.valueOf(profile.getHorizontal()));
        MessageManager.getInstance().sendMessage(sender, "knockback.profile.info.horizontal", horizontalPlaceholders);
        
        Map<String, String> verticalPlaceholders = new HashMap<>();
        verticalPlaceholders.put("value", String.valueOf(profile.getVertical()));
        MessageManager.getInstance().sendMessage(sender, "knockback.profile.info.vertical", verticalPlaceholders);
        
        Map<String, String> verticalLimitPlaceholders = new HashMap<>();
        verticalLimitPlaceholders.put("value", String.valueOf(profile.getVerticalLimit()));
        MessageManager.getInstance().sendMessage(sender, "knockback.profile.info.vertical-limit", verticalLimitPlaceholders);
        
        Map<String, String> extraHorizontalPlaceholders = new HashMap<>();
        extraHorizontalPlaceholders.put("value", String.valueOf(profile.getExtraHorizontal()));
        MessageManager.getInstance().sendMessage(sender, "knockback.profile.info.extra-horizontal", extraHorizontalPlaceholders);
        
        Map<String, String> extraVerticalPlaceholders = new HashMap<>();
        extraVerticalPlaceholders.put("value", String.valueOf(profile.getExtraVertical()));
        MessageManager.getInstance().sendMessage(sender, "knockback.profile.info.extra-vertical", extraVerticalPlaceholders);
        
        Map<String, String> extraVerticalLimitPlaceholders = new HashMap<>();
        extraVerticalLimitPlaceholders.put("value", String.valueOf(profile.getExtraVerticalLimit()));
        MessageManager.getInstance().sendMessage(sender, "knockback.profile.info.extra-vertical-limit", extraVerticalLimitPlaceholders);
        
        Map<String, String> netheritePlaceholders = new HashMap<>();
        netheritePlaceholders.put("value", String.valueOf(profile.isNetheriteKnockbackResistance()));
        MessageManager.getInstance().sendMessage(sender, "knockback.profile.info.netherite-resistance", netheritePlaceholders);
        
        Map<String, String> verticalFrictionPlaceholders = new HashMap<>();
        verticalFrictionPlaceholders.put("value", String.valueOf(profile.isVerticalFrictionReduction()));
        MessageManager.getInstance().sendMessage(sender, "knockback.profile.info.vertical-friction-reduction", verticalFrictionPlaceholders);
        
        Map<String, String> horizontalFrictionPlaceholders = new HashMap<>();
        horizontalFrictionPlaceholders.put("value", String.valueOf(profile.getHorizontalFriction()));
        MessageManager.getInstance().sendMessage(sender, "knockback.profile.info.horizontal-friction", horizontalFrictionPlaceholders);
        
        Map<String, String> verticalFrictionValuePlaceholders = new HashMap<>();
        verticalFrictionValuePlaceholders.put("value", String.valueOf(profile.getVerticalFriction()));
        MessageManager.getInstance().sendMessage(sender, "knockback.profile.info.vertical-friction", verticalFrictionValuePlaceholders);
        
        Map<String, String> sprintTogglePlaceholders = new HashMap<>();
        sprintTogglePlaceholders.put("value", String.valueOf(profile.isDisableSprintToggle()));
        MessageManager.getInstance().sendMessage(sender, "knockback.profile.info.disable-sprint-toggle", sprintTogglePlaceholders);
        
        Map<String, String> sprintResetPlaceholders = new HashMap<>();
        sprintResetPlaceholders.put("value", String.valueOf(profile.getSprintResetTicks()));
        MessageManager.getInstance().sendMessage(sender, "knockback.profile.info.sprint-reset-ticks", sprintResetPlaceholders);
        
        Map<String, String> enabledPlaceholders = new HashMap<>();
        enabledPlaceholders.put("status", profile.isEnabled() ? "&aEnabled" : "&cDisabled");
        MessageManager.getInstance().sendMessage(sender, "knockback.profile.info.enabled", enabledPlaceholders);
    }

    private void setProfileEnabled(CommandSender sender, String[] args, boolean enabled) {
        if (args.length < 2) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("usage", "/kb " + (enabled ? "enable" : "disable") + " <name>");
            MessageManager.getInstance().sendMessage(sender, "knockback.profile.enable.usage", placeholders);
            return;
        }

        String name = args[1];
        if (Practice.getInstance().getKnockbackManager().setProfileEnabled(name, enabled)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("profile", name);
            placeholders.put("status", enabled ? "enabled" : "disabled");
            MessageManager.getInstance().sendMessage(sender, "knockback.profile.enable.success", placeholders);
        } else {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("profile", name);
            MessageManager.getInstance().sendMessage(sender, "knockback.profile.enable.not-found", placeholders);
        }
    }

    private void updateProfile(CommandSender sender, String[] args) {
        if (args.length < 7) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("usage", "/kb update <name> <horizontal> <vertical> <verticalLimit> <extraHorizontal> <extraVertical>");
            MessageManager.getInstance().sendMessage(sender, "knockback.profile.update.usage", placeholders);
            MessageManager.getInstance().sendMessage(sender, "knockback.profile.update.example");
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
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("profile", name);
                MessageManager.getInstance().sendMessage(sender, "knockback.profile.update.success", placeholders);
            } else {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("profile", name);
                MessageManager.getInstance().sendMessage(sender, "knockback.profile.update.not-found", placeholders);
            }
        } catch (NumberFormatException e) {
            MessageManager.getInstance().sendMessage(sender, "knockback.profile.update.invalid-number");
        }
    }

    private void setFriction(CommandSender sender, String[] args) {
        if (args.length < 5) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("usage", "/kb friction <name> <verticalFrictionReduction> <horizontalFriction> <verticalFriction>");
            MessageManager.getInstance().sendMessage(sender, "knockback.friction.usage", placeholders);
            MessageManager.getInstance().sendMessage(sender, "knockback.friction.example");
            return;
        }

        String name = args[1];
        
        try {
            boolean verticalFrictionReduction = Boolean.parseBoolean(args[2]);
            double horizontalFriction = Double.parseDouble(args[3]);
            double verticalFriction = Double.parseDouble(args[4]);

            var profile = Practice.getInstance().getKnockbackManager().getProfile(name);
            if (profile == null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("profile", name);
                MessageManager.getInstance().sendMessage(sender, "knockback.friction.profile-not-found", placeholders);
                return;
            }

            profile.setVerticalFrictionReduction(verticalFrictionReduction);
            profile.setHorizontalFriction(horizontalFriction);
            profile.setVerticalFriction(verticalFriction);
            Practice.getInstance().getKnockbackManager().saveProfiles();
            
            Map<String, String> successPlaceholders = new HashMap<>();
            successPlaceholders.put("profile", name);
            MessageManager.getInstance().sendMessage(sender, "knockback.friction.success", successPlaceholders);
            
            Map<String, String> vfrPlaceholders = new HashMap<>();
            vfrPlaceholders.put("value", String.valueOf(verticalFrictionReduction));
            MessageManager.getInstance().sendMessage(sender, "knockback.friction.vertical-friction-reduction", vfrPlaceholders);
            
            Map<String, String> hfPlaceholders = new HashMap<>();
            hfPlaceholders.put("value", String.valueOf(horizontalFriction));
            MessageManager.getInstance().sendMessage(sender, "knockback.friction.horizontal-friction", hfPlaceholders);
            
            Map<String, String> vfPlaceholders = new HashMap<>();
            vfPlaceholders.put("value", String.valueOf(verticalFriction));
            MessageManager.getInstance().sendMessage(sender, "knockback.friction.vertical-friction", vfPlaceholders);
        } catch (NumberFormatException e) {
            MessageManager.getInstance().sendMessage(sender, "knockback.friction.invalid-number");
        }
    }

    private void setSprintToggle(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("usage", "/kb sprint <name> <disableSprintToggle>");
            MessageManager.getInstance().sendMessage(sender, "knockback.sprint.usage", placeholders);
            MessageManager.getInstance().sendMessage(sender, "knockback.sprint.example");
            return;
        }

        String name = args[1];
        
        try {
            boolean disableSprintToggle = Boolean.parseBoolean(args[2]);

            var profile = Practice.getInstance().getKnockbackManager().getProfile(name);
            if (profile == null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("profile", name);
                MessageManager.getInstance().sendMessage(sender, "knockback.sprint.profile-not-found", placeholders);
                return;
            }

            profile.setDisableSprintToggle(disableSprintToggle);
            Practice.getInstance().getKnockbackManager().saveProfiles();
            
            Map<String, String> successPlaceholders = new HashMap<>();
            successPlaceholders.put("profile", name);
            MessageManager.getInstance().sendMessage(sender, "knockback.sprint.success", successPlaceholders);
            
            Map<String, String> togglePlaceholders = new HashMap<>();
            togglePlaceholders.put("value", String.valueOf(disableSprintToggle));
            MessageManager.getInstance().sendMessage(sender, "knockback.sprint.disable-sprint-toggle", togglePlaceholders);
        } catch (NumberFormatException e) {
            MessageManager.getInstance().sendMessage(sender, "knockback.sprint.invalid-boolean");
        }
    }

    private void setSprintResetTicks(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("usage", "/kb sprintreset <name> <sprintResetTicks>");
            MessageManager.getInstance().sendMessage(sender, "knockback.sprintreset.usage", placeholders);
            MessageManager.getInstance().sendMessage(sender, "knockback.sprintreset.example");
            return;
        }

        String name = args[1];
        
        try {
            int sprintResetTicks = Integer.parseInt(args[2]);

            var profile = Practice.getInstance().getKnockbackManager().getProfile(name);
            if (profile == null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("profile", name);
                MessageManager.getInstance().sendMessage(sender, "knockback.sprintreset.profile-not-found", placeholders);
                return;
            }

            profile.setSprintResetTicks(sprintResetTicks);
            Practice.getInstance().getKnockbackManager().saveProfiles();
            
            Map<String, String> successPlaceholders = new HashMap<>();
            successPlaceholders.put("profile", name);
            MessageManager.getInstance().sendMessage(sender, "knockback.sprintreset.success", successPlaceholders);
            
            Map<String, String> ticksPlaceholders = new HashMap<>();
            ticksPlaceholders.put("value", String.valueOf(sprintResetTicks));
            MessageManager.getInstance().sendMessage(sender, "knockback.sprintreset.sprint-reset-ticks", ticksPlaceholders);
        } catch (NumberFormatException e) {
            MessageManager.getInstance().sendMessage(sender, "knockback.sprintreset.invalid-number");
        }
    }

    private void sendHelp(CommandSender sender) {
        MessageManager.getInstance().sendMessage(sender, "knockback.help.header");
        MessageManager.getInstance().sendMessage(sender, "knockback.help.create");
        MessageManager.getInstance().sendMessage(sender, "knockback.help.delete");
        MessageManager.getInstance().sendMessage(sender, "knockback.help.update");
        MessageManager.getInstance().sendMessage(sender, "knockback.help.list");
        MessageManager.getInstance().sendMessage(sender, "knockback.help.info");
        MessageManager.getInstance().sendMessage(sender, "knockback.help.enable");
        MessageManager.getInstance().sendMessage(sender, "knockback.help.disable");
        MessageManager.getInstance().sendMessage(sender, "knockback.help.friction");
        MessageManager.getInstance().sendMessage(sender, "knockback.help.sprint");
        MessageManager.getInstance().sendMessage(sender, "knockback.help.sprintreset");
        MessageManager.getInstance().sendMessage(sender, "knockback.help.parameters");
        MessageManager.getInstance().sendMessage(sender, "knockback.help.friction-params");
        MessageManager.getInstance().sendMessage(sender, "knockback.help.sprint-params");
    }
}
