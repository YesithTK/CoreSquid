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

public class RuletaRusa extends MiniGame {

    private Location origen;
    private List<Player> jugadores;
    private int turno;
    private int tiempoJuego;
    private BukkitTask task;

    public RuletaRusa(CoreSquid plugin) { super(plugin); }

    @Override public String getNombre() { return "Ruleta Rusa"; }
    @Override public String getDescripcion() { return "Bebe la pocion. Puede que sea la mala!"; }
    @Override public int getTiempoLimite() { return 120; }

    @Override
    public void generarArena(Location origen) {
        this.origen = origen.clone();
        for (int x = -6; x <= 6; x++)
            for (int z = -6; z <= 6; z++)
                if (x*x + z*z <= 36)
                    origen.clone().add(x, -1, z).getBlock().setType(Material.DARK_OAK_PLANKS);
    }

    @Override
    public void iniciar(List<Player> jugadores) {
        this.jugadores = new ArrayList<>(jugadores);
        this.turno = 0;
        this.tiempoJuego = 0;
        this.activo = true;
        int ang = 0;
        for (Player p : jugadores) {
            double rad = Math.toRadians(ang);
            p.teleport(origen.clone().add(4 * Math.cos(rad), 1, 4 * Math.sin(rad)));
            ang += 360 / jugadores.size();
        }
        anunciarTurno();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void anunciarTurno() {
        if (jugadores.isEmpty()) return;
        Player actual = jugadores.get(turno % jugadores.size());
        for (Player p : jugadores) {
            p.sendTitle(ColorUtils.color(p == actual ? "&c&lTU TURNO" : "&7Turno de &e" + actual.getName()),
                    ColorUtils.color("&7Escribe /squid beber"), 5, 40, 5);
        }
        actual.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&7Escribe &e/squid beber"));
    }

    public void beber(Player player) {
        if (!activo) return;
        Player actual = jugadores.get(turno % jugadores.size());
        if (player != actual) {
            player.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&cNo es tu turno!"));
            return;
        }
        if (new Random().nextInt(6) == 0) {
            player.sendTitle(ColorUtils.color("&c&lPOCION MALA!"), ColorUtils.color("&7Eliminado!"), 5, 60, 5);
            SoundUtils.play(player, "coresquid.ruleta_mala");
            plugin.getGameManager().eliminarJugador(player);
            jugadores.remove(player);
            if (jugadores.size() <= 1) { terminar(); return; }
        } else {
            player.sendTitle(ColorUtils.color("&a&lPOCION BUENA!"), ColorUtils.color("&7Salvado!"), 5, 40, 5);
            SoundUtils.play(player, "coresquid.ruleta_buena");
            turno++;
        }
        anunciarTurno();
    }

    @Override public void tick() {
        if (!activo) return;
        tiempoJuego++;
        if (tiempoJuego >= getTiempoLimite()) terminar();
    }

    @Override public void terminar() {
        activo = false;
        if (task != null) task.cancel();
        ArenaBuilder.limpiar(origen, 15, 5, 15);
    }

    @Override public void onPlayerQuit(Player player) { jugadores.remove(player); }
}
