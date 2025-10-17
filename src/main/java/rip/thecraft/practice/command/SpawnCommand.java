package rip.thecraft.practice.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rip.thecraft.practice.Practice;
import rip.thecraft.practice.player.PlayerData;
import rip.thecraft.practice.player.PlayerState;
import rip.thecraft.practice.util.MessageManager;

public class SpawnCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageManager.getInstance().sendPlayerOnly(sender);
            return true;
        }

        Player player = (Player) sender;
        PlayerData playerData = Practice.getInstance().getPlayerManager().getPlayerData(player);
        
        if (playerData == null) {
            MessageManager.getInstance().sendMessage(player, "spawn.playerdata.error");
            return true;
        }

        // Check if player is in a match
        if (playerData.getState() == PlayerState.MATCH) {
            if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
                MessageManager.getInstance().sendMessage(player, "spawn.state.invalid");
            }
            return true;
        }

        // Teleport to spawn
        Practice.getInstance().getMatchManager().teleportToPracticeSpawn(player);
        
        // Reset player state to LOBBY if they're not spectating
        if (playerData.getState() != PlayerState.SPECTATING) {
            playerData.setState(PlayerState.LOBBY);
        }
        
        // Use PaperAPI for Folia compatibility - run on entity scheduler
        rip.thecraft.practice.util.PaperAPI.runForEntity(Practice.getInstance(), player, () -> {
            // Clear inventory and give spawn items
            player.getInventory().clear();
            Practice.getInstance().getItemManager().giveSpawnItems(player);
            
            // Reset player stats
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(5.0f);
            player.setFireTicks(0);
            player.setFallDistance(0);
            
            // Clear potion effects
            for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
        });
        
        // Update scoreboard
        Practice.getInstance().getScoreboardService().forceUpdatePlayerScoreboard(player);
        
        if (Practice.getInstance().getSettingsManager().hasMessagesEnabled(player)) {
            MessageManager.getInstance().sendMessage(player, "spawn.success");
        }
        
        return true;
    }
}
