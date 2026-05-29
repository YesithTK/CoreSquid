package dev.core.squid.games;

import dev.core.squid.CoreSquid;
import dev.core.squid.model.ArenaData;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class MiniGame {

    protected final CoreSquid plugin;
    protected boolean active;
    protected ArenaData arenaData;

    public MiniGame(CoreSquid plugin) {
        this.plugin = plugin;
        this.active = false;
    }

    /** Unique game ID (used for arena lookup and config) */
    public abstract String getId();

    /** Display name */
    public abstract String getName();

    /** Short description shown before game starts */
    public abstract String getDescription();

    /** Time limit in seconds */
    public abstract int getTimeLimit();

    /**
     * Build the arena at the given origin location.
     * Only called when no custom arena is set.
     */
    public abstract void buildArena(Location origin);

    /**
     * Start the minigame with the given players.
     * Arena is already built or custom.
     */
    public abstract void start(List<Player> players);

    /**
     * Called every second while game is active.
     */
    public abstract void tick();

    /**
     * End the game and clean up.
     */
    public abstract void end();

    /**
     * Called when a player disconnects during the game.
     */
    public abstract void onQuit(Player player);

    public boolean isActive() { return active; }
    public void setActive(boolean a) { active = a; }

    public ArenaData getArenaData() { return arenaData; }
    public void setArenaData(ArenaData a) { arenaData = a; }

    protected Location getOrigin() {
        if (arenaData != null) return arenaData.getCenter();
        String world = plugin.getConfig().getString("arenas.world", "world");
        org.bukkit.World w = org.bukkit.Bukkit.getWorld(world);
        if (w == null) w = org.bukkit.Bukkit.getWorlds().get(0);
        int x = plugin.getConfig().getInt("arenas.origin-x", 10000);
        int y = plugin.getConfig().getInt("arenas.origin-y", 100);
        int z = plugin.getConfig().getInt("arenas.origin-z", 10000);
        return new Location(w, x, y, z);
    }

    protected String cfg(String key, String def) {
        return plugin.getConfig().getString("minigame-settings." + getId() + "." + key, def);
    }

    protected int cfgInt(String key, int def) {
        return plugin.getConfig().getInt("minigame-settings." + getId() + "." + key, def);
    }

    protected double cfgDouble(String key, double def) {
        return plugin.getConfig().getDouble("minigame-settings." + getId() + "." + key, def);
    }
}
