package dev.core.squid.games;

import dev.core.squid.CoreSquid;
import dev.core.squid.utils.ArenaBuilder;
import dev.core.squid.utils.ColorUtils;
import dev.core.squid.utils.SoundUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class BombaCaliente extends MiniGame {

    private Location origen;
    private List<Player> jugadores;
    private Player conBomba;
    private int tiempoBomba;
    private int tiempoJuego;
    private BukkitTask task;

    public BombaCaliente(CoreSquid plugin) { super(plugin); }

    @Override public String getNombre() { return "Bomba Caliente"; }
    @Override public String getDescripcion() { return "Pasa la bomba antes de que explote!"; }
    @Override public int getTiempoLimite() { return 90; }

    @Override
    public void generarArena(Location origen) {
        this.origen = origen.clone();
        for (int x = -8; x <= 8; x++)
            for (int z = -8; z <= 8; z++)
                if (x*x + z*z <= 64)
                    origen.clone().add(x, -1, z).getBlock().setType(Material.CRIMSON_PLANKS);
    }

    @Override
    public void iniciar(List<Player> jugadores) {
        this.jugadores = new ArrayList<>(jugadores);
        this.activo = true;
        this.tiempoJuego = 0;
        resetearBomba();

        int ang = 0;
        for (Player p : jugadores) {
            double rad = Math.toRadians(ang);
            p.teleport(origen.clone().add(5 * Math.cos(rad), 1, 5 * Math.sin(rad)));
            ang += 360 / jugadores.size();
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void resetearBomba() {
        conBomba = jugadores.get(new Random().nextInt(jugadores.size()));
        tiempoBomba = new Random().nextInt(10) + 5;
        conBomba.getInventory().setItem(4, new ItemStack(Material.TNT));
        conBomba.sendTitle(ColorUtils.color("&c&lTIENES LA BOMBA!"),
                ColorUtils.color("&7Pasala haciendo clic en otro jugador!"), 5, 40, 5);
        SoundUtils.play(conBomba, "coresquid.bomba_activa");
    }

    @Override
    public void tick() {
        if (!activo) return;
        tiempoJuego++;
        tiempoBomba--;

        if (conBomba != null) {
            conBomba.sendTitle(ColorUtils.color("&c&l" + tiempoBomba),
                    ColorUtils.color("&7PASA LA BOMBA!"), 0, 25, 5);
        }

        if (tiempoBomba <= 0) {
            if (conBomba != null) {
                conBomba.getInventory().remove(Material.TNT);
                conBomba.getWorld().createExplosion(conBomba.getLocation(), 0F, false, false);
                plugin.getGameManager().eliminarJugador(conBomba);
                jugadores.remove(conBomba);
                conBomba = null;
            }
            if (jugadores.size() <= 1) { terminar(); return; }
            resetearBomba();
        }

        if (tiempoJuego >= getTiempoLimite()) terminar();
    }

    public void pasarBomba(Player de, Player a) {
        if (!activo || de != conBomba) return;
        if (!jugadores.contains(a)) return;
        de.getInventory().remove(Material.TNT);
        conBomba = a;
        a.getInventory().setItem(4, new ItemStack(Material.TNT));
        a.sendTitle(ColorUtils.color("&c&lTIENES LA BOMBA!"), ColorUtils.color("&7Pasala rapido!"), 5, 30, 5);
        SoundUtils.play(a, "coresquid.bomba_activa");
    }

    @Override
    public void terminar() {
        activo = false;
        if (task != null) task.cancel();
        if (conBomba != null) conBomba.getInventory().remove(Material.TNT);
        ArenaBuilder.limpiar(origen, 20, 5, 20);
    }

    @Override
    public void onPlayerQuit(Player player) {
        jugadores.remove(player);
        if (conBomba == player) {
            conBomba = null;
            if (!jugadores.isEmpty()) resetearBomba();
        }
    }
}
