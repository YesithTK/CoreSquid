package dev.core.squid.games;

import dev.core.squid.CoreSquid;
import dev.core.squid.utils.ArenaBuilder;
import dev.core.squid.utils.ColorUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class Laberinto extends MiniGame {

    private Location origen;
    private List<Player> jugadores;
    private Location meta;
    private int tiempoJuego;
    private BukkitTask task;

    public Laberinto(CoreSquid plugin) { super(plugin); }

    @Override public String getNombre() { return "Laberinto"; }
    @Override public String getDescripcion() { return "Llega a la meta antes que los demas!"; }
    @Override public int getTiempoLimite() { return 120; }

    @Override
    public void generarArena(Location origen) {
        this.origen = origen.clone();
        Random rand = new Random();
        for (int x = 0; x <= 20; x++) {
            for (int z = 0; z <= 20; z++) {
                origen.clone().add(x, -1, z).getBlock().setType(Material.STONE_BRICKS);
                if (x == 0 || x == 20 || z == 0 || z == 20) {
                    origen.clone().add(x, 0, z).getBlock().setType(Material.STONE_BRICK_WALL);
                    origen.clone().add(x, 1, z).getBlock().setType(Material.STONE_BRICK_WALL);
                } else if (rand.nextInt(4) == 0) {
                    Material wall = rand.nextBoolean() ? Material.STONE_BRICK_WALL : Material.AIR;
                    origen.clone().add(x, 0, z).getBlock().setType(wall);
                    if (wall != Material.AIR)
                        origen.clone().add(x, 1, z).getBlock().setType(wall);
                }
            }
        }
        this.meta = origen.clone().add(19, 0, 19);
        meta.getBlock().setType(Material.BEACON);
        meta.clone().add(0, -1, 0).getBlock().setType(Material.EMERALD_BLOCK);
    }

    @Override
    public void iniciar(List<Player> jugadores) {
        this.jugadores = new ArrayList<>(jugadores);
        this.tiempoJuego = 0;
        this.activo = true;
        for (Player p : jugadores) {
            p.teleport(origen.clone().add(1, 1, 1));
            p.sendTitle(ColorUtils.color("&e&lLABERINTO"),
                    ColorUtils.color("&7Llega a la meta!"), 10, 60, 10);
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    @Override
    public void tick() {
        if (!activo) return;
        tiempoJuego++;
        for (Player p : new ArrayList<>(jugadores)) {
            if (p.getLocation().distance(meta) <= 2) {
                p.sendTitle(ColorUtils.color("&a&lLLEGASTE!"),
                        ColorUtils.color("&7Eres el primero!"), 10, 60, 10);
                jugadores.remove(p);
                for (Player otro : new ArrayList<>(jugadores)) plugin.getGameManager().eliminarJugador(otro);
                terminar();
                return;
            }
        }
        for (Player p : jugadores)
            p.sendTitle(ColorUtils.color("&e" + (getTiempoLimite() - tiempoJuego) + "s"),
                    ColorUtils.color("&7Llega a la &ameta!"), 0, 25, 5);
        if (tiempoJuego >= getTiempoLimite()) {
            for (Player p : new ArrayList<>(jugadores)) plugin.getGameManager().eliminarJugador(p);
            terminar();
        }
    }

    @Override public void terminar() {
        activo = false;
        if (task != null) task.cancel();
        ArenaBuilder.limpiar(origen, 25, 5, 25);
    }

    @Override public void onPlayerQuit(Player player) { jugadores.remove(player); }
}
