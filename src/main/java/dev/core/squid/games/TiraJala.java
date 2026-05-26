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

public class TiraJala extends MiniGame {

    private Location origen;
    private List<Player> equipoA;
    private List<Player> equipoB;
    private List<Player> jugadores;
    private int posicionCuerda;
    private int tiempoJuego;
    private BukkitTask task;

    public TiraJala(CoreSquid plugin) {
        super(plugin);
        this.equipoA = new ArrayList<>();
        this.equipoB = new ArrayList<>();
    }

    @Override public String getNombre() { return "Tira y Jala"; }
    @Override public String getDescripcion() { return "Haz clic rapido para jalar la cuerda!"; }
    @Override public int getTiempoLimite() { return 60; }

    @Override
    public void generarArena(Location origen) {
        this.origen = origen.clone();
        for (int x = -12; x <= 12; x++) {
            for (int z = -3; z <= 3; z++) {
                Material mat = x < 0 ? Material.BLUE_CONCRETE : (x > 0 ? Material.RED_CONCRETE : Material.LAVA);
                origen.clone().add(x, -1, z).getBlock().setType(mat);
            }
        }
        for (int z = -3; z <= 3; z++) {
            origen.clone().add(-10, 0, z).getBlock().setType(Material.BLUE_STAINED_GLASS);
            origen.clone().add(10, 0, z).getBlock().setType(Material.RED_STAINED_GLASS);
        }
    }

    @Override
    public void iniciar(List<Player> jugadores) {
        this.jugadores = new ArrayList<>(jugadores);
        this.posicionCuerda = 0;
        this.tiempoJuego = 0;
        this.activo = true;
        Collections.shuffle(this.jugadores);
        for (int i = 0; i < this.jugadores.size(); i++) {
            if (i % 2 == 0) equipoA.add(this.jugadores.get(i));
            else equipoB.add(this.jugadores.get(i));
        }
        for (Player p : equipoA) {
            p.teleport(origen.clone().add(-5, 1, 0));
            p.sendTitle(ColorUtils.color("&9&lEQUIPO AZUL"), ColorUtils.color("&7Haz clic rapido!"), 10, 60, 10);
        }
        for (Player p : equipoB) {
            p.teleport(origen.clone().add(5, 1, 0));
            p.sendTitle(ColorUtils.color("&c&lEQUIPO ROJO"), ColorUtils.color("&7Haz clic rapido!"), 10, 60, 10);
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void jalar(Player player) {
        if (!activo) return;
        if (equipoA.contains(player)) posicionCuerda--;
        else posicionCuerda++;
        SoundUtils.play(player, "coresquid.tira_cuerda");
        checkVictoria();
    }

    private void checkVictoria() {
        if (posicionCuerda <= -10) {
            for (Player p : new ArrayList<>(equipoB)) plugin.getGameManager().eliminarJugador(p);
            terminar();
        } else if (posicionCuerda >= 10) {
            for (Player p : new ArrayList<>(equipoA)) plugin.getGameManager().eliminarJugador(p);
            terminar();
        }
    }

    @Override
    public void tick() {
        if (!activo) return;
        tiempoJuego++;
        if (tiempoJuego >= getTiempoLimite()) {
            if (posicionCuerda <= 0) for (Player p : new ArrayList<>(equipoB)) plugin.getGameManager().eliminarJugador(p);
            else for (Player p : new ArrayList<>(equipoA)) plugin.getGameManager().eliminarJugador(p);
            terminar();
        }
    }

    @Override public void terminar() {
        activo = false;
        if (task != null) task.cancel();
        equipoA.clear();
        equipoB.clear();
        ArenaBuilder.limpiar(origen, 28, 5, 8);
    }

    @Override public void onPlayerQuit(Player player) {
        jugadores.remove(player);
        equipoA.remove(player);
        equipoB.remove(player);
    }
}
