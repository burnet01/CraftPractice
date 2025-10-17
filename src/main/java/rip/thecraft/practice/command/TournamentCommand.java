package rip.thecraft.practice.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import rip.thecraft.practice.kit.Kit;
import rip.thecraft.practice.kit.KitManager;
import rip.thecraft.practice.tournament.Tournament;
import rip.thecraft.practice.tournament.TournamentMatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TournamentCommand implements CommandExecutor {

    private final Random random = new Random();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                handleStart(sender, args);
                break;
            case "end":
                handleEnd(sender, args);
                break;
            case "join":
                handleJoin(sender, args);
                break;
            case "spec":
                handleSpectate(sender, args);
                break;
            case "reward":
                handleReward(sender, args);
                break;
            default:
                sendUsage(sender);
                break;
        }

        return true;
    }

    private void handleStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("practice.tournament.start")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to start tournaments!");
            return;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by players!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /tournament start <kitname> [maxPlayers] [requiredPlayers]");
            return;
        }

        Player player = (Player) sender;
        String kitName = args[1];
        KitManager kitManager = rip.thecraft.practice.Practice.getInstance().getKitManager();
        Kit kit = kitManager.getKit(kitName);

        if (kit == null) {
            player.sendMessage(ChatColor.RED + "Kit '" + kitName + "' not found!");
            return;
        }

        // Check if kit is a build kit
        if (kit.isBuildMode()) {
            player.sendMessage(ChatColor.RED + "Build kits cannot be used for tournaments!");
            return;
        }

        int maxPlayers = 32;
        int requiredPlayers = 8;

        if (args.length >= 3) {
            try {
                maxPlayers = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid max players number!");
                return;
            }
        }

        if (args.length >= 4) {
            try {
                requiredPlayers = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid required players number!");
                return;
            }
        }

        // Create tournament
        Tournament tournament = rip.thecraft.practice.Practice.getInstance().getTournamentManager().createTournament(
            kit, player, maxPlayers, requiredPlayers, getPrizeCommands()
        );

        if (tournament != null) {
            // Add host as first participant
            tournament.addParticipant(player);
            
            // Broadcast tournament start
            String message = ChatColor.GOLD + "=== TOURNAMENT STARTING ===" +
                "\n" + ChatColor.YELLOW + "Kit: " + ChatColor.WHITE + kit.getDisplayName() +
                "\n" + ChatColor.YELLOW + "Host: " + ChatColor.WHITE + player.getName() +
                "\n" + ChatColor.YELLOW + "Players: " + ChatColor.WHITE + "1/" + maxPlayers +
                "\n" + ChatColor.YELLOW + "Click to join!" +
                "\n" + ChatColor.GOLD + "==========================";
            
            // Create clickable join message
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.sendMessage(message);
                sendClickableJoinMessage(onlinePlayer, kitName);
            }
            
            player.sendMessage(ChatColor.GREEN + "Tournament created successfully!");
        }
    }

    private void handleEnd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("practice.tournament.end")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to end tournaments!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /tournament end <kitname>");
            return;
        }

        String kitName = args[1];
        rip.thecraft.practice.Practice.getInstance().getTournamentManager().endTournament(kitName);
        sender.sendMessage(ChatColor.GREEN + "Tournament for " + kitName + " has been ended!");
    }

    private void handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by players!");
            return;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /tournament join <kitname>");
            return;
        }

        String kitName = args[1];
        boolean success = rip.thecraft.practice.Practice.getInstance().getTournamentManager().joinTournament(player, kitName);
        
        if (!success) {
            player.sendMessage(ChatColor.RED + "Failed to join tournament for " + kitName + "!");
        }
    }

    private void handleSpectate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by players!");
            return;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /tournament spec <kitname|list>");
            return;
        }

        String subCommand = args[1].toLowerCase();

        if (subCommand.equals("list")) {
            handleSpectateList(player, args);
        } else {
            handleSpectateRandom(player, subCommand);
        }
    }

    private void handleSpectateList(Player player, String[] args) {
        Tournament tournament = null;
        
        // Try to find tournament by kit name if provided
        if (args.length >= 3) {
            String kitName = args[2];
            tournament = rip.thecraft.practice.Practice.getInstance().getTournamentManager().getTournamentByKit(kitName);
        } else {
            // Show all active tournaments
            List<Tournament> activeTournaments = rip.thecraft.practice.Practice.getInstance().getTournamentManager().getActiveTournaments();
            if (activeTournaments.isEmpty()) {
                player.sendMessage(ChatColor.RED + "No active tournaments found!");
                return;
            }
            
            player.sendMessage(ChatColor.GOLD + "=== ACTIVE TOURNAMENTS ===");
            for (Tournament t : activeTournaments) {
                player.sendMessage(ChatColor.YELLOW + "- " + t.getKit().getDisplayName() + 
                    " (" + t.getRemainingPlayerCount() + "/" + t.getMaxPlayers() + ", Round " + t.getCurrentRound() + ")");
            }
            player.sendMessage(ChatColor.GOLD + "==========================");
            return;
        }

        if (tournament == null) {
            player.sendMessage(ChatColor.RED + "No active tournament found!");
            return;
        }

        List<TournamentMatch> activeMatches = tournament.getActiveMatches();
        if (activeMatches.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No active matches in this tournament.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== ACTIVE MATCHES ===");
        for (TournamentMatch match : activeMatches) {
            Player p1 = Bukkit.getPlayer(match.getPlayer1());
            Player p2 = Bukkit.getPlayer(match.getPlayer2());
            
            if (p1 != null && p2 != null) {
                player.sendMessage(ChatColor.YELLOW + "- " + p1.getName() + " vs " + p2.getName() + 
                    " (Round " + match.getRound() + ")");
            }
        }
        player.sendMessage(ChatColor.GOLD + "=====================");
    }

    private void handleSpectateRandom(Player player, String kitName) {
        Tournament tournament = rip.thecraft.practice.Practice.getInstance().getTournamentManager().getTournamentByKit(kitName);
        
        if (tournament == null) {
            player.sendMessage(ChatColor.RED + "No active tournament found for " + kitName + "!");
            return;
        }

        List<TournamentMatch> activeMatches = tournament.getActiveMatches();
        if (activeMatches.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No active matches in this tournament.");
            return;
        }

        // Pick a random match
        TournamentMatch randomMatch = activeMatches.get(random.nextInt(activeMatches.size()));
        
        // Spectate the match
        spectateMatch(player, randomMatch);
    }

    private void spectateMatch(Player spectator, TournamentMatch match) {
        Player p1 = Bukkit.getPlayer(match.getPlayer1());
        Player p2 = Bukkit.getPlayer(match.getPlayer2());
        
        if (p1 == null || p2 == null) {
            spectator.sendMessage(ChatColor.RED + "Cannot spectate this match - players are not online!");
            return;
        }

        // Set player state
        rip.thecraft.practice.player.PlayerData playerData = rip.thecraft.practice.Practice.getInstance().getPlayerManager().getPlayerData(spectator);
        if (playerData != null) {
            playerData.setState(rip.thecraft.practice.player.PlayerState.SPECTATING);
            playerData.setSpectatingTournament(match.getTournament());
        }

        // Teleport to arena
        rip.thecraft.practice.Practice.getInstance().getMatchManager().teleportPlayerSafely(spectator, 
            match.getMatch().getArena().getSpectatorSpawn());

        // Hide spectator from players
        p1.hidePlayer(rip.thecraft.practice.Practice.getInstance(), spectator);
        p2.hidePlayer(rip.thecraft.practice.Practice.getInstance(), spectator);

        spectator.sendMessage(ChatColor.GREEN + "Now spectating: " + p1.getName() + " vs " + p2.getName());
        spectator.sendMessage(ChatColor.YELLOW + "Use /leave to stop spectating.");
    }

    private void sendClickableJoinMessage(Player player, String kitName) {
        // Create a clickable message component
        TextComponent message = new TextComponent(ChatColor.GREEN + "[" + ChatColor.YELLOW + "Click to Join" + ChatColor.GREEN + "]");
        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tournament join " + kitName));
        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new ComponentBuilder(ChatColor.GREEN + "Click to join the " + kitName + " tournament!").create()));
        
        player.spigot().sendMessage(message);
    }

    private void handleReward(CommandSender sender, String[] args) {
        if (!sender.hasPermission("practice.tournament.reward")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to set tournament rewards!");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /tournament reward <kitname> <command>");
            sender.sendMessage(ChatColor.YELLOW + "Example: /tournament reward nodebuff setrank %p% Admin");
            sender.sendMessage(ChatColor.YELLOW + "Note: %p% will be replaced with the winner's name");
            return;
        }

        String kitName = args[1];
        StringBuilder commandBuilder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            commandBuilder.append(args[i]).append(" ");
        }
        String command = commandBuilder.toString().trim();

        // Store the reward command for this kit
        // This would typically be stored in a config file
        // For now, we'll store it in memory (resets on server restart)
        kitRewards.put(kitName.toLowerCase(), command);
        
        sender.sendMessage(ChatColor.GREEN + "Reward set for " + kitName + " tournament: " + ChatColor.YELLOW + command);
        sender.sendMessage(ChatColor.YELLOW + "This reward will be applied to the next " + kitName + " tournament winner.");
        sender.sendMessage(ChatColor.YELLOW + "Note: %p% will be replaced with the winner's name");
    }

    private List<String> getPrizeCommands() {
        // Get rewards from the kitRewards map
        List<String> prizes = new ArrayList<>();
        
        // Add all configured rewards
        for (Map.Entry<String, String> entry : kitRewards.entrySet()) {
            prizes.add(entry.getValue());
        }
        
        return prizes;
    }
    
    // Map to store kit rewards (kit name -> reward command)
    private final Map<String, String> kitRewards = new HashMap<>();

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Tournament Commands ===");
        if (sender.hasPermission("practice.tournament.start")) {
            sender.sendMessage(ChatColor.YELLOW + "/tournament start <kitname> [maxPlayers] [requiredPlayers]");
        }
        if (sender.hasPermission("practice.tournament.end")) {
            sender.sendMessage(ChatColor.YELLOW + "/tournament end <kitname>");
        }
        if (sender.hasPermission("practice.tournament.reward")) {
            sender.sendMessage(ChatColor.YELLOW + "/tournament reward <kitname> <command>");
        }
        sender.sendMessage(ChatColor.YELLOW + "/tournament join <kitname>");
        sender.sendMessage(ChatColor.YELLOW + "/tournament spec <kitname>");
        sender.sendMessage(ChatColor.YELLOW + "/tournament spec list [kitname]");
        sender.sendMessage(ChatColor.GOLD + "==========================");
    }
}
