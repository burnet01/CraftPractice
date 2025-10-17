package rip.thecraft.practice.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rip.thecraft.practice.Practice;
import rip.thecraft.practice.match.Match;
import rip.thecraft.practice.match.MatchManager;
import rip.thecraft.practice.player.PlayerData;
import rip.thecraft.practice.player.PlayerState;
import rip.thecraft.practice.tournament.Tournament;
import rip.thecraft.practice.tournament.TournamentMatch;

import java.util.UUID;

public class SpectateCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by players.");
            return true;
        }

        Player player = (Player) sender;
        PlayerData playerData = Practice.getInstance().getPlayerManager().getPlayerData(player);
        
        if (playerData == null) {
            player.sendMessage(ChatColor.RED + "Failed to load your player data.");
            return true;
        }

        // Check if player is already spectating
        if (playerData.getState() == PlayerState.SPECTATING) {
            leaveSpectate(player, playerData);
            return true;
        }

        // Check if player is in a match or queue
        if (playerData.getState() != PlayerState.LOBBY) {
            player.sendMessage(ChatColor.RED + "You cannot spectate while in a match or queue.");
            return true;
        }

        if (args.length == 0) {
            // Show help message and active matches
            showHelpAndActiveMatches(player);
            return true;
        }

        // Try to spectate specific player
        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player '" + targetName + "' not found or is offline.");
            return true;
        }

        MatchManager matchManager = Practice.getInstance().getMatchManager();
        Match match = matchManager.getPlayerMatch(target.getUniqueId());
        
        if (match == null || !match.isStarted()) {
            player.sendMessage(ChatColor.RED + "That player is not in an active match.");
            return true;
        }

        // Start spectating
        startSpectate(player, playerData, match, args);
        return true;
    }

    private void showHelpAndActiveMatches(Player player) {
        // Show help message
        player.sendMessage(ChatColor.GOLD + "=== Spectate Command ===");
        player.sendMessage(ChatColor.YELLOW + "/spec <player>" + ChatColor.GRAY + " - Spectate a specific player's match");
        player.sendMessage(ChatColor.YELLOW + "/spec" + ChatColor.GRAY + " - Stop spectating (while in spectator mode)");
        player.sendMessage("");
        
        MatchManager matchManager = Practice.getInstance().getMatchManager();
        var activeMatches = matchManager.getAllActiveMatches();
        
        if (activeMatches.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "There are no active matches to spectate.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== Active Matches ===");
        for (Match match : activeMatches) {
            if (!match.isStarted()) continue;
            
            Player player1 = Bukkit.getPlayer(match.getPlayer1());
            Player player2 = Bukkit.getPlayer(match.getPlayer2());
            
            if (player1 != null && player2 != null) {
                String kitName = match.getKit() != null ? match.getKit().getName() : "Unknown";
                String matchType = match.getType() != null ? match.getType().name() : "Unknown";
                
                player.sendMessage(ChatColor.YELLOW + "• " + player1.getName() + " vs " + player2.getName() + 
                    ChatColor.GRAY + " - " + kitName + " (" + matchType + ")");
            }
        }
        player.sendMessage(ChatColor.GRAY + "Use /spec <player> to spectate a specific match.");
    }

    private void startSpectate(Player spectator, PlayerData spectatorData, Match match, String[] args) {
        Player player1 = Bukkit.getPlayer(match.getPlayer1());
        Player player2 = Bukkit.getPlayer(match.getPlayer2());
        
        if (player1 == null || player2 == null) {
            spectator.sendMessage(ChatColor.RED + "One or both players in the match are no longer online.");
            return;
        }

        // Check if this is a tournament match
        Tournament tournament = findTournamentForMatch(match);
        if (tournament != null) {
            spectatorData.setSpectatingTournament(tournament);
        }

        // Use PaperAPI for Folia compatibility - run on entity scheduler
        rip.thecraft.practice.util.PaperAPI.runForEntity(Practice.getInstance(), spectator, () -> {
            // Clear lobby items before starting to spectate
            spectator.getInventory().clear();
        });

        // Set spectator state
        spectatorData.setState(PlayerState.SPECTATING);
        spectatorData.setSpectatingMatch(match);
        
        // Update scoreboard for state change
        Practice.getInstance().getScoreboardService().forceUpdatePlayerScoreboard(spectator);

        // Set spectator to survival mode but with flight enabled
        spectator.setGameMode(org.bukkit.GameMode.SURVIVAL);
        spectator.setAllowFlight(true);
        spectator.setFlying(true);
        
        // Teleport spectator to the player they requested to spectate
        String targetName = args[0]; // Get the target player name from command arguments
        Player targetPlayer = Bukkit.getPlayer(targetName);
        if (targetPlayer != null && (targetPlayer.equals(player1) || targetPlayer.equals(player2))) {
            Practice.getInstance().getMatchManager().teleportPlayerSafely(spectator, targetPlayer.getLocation());
        } else {
            // Fallback to player1 if target not found
            Practice.getInstance().getMatchManager().teleportPlayerSafely(spectator, player1.getLocation());
        }

        // Setup visibility - hide spectator from players and hide all players from spectator except match players
        setupSpectatorVisibility(spectator, match);

        // Send messages
        String matchInfo = player1.getName() + " vs " + player2.getName();
        if (tournament != null) {
            matchInfo += " (Tournament)";
        }
        spectator.sendMessage(ChatColor.GREEN + "You are now spectating " + matchInfo);
        spectator.sendMessage(ChatColor.GRAY + "Use /spec again to stop spectating.");

        // Update scoreboard
        Practice.getInstance().getScoreboardService().updatePlayerScoreboard(spectator);
    }

    private void leaveSpectate(Player spectator, PlayerData spectatorData) {
        Match spectatingMatch = spectatorData.getSpectatingMatch();
        Tournament spectatingTournament = spectatorData.getSpectatingTournament();
        
        // Reset player state
        spectatorData.setState(PlayerState.LOBBY);
        spectatorData.setSpectatingMatch(null);
        spectatorData.setSpectatingTournament(null);
        
        // Update scoreboard for state change
        Practice.getInstance().getScoreboardService().forceUpdatePlayerScoreboard(spectator);

        // Reset to survival mode
        spectator.setGameMode(org.bukkit.GameMode.SURVIVAL);
        spectator.setAllowFlight(false);
        spectator.setFlying(false);

        // Restore visibility
        restoreSpectatorVisibility(spectator);

        // Teleport back to spawn
        Practice.getInstance().getMatchManager().teleportToPracticeSpawn(spectator);

        // Use PaperAPI for Folia compatibility - run on entity scheduler
        rip.thecraft.practice.util.PaperAPI.runForEntity(Practice.getInstance(), spectator, () -> {
            // Clear inventory and reset stats
            spectator.getInventory().clear();
            spectator.setHealth(20.0);
            spectator.setFoodLevel(20);

            // Give lobby items
            Practice.getInstance().getItemManager().giveSpawnItems(spectator);
        });

        // Send message
        String message = ChatColor.YELLOW + "You are no longer spectating.";
        if (spectatingTournament != null) {
            message += " (Tournament)";
        }
        spectator.sendMessage(message);

        // Update scoreboard
        Practice.getInstance().getScoreboardService().updatePlayerScoreboard(spectator);
    }

    private void setupSpectatorVisibility(Player spectator, Match match) {
        Player player1 = Bukkit.getPlayer(match.getPlayer1());
        Player player2 = Bukkit.getPlayer(match.getPlayer2());
        
        if (player1 == null || player2 == null) return;

        // Hide spectator from all players
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(spectator)) {
                onlinePlayer.hidePlayer(Practice.getInstance(), spectator);
            }
        }

        // Show match players to spectator
        spectator.showPlayer(Practice.getInstance(), player1);
        spectator.showPlayer(Practice.getInstance(), player2);

        // Hide all other players from spectator
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(player1) && !onlinePlayer.equals(player2) && !onlinePlayer.equals(spectator)) {
                spectator.hidePlayer(Practice.getInstance(), onlinePlayer);
            }
        }

        // Hide spectator from match players
        player1.hidePlayer(Practice.getInstance(), spectator);
        player2.hidePlayer(Practice.getInstance(), spectator);
    }

    private void restoreSpectatorVisibility(Player spectator) {
        // Show spectator to all players
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(spectator)) {
                onlinePlayer.showPlayer(Practice.getInstance(), spectator);
            }
        }

        // Show all players to spectator
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            spectator.showPlayer(Practice.getInstance(), onlinePlayer);
        }
    }

    /**
     * Find the tournament that contains the given match
     */
    private Tournament findTournamentForMatch(Match match) {
        for (Tournament tournament : Practice.getInstance().getTournamentManager().getActiveTournaments()) {
            for (TournamentMatch tournamentMatch : tournament.getActiveMatches()) {
                if (tournamentMatch.getMatch() != null && tournamentMatch.getMatch().equals(match)) {
                    return tournament;
                }
            }
        }
        return null;
    }
}
