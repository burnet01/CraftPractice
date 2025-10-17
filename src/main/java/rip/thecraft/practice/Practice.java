package rip.thecraft.practice;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.bukkit.plugin.java.JavaPlugin;
import rip.thecraft.practice.arena.ArenaManager;
import rip.thecraft.practice.arena.SelectionManager;
import rip.thecraft.practice.hitdelay.HitDelayHandler;
import rip.thecraft.practice.hitdelay.HitDelayManager;
import rip.thecraft.practice.item.ItemManager;
import rip.thecraft.practice.kit.KitManager;
import rip.thecraft.practice.listener.MatchListener;
import rip.thecraft.practice.match.MatchManager;
import rip.thecraft.practice.player.PlayerManager;
import rip.thecraft.practice.queue.QueueManager;
import rip.thecraft.practice.settings.SettingsManager;
import rip.thecraft.practice.world.WorldManager;

public class Practice extends JavaPlugin {

    private static Practice instance;

    private MongoClient mongoClient;
    private MongoDatabase database;

    private ArenaManager arenaManager;
    private KitManager kitManager;
    private PlayerManager playerManager;
    private QueueManager queueManager;
    private MatchManager matchManager;
    private MatchListener matchListener;
    private rip.thecraft.practice.listener.ArenaListener arenaListener;
    private rip.thecraft.practice.command.BuildCommand buildCommand;
    private rip.thecraft.practice.knockback.KnockbackManager knockbackManager;
    private HitDelayManager hitDelayManager;
    private HitDelayHandler hitDelayHandler;
    private WorldManager worldManager;
    private rip.thecraft.practice.scoreboard.ScoreboardService scoreboardService;
    private ItemManager itemManager;
    private SettingsManager settingsManager;
    private rip.thecraft.practice.tournament.TournamentManager tournamentManager;

