package dev.core.squid.managers;

import dev.core.squid.CoreSquid;
import dev.core.squid.model.ArenaData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.*;

public class ArenaManager {

    private final CoreSquid plugin;
    private final Map<String, ArenaData> arenas = new HashMap<>();
    // Wand selections per player UUID
    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();

    private File file;
    private FileConfiguration config;

    // Lobby and global spawn
    private Location lobbySpawn;
    private Location gameSpawn;

    public ArenaManager(CoreSquid plugin) {
        this.plugin = plugin;
        load();
    }

    // ── Wand selections ───────────────────────────────────────────

    public void setPos1(UUID uuid, Location loc) { pos1.put(uuid, loc.clone()); }
    public void setPos2(UUID uuid, Location loc) { pos2.put(uuid, loc.clone()); }
    public Location getPos1(UUID uuid) { return pos1.get(uuid); }
    public Location getPos2(UUID uuid) { return pos2.get(uuid); }
    public boolean hasSelection(UUID uuid) { return pos1.containsKey(uuid) && pos2.containsKey(uuid); }
    public void clearSelection(UUID uuid) { pos1.remove(uuid); pos2.remove(uuid); }

    // ── Arenas ────────────────────────────────────────────────────

    public ArenaData getArena(String gameId) { return arenas.get(gameId.toLowerCase()); }
    public boolean hasArena(String gameId) { return arenas.containsKey(gameId.toLowerCase()); }
    public Map<String, ArenaData> getAll() { return arenas; }

    public ArenaData createArena(String gameId, Location p1, Location p2) {
        ArenaData arena = new ArenaData(gameId.toLowerCase(), p1, p2);
        arenas.put(gameId.toLowerCase(), arena);
        save();
        return arena;
    }

    public void deleteArena(String gameId) {
        arenas.remove(gameId.toLowerCase());
        save();
    }

    // ── Lobby & Spawn ─────────────────────────────────────────────

    public Location getLobbySpawn() { return lobbySpawn; }
    public void setLobbySpawn(Location loc) { lobbySpawn = loc.clone(); save(); }

    public Location getGameSpawn() { return gameSpawn; }
    public void setGameSpawn(Location loc) { gameSpawn = loc.clone(); save(); }

    // ── Persistence ───────────────────────────────────────────────

    public void load() {
        file = new File(plugin.getDataFolder(), "arenas.yml");
        if (!file.exists()) try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        config = YamlConfiguration.loadConfiguration(file);
        arenas.clear();

        // Load lobby spawn
        if (config.contains("lobby-spawn")) {
            lobbySpawn = loadLocation(config, "lobby-spawn");
        }
        if (config.contains("game-spawn")) {
            gameSpawn = loadLocation(config, "game-spawn");
        }

        // Load arenas
        if (config.contains("arenas")) {
            for (String key : config.getConfigurationSection("arenas").getKeys(false)) {
                try {
                    Location p1 = loadLocation(config, "arenas." + key + ".pos1");
                    Location p2 = loadLocation(config, "arenas." + key + ".pos2");
                    if (p1 == null || p2 == null) continue;
                    ArenaData arena = new ArenaData(key, p1, p2);
                    if (config.contains("arenas." + key + ".spawn")) {
                        Location spawn = loadLocation(config, "arenas." + key + ".spawn");
                        if (spawn != null) arena.setSpawn(spawn);
                    }
                    arenas.put(key, arena);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error loading arena: " + key);
                }
            }
        }
    }

    public void save() {
        config = new YamlConfiguration();

        if (lobbySpawn != null) saveLocation(config, "lobby-spawn", lobbySpawn);
        if (gameSpawn != null) saveLocation(config, "game-spawn", gameSpawn);

        for (Map.Entry<String, ArenaData> entry : arenas.entrySet()) {
            String path = "arenas." + entry.getKey();
            ArenaData a = entry.getValue();
            saveLocation(config, path + ".pos1", a.getPos1());
            saveLocation(config, path + ".pos2", a.getPos2());
            saveLocation(config, path + ".spawn", a.getSpawn());
        }

        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    private void saveLocation(FileConfiguration cfg, String path, Location loc) {
        cfg.set(path + ".world", loc.getWorld().getName());
        cfg.set(path + ".x", loc.getX());
        cfg.set(path + ".y", loc.getY());
        cfg.set(path + ".z", loc.getZ());
        cfg.set(path + ".yaw", loc.getYaw());
        cfg.set(path + ".pitch", loc.getPitch());
    }

    private Location loadLocation(FileConfiguration cfg, String path) {
        String worldName = cfg.getString(path + ".world");
        if (worldName == null) return null;
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(
            world,
            cfg.getDouble(path + ".x"),
            cfg.getDouble(path + ".y"),
            cfg.getDouble(path + ".z"),
            (float) cfg.getDouble(path + ".yaw"),
            (float) cfg.getDouble(path + ".pitch")
        );
    }
}
