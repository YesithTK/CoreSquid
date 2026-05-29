package dev.core.squid.managers;

import dev.core.squid.CoreSquid;
import dev.core.squid.model.PlayerStats;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.*;

public class StatsManager {

    private final CoreSquid plugin;
    private final Map<UUID, PlayerStats> cache = new HashMap<>();
    private File statsFile;
    private FileConfiguration statsConfig;

    public StatsManager(CoreSquid plugin) {
        this.plugin = plugin;
        load();
    }

    public PlayerStats getStats(UUID uuid, String name) {
        return cache.computeIfAbsent(uuid, k -> {
            PlayerStats ps = new PlayerStats(uuid, name);
            loadPlayer(ps);
            return ps;
        });
    }

    public PlayerStats getStats(UUID uuid) {
        return cache.get(uuid);
    }

    private void loadPlayer(PlayerStats ps) {
        String path = "stats." + ps.getUuid();
        if (!statsConfig.contains(path)) return;
        ps.setWins(statsConfig.getInt(path + ".wins", 0));
        ps.setEliminations(statsConfig.getInt(path + ".eliminations", 0));
        ps.setGamesPlayed(statsConfig.getInt(path + ".games", 0));
    }

    public void save() {
        statsConfig = new YamlConfiguration();
        for (PlayerStats ps : cache.values()) {
            String path = "stats." + ps.getUuid();
            statsConfig.set(path + ".name", ps.getName());
            statsConfig.set(path + ".wins", ps.getWins());
            statsConfig.set(path + ".eliminations", ps.getEliminations());
            statsConfig.set(path + ".games", ps.getGamesPlayed());
        }
        try { statsConfig.save(statsFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public void load() {
        statsFile = new File(plugin.getDataFolder(), "stats.yml");
        if (!statsFile.exists()) try { statsFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
    }

    public Map<UUID, PlayerStats> getAll() { return cache; }
}
