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

public class SillasMusicales extends MiniGame {

    private Location origen;
    private List<Player> jugadores;
    private List<Location> sillas;
    private boolean musicaActiva;
    private int tiempoJuego;
    private int tiempoMusica;
    private BukkitTask task;
    private Set<UUID> sentados;

    public SillasMusicales(CoreSquid plugin) {
        super(plugin);
        this.sillas = new ArrayList<>();
        this.sentados = new HashSet<>();
    }

    @Override
    public String getNombre() { return "Sillas Musicales"; }

    @Override
    public String getDescripcion() { return "Cuando pare la musica, siéntate en una silla!"; }

    @Override
    public int getTiempoLimite() { return 120; }

    @Override
    public void generarArena(Location origen) {
        this.origen = origen.clone();

        // Suelo circular
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                if (x * x + z * z <= 100) {
                    origen.clone().add(x, -1, z).getBlock().setType(Material.POLISHED_ANDESITE);
                }
            }
        }
    }

    private void generarSillas(int cantidad) {
        sillas.clear();
        double angulo = 2 * Math.PI / cantidad;
        for (int i = 0; i < cantidad; i++) {
            double x = 6 * Math.cos(angulo * i);
            double z = 6 * Math.sin(angulo * i);
            Location silla = origen.clone().add(x, 0, z);
            silla.getBlock().setType(Material.OAK_STAIRS);
            sillas.add(silla);
        }
    }

    @Override
    public void iniciar(List<Player> jugadores) {
        this.jugadores = new ArrayList<>(jugadores);
        this.tiempoJuego = 0;
        this.musicaActiva = true;
        this.tiempoMusica = new Random().nextInt(8) + 5;
        this.activo = true;

        generarSillas(jugadores.size() - 1);

        for (Player p : jugadores) {
            p.teleport(origen.clone().add(0, 1, 0));
            p.sendTitle(ColorUtils.color("&e&lSILLAS MUSICALES"),
                    ColorUtils.color("&7Cuando pare la musica, busca una silla!"), 10, 60, 10);
        }

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    @Override
    public void tick() {
        if (!activo) return;
        tiempoJuego++;
        tiempoMusica--;

        if (musicaActiva) {
            // Reproducir sonido de musica cada tick
            for (Player p : jugadores) {
                SoundUtils.play(p, "coresquid.musica_sillas");
            }

            if (tiempoMusica <= 0) {
                // Parar musica
                musicaActiva = false;
                tiempoMusica = 5; // 5 segundos para sentarse
                sentados.clear();

                for (Player p : jugadores) {
                    p.sendTitle(ColorUtils.color("&c&lMUSICA PARADA!"),
                            ColorUtils.color("&7Busca una silla!"), 5, 30, 5);
                    SoundUtils.play(p, "coresquid.musica_parada");
                }
            }
        } else {
            // Verificar quienes estan en silla
            for (Player p : jugadores) {
                for (Location silla : sillas) {
                    if (p.getLocation().getBlock().getLocation().equals(silla) ||
                        p.getLocation().clone().subtract(0, 1, 0).getBlock().getLocation().equals(silla)) {
                        sentados.add(p.getUniqueId());
                        break;
                    }
                }
            }

            if (tiempoMusica <= 0) {
                // Eliminar al que no se sento
                Player eliminado = null;
                for (Player p : jugadores) {
                    if (!sentados.contains(p.getUniqueId())) {
                        eliminado = p;
                        break;
                    }
                }

                if (eliminado != null) {
                    plugin.getGameManager().eliminarJugador(eliminado);
                    jugadores.remove(eliminado);
                }

                if (jugadores.size() <= 1) {
                    terminar();
                    return;
                }

                // Quitar una silla
                for (Location silla : new ArrayList<>(sillas)) {
                    silla.getBlock().setType(Material.AIR);
                    sillas.remove(silla);
                    break;
                }

                generarSillas(jugadores.size() - 1);

                // Reiniciar musica
                musicaActiva = true;
                tiempoMusica = new Random().nextInt(8) + 5;
                sentados.clear();
            }
        }

        if (tiempoJuego >= getTiempoLimite()) terminar();
    }

    @Override
    public void terminar() {
        activo = false;
        if (task != null) task.cancel();
        ArenaBuilder.limpiar(origen, 25, 5, 25);
    }

    @Override
    public void onPlayerQuit(Player player) {
        jugadores.remove(player);
        sentados.remove(player.getUniqueId());
    }
}
