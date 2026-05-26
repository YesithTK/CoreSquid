package dev.core.squid;

import dev.core.squid.commands.SquidCommand;
import dev.core.squid.listeners.GameListener;
import dev.core.squid.managers.GameManager;
import dev.core.squid.managers.ScoreboardManager;
import dev.core.squid.managers.StatsManager;
import dev.core.squid.utils.ColorUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class CoreSquid extends JavaPlugin {

    private static CoreSquid instancia;
    private GameManager gameManager;
    private StatsManager statsManager;
    private ScoreboardManager scoreboardManager;
    private Economy economy;

    @Override
    public void onEnable() {
        instancia = this;
        saveDefaultConfig();

        // Vault
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp =
                    getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) economy = rsp.getProvider();
        }

        statsManager = new StatsManager(this);
        gameManager = new GameManager(this);
        scoreboardManager = new ScoreboardManager(this);

        // Listeners
        getServer().getPluginManager().registerEvents(new GameListener(this), this);

        // Comando
        SquidCommand cmd = new SquidCommand(this);
        getCommand("squid").setExecutor(cmd);
        getCommand("squid").setTabCompleter(cmd);

        // Task de scoreboard (cada 2 segundos)
        getServer().getScheduler().runTaskTimer(this,
                () -> scoreboardManager.actualizar(), 40L, 40L);

        // Resource pack
        if (getConfig().getBoolean("resourcepack-activado", false)) {
            String url = getConfig().getString("resourcepack-url", "");
            if (!url.isEmpty()) {
                getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
                    @org.bukkit.event.EventHandler
                    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
                        event.getPlayer().setResourcePack(url);
                    }
                }, this);
            }
        }

        getLogger().info("CoreSquid v1.0.0 activado!");
        getLogger().info("Vault: " + (economy != null ? "OK" : "No disponible"));
        getLogger().info("Minijuegos cargados: " + gameManager.getJugadoresActivos().size());
    }

    @Override
    public void onDisable() {
        if (gameManager != null) gameManager.forzarDetener();
        if (statsManager != null) statsManager.guardar();
        if (scoreboardManager != null) scoreboardManager.limpiarTodo();
        getLogger().info("CoreSquid desactivado.");
    }

    public static CoreSquid getInstancia() { return instancia; }
    public GameManager getGameManager() { return gameManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public Economy getEconomy() { return economy; }

    public String getPrefijo() {
        return ColorUtils.color(getConfig().getString("mensajes.prefijo", "&8[&cCoreSquid&8] &r"));
    }
}
