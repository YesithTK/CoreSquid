package dev.core.squid.games;

import dev.core.squid.CoreSquid;
import dev.core.squid.utils.ArenaBuilder;
import dev.core.squid.utils.ColorUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class AdivinaNumero extends MiniGame {

    private Location origen;
    private List<Player> jugadores;
    private int numeroSecreto;
    private int intentosMax;
    private Map<UUID, Integer> intentos;
    private int tiempoJuego;
    private BukkitTask task;

    public AdivinaNumero(CoreSquid plugin) {
        super(plugin);
        this.intentos = new HashMap<>();
    }

    @Override public String getNombre() { return "Adivina el Numero"; }
    @Override public String getDescripcion() { return "Adivina el numero entre 1 y 100!"; }
    @Override public int getTiempoLimite() { return 60; }

    @Override
    public void generarArena(Location origen) {
        this.origen = origen.clone();
        for (int x = -5; x <= 5; x++)
            for (int z = -5; z <= 5; z++)
                origen.clone().add(x, -1, z).getBlock().setType(Material.BLUE_CONCRETE);
    }

    @Override
    public void iniciar(List<Player> jugadores) {
        this.jugadores = new ArrayList<>(jugadores);
        this.numeroSecreto = new Random().nextInt(100) + 1;
        this.intentosMax = 7;
        this.tiempoJuego = 0;
        this.activo = true;
        for (Player p : jugadores) {
            p.teleport(origen.clone().add(0, 1, 0));
            intentos.put(p.getUniqueId(), 0);
            p.sendTitle(ColorUtils.color("&e&lADIVINA EL NUMERO"),
                    ColorUtils.color("&7Entre 1 y 100. /squid adivinar <numero>"), 10, 80, 10);
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void procesarIntento(Player player, int numero) {
        if (!activo) return;
        int usados = intentos.getOrDefault(player.getUniqueId(), 0) + 1;
        intentos.put(player.getUniqueId(), usados);
        if (numero == numeroSecreto) {
            player.sendTitle(ColorUtils.color("&a&lCORRECTO!"),
                    ColorUtils.color("&7Era el &e" + numeroSecreto), 10, 60, 10);
            jugadores.remove(player);
            if (jugadores.isEmpty()) terminar();
        } else {
            String pista = numero < numeroSecreto ? "&amayor" : "&cmenor";
            player.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&7Es " + pista +
                    ". Intentos: &e" + usados + "&7/&e" + intentosMax));
            if (usados >= intentosMax) {
                player.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&cEra el &e" + numeroSecreto));
                plugin.getGameManager().eliminarJugador(player);
                jugadores.remove(player);
                if (jugadores.isEmpty()) terminar();
            }
        }
    }

    @Override public void tick() {
        if (!activo) return;
        tiempoJuego++;
        if (tiempoJuego >= getTiempoLimite()) {
            for (Player p : new ArrayList<>(jugadores)) plugin.getGameManager().eliminarJugador(p);
            terminar();
        }
    }

    @Override public void terminar() {
        activo = false;
        if (task != null) task.cancel();
        ArenaBuilder.limpiar(origen, 12, 5, 12);
    }

    @Override public void onPlayerQuit(Player player) {
        jugadores.remove(player);
        intentos.remove(player.getUniqueId());
    }
}
