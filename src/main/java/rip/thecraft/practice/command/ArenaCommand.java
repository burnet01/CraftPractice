package rip.thecraft.practice.command;


import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rip.thecraft.practice.Practice;
import rip.thecraft.practice.arena.SelectionManager;
import rip.thecraft.practice.util.MessageManager;

import java.util.HashMap;
import java.util.Map;

public class ArenaCommand implements CommandExecutor {

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
            Map<String, String> usagePlaceholders = new HashMap<>();
            usagePlaceholders.put("usage", "/arena create <name>");
            MessageManager.getInstance().sendMessage(player, "arena.create.usage", usagePlaceholders);
            return;
        }

        String name = args[1];
        Location spawn1 = player.getLocation();
        Location spawn2 = player.getLocation().add(10, 0, 0); // Default second spawn

        if (Practice.getInstance().getArenaManager().createArena(name, spawn1, spawn2)) {
            Map<String, String> successPlaceholders = new HashMap<>();
            successPlaceholders.put("arena", name);
            MessageManager.getInstance().sendMessage(player, "arena.create.success", successPlaceholders);
            
            Map<String, String> instructionsPlaceholders = new HashMap<>();
            instructionsPlaceholders.put("arena", name);
            MessageManager.getInstance().sendMessage(player, "arena.create.instructions", instructionsPlaceholders);
        } else {
            Map<String, String> errorPlaceholders = new HashMap<>();
            errorPlaceholders.put("arena", name);
            MessageManager.getInstance().sendMessage(player, "arena.create.exists", errorPlaceholders);
        }
    }

    private void setSpawn(Player player, String[] args) {
        if (args.length < 3) {
            Map<String, String> usagePlaceholders = new HashMap<>();
            usagePlaceholders.put("usage", "/arena setspawn <name> <1|2>");
            MessageManager.getInstance().sendMessage(player, "arena.setspawn.usage", usagePlaceholders);
            return;
        }

        String name = args[1];
        String spawnNumber = args[2];
        var arena = Practice.getInstance().getArenaManager().getArena(name);

        if (arena == null) {
            Map<String, String> errorPlaceholders = new HashMap<>();
            errorPlaceholders.put("arena", name);
            MessageManager.getInstance().sendMessage(player, "arena.notfound", errorPlaceholders);
            return;
        }

        Location spawn = player.getLocation();
        
        // Update spawn location in arena
        if (spawnNumber.equals("1")) {
            arena.setSpawn1(spawn);
            Map<String, String> successPlaceholders = new HashMap<>();
            successPlaceholders.put("arena", name);
            successPlaceholders.put("spawn", "1");
            MessageManager.getInstance().sendMessage(player, "arena.setspawn.success", successPlaceholders);
        } else if (spawnNumber.equals("2")) {
            arena.setSpawn2(spawn);
            Map<String, String> successPlaceholders = new HashMap<>();
            successPlaceholders.put("arena", name);
            successPlaceholders.put("spawn", "2");
            MessageManager.getInstance().sendMessage(player, "arena.setspawn.success", successPlaceholders);
        } else {
            MessageManager.getInstance().sendMessage(player, "arena.setspawn.invalid");
            return;
        }
        
        // Save the arena
        Practice.getInstance().getArenaManager().saveArenas();
    }

    private void setBounds(Player player, String[] args) {
        if (args.length < 2) {
            Map<String, String> usagePlaceholders = new HashMap<>();
            usagePlaceholders.put("usage", "/arena setbounds <name>");
            MessageManager.getInstance().sendMessage(player, "arena.setbounds.usage", usagePlaceholders);
            MessageManager.getInstance().sendMessage(player, "arena.setbounds.instructions");
            return;
        }

        String name = args[1];
        var arena = Practice.getInstance().getArenaManager().getArena(name);

        if (arena == null) {
            Map<String, String> errorPlaceholders = new HashMap<>();
            errorPlaceholders.put("arena", name);
            MessageManager.getInstance().sendMessage(player, "arena.notfound", errorPlaceholders);
            return;
        }

        // Check if player has a selection
        var selectionManager = SelectionManager.getInstance();
        
        if (!selectionManager.hasBothPositions(player)) {
            MessageManager.getInstance().sendMessage(player, "arena.setbounds.noselection");
            MessageManager.getInstance().sendMessage(player, "arena.setbounds.instructions");
            MessageManager.getInstance().sendMessage(player, "arena.setbounds.tool");
            return;
        }

        Location pos1 = selectionManager.getPos1(player);
        Location pos2 = selectionManager.getPos2(player);

        Map<String, String> progressPlaceholders = new HashMap<>();
        progressPlaceholders.put("arena", name);
        MessageManager.getInstance().sendMessage(player, "arena.setbounds.progress", progressPlaceholders);
        
        Map<String, String> pos1Placeholders = new HashMap<>();
        pos1Placeholders.put("position", "1");
        pos1Placeholders.put("location", formatLocation(pos1));
        MessageManager.getInstance().sendMessage(player, "arena.setbounds.position", pos1Placeholders);
        
        Map<String, String> pos2Placeholders = new HashMap<>();
        pos2Placeholders.put("position", "2");
        pos2Placeholders.put("location", formatLocation(pos2));
        MessageManager.getInstance().sendMessage(player, "arena.setbounds.position", pos2Placeholders);

        if (Practice.getInstance().getArenaManager().setArenaBounds(name, pos1, pos2)) {
            Map<String, String> successPlaceholders = new HashMap<>();
            successPlaceholders.put("arena", name);
            MessageManager.getInstance().sendMessage(player, "arena.setbounds.success", successPlaceholders);
            
            Map<String, String> areaPlaceholders = new HashMap<>();
            areaPlaceholders.put("area", calculateVolume(pos1, pos2));
            MessageManager.getInstance().sendMessage(player, "arena.setbounds.area", areaPlaceholders);
            
            MessageManager.getInstance().sendMessage(player, "arena.setbounds.regeneration");
            selectionManager.clearSelection(player);
        } else {
            MessageManager.getInstance().sendMessage(player, "arena.setbounds.failed");
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
            MessageManager.getInstance().sendMessage(player, "arena.list.empty");
            return;
        }

        MessageManager.getInstance().sendMessage(player, "arena.list.header");
        for (String name : arenas) {
            var arena = Practice.getInstance().getArenaManager().getArena(name);
            String statusKey;
            if (!arena.isSetupComplete()) {
                statusKey = "arena.list.status.incomplete";
            } else if (arena.isAvailable()) {
                statusKey = "arena.list.status.available";
            } else {
                statusKey = "arena.list.status.inuse";
            }
            
            Map<String, String> arenaPlaceholders = new HashMap<>();
            arenaPlaceholders.put("arena", name);
            arenaPlaceholders.put("status", MessageManager.getInstance().getMessage(statusKey));
            MessageManager.getInstance().sendMessage(player, "arena.list.entry", arenaPlaceholders);
        }
    }

    private void deleteArena(Player player, String[] args) {
        if (args.length < 2) {
            Map<String, String> usagePlaceholders = new HashMap<>();
            usagePlaceholders.put("usage", "/arena delete <name>");
            MessageManager.getInstance().sendMessage(player, "arena.delete.usage", usagePlaceholders);
            return;
        }

        String name = args[1];
        if (Practice.getInstance().getArenaManager().deleteArena(name)) {
            Map<String, String> successPlaceholders = new HashMap<>();
            successPlaceholders.put("arena", name);
            MessageManager.getInstance().sendMessage(player, "arena.delete.success", successPlaceholders);
        } else {
            Map<String, String> errorPlaceholders = new HashMap<>();
            errorPlaceholders.put("arena", name);
            MessageManager.getInstance().sendMessage(player, "arena.notfound", errorPlaceholders);
        }
    }

    private void setArenaOnlyKit(Player player, String[] args) {
        if (args.length < 3) {
            Map<String, String> usagePlaceholders = new HashMap<>();
            usagePlaceholders.put("usage", "/arena onlykit <arenaname> <kitname>");
            MessageManager.getInstance().sendMessage(player, "arena.onlykit.usage", usagePlaceholders);
            MessageManager.getInstance().sendMessage(player, "arena.onlykit.none");
            return;
        }

        String arenaName = args[1];
        String kitName = args[2];
        var arena = Practice.getInstance().getArenaManager().getArena(arenaName);

        if (arena == null) {
            Map<String, String> errorPlaceholders = new HashMap<>();
            errorPlaceholders.put("arena", arenaName);
            MessageManager.getInstance().sendMessage(player, "arena.notfound", errorPlaceholders);
            return;
        }

        if (kitName.equalsIgnoreCase("none")) {
            if (Practice.getInstance().getArenaManager().removeArenaRestrictedKit(arenaName)) {
                Map<String, String> successPlaceholders = new HashMap<>();
                successPlaceholders.put("arena", arenaName);
                MessageManager.getInstance().sendMessage(player, "arena.onlykit.remove.success", successPlaceholders);
                MessageManager.getInstance().sendMessage(player, "arena.onlykit.remove.info");
            } else {
                MessageManager.getInstance().sendMessage(player, "arena.onlykit.remove.failed");
            }
        } else {
            // Check if kit exists
            var kit = Practice.getInstance().getKitManager().getKit(kitName);
            if (kit == null) {
                Map<String, String> errorPlaceholders = new HashMap<>();
                errorPlaceholders.put("kit", kitName);
                MessageManager.getInstance().sendMessage(player, "kit.notfound", errorPlaceholders);
                return;
            }

            if (Practice.getInstance().getArenaManager().setArenaRestrictedKit(arenaName, kitName)) {
                Map<String, String> successPlaceholders = new HashMap<>();
                successPlaceholders.put("arena", arenaName);
                successPlaceholders.put("kit", kitName);
                MessageManager.getInstance().sendMessage(player, "arena.onlykit.set.success", successPlaceholders);
                MessageManager.getInstance().sendMessage(player, "arena.onlykit.set.info");
            } else {
                MessageManager.getInstance().sendMessage(player, "arena.onlykit.set.failed");
            }
        }
    }

    private void setArenaBuild(Player player, String[] args) {
        if (args.length < 2) {
            Map<String, String> usagePlaceholders = new HashMap<>();
            usagePlaceholders.put("usage", "/arena build <arenaname>");
            MessageManager.getInstance().sendMessage(player, "arena.build.usage", usagePlaceholders);
            MessageManager.getInstance().sendMessage(player, "arena.build.info");
            return;
        }

        String arenaName = args[1];
        var arena = Practice.getInstance().getArenaManager().getArena(arenaName);

        if (arena == null) {
            Map<String, String> errorPlaceholders = new HashMap<>();
            errorPlaceholders.put("arena", arenaName);
            MessageManager.getInstance().sendMessage(player, "arena.notfound", errorPlaceholders);
            return;
        }

        // Toggle build arena status
        boolean newBuildArenaStatus = !arena.isBuildArena();
        arena.setBuildArena(newBuildArenaStatus);
        
        // Save the arena
        Practice.getInstance().getArenaManager().saveArenas();

        if (newBuildArenaStatus) {
            Map<String, String> successPlaceholders = new HashMap<>();
            successPlaceholders.put("arena", arenaName);
            MessageManager.getInstance().sendMessage(player, "arena.build.enabled", successPlaceholders);
            MessageManager.getInstance().sendMessage(player, "arena.build.enabled.info");
            MessageManager.getInstance().sendMessage(player, "arena.build.regeneration");
        } else {
            Map<String, String> successPlaceholders = new HashMap<>();
            successPlaceholders.put("arena", arenaName);
            MessageManager.getInstance().sendMessage(player, "arena.build.disabled", successPlaceholders);
            MessageManager.getInstance().sendMessage(player, "arena.build.disabled.info");
        }
    }

    private void sendHelp(Player player) {
        MessageManager.getInstance().sendMessage(player, "arena.help.header");
        MessageManager.getInstance().sendMessage(player, "arena.help.create");
        MessageManager.getInstance().sendMessage(player, "arena.help.setspawn");
        MessageManager.getInstance().sendMessage(player, "arena.help.setbounds");
        MessageManager.getInstance().sendMessage(player, "arena.help.onlykit");
        MessageManager.getInstance().sendMessage(player, "arena.help.build");
        MessageManager.getInstance().sendMessage(player, "arena.help.list");
        MessageManager.getInstance().sendMessage(player, "arena.help.delete");
        MessageManager.getInstance().sendMessage(player, "arena.help.tool");
        MessageManager.getInstance().sendMessage(player, "arena.help.world");
    }

    private void giveSelectionTool(Player player) {
        if (!player.hasPermission("practice.admin")) {
            MessageManager.getInstance().sendNoPermission(player);
            return;
        }
        
        SelectionManager.getInstance().giveSelectionTool(player);
    }

    private void teleportToArenaWorld(Player player) {
        if (!player.hasPermission("practice.admin")) {
            MessageManager.getInstance().sendNoPermission(player);
            return;
        }
        
        // World manager temporarily disabled due to initialization issues
        MessageManager.getInstance().sendMessage(player, "arena.world.disabled");
        /*
        if (Practice.getInstance().getWorldManager().teleportToArenaWorld(player)) {
            // Success message is sent by the world manager
        } else {
            MessageManager.getInstance().sendMessage(player, "arena.world.failed");
        }
        */
    }
}