    @Override
    public void onEnable() {
        instance = this;

        // Load configuration
        saveDefaultConfig();
        
        // Save scoreboard configuration only if it doesn't exist (don't overwrite)
        saveResource("scoreboard.yml", false);

        // Initialize MongoDB connection
        initializeMongoDB();

        // Initialize managers
        this.arenaManager = new ArenaManager(this);
        this.kitManager = new KitManager(this);
        this.playerManager = new PlayerManager(this);
        this.matchManager = new MatchManager(this);
        this.queueManager = new QueueManager(this);
        this.knockbackManager = new rip.thecraft.practice.knockback.KnockbackManager(this);
        this.hitDelayManager = new HitDelayManager(this);
        this.hitDelayHandler = new HitDelayHandler(this.hitDelayManager);
        
        // Initialize world manager last to avoid conflicts with other managers
        // Temporarily disabled due to initialization issues
        /*
        try {
            this.worldManager = new WorldManager(this);
        } catch (Exception e) {
            getLogger().severe("Failed to initialize WorldManager: " + e.getMessage());
            e.printStackTrace();
        }
        */
        
        this.matchListener = new MatchListener();
        this.arenaListener = new rip.thecraft.practice.listener.ArenaListener();
        this.buildCommand = new rip.thecraft.practice.command.BuildCommand();
        this.itemManager = new ItemManager(this);
        this.settingsManager = new SettingsManager(this);
        this.tournamentManager = new rip.thecraft.practice.tournament.TournamentManager();
        
        // Set up cross-references after all managers are initialized
        this.queueManager.setMatchManager(this.matchManager);
        
        // Initialize selection manager singleton
        rip.thecraft.practice.arena.SelectionManager.initialize();

        // Register commands
        getCommand("practice").setExecutor(new rip.thecraft.practice.command.PracticeCommand());
        getCommand("arena").setExecutor(new rip.thecraft.practice.command.ArenaCommand());
        getCommand("kit").setExecutor(new rip.thecraft.practice.command.KitCommand());
        getCommand("queue").setExecutor(new rip.thecraft.practice.command.QueueCommand());
        getCommand("test").setExecutor(new rip.thecraft.practice.command.TestCommand());
        getCommand("build").setExecutor(this.buildCommand);
        getCommand("kb").setExecutor(new rip.thecraft.practice.command.KnockbackCommand());
        getCommand("hd").setExecutor(new rip.thecraft.practice.command.HitDelayCommand());
        getCommand("leaderboard").setExecutor(new rip.thecraft.practice.command.LeaderboardCommand());
        getCommand("spec").setExecutor(new rip.thecraft.practice.command.SpectateCommand());
        getCommand("settings").setExecutor(new rip.thecraft.practice.command.SettingsCommand());
        getCommand("tournament").setExecutor(new rip.thecraft.practice.command.TournamentCommand());

        // Register listeners
        getServer().getPluginManager().registerEvents(new rip.thecraft.practice.listener.PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new rip.thecraft.practice.listener.ArenaListener(), this);
        getServer().getPluginManager().registerEvents(SelectionManager.getInstance(), this);
        getServer().getPluginManager().registerEvents(this.matchListener, this);
        getServer().getPluginManager().registerEvents(new rip.thecraft.practice.knockback.PracticeKnockbackHandler(), this);
        getServer().getPluginManager().registerEvents(this.itemManager, this);
        getServer().getPluginManager().registerEvents(this.settingsManager, this);
        getServer().getPluginManager().registerEvents(new rip.thecraft.practice.listener.AdvancementListener(), this);

        // Initialize optimized scoreboard service
        this.scoreboardService = new rip.thecraft.practice.scoreboard.ScoreboardService(this);
        


        getLogger().info("Practice plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        if (mongoClient != null) {
            mongoClient.close();
        }

        if (arenaManager != null) {
            arenaManager.shutdown();
        }

        if (queueManager != null) {
            queueManager.shutdown();
        }

        if (matchManager != null) {
            matchManager.shutdown();
        }

        if (knockbackManager != null) {
            knockbackManager.shutdown();
        }

        if (worldManager != null) {
            worldManager.shutdown();
        }

        if (scoreboardService != null) {
            scoreboardService.shutdown();
        }

        if (hitDelayHandler != null) {
            hitDelayHandler.clearAll();
        }

        if (settingsManager != null) {
            settingsManager.shutdown();
        }

        if (tournamentManager != null) {
            tournamentManager.shutdown();
        }

        getLogger().info("Practice plugin has been disabled!");
    }

    private void initializeMongoDB() {
        String connectionString = getConfig().getString("mongodb.uri", "mongodb://localhost:27017");
        String databaseName = getConfig().getString("mongodb.database", "practice");

        try {
            MongoClientURI uri = new MongoClientURI(connectionString);
            this.mongoClient = new MongoClient(uri);
            this.database = mongoClient.getDatabase(databaseName);
            getLogger().info("Successfully connected to MongoDB!");
        } catch (Exception e) {
            getLogger().severe("Failed to connect to MongoDB: " + e.getMessage());
        }
    }

    public static Practice getInstance() {
        return instance;
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }

    public MatchManager getMatchManager() {
        return matchManager;
    }

    public MatchListener getMatchListener() {
        return matchListener;
    }

    public rip.thecraft.practice.listener.ArenaListener getArenaListener() {
        return arenaListener;
    }

    public rip.thecraft.practice.command.BuildCommand getBuildCommand() {
        return buildCommand;
    }

    public rip.thecraft.practice.knockback.KnockbackManager getKnockbackManager() {
        return knockbackManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public rip.thecraft.practice.scoreboard.ScoreboardService getScoreboardService() {
        return scoreboardService;
    }

    public HitDelayManager getHitDelayManager() {
        return hitDelayManager;
    }

    public HitDelayHandler getHitDelayHandler() {
        return hitDelayHandler;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public rip.thecraft.practice.tournament.TournamentManager getTournamentManager() {
        return tournamentManager;
    }
}
