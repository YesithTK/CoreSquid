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

public class PuenteCristal extends MiniGame {

    private Location origen;
    private List<Player> jugadores;
    private boolean[] ladoCorrecto; // true = izquierda, false = derecha
    private int columnas;
    private int tiempoJuego;
    private BukkitTask task;
    private int turnoActual;
    private Queue<Player> cola;

    public PuenteCristal(CoreSquid plugin) {
        super(plugin);
    }

    @Override
    public String getNombre() { return "Puente de Cristal"; }

    @Override
    public String getDescripcion() { return "Elige el panel de cristal correcto para cruzar!"; }

    @Override
    public int getTiempoLimite() { return 120; }

    @Override
    public void generarArena(Location origen) {
        this.origen = origen.clone();
        this.columnas = 18;
        this.ladoCorrecto = new boolean[columnas];
        Random rand = new Random();

        // Generar el patron aleatorio del puente
        for (int i = 0; i < columnas; i++) {
            ladoCorrecto[i] = rand.nextBoolean();
        }

        // Suelo abajo (vacio - caen al vacio o lava)
        for (int x = -2; x <= columnas + 2; x++) {
            for (int z = -3; z <= 3; z++) {
                origen.clone().add(x, -10, z).getBlock().setType(Material.LAVA);
            }
        }

        // Plataforma de inicio
        for (int z = -2; z <= 2; z++) {
            origen.clone().add(-2, 0, z).getBlock().setType(Material.RED_CONCRETE);
            origen.clone().add(-3, 0, z).getBlock().setType(Material.RED_CONCRETE);
        }

        // Plataforma de llegada
        for (int z = -2; z <= 2; z++) {
            origen.clone().add(columnas + 1, 0, z).getBlock().setType(Material.LIME_CONCRETE);
            origen.clone().add(columnas + 2, 0, z).getBlock().setType(Material.LIME_CONCRETE);
        }

        // Generar paneles de cristal (ambos lados parecen iguales)
        for (int col = 0; col < columnas; col++) {
            // Panel izquierda (z = -1) y derecha (z = 1)
            Location izq = origen.clone().add(col, 0, -1);
            Location der = origen.clone().add(col, 0, 1);
            izq.getBlock().setType(Material.GLASS);
            der.getBlock().setType(Material.GLASS);
        }
    }

    @Override
    public void iniciar(List<Player> jugadores) {
        this.jugadores = new ArrayList<>(jugadores);
        this.tiempoJuego = 0;
        this.turnoActual = 0;
        this.activo = true;
        this.cola = new LinkedList<>(jugadores);

        // Teleportar a plataforma de inicio
        int z = -1;
        for (Player p : jugadores) {
            p.teleport(origen.clone().add(-2, 1, z));
            z = z == -1 ? 1 : -1;
        }

        anunciarTurno();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void anunciarTurno() {
        Player actual = cola.peek();
        if (actual == null) return;
        for (Player p : jugadores) {
            p.sendTitle(
                    ColorUtils.color("&eTurno de &f" + actual.getName()),
                    ColorUtils.color("&7Elige un panel de cristal"),
                    10, 40, 10
            );
        }
    }

    @Override
    public void tick() {
        if (!activo) return;
        tiempoJuego++;

        if (tiempoJuego >= getTiempoLimite()) {
            // Eliminar a todos los que quedan en cola
            for (Player p : new ArrayList<>(cola)) {
                plugin.getGameManager().eliminarJugador(p);
            }
            terminar();
        }
    }

    /**
     * Llamado desde el listener cuando un jugador pisa un panel.
     */
    public void onPisarPanel(Player player, Location loc) {
        if (!activo) return;
        if (cola.peek() != player) {
            player.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&cNo es tu turno!"));
            return;
        }

        int col = loc.getBlockX() - origen.getBlockX();
        int z = loc.getBlockZ() - origen.getBlockZ();
        if (col < 0 || col >= columnas) return;

        boolean eligioIzquierda = z == -1;
        boolean correcto = (eligioIzquierda == ladoCorrecto[col]);

        if (correcto) {
            // Panel correcto - templado
            loc.getBlock().setType(Material.GLASS);
            SoundUtils.play(player, "coresquid.cristal_correcto");
            turnoActual++;
            cola.poll();

            if (col >= columnas - 1) {
                // Llego al final
                player.sendTitle(ColorUtils.color("&a&lCRUZASTE!"), ColorUtils.color("&7Bien hecho!"), 10, 40, 10);
                jugadores.remove(player);
            } else {
                anunciarTurno();
            }
        } else {
            // Panel incorrecto - roto
            loc.getBlock().setType(Material.AIR);
            SoundUtils.play(player, "coresquid.cristal_roto");
            cola.poll();
            plugin.getGameManager().eliminarJugador(player);
            jugadores.remove(player);
            anunciarTurno();
        }

        if (cola.isEmpty()) terminar();
    }

    @Override
    public void terminar() {
        activo = false;
        if (task != null) task.cancel();
        ArenaBuilder.limpiar(origen, columnas + 5, 15, 5);
    }

    @Override
    public void onPlayerQuit(Player player) {
        jugadores.remove(player);
        cola.remove(player);
    }
}
