package dev.core.squid;

import dev.core.squid.commands.SquidCommand;
import dev.core.squid.listeners.GameListener;
import dev.core.squid.managers.*;
import dev.core.squid.utils.ColorUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class CoreSquid extends JavaPlugin {

    private static CoreSquid instance;

    private LangManager langManager;
    private StatsManager statsManager;
    private ArenaManager arenaManager;
    private GameManager gameManager;
    private ScoreboardManager scoreboardManager;
    private Economy economy;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveResource("lang/es.yml", false);
        saveResource("lang/en.yml", false);

        // Vault
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) economy = rsp.getProvider();
        }

        // Managers (order matters)
        langManager = new LangManager(this);
        statsManager = new StatsManager(this);
        arenaManager = new ArenaManager(this);
        gameManager = new GameManager(this);
        scoreboardManager = new ScoreboardManager(this);

        // Listeners
        getServer().getPluginManager().registerEvents(new GameListener(this), this);

        // Commands
        SquidCommand cmd = new SquidCommand(this);
        getCommand("squid").setExecutor(cmd);
        getCommand("squid").setTabCompleter(cmd);

        // Scoreboard update task
        int interval = getConfig().getInt("scoreboard.update-interval", 20);
        getServer().getScheduler().runTaskTimer(this, scoreboardManager::update, interval, interval);

        // Startup log
        getLogger().info("╔════════════════════════════════╗");
        getLogger().info("║   CoreSquid v1.0.0  ONLINE     ║");
        getLogger().info("║   Lang: " + String.format("%-25s", langManager.getLang()) + "║");
        getLogger().info("║   Vault: " + String.format("%-24s", economy != null ? "OK" : "Not found") + "║");
        getLogger().info("║   Arenas: " + String.format("%-23s", arenaManager.getAll().size() + " configured") + "║");
        getLogger().info("╚════════════════════════════════╝");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) gameManager.forceStop();
        if (statsManager != null) statsManager.save();
        if (scoreboardManager != null) scoreboardManager.removeAll();
        getLogger().info("CoreSquid disabled.");
    }

    // ── Getters ───────────────────────────────────────────────────

    public static CoreSquid getInstance() { return instance; }
    public LangManager getLang() { return langManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public GameManager getGameManager() { return gameManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public Economy getEconomy() { return economy; }

    public String getPrefijo() {
        return ColorUtils.color(langManager.getRaw("prefix"));
    }
}
