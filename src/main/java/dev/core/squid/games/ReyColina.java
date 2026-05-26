package dev.core.squid.games;

import dev.core.squid.CoreSquid;
import dev.core.squid.utils.ArenaBuilder;
import dev.core.squid.utils.ColorUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class ReyColina extends MiniGame {

    private Location origen;
    private List<Player> jugadores;
    private Player rey;
    private Map<UUID, Integer> puntos;
    private int tiempoJuego;
    private BukkitTask task;
    private static final int PUNTOS_PARA_GANAR = 30;

    public ReyColina(CoreSquid plugin) {
        super(plugin);
        this.puntos = new HashMap<>();
    }

    @Override public String getNombre() { return "Rey de la Colina"; }
    @Override public String getDescripcion() { return "Mantente en la cima el mayor tiempo posible!"; }
    @Override public int getTiempoLimite() { return 120; }

    @Override
    public void generarArena(Location origen) {
        this.origen = origen.clone();
        for (int nivel = 0; nivel <= 5; nivel++) {
            int radio = 5 - nivel;
            for (int x = -radio; x <= radio; x++)
                for (int z = -radio; z <= radio; z++)
                    if (x*x + z*z <= radio*radio + radio)
                        origen.clone().add(x, nivel - 1, z).getBlock().setType(Material.STONE);
        }
    }

    @Override
    public void iniciar(List<Player> jugadores) {
        this.jugadores = new ArrayList<>(jugadores);
        this.tiempoJuego = 0;
        this.activo = true;
        jugadores.forEach(p -> puntos.put(p.getUniqueId(), 0));
        int ang = 0;
        for (Player p : jugadores) {
            double rad = Math.toRadians(ang);
            p.teleport(origen.clone().add(8 * Math.cos(rad), 1, 8 * Math.sin(rad)));
            ang += 360 / jugadores.size();
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    @Override
    public void tick() {
        if (!activo) return;
        tiempoJuego++;

        Location cima = origen.clone().add(0, 5, 0);
        Player enCima = null;
        for (Player p : jugadores) {
            if (p.getLocation().distance(cima) <= 2) { enCima = p; break; }
        }

        if (enCima != null) {
            rey = enCima;
            puntos.merge(rey.getUniqueId(), 1, Integer::sum);
            int pts = puntos.get(rey.getUniqueId());
            rey.sendTitle(ColorUtils.color("&6\u2654 REY"),
                    ColorUtils.color("&e" + pts + "/" + PUNTOS_PARA_GANAR + "s"), 0, 25, 5);
            if (pts >= PUNTOS_PARA_GANAR) {
                for (Player p : new ArrayList<>(jugadores))
                    if (p != rey) plugin.getGameManager().eliminarJugador(p);
                terminar();
                return;
            }
        }

        if (tiempoJuego >= getTiempoLimite()) {
            UUID ganadorUUID = puntos.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey).orElse(null);
            for (Player p : new ArrayList<>(jugadores))
                if (ganadorUUID == null || !p.getUniqueId().equals(ganadorUUID))
                    plugin.getGameManager().eliminarJugador(p);
            terminar();
        }
    }

    @Override public void terminar() {
        activo = false;
        if (task != null) task.cancel();
        ArenaBuilder.limpiar(origen, 15, 10, 15);
    }

    @Override public void onPlayerQuit(Player player) {
        jugadores.remove(player);
        puntos.remove(player.getUniqueId());
        if (player == rey) rey = null;
    }
}
