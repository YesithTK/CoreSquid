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

public class MemoriaBloques extends MiniGame {

    private Location origen;
    private List<Player> jugadores;
    private List<Material> secuencia;
    private List<Location> botones;
    private Map<UUID, List<Material>> respuestas;
    private int tiempoJuego;
    private BukkitTask task;

    private static final Material[] COLORES = {
        Material.RED_WOOL, Material.BLUE_WOOL, Material.GREEN_WOOL,
        Material.YELLOW_WOOL, Material.PURPLE_WOOL, Material.ORANGE_WOOL
    };

    public MemoriaBloques(CoreSquid plugin) {
        super(plugin);
        this.secuencia = new ArrayList<>();
        this.botones = new ArrayList<>();
        this.respuestas = new HashMap<>();
    }

    @Override public String getNombre() { return "Memoria de Bloques"; }
    @Override public String getDescripcion() { return "Repite la secuencia de bloques de colores!"; }
    @Override public int getTiempoLimite() { return 120; }

    @Override
    public void generarArena(Location origen) {
        this.origen = origen.clone();
        for (int x = -6; x <= 6; x++)
            for (int z = -6; z <= 6; z++)
                origen.clone().add(x, -1, z).getBlock().setType(Material.BLACK_CONCRETE);

        int[] bx = {-4, -2, 0, 2, 4, -1};
        int[] bz = {0, 0, 0, 0, 0, 2};
        for (int i = 0; i < 6; i++) {
            Location btn = origen.clone().add(bx[i], 0, bz[i]);
            btn.getBlock().setType(COLORES[i]);
            botones.add(btn);
        }
    }

    @Override
    public void iniciar(List<Player> jugadores) {
        this.jugadores = new ArrayList<>(jugadores);
        this.tiempoJuego = 0;
        this.activo = true;
        for (Player p : jugadores) p.teleport(origen.clone().add(0, 1, -4));
        mostrarSecuencia();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void mostrarSecuencia() {
        secuencia.add(COLORES[new Random().nextInt(COLORES.length)]);
        for (int i = 0; i < secuencia.size(); i++) {
            final Material mat = secuencia.get(i);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (Location btn : botones) {
                    if (btn.getBlock().getType() == mat) {
                        btn.getBlock().setType(Material.GLOWSTONE);
                        plugin.getServer().getScheduler().runTaskLater(plugin,
                                () -> btn.getBlock().setType(mat), 10L);
                    }
                }
                for (Player p : jugadores) SoundUtils.play(p, "coresquid.memoria_bloque");
            }, (i + 1) * 15L);
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            respuestas.clear();
            for (Player p : jugadores) {
                respuestas.put(p.getUniqueId(), new ArrayList<>());
                p.sendTitle(ColorUtils.color("&e&lTU TURNO"),
                        ColorUtils.color("&7Repite la secuencia!"), 5, 30, 5);
            }
        }, (secuencia.size() + 1) * 15L);
    }

    public void onClic(Player player, Location loc) {
        if (!activo) return;
        List<Material> resp = respuestas.get(player.getUniqueId());
        if (resp == null) return;
        resp.add(loc.getBlock().getType());
        SoundUtils.play(player, "coresquid.memoria_clic");
        if (resp.size() == secuencia.size()) {
            if (!resp.equals(secuencia)) {
                plugin.getGameManager().eliminarJugador(player);
                jugadores.remove(player);
                respuestas.remove(player.getUniqueId());
            } else {
                player.sendTitle(ColorUtils.color("&a&lCORRECTO!"), ColorUtils.color(""), 5, 30, 5);
            }
            if (respuestas.values().stream().allMatch(r -> r.size() >= secuencia.size())) {
                if (jugadores.size() <= 1) { terminar(); return; }
                plugin.getServer().getScheduler().runTaskLater(plugin, this::mostrarSecuencia, 40L);
            }
        }
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

    @Override public void onPlayerQuit(Player player) {
        jugadores.remove(player);
        respuestas.remove(player.getUniqueId());
    }
}
