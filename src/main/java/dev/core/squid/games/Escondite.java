package dev.core.squid.games;

import dev.core.squid.CoreSquid;
import dev.core.squid.utils.ArenaBuilder;
import dev.core.squid.utils.ColorUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class Escondite extends MiniGame {

    private Location origen;
    private List<Player> jugadores;
    private Player buscador;
    private Set<UUID> encontrados;
    private int tiempoJuego;
    private BukkitTask task;

    public Escondite(CoreSquid plugin) {
        super(plugin);
        this.encontrados = new HashSet<>();
    }

    @Override public String getNombre() { return "Escondite"; }
    @Override public String getDescripcion() { return "Escondete del buscador!"; }
    @Override public int getTiempoLimite() { return 90; }

    @Override
    public void generarArena(Location origen) {
        this.origen = origen.clone();
        Material[] mats = {Material.OAK_LOG, Material.SPRUCE_LOG, Material.DARK_OAK_LOG};
        for (int x = -15; x <= 15; x++) {
            for (int z = -15; z <= 15; z++) {
                origen.clone().add(x, -1, z).getBlock().setType(Material.GRASS_BLOCK);
                if ((x % 4 == 0 || z % 4 == 0) && Math.random() < 0.4) {
                    Material mat = mats[new Random().nextInt(mats.length)];
                    origen.clone().add(x, 0, z).getBlock().setType(mat);
                    origen.clone().add(x, 1, z).getBlock().setType(mat);
                }
            }
        }
    }

    @Override
    public void iniciar(List<Player> jugadores) {
        this.jugadores = new ArrayList<>(jugadores);
        this.encontrados = new HashSet<>();
        this.tiempoJuego = 0;
        this.activo = true;

        buscador = jugadores.get(new Random().nextInt(jugadores.size()));
        buscador.teleport(origen.clone().add(0, 1, 0));
        buscador.sendTitle(ColorUtils.color("&c&lERES EL BUSCADOR"),
                ColorUtils.color("&7Espera 10 segundos!"), 10, 80, 10);

        int ang = 0;
        for (Player p : jugadores) {
            if (p == buscador) continue;
            double rad = Math.toRadians(ang);
            p.teleport(origen.clone().add(12 * Math.cos(rad), 1, 12 * Math.sin(rad)));
            ang += 360 / (jugadores.size() - 1);
            p.sendTitle(ColorUtils.color("&e&lESCONDETE!"),
                    ColorUtils.color("&7Tienes 10 segundos!"), 10, 80, 10);
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
            buscador.sendTitle(ColorUtils.color("&c&lA BUSCAR!"),
                    ColorUtils.color(""), 5, 30, 5), 200L);

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void onGolpear(Player atacante, Player victima) {
        if (!activo || atacante != buscador) return;
        if (encontrados.contains(victima.getUniqueId())) return;
        encontrados.add(victima.getUniqueId());
        plugin.getGameManager().eliminarJugador(victima);
        jugadores.remove(victima);
        for (Player p : jugadores)
            p.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&e" + victima.getName() + " &7fue encontrado!"));
        if (jugadores.stream().allMatch(p -> p == buscador || encontrados.contains(p.getUniqueId())))
            terminar();
    }

    @Override
    public void tick() {
        if (!activo) return;
        tiempoJuego++;
        if (buscador != null && buscador.isOnline()) {
            buscador.sendTitle(ColorUtils.color("&c" + (getTiempoLimite() - tiempoJuego) + "s"),
                    ColorUtils.color("&7Faltan &e" + (jugadores.size() - 1 - encontrados.size()) + " &7jugadores"),
                    0, 25, 5);
        }
        if (tiempoJuego >= getTiempoLimite()) {
            if (buscador != null) plugin.getGameManager().eliminarJugador(buscador);
            terminar();
        }
    }

    @Override public void terminar() {
        activo = false;
        if (task != null) task.cancel();
        ArenaBuilder.limpiar(origen, 35, 5, 35);
    }

    @Override public void onPlayerQuit(Player player) {
        jugadores.remove(player);
        if (player == buscador) buscador = jugadores.isEmpty() ? null : jugadores.get(0);
    }
}
