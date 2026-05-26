package dev.core.squid.games;

import dev.core.squid.CoreSquid;
import dev.core.squid.utils.ArenaBuilder;
import dev.core.squid.utils.ColorUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class CarreraObstaculos extends MiniGame {

    private Location origen;
    private List<Player> jugadores;
    private int tiempoJuego;
    private BukkitTask task;
    private Set<UUID> llegaron;

    public CarreraObstaculos(CoreSquid plugin) {
        super(plugin);
        this.llegaron = new HashSet<>();
    }

    @Override public String getNombre() { return "Carrera de Obstaculos"; }
    @Override public String getDescripcion() { return "Llega al final superando los obstaculos!"; }
    @Override public int getTiempoLimite() { return 120; }

    @Override
    public void generarArena(Location origen) {
        this.origen = origen.clone();
        for (int x = 0; x <= 50; x++)
            for (int z = -3; z <= 3; z++)
                origen.clone().add(x, -1, z).getBlock().setType(Material.SMOOTH_STONE);

        for (int i = 1; i <= 9; i++) {
            int ox = i * 5;
            switch (i % 5) {
                case 0 -> { for (int z = -3; z <= 3; z++) origen.clone().add(ox, 0, z).getBlock().setType(Material.STONE); }
                case 1 -> { for (int z = -3; z <= 3; z++) origen.clone().add(ox, -1, z).getBlock().setType(Material.AIR); }
                case 2 -> { for (int x = 0; x <= 2; x++) for (int z = -3; z <= 3; z++) origen.clone().add(ox + x, -1, z).getBlock().setType(Material.ICE); }
                case 3 -> { for (int z = -3; z <= 3; z++) origen.clone().add(ox, 0, z).getBlock().setType(Material.OAK_FENCE); }
                case 4 -> { origen.clone().add(ox, -1, -3).getBlock().setType(Material.LAVA); origen.clone().add(ox, -1, 3).getBlock().setType(Material.LAVA); }
            }
        }
        for (int z = -3; z <= 3; z++)
            origen.clone().add(50, 0, z).getBlock().setType(Material.GOLD_BLOCK);
    }

    @Override
    public void iniciar(List<Player> jugadores) {
        this.jugadores = new ArrayList<>(jugadores);
        this.tiempoJuego = 0;
        this.activo = true;
        int z = -2;
        for (Player p : jugadores) {
            p.teleport(origen.clone().add(0, 1, z++));
            p.sendTitle(ColorUtils.color("&e&lCARRERA"), ColorUtils.color("&7Llega al final!"), 10, 60, 10);
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    @Override
    public void tick() {
        if (!activo) return;
        tiempoJuego++;
        for (Player p : new ArrayList<>(jugadores)) {
            if (p.getLocation().getBlockX() >= origen.getBlockX() + 50 && !llegaron.contains(p.getUniqueId())) {
                llegaron.add(p.getUniqueId());
                p.sendTitle(ColorUtils.color("&a&lLLEGASTE!"),
                        ColorUtils.color("&7Posicion: #" + llegaron.size()), 10, 60, 10);
                jugadores.remove(p);
            }
        }
        if (jugadores.isEmpty() || tiempoJuego >= getTiempoLimite()) {
            for (Player p : new ArrayList<>(jugadores)) plugin.getGameManager().eliminarJugador(p);
            terminar();
        }
    }

    @Override public void terminar() {
        activo = false;
        if (task != null) task.cancel();
        ArenaBuilder.limpiar(origen, 55, 5, 8);
    }

    @Override public void onPlayerQuit(Player player) { jugadores.remove(player); }
}
