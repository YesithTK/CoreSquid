package dev.core.squid.games;

import dev.core.squid.CoreSquid;
import dev.core.squid.utils.ArenaBuilder;
import dev.core.squid.utils.ColorUtils;
import dev.core.squid.utils.SoundUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class LuzRojaLuzVerde extends MiniGame {

    private Location origen;
    private List<Player> jugadores;
    private List<Player> eliminados;
    private Map<UUID, Location> posicionesAnteriores;

    private boolean luzVerde;
    private int tiempoJuego;
    private int tiempoCambio;
    private BukkitTask task;

    // Meta: X final de la arena
    private int metaX;

    public LuzRojaLuzVerde(CoreSquid plugin) {
        super(plugin);
        this.eliminados = new ArrayList<>();
        this.posicionesAnteriores = new HashMap<>();
    }

    @Override
    public String getNombre() { return "Luz Roja Luz Verde"; }

    @Override
    public String getDescripcion() { return "No te muevas cuando sea luz roja!"; }

    @Override
    public int getTiempoLimite() { return 90; }

    @Override
    public void generarArena(Location origen) {
        this.origen = origen.clone();
        this.metaX = origen.getBlockX() + 40;

        // Plataforma principal (40 bloques de largo, 10 de ancho)
        for (int x = 0; x <= 40; x++) {
            for (int z = -5; z <= 5; z++) {
                Location suelo = origen.clone().add(x, -1, z);
                suelo.getBlock().setType(Material.GREEN_CONCRETE);
            }
        }

        // Linea de meta
        for (int z = -5; z <= 5; z++) {
            origen.clone().add(40, 0, z).getBlock().setType(Material.GOLD_BLOCK);
            origen.clone().add(40, 1, z).getBlock().setType(Material.GOLD_BLOCK);
        }

        // Muñeca (representada con bloques de cabeza)
        Location muneca = origen.clone().add(42, 0, 0);
        muneca.getBlock().setType(Material.CRYING_OBSIDIAN);
        muneca.clone().add(0, 1, 0).getBlock().setType(Material.JACK_O_LANTERN);
    }

    @Override
    public void iniciar(List<Player> jugadores) {
        this.jugadores = new ArrayList<>(jugadores);
        this.eliminados = new ArrayList<>();
        this.luzVerde = true;
        this.tiempoJuego = 0;
        this.tiempoCambio = 5;
        this.activo = true;

        // Teleportar jugadores a la linea de inicio
        int z = -4;
        for (Player p : jugadores) {
            p.teleport(origen.clone().add(0, 1, z));
            z += 1;
            posicionesAnteriores.put(p.getUniqueId(), p.getLocation().clone());
            p.sendTitle(ColorUtils.color("&a&lLUZ VERDE"), ColorUtils.color("&7Muevete hacia la meta!"), 10, 40, 10);
        }

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    @Override
    public void tick() {
        if (!activo) return;
        tiempoJuego++;
        tiempoCambio--;

        // Verificar jugadores que se movieron en luz roja
        if (!luzVerde) {
            for (Player p : new ArrayList<>(jugadores)) {
                Location anterior = posicionesAnteriores.get(p.getUniqueId());
                if (anterior == null) continue;
                Location actual = p.getLocation();
                double distancia = anterior.distance(actual);
                if (distancia > 0.1) {
                    eliminarJugador(p);
                }
            }
        }

        // Guardar posiciones actuales
        for (Player p : jugadores) {
            posicionesAnteriores.put(p.getUniqueId(), p.getLocation().clone());
        }

        // Cambiar luz
        if (tiempoCambio <= 0) {
            luzVerde = !luzVerde;
            tiempoCambio = luzVerde ? new Random().nextInt(4) + 3 : new Random().nextInt(3) + 2;

            for (Player p : jugadores) {
                if (luzVerde) {
                    p.sendTitle(ColorUtils.color("&a&lLUZ VERDE"), ColorUtils.color("&7Corre!"), 5, 30, 5);
                    SoundUtils.play(p, "coresquid.luz_verde");
                } else {
                    p.sendTitle(ColorUtils.color("&c&lLUZ ROJA"), ColorUtils.color("&7No te muevas!"), 5, 30, 5);
                    SoundUtils.play(p, "coresquid.luz_roja");
                }
            }
        }

        // Verificar llegada a la meta
        for (Player p : new ArrayList<>(jugadores)) {
            if (p.getLocation().getBlockX() >= metaX) {
                p.sendTitle(ColorUtils.color("&a&lLLEGASTE"), ColorUtils.color("&7Esperando a los demas..."), 10, 40, 10);
                jugadores.remove(p);
                // Jugador seguro, no eliminado
            }
        }

        // Tiempo agotado: eliminar a los que no llegaron
        if (tiempoJuego >= getTiempoLimite()) {
            for (Player p : new ArrayList<>(jugadores)) {
                eliminarJugador(p);
            }
            terminar();
        }

        // Todos llegaron
        if (jugadores.isEmpty()) terminar();
    }

    private void eliminarJugador(Player player) {
        jugadores.remove(player);
        eliminados.add(player);
        plugin.getGameManager().eliminarJugador(player);
    }

    @Override
    public void terminar() {
        activo = false;
        if (task != null) task.cancel();
        ArenaBuilder.limpiar(origen, 50, 15, 10);
    }

    @Override
    public void onPlayerQuit(Player player) {
        jugadores.remove(player);
        eliminados.remove(player);
        posicionesAnteriores.remove(player.getUniqueId());
    }
}
