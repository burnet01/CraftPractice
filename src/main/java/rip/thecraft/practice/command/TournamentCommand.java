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
import rip.thecraft.practice.util.MessageManager;

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
            MessageManager.getInstance().sendNoPermission(sender);
            return;
        }

        if (!(sender instanceof Player)) {
            MessageManager.getInstance().sendPlayerOnly(sender);
            return;
        }

        if (args.length < 2) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("usage", "/tournament start <kitname> [maxPlayers] [requiredPlayers]");
            MessageManager.getInstance().sendInvalidUsage(sender, "/tournament start <kitname> [maxPlayers] [requiredPlayers]");
            return;
        }

        Player player = (Player) sender;
        String kitName = args[1];
        KitManager kitManager = rip.thecraft.practice.Practice.getInstance().getKitManager();
        Kit kit = kitManager.getKit(kitName);

        if (kit == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("kit", kitName);
            MessageManager.getInstance().sendMessage(sender, "tournament.start.kit-not-found", placeholders);
            return;
        }

        // Check if kit is a build kit
        if (kit.isBuildMode()) {
            MessageManager.getInstance().sendMessage(sender, "tournament.start.build-kit-not-allowed");
            return;
        }

        int maxPlayers = 32;
        int requiredPlayers = 8;

        if (args.length >= 3) {
            try {
                maxPlayers = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                MessageManager.getInstance().sendMessage(sender, "tournament.start.invalid-max-players");
                return;
            }
        }

        if (args.length >= 4) {
            try {
                requiredPlayers = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                MessageManager.getInstance().sendMessage(sender, "tournament.start.invalid-required-players");
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
            Map<String, String> broadcastPlaceholders = new HashMap<>();
            broadcastPlaceholders.put("kit", kit.getDisplayName());
            broadcastPlaceholders.put("host", player.getName());
            broadcastPlaceholders.put("current", "1");
            broadcastPlaceholders.put("max", String.valueOf(maxPlayers));
            
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                MessageManager.getInstance().sendMessage(onlinePlayer, "tournament.start.broadcast", broadcastPlaceholders);
                sendClickableJoinMessage(onlinePlayer, kitName);
            }
            
            MessageManager.getInstance().sendMessage(player, "tournament.start.success");
        }
    }

    private void handleEnd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("practice.tournament.end")) {
            MessageManager.getInstance().sendNoPermission(sender);
            return;
        }

        if (args.length < 2) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("usage", "/tournament end <kitname>");
            MessageManager.getInstance().sendInvalidUsage(sender, "/tournament end <kitname>");
            return;
        }

        String kitName = args[1];
        rip.thecraft.practice.Practice.getInstance().getTournamentManager().endTournament(kitName);
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("kit", kitName);
        MessageManager.getInstance().sendMessage(sender, "tournament.end.success", placeholders);
    }

    private void handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageManager.getInstance().sendPlayerOnly(sender);
            return;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("usage", "/tournament join <kitname>");
            MessageManager.getInstance().sendInvalidUsage(sender, "/tournament join <kitname>");
            return;
        }

        String kitName = args[1];
        boolean success = rip.thecraft.practice.Practice.getInstance().getTournamentManager().joinTournament(player, kitName);
        
        if (!success) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("kit", kitName);
            MessageManager.getInstance().sendMessage(player, "tournament.join.failed", placeholders);
        }
    }

    private void handleSpectate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageManager.getInstance().sendPlayerOnly(sender);
            return;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("usage", "/tournament spec <kitname|list>");
            MessageManager.getInstance().sendInvalidUsage(sender, "/tournament spec <kitname|list>");
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
                MessageManager.getInstance().sendMessage(player, "tournament.spec.list.no-active-tournaments");
                return;
            }
            
            MessageManager.getInstance().sendMessage(player, "tournament.spec.list.header");
            for (Tournament t : activeTournaments) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("kit", t.getKit().getDisplayName());
                placeholders.put("current", String.valueOf(t.getRemainingPlayerCount()));
                placeholders.put("max", String.valueOf(t.getMaxPlayers()));
                placeholders.put("round", String.valueOf(t.getCurrentRound()));
                MessageManager.getInstance().sendMessage(player, "tournament.spec.list.entry", placeholders);
            }
            MessageManager.getInstance().sendMessage(player, "tournament.spec.list.footer");
            return;
        }

        if (tournament == null) {
            MessageManager.getInstance().sendMessage(player, "tournament.spec.list.tournament-not-found");
            return;
        }

        List<TournamentMatch> activeMatches = tournament.getActiveMatches();
        if (activeMatches.isEmpty()) {
            MessageManager.getInstance().sendMessage(player, "tournament.spec.list.no-active-matches");
            return;
        }

        MessageManager.getInstance().sendMessage(player, "tournament.spec.list.matches-header");
        for (TournamentMatch match : activeMatches) {
            Player p1 = Bukkit.getPlayer(match.getPlayer1());
            Player p2 = Bukkit.getPlayer(match.getPlayer2());
            
            if (p1 != null && p2 != null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player1", p1.getName());
                placeholders.put("player2", p2.getName());
                placeholders.put("round", String.valueOf(match.getRound()));
                MessageManager.getInstance().sendMessage(player, "tournament.spec.list.match-entry", placeholders);
            }
        }
        MessageManager.getInstance().sendMessage(player, "tournament.spec.list.matches-footer");
    }

    private void handleSpectateRandom(Player player, String kitName) {
        Tournament tournament = rip.thecraft.practice.Practice.getInstance().getTournamentManager().getTournamentByKit(kitName);
        
        if (tournament == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("kit", kitName);
            MessageManager.getInstance().sendMessage(player, "tournament.spec.random.tournament-not-found", placeholders);
            return;
        }

        List<TournamentMatch> activeMatches = tournament.getActiveMatches();
        if (activeMatches.isEmpty()) {
            MessageManager.getInstance().sendMessage(player, "tournament.spec.random.no-active-matches");
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
            MessageManager.getInstance().sendMessage(spectator, "tournament.spec.spectate.players-offline");
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

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player1", p1.getName());
        placeholders.put("player2", p2.getName());
        MessageManager.getInstance().sendMessage(spectator, "tournament.spec.spectate.now-spectating", placeholders);
        MessageManager.getInstance().sendMessage(spectator, "tournament.spec.spectate.leave-instruction");
    }

    private void sendClickableJoinMessage(Player player, String kitName) {
        // Create a clickable message component
        String clickableText = MessageManager.getInstance().getMessage("tournament.start.clickable-join.text");
        String hoverText = MessageManager.getInstance().getMessage("tournament.start.clickable-join.hover");
        
        TextComponent message = new TextComponent(clickableText);
        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tournament join " + kitName));
        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new ComponentBuilder(hoverText).create()));
        
        player.spigot().sendMessage(message);
    }

    private void handleReward(CommandSender sender, String[] args) {
        if (!sender.hasPermission("practice.tournament.reward")) {
            MessageManager.getInstance().sendNoPermission(sender);
            return;
        }

        if (args.length < 3) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("usage", "/tournament reward <kitname> <command>");
            MessageManager.getInstance().sendInvalidUsage(sender, "/tournament reward <kitname> <command>");
            MessageManager.getInstance().sendMessage(sender, "tournament.reward.example");
            MessageManager.getInstance().sendMessage(sender, "tournament.reward.placeholder-note");
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
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("kit", kitName);
        placeholders.put("command", command);
        MessageManager.getInstance().sendMessage(sender, "tournament.reward.success", placeholders);
        MessageManager.getInstance().sendMessage(sender, "tournament.reward.applied-next");
        MessageManager.getInstance().sendMessage(sender, "tournament.reward.placeholder-note");
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
        MessageManager.getInstance().sendMessage(sender, "tournament.usage.header");
        if (sender.hasPermission("practice.tournament.start")) {
            MessageManager.getInstance().sendMessage(sender, "tournament.usage.start");
        }
        if (sender.hasPermission("practice.tournament.end")) {
            MessageManager.getInstance().sendMessage(sender, "tournament.usage.end");
        }
        if (sender.hasPermission("practice.tournament.reward")) {
            MessageManager.getInstance().sendMessage(sender, "tournament.usage.reward");
        }
        MessageManager.getInstance().sendMessage(sender, "tournament.usage.join");
        MessageManager.getInstance().sendMessage(sender, "tournament.usage.spec");
        MessageManager.getInstance().sendMessage(sender, "tournament.usage.spec-list");
        MessageManager.getInstance().sendMessage(sender, "tournament.usage.footer");
    }
}
