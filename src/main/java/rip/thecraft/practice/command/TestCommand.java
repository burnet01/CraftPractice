package rip.thecraft.practice.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rip.thecraft.practice.Practice;
import rip.thecraft.practice.match.Match;
import rip.thecraft.practice.match.MatchManager;

public class TestCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("practice.test")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§6Practice Test Commands:");
            sender.sendMessage("§e/test debug §7- Show match debug information");
            sender.sendMessage("§e/test check <player> §7- Check if player is in match");
            sender.sendMessage("§e/test forceend <player> §7- Force end player's match");
            return true;
        }

        MatchManager matchManager = Practice.getInstance().getMatchManager();
        
        switch (args[0].toLowerCase()) {
            case "debug":
                matchManager.debugActiveMatches();
                sender.sendMessage("§aDebug information printed to console!");
                break;
                
            case "check":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /test check <player>");
                    return true;
                }
                
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found!");
                    return true;
                }
                
                Match match = matchManager.getPlayerMatch(target.getUniqueId());
                if (match != null) {
                    sender.sendMessage("§a" + target.getName() + " is in a match!");
                    sender.sendMessage("§7Match started: " + match.isStarted());
                    sender.sendMessage("§7Arena: " + match.getArena().getName());
                    sender.sendMessage("§7Duration: " + (match.getDuration() / 1000) + "s");
                    
                    Player player1 = Bukkit.getPlayer(match.getPlayer1());
                    Player player2 = Bukkit.getPlayer(match.getPlayer2());
                    sender.sendMessage("§7Players: " + 
                        (player1 != null ? player1.getName() : match.getPlayer1()) + " vs " + 
                        (player2 != null ? player2.getName() : match.getPlayer2()));
                } else {
                    sender.sendMessage("§c" + target.getName() + " is not in a match.");
                }
                break;
                
            case "forceend":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /test forceend <player>");
                    return true;
                }
                
                Player targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer == null) {
                    sender.sendMessage("§cPlayer not found!");
                    return true;
                }
                
                Match playerMatch = matchManager.getPlayerMatch(targetPlayer.getUniqueId());
                if (playerMatch != null) {
                    matchManager.endMatch(playerMatch, null);
                    sender.sendMessage("§aForce ended match for " + targetPlayer.getName());
                } else {
                    sender.sendMessage("§c" + targetPlayer.getName() + " is not in a match.");
                }
                break;
                
            default:
                sender.sendMessage("§cUnknown test command!");
                break;
        }
        
        return true;
    }
}
