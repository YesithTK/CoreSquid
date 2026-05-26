package dev.core.squid.managers;

import dev.core.squid.CoreSquid;
import dev.core.squid.model.PlayerData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsManager {

    private final CoreSquid plugin;
    private final Map<UUID, PlayerData> datos = new HashMap<>();
    private File statsFile;
    private FileConfiguration statsConfig;

    public StatsManager(CoreSquid plugin) {
        this.plugin = plugin;
        cargar();
    }

    public PlayerData getData(UUID uuid, String nombre) {
        return datos.computeIfAbsent(uuid, k -> {
            PlayerData pd = new PlayerData(uuid, nombre);
            cargarJugador(pd);
            return pd;
        });
    }

    public PlayerData getData(UUID uuid) {
        return datos.get(uuid);
    }

    private void cargarJugador(PlayerData pd) {
        if (statsConfig == null) return;
        String path = "stats." + pd.getUuid().toString();
        if (!statsConfig.contains(path)) return;
        pd.setVictorias(statsConfig.getInt(path + ".victorias", 0));
        pd.setEliminaciones(statsConfig.getInt(path + ".eliminaciones", 0));
        pd.setPartidas(statsConfig.getInt(path + ".partidas", 0));
    }

    public void guardar() {
        if (statsFile == null) return;
        statsConfig = new YamlConfiguration();
        for (PlayerData pd : datos.values()) {
            String path = "stats." + pd.getUuid().toString();
            statsConfig.set(path + ".nombre", pd.getNombre());
            statsConfig.set(path + ".victorias", pd.getVictorias());
            statsConfig.set(path + ".eliminaciones", pd.getEliminaciones());
            statsConfig.set(path + ".partidas", pd.getPartidas());
        }
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cargar() {
        statsFile = new File(plugin.getDataFolder(), "stats.yml");
        if (!statsFile.exists()) {
            try { statsFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
    }

    public Map<UUID, PlayerData> getTodos() { return datos; }
}
