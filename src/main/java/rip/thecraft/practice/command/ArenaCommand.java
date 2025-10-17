package rip.thecraft.practice.command;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rip.thecraft.practice.Practice;
import rip.thecraft.practice.arena.SelectionManager;

public class ArenaCommand implements CommandExecutor {

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
                createArena(player, args);
                break;
            case "setspawn":
                setSpawn(player, args);
                break;
            case "setbounds":
                setBounds(player, args);
                break;
            case "list":
                listArenas(player);
                break;
            case "delete":
                deleteArena(player, args);
                break;
            case "tool":
                giveSelectionTool(player);
                break;
            case "onlykit":
                setArenaOnlyKit(player, args);
                break;
            case "build":
                setArenaBuild(player, args);
                break;
            case "world":
                teleportToArenaWorld(player);
                break;
            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void createArena(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /arena create <name>");
            return;
        }

        String name = args[1];
        Location spawn1 = player.getLocation();
        Location spawn2 = player.getLocation().add(10, 0, 0); // Default second spawn

        if (Practice.getInstance().getArenaManager().createArena(name, spawn1, spawn2)) {
            player.sendMessage(ChatColor.GREEN + "Arena '" + name + "' created!");
            player.sendMessage(ChatColor.YELLOW + "Now set the bounds with /arena setbounds " + name + " and second spawn with /arena setspawn " + name + " 2");
        } else {
            player.sendMessage(ChatColor.RED + "An arena with that name already exists!");
        }
    }

    private void setSpawn(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /arena setspawn <name> <1|2>");
            return;
        }

        String name = args[1];
        String spawnNumber = args[2];
        var arena = Practice.getInstance().getArenaManager().getArena(name);

        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Arena not found!");
            return;
        }

        Location spawn = player.getLocation();
        
        // Update spawn location in arena
        if (spawnNumber.equals("1")) {
            arena.setSpawn1(spawn);
            player.sendMessage(ChatColor.GREEN + "Spawn 1 set for arena '" + name + "'!");
        } else if (spawnNumber.equals("2")) {
            arena.setSpawn2(spawn);
            player.sendMessage(ChatColor.GREEN + "Spawn 2 set for arena '" + name + "'!");
        } else {
            player.sendMessage(ChatColor.RED + "Invalid spawn number! Use 1 or 2.");
            return;
        }
        
        // Save the arena
        Practice.getInstance().getArenaManager().saveArenas();
    }

    private void setBounds(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /arena setbounds <name>");
            player.sendMessage(ChatColor.YELLOW + "Use a stick to select positions:");
            player.sendMessage(ChatColor.YELLOW + "- Left-click: Set position 1");
            player.sendMessage(ChatColor.YELLOW + "- Right-click: Set position 2");
            player.sendMessage(ChatColor.YELLOW + "Then use this command to apply the selection.");
            return;
        }

        String name = args[1];
        var arena = Practice.getInstance().getArenaManager().getArena(name);

        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Arena not found!");
            return;
        }

        // Check if player has a selection
        var selectionManager = SelectionManager.getInstance();
        
        if (!selectionManager.hasBothPositions(player)) {
            player.sendMessage(ChatColor.RED + "You need to select both positions first!");
            player.sendMessage(ChatColor.YELLOW + "Use a stick to select positions:");
            player.sendMessage(ChatColor.YELLOW + "- Left-click: Set position 1");
            player.sendMessage(ChatColor.YELLOW + "- Right-click: Set position 2");
            player.sendMessage(ChatColor.YELLOW + "Get the selection tool with /arena tool");
            return;
        }

        Location pos1 = selectionManager.getPos1(player);
        Location pos2 = selectionManager.getPos2(player);

        player.sendMessage(ChatColor.YELLOW + "Setting bounds for arena '" + name + "'...");
        player.sendMessage(ChatColor.YELLOW + "Position 1: " + formatLocation(pos1));
        player.sendMessage(ChatColor.YELLOW + "Position 2: " + formatLocation(pos2));

        if (Practice.getInstance().getArenaManager().setArenaBounds(name, pos1, pos2)) {
            player.sendMessage(ChatColor.GREEN + "Bounds set for arena '" + name + "'!");
            player.sendMessage(ChatColor.YELLOW + "Area: " + calculateVolume(pos1, pos2) + " blocks");
            player.sendMessage(ChatColor.YELLOW + "This area will be regenerated after each match.");
            selectionManager.clearSelection(player);
        } else {
            player.sendMessage(ChatColor.RED + "Failed to set bounds!");
        }
    }

    private String formatLocation(Location location) {
        return String.format("X: %d, Y: %d, Z: %d", 
            location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private String calculateVolume(Location pos1, Location pos2) {
        int width = Math.abs(pos1.getBlockX() - pos2.getBlockX()) + 1;
        int height = Math.abs(pos1.getBlockY() - pos2.getBlockY()) + 1;
        int length = Math.abs(pos1.getBlockZ() - pos2.getBlockZ()) + 1;
        return width + "x" + height + "x" + length;
    }

    private void listArenas(Player player) {
        var arenas = Practice.getInstance().getArenaManager().getArenaNames();
        if (arenas.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No arenas configured.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== Arenas ===");
        for (String name : arenas) {
            var arena = Practice.getInstance().getArenaManager().getArena(name);
            String status;
            if (!arena.isSetupComplete()) {
                status = ChatColor.YELLOW + "Setup Incomplete";
            } else if (arena.isAvailable()) {
                status = ChatColor.GREEN + "Available";
            } else {
                status = ChatColor.RED + "In Use";
            }
            player.sendMessage(ChatColor.YELLOW + "- " + name + " " + status);
        }
    }

    private void deleteArena(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /arena delete <name>");
            return;
        }

        String name = args[1];
        if (Practice.getInstance().getArenaManager().deleteArena(name)) {
            player.sendMessage(ChatColor.GREEN + "Arena '" + name + "' deleted!");
        } else {
            player.sendMessage(ChatColor.RED + "Arena not found!");
        }
    }

    private void setArenaOnlyKit(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /arena onlykit <arenaname> <kitname>");
            player.sendMessage(ChatColor.YELLOW + "Use 'none' as kitname to remove restriction");
            return;
        }

        String arenaName = args[1];
        String kitName = args[2];
        var arena = Practice.getInstance().getArenaManager().getArena(arenaName);

        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Arena not found!");
            return;
        }

        if (kitName.equalsIgnoreCase("none")) {
            if (Practice.getInstance().getArenaManager().removeArenaRestrictedKit(arenaName)) {
                player.sendMessage(ChatColor.GREEN + "Kit restriction removed from arena '" + arenaName + "'!");
                player.sendMessage(ChatColor.YELLOW + "This arena can now be used by any kit.");
            } else {
                player.sendMessage(ChatColor.RED + "Failed to remove kit restriction!");
            }
        } else {
            // Check if kit exists
            var kit = Practice.getInstance().getKitManager().getKit(kitName);
            if (kit == null) {
                player.sendMessage(ChatColor.RED + "Kit '" + kitName + "' not found!");
                return;
            }

            if (Practice.getInstance().getArenaManager().setArenaRestrictedKit(arenaName, kitName)) {
                player.sendMessage(ChatColor.GREEN + "Arena '" + arenaName + "' now restricted to kit '" + kitName + "'!");
                player.sendMessage(ChatColor.YELLOW + "This kit can only be used in this arena and other arenas tagged with this kit.");
            } else {
                player.sendMessage(ChatColor.RED + "Failed to set kit restriction!");
            }
        }
    }

    private void setArenaBuild(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /arena build <arenaname>");
            player.sendMessage(ChatColor.YELLOW + "This arena will be restricted to build kits only and can only be used one match at a time.");
            return;
        }

        String arenaName = args[1];
        var arena = Practice.getInstance().getArenaManager().getArena(arenaName);

        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Arena not found!");
            return;
        }

        // Toggle build arena status
        boolean newBuildArenaStatus = !arena.isBuildArena();
        arena.setBuildArena(newBuildArenaStatus);
        
        // Save the arena
        Practice.getInstance().getArenaManager().saveArenas();

        if (newBuildArenaStatus) {
            player.sendMessage(ChatColor.GREEN + "Arena '" + arenaName + "' is now a build arena!");
            player.sendMessage(ChatColor.YELLOW + "This arena can only be used by build kits and only one match at a time.");
            player.sendMessage(ChatColor.YELLOW + "The arena will be regenerated after each match.");
        } else {
            player.sendMessage(ChatColor.GREEN + "Arena '" + arenaName + "' is no longer a build arena.");
            player.sendMessage(ChatColor.YELLOW + "This arena can now be used by any kit and multiple matches.");
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Arena Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/arena create <name>" + ChatColor.WHITE + " - Create new arena");
        player.sendMessage(ChatColor.YELLOW + "/arena setspawn <name> <1|2>" + ChatColor.WHITE + " - Set spawn point");
        player.sendMessage(ChatColor.YELLOW + "/arena setbounds <name>" + ChatColor.WHITE + " - Set arena bounds");
        player.sendMessage(ChatColor.YELLOW + "/arena onlykit <arenaname> <kitname>" + ChatColor.WHITE + " - Restrict arena to specific kit");
        player.sendMessage(ChatColor.YELLOW + "/arena build <arenaname>" + ChatColor.WHITE + " - Mark arena for build kits only (one match at a time)");
        player.sendMessage(ChatColor.YELLOW + "/arena list" + ChatColor.WHITE + " - List all arenas");
        player.sendMessage(ChatColor.YELLOW + "/arena delete <name>" + ChatColor.WHITE + " - Delete arena");
        player.sendMessage(ChatColor.YELLOW + "/arena tool" + ChatColor.WHITE + " - Get arena selection tool");
        player.sendMessage(ChatColor.YELLOW + "/arena world" + ChatColor.WHITE + " - Teleport to arena world");
    }

    private void giveSelectionTool(Player player) {
        if (!player.hasPermission("practice.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return;
        }
        
        SelectionManager.getInstance().giveSelectionTool(player);
    }

    private void teleportToArenaWorld(Player player) {
        if (!player.hasPermission("practice.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return;
        }
        
        // World manager temporarily disabled due to initialization issues
        player.sendMessage(ChatColor.RED + "Arena world feature is temporarily disabled. Check console for details.");
        /*
        if (Practice.getInstance().getWorldManager().teleportToArenaWorld(player)) {
            // Success message is sent by the world manager
        } else {
            player.sendMessage(ChatColor.RED + "Failed to teleport to arena world!");
        }
        */
    }
}
