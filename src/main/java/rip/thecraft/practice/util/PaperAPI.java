package rip.thecraft.practice.util;

import org.bukkit.Bukkit;

/**
 * Utility class for detecting Paper API features and providing fallbacks
 * for Folia compatibility on ShreddedPaper servers.
 */
public class PaperAPI {
    
    /**
     * Check if the server supports Paper's region scheduler (Folia)
     * @return true if region scheduler is available
     */
    public static boolean hasRegionScheduler() {
        try {
            Bukkit.class.getMethod("getRegionScheduler");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
    
    /**
     * Check if the server supports global region scheduler (Folia)
     * @return true if global region scheduler is available
     */
    public static boolean hasGlobalRegionScheduler() {
        try {
            Bukkit.class.getMethod("getGlobalRegionScheduler");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
    
    /**
     * Check if the server is running Folia
     * @return true if Folia is detected
     */
    public static boolean isFolia() {
        return hasGlobalRegionScheduler() || hasRegionScheduler();
    }
    
    /**
     * Check if the server supports entity scheduler (Folia)
     * @return true if entity scheduler is available
     */
    public static boolean hasEntityScheduler() {
        try {
            org.bukkit.entity.Entity.class.getMethod("getScheduler");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
    
    /**
     * Check if the server supports async teleport
     * @return true if teleportAsync is available
     */
    public static boolean hasAsyncTeleport() {
        try {
            org.bukkit.entity.Entity.class.getMethod("teleportAsync", org.bukkit.Location.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
    
    /**
     * Execute a task on the region scheduler if available, otherwise use the global scheduler
     * @param plugin The plugin instance
     * @param location The location to run the task at
     * @param task The task to run
     */
    public static void runAtLocation(org.bukkit.plugin.Plugin plugin, org.bukkit.Location location, Runnable task) {
        if (hasRegionScheduler()) {
            Bukkit.getRegionScheduler().run(plugin, location, scheduledTask -> task.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
    
    /**
     * Execute a task on the region scheduler with delay if available, otherwise use the global scheduler
     * @param plugin The plugin instance
     * @param location The location to run the task at
     * @param task The task to run
     * @param delayTicks The delay in ticks
     */
    public static void runAtLocationLater(org.bukkit.plugin.Plugin plugin, org.bukkit.Location location, Runnable task, long delayTicks) {
        if (hasRegionScheduler()) {
            Bukkit.getRegionScheduler().runDelayed(plugin, location, scheduledTask -> task.run(), delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }
    
    /**
     * Execute a task on the entity scheduler if available, otherwise use the global scheduler
     * @param plugin The plugin instance
     * @param entity The entity to run the task for
     * @param task The task to run
     */
    public static void runForEntity(org.bukkit.plugin.Plugin plugin, org.bukkit.entity.Entity entity, Runnable task) {
        if (hasEntityScheduler()) {
            entity.getScheduler().run(plugin, scheduledTask -> task.run(), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
    
    /**
     * Teleport an entity asynchronously if available, otherwise use sync teleport
     * @param entity The entity to teleport
     * @param location The location to teleport to
     * @return CompletableFuture if async, null if sync
     */
    public static java.util.concurrent.CompletableFuture<Boolean> teleportAsync(org.bukkit.entity.Entity entity, org.bukkit.Location location) {
        if (hasAsyncTeleport()) {
            return entity.teleportAsync(location);
        } else {
            entity.teleport(location);
            return java.util.concurrent.CompletableFuture.completedFuture(true);
        }
    }
}
