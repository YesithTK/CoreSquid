package dev.core.squid.games;

import dev.core.squid.CoreSquid;
import dev.core.squid.utils.ArenaBuilder;
import dev.core.squid.utils.ColorUtils;
import dev.core.squid.utils.SoundUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class PlataformasCaen extends MiniGame {

    private Location origen;
    private List<Player> jugadores;
    private List<Location> plataformas;
    private int tiempoJuego;
    private BukkitTask task;

    public PlataformasCaen(CoreSquid plugin) {
        super(plugin);
        this.plataformas = new ArrayList<>();
    }

    @Override public String getNombre() { return "Plataformas que Caen"; }
    @Override public String getDescripcion() { return "No caigas! Las plataformas desaparecen!"; }
    @Override public int getTiempoLimite() { return 90; }

    @Override
    public void generarArena(Location origen) {
        this.origen = origen.clone();
        Material[] mats = {Material.RED_CONCRETE, Material.ORANGE_CONCRETE,
                           Material.YELLOW_CONCRETE, Material.LIME_CONCRETE};
        for (int nivel = 0; nivel < 4; nivel++) {
            int y = nivel * 5;
            for (int x = -8; x <= 8; x += 3) {
                for (int z = -8; z <= 8; z += 3) {
                    Location plat = origen.clone().add(x, y, z);
                    plat.getBlock().setType(mats[nivel]);
                    plataformas.add(plat);
                }
            }
        }
        for (int x = -10; x <= 10; x++)
            for (int z = -10; z <= 10; z++)
                origen.clone().add(x, -2, z).getBlock().setType(Material.LAVA);
    }

    @Override
    public void iniciar(List<Player> jugadores) {
        this.jugadores = new ArrayList<>(jugadores);
        this.tiempoJuego = 0;
        this.activo = true;
        int idx = 0;
        for (Player p : jugadores) {
            p.teleport(origen.clone().add((idx % 6) * 3 - 7, 16, (idx / 6) * 3 - 7));
            idx++;
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    @Override
    public void tick() {
        if (!activo) return;
        tiempoJuego++;
        if (tiempoJuego % 3 == 0 && !plataformas.isEmpty()) {
            Location plat = plataformas.remove(new Random().nextInt(plataformas.size()));
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> plat.getBlock().setType(Material.AIR), 10L);
            for (Player p : jugadores) SoundUtils.play(p, "coresquid.plataforma_cae");
        }
        for (Player p : new ArrayList<>(jugadores)) {
            if (p.getLocation().getY() <= origen.getY() - 1) {
                plugin.getGameManager().eliminarJugador(p);
                jugadores.remove(p);
            }
        }
        if (jugadores.size() <= 1 || tiempoJuego >= getTiempoLimite()) terminar();
    }

    @Override public void terminar() {
        activo = false;
        if (task != null) task.cancel();
        ArenaBuilder.limpiar(origen, 20, 25, 20);
    }

    @Override public void onPlayerQuit(Player player) { jugadores.remove(player); }
}
