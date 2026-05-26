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

public class Dalgona extends MiniGame {

    private Location origen;
    private List<Player> jugadores;
    private Map<UUID, Set<Location>> bloquesFigura;
    private int tiempoJuego;
    private BukkitTask task;

    public Dalgona(CoreSquid plugin) {
        super(plugin);
        this.bloquesFigura = new HashMap<>();
    }

    @Override public String getNombre() { return "Dalgona"; }
    @Override public String getDescripcion() { return "Rompe la figura sin romper el borde!"; }
    @Override public int getTiempoLimite() { return 120; }

    @Override
    public void generarArena(Location origen) {
        this.origen = origen.clone();
        for (int i = 0; i < 16; i++) {
            int ox = (i % 4) * 12;
            int oz = (i / 4) * 12;
            for (int x = 0; x <= 9; x++)
                for (int z = 0; z <= 9; z++)
                    origen.clone().add(ox + x, -1, oz + z).getBlock().setType(Material.BROWN_CONCRETE);
            for (int x = 0; x <= 9; x++) {
                origen.clone().add(ox + x, 0, oz).getBlock().setType(Material.BARRIER);
                origen.clone().add(ox + x, 0, oz + 9).getBlock().setType(Material.BARRIER);
            }
            for (int z = 0; z <= 9; z++) {
                origen.clone().add(ox, 0, oz + z).getBlock().setType(Material.BARRIER);
                origen.clone().add(ox + 9, 0, oz + z).getBlock().setType(Material.BARRIER);
            }
        }
    }

    @Override
    public void iniciar(List<Player> jugadores) {
        this.jugadores = new ArrayList<>(jugadores);
        this.tiempoJuego = 0;
        this.activo = true;
        for (int i = 0; i < jugadores.size(); i++) {
            Player p = jugadores.get(i);
            int ox = (i % 4) * 12;
            int oz = (i / 4) * 12;
            p.teleport(origen.clone().add(ox + 4, 1, oz + 4));
            bloquesFigura.put(p.getUniqueId(), generarFigura(origen.clone().add(ox + 4, 0, oz + 4)));
        }
        for (Player p : jugadores)
            p.sendTitle(ColorUtils.color("&e&lDALGONA"),
                    ColorUtils.color("&7Rompe la figura sin romper el borde!"), 10, 80, 10);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private Set<Location> generarFigura(Location centro) {
        Set<Location> bloques = new HashSet<>();
        for (int x = -2; x <= 2; x++)
            for (int z = -2; z <= 2; z++)
                if (x*x + z*z <= 4) {
                    Location loc = centro.clone().add(x, 0, z);
                    loc.getBlock().setType(Material.HONEY_BLOCK);
                    bloques.add(loc);
                }
        return bloques;
    }

    public void onRomperBloque(Player player, Location loc) {
        if (!activo) return;
        Set<Location> bloques = bloquesFigura.get(player.getUniqueId());
        if (bloques == null) return;
        if (bloques.contains(loc)) {
            bloques.remove(loc);
            SoundUtils.play(player, "coresquid.dalgona_rompe");
            if (bloques.isEmpty()) {
                player.sendTitle(ColorUtils.color("&a&lCOMPLETADO!"), ColorUtils.color(""), 10, 60, 10);
                jugadores.remove(player);
                if (jugadores.isEmpty()) terminar();
            }
        } else {
            player.sendTitle(ColorUtils.color("&c&lELIMINADO!"),
                    ColorUtils.color("&7Rompiste el borde!"), 10, 60, 10);
            plugin.getGameManager().eliminarJugador(player);
            jugadores.remove(player);
            if (jugadores.isEmpty()) terminar();
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
        ArenaBuilder.limpiar(origen, 50, 5, 50);
    }

    @Override public void onPlayerQuit(Player player) {
        jugadores.remove(player);
        bloquesFigura.remove(player.getUniqueId());
    }
}
