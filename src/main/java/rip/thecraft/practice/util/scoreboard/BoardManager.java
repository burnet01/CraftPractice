package rip.thecraft.practice.util.scoreboard;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import rip.thecraft.practice.Practice;
import rip.thecraft.practice.util.ColorHelper;
import rip.thecraft.practice.util.PaperAPI;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by: ThatKawaiiSam
 * Project: Assemble
 * Optimized for 500+ players with Folia support
 */
@Getter
public class BoardManager {

    private BoardAdapter adapter;
    private Map<UUID, Board> boardMap;
    private Map<UUID, List<String>> cachedLines;
    private Map<UUID, String> cachedTitles;
    private Map<String, String> colorTranslationCache;
    
    // Performance optimizations
    private final org.bukkit.Server server = Bukkit.getServer();
    private final int updateTick;
    private long lastUpdateTime = 0;
    private final int BATCH_SIZE = 50; // Process players in batches

    public BoardManager(BoardAdapter adapter, int updateTick) {
        this.adapter = adapter;
        this.updateTick = Math.max(4, updateTick);
        this.boardMap = new ConcurrentHashMap<>();
        this.cachedLines = new ConcurrentHashMap<>();
        this.cachedTitles = new ConcurrentHashMap<>();
        this.colorTranslationCache = new ConcurrentHashMap<>();

        Bukkit.getPluginManager().registerEvents(new BoardListener(this), Practice.getInstance());
        
        // Use PaperAPI for Folia compatibility - run scoreboard updates on global scheduler
        if (PaperAPI.isFolia()) {
            // Use global region scheduler for Folia - start with 1 tick delay
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                Practice.getInstance(), 
                task -> sendScoreboard(), 
                1, 
                this.updateTick
            );
        } else {
            // Fallback to Bukkit scheduler for non-Folia servers
            Practice.getInstance().getServer().getScheduler().runTaskTimer(
                Practice.getInstance(), 
                this::sendScoreboard, 
                1, 
                this.updateTick
            );
        }
    }

    public void sendScoreboard() {
        // Cache online players to avoid repeated calls
        Collection<? extends Player> onlinePlayers = server.getOnlinePlayers();
        
        // Early exit if no players online
        if (onlinePlayers.isEmpty()) {
            return;
        }

        // Process players in batches to reduce memory pressure
        List<Player> players = new ArrayList<>(onlinePlayers);
        int totalPlayers = players.size();
        
        for (int batchStart = 0; batchStart < totalPlayers; batchStart += BATCH_SIZE) {
            int batchEnd = Math.min(batchStart + BATCH_SIZE, totalPlayers);
            
            for (int i = batchStart; i < batchEnd; i++) {
                Player player = players.get(i);
                UUID playerId = player.getUniqueId();
                Board board = this.boardMap.get(playerId);
                
                // Check if player has scoreboard disabled in settings
                if (Practice.getInstance().getSettingsManager() != null && 
                    !Practice.getInstance().getSettingsManager().getPlayerSettings(player).isScoreboardEnabled()) {
                    // If player has scoreboard disabled, remove from board manager and set blank scoreboard
                    if (board != null) {
                        clearBoard(board);
                        this.boardMap.remove(playerId);
                        this.cachedLines.remove(playerId);
                        this.cachedTitles.remove(playerId);
                    }
                    // Set blank scoreboard and skip processing
                    player.setScoreboard(server.getScoreboardManager().getNewScoreboard());
                    continue;
                }
                
                // Create board if it doesn't exist (safety check)
                if (board == null) {
                    board = new Board(player, this);
                    this.boardMap.put(playerId, board);
                }

                Scoreboard scoreboard = board.getScoreboard();
                Objective objective = board.getObjective();

                // Optimized title handling - minimize display name updates
                String newTitle = this.adapter.getTitle(player);
                String cachedTitle = this.cachedTitles.get(playerId);

                // Only translate and update if title changed
                if (cachedTitle == null || !cachedTitle.equals(newTitle)) {
                    String translatedTitle = getCachedTranslation(newTitle);
                    objective.setDisplayName(translatedTitle);
                    this.cachedTitles.put(playerId, newTitle);
                }

                List<String> newLines = this.adapter.getLines(player);
                List<String> cachedPlayerLines = this.cachedLines.get(playerId);

                // Fast path: skip if lines haven't changed
                if (cachedPlayerLines != null && fastLinesEqual(cachedPlayerLines, newLines)) {
                    continue;
                }

                if (newLines == null || newLines.isEmpty()) {
                    // Clear scoreboard efficiently
                    clearBoard(board);
                    this.cachedLines.put(playerId, Collections.emptyList());
                } else {
                    updateBoard(player, board, newLines);
                    this.cachedLines.put(playerId, new ArrayList<>(newLines));
                }

                // Always ensure player has the correct scoreboard
                if (player.getScoreboard() != scoreboard) {
                    player.setScoreboard(scoreboard);
                }
            }
        }
    }

    private void clearBoard(Board board) {
        board.getEntries().forEach(BoardEntry::quit);
        board.getEntries().clear();
    }

    private void updateBoard(Player player, Board board, List<String> newLines) {
        // Pre-translate all lines at once with caching
        List<String> translatedLines = new ArrayList<>(newLines.size());
        for (String line : newLines) {
            translatedLines.add(getCachedTranslation(line));
        }

        BoardStyle style = this.adapter.getBoardStyle(player);
        boolean descending = style.isDescending();

        if (!descending) {
            Collections.reverse(translatedLines);
        }

        // Remove excess entries more efficiently
        List<BoardEntry> entries = board.getEntries();
        int currentSize = entries.size();
        int targetSize = translatedLines.size();
        
        if (currentSize > targetSize) {
            // Remove entries from the end first (more efficient)
            for (int j = currentSize - 1; j >= targetSize; j--) {
                BoardEntry entry = board.getEntryAtPosition(j);
                if (entry != null) {
                    entry.quit();
                }
            }
            // Trim the entries list
            if (currentSize > targetSize) {
                entries.subList(targetSize, currentSize).clear();
            }
        }

        int cache = style.getStart();
        for (int i = 0; i < translatedLines.size(); i++) {
            BoardEntry entry = board.getEntryAtPosition(i);
            String line = translatedLines.get(i);

            if (entry == null) {
                entry = new BoardEntry(board, line);
                entry.send(descending ? cache-- : cache++);
            } else if (!entry.getText().equals(line)) {
                entry.setText(line);
                entry.send(descending ? cache-- : cache++);
            } else {
                entry.send(descending ? cache-- : cache++);
            }
        }
    }

    // Optimized line comparison - avoids iterator overhead
    private boolean fastLinesEqual(List<String> list1, List<String> list2) {
        if (list1 == list2) return true;
        if (list1 == null || list2 == null || list1.size() != list2.size()) return false;

        int size = list1.size();
        for (int i = 0; i < size; i++) {
            if (!Objects.equals(list1.get(i), list2.get(i))) {
                return false;
            }
        }
        return true;
    }

    public void clearCache(UUID playerId) {
        this.cachedLines.remove(playerId);
        this.cachedTitles.remove(playerId);
    }
    
    // Cached color translation to reduce overhead
    private String getCachedTranslation(String text) {
        if (text == null) return null;
        
        // Check cache first
        String cached = colorTranslationCache.get(text);
        if (cached != null) {
            return cached;
        }
        
        // Translate and cache
        String translated = ColorHelper.translate(text);
        colorTranslationCache.put(text, translated);
        
        // Limit cache size to prevent memory leaks
        if (colorTranslationCache.size() > 1000) {
            // Remove oldest entries (simple FIFO)
            Iterator<String> iterator = colorTranslationCache.keySet().iterator();
            for (int i = 0; i < 100 && iterator.hasNext(); i++) {
                iterator.next();
                iterator.remove();
            }
        }
        
        return translated;
    }
}
