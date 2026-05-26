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

public class Canicas extends MiniGame {

    private Location origen;
    private List<Player> jugadores;
    private Map<UUID, Player> parejas;
    private Map<UUID, Integer> canicas;
    private Map<UUID, Boolean> apuestas; // true = par, false = impar
    private Map<UUID, Integer> apuestaCantidad;
    private int tiempoJuego;
    private int fase; // 0 = elegir apuesta, 1 = resultado
    private BukkitTask task;

    public Canicas(CoreSquid plugin) {
        super(plugin);
        this.parejas = new HashMap<>();
        this.canicas = new HashMap<>();
        this.apuestas = new HashMap<>();
        this.apuestaCantidad = new HashMap<>();
    }

    @Override
    public String getNombre() { return "Canicas"; }

    @Override
    public String getDescripcion() { return "Adivina si el numero de canicas es par o impar!"; }

    @Override
    public int getTiempoLimite() { return 60; }

    @Override
    public void generarArena(Location origen) {
        this.origen = origen.clone();

        // Celdas individuales para cada pareja (5x5 cada una)
        int pares = 8;
        for (int i = 0; i < pares; i++) {
            int ox = i * 7;
            for (int x = 0; x <= 4; x++) {
                for (int z = 0; z <= 4; z++) {
                    origen.clone().add(ox + x, -1, z).getBlock().setType(Material.GRAY_CONCRETE);
                    if (x == 0 || x == 4 || z == 0 || z == 4) {
                        origen.clone().add(ox + x, 0, z).getBlock().setType(Material.BARRIER);
                        origen.clone().add(ox + x, 1, z).getBlock().setType(Material.BARRIER);
                    }
                }
            }
        }
    }

    @Override
    public void iniciar(List<Player> jugadores) {
        this.jugadores = new ArrayList<>(jugadores);
        this.tiempoJuego = 0;
        this.fase = 0;
        this.activo = true;

        // Emparejar jugadores
        List<Player> shuffled = new ArrayList<>(jugadores);
        Collections.shuffle(shuffled);

        for (int i = 0; i + 1 < shuffled.size(); i += 2) {
            Player a = shuffled.get(i);
            Player b = shuffled.get(i + 1);
            parejas.put(a.getUniqueId(), b);
            parejas.put(b.getUniqueId(), a);

            // Dar 10 canicas a cada uno
            canicas.put(a.getUniqueId(), 10);
            canicas.put(b.getUniqueId(), 10);

            // Teleportar a celda
            int celda = i / 2;
            a.teleport(origen.clone().add(celda * 7 + 1, 1, 1));
            b.teleport(origen.clone().add(celda * 7 + 3, 1, 1));
        }

        // Si hay jugador sin pareja, pasa automaticamente
        if (shuffled.size() % 2 != 0) {
            Player solito = shuffled.get(shuffled.size() - 1);
            solito.sendTitle(ColorUtils.color("&aSin pareja"), ColorUtils.color("&7Pasas automaticamente!"), 10, 60, 10);
            jugadores.remove(solito);
        }

        // Instrucciones
        for (Player p : jugadores) {
            p.sendTitle(ColorUtils.color("&e&lCANICAS"), ColorUtils.color("&7Escribe /squid par o /squid impar y una cantidad"), 10, 80, 10);
            p.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&7Tienes &e10 &7canicas. Escribe: &e/squid apostar <par|impar> <cantidad>"));
        }

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void procesarApuesta(Player player, boolean esPar, int cantidad) {
        if (!activo || fase != 0) return;
        int misCanicas = canicas.getOrDefault(player.getUniqueId(), 0);
        if (cantidad < 1 || cantidad > misCanicas) {
            player.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&cCantidad invalida. Tienes &e" + misCanicas + " &7canicas."));
            return;
        }
        apuestas.put(player.getUniqueId(), esPar);
        apuestaCantidad.put(player.getUniqueId(), cantidad);
        player.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&aApuesta registrada: &e" + (esPar ? "PAR" : "IMPAR") + " &7x" + cantidad));

        // Verificar si todos apostaron
        if (apuestas.size() >= jugadores.size()) {
            resolverApuestas();
        }
    }

    private void resolverApuestas() {
        fase = 1;
        Random rand = new Random();

        for (UUID uuid : new HashSet<>(parejas.keySet())) {
            Player jugador = plugin.getServer().getPlayer(uuid);
            Player rival = parejas.get(uuid);
            if (jugador == null || rival == null) continue;
            if (!apuestas.containsKey(uuid)) continue;

            // Numero aleatorio de canicas ocultas del rival (1-10)
            int canicasOcultas = rand.nextInt(canicas.getOrDefault(rival.getUniqueId(), 10)) + 1;
            boolean esPar = canicasOcultas % 2 == 0;
            boolean adivinó = apuestas.get(uuid) == esPar;
            int cantidad = apuestaCantidad.getOrDefault(uuid, 1);

            if (adivinó) {
                canicas.merge(uuid, cantidad, Integer::sum);
                canicas.merge(rival.getUniqueId(), -cantidad, Integer::sum);
                jugador.sendTitle(ColorUtils.color("&a&lCORRECTO!"),
                        ColorUtils.color("&7+" + cantidad + " canicas"), 10, 60, 10);
            } else {
                canicas.merge(uuid, -cantidad, Integer::sum);
                canicas.merge(rival.getUniqueId(), cantidad, Integer::sum);
                jugador.sendTitle(ColorUtils.color("&c&lINCORRECTO"),
                        ColorUtils.color("&7-" + cantidad + " canicas"), 10, 60, 10);
            }

            jugador.sendMessage(ColorUtils.color(plugin.getPrefijo() +
                    "&7Canicas ocultas del rival: &e" + canicasOcultas + " &7(" + (esPar ? "PAR" : "IMPAR") + "). Tienes: &e" + canicas.get(uuid)));
        }

        // Eliminar a los que quedaron sin canicas
        for (UUID uuid : new HashSet<>(canicas.keySet())) {
            if (canicas.getOrDefault(uuid, 0) <= 0) {
                Player p = plugin.getServer().getPlayer(uuid);
                if (p != null) plugin.getGameManager().eliminarJugador(p);
            }
        }

        apuestas.clear();
        apuestaCantidad.clear();

        // Verificar si quedan jugadores para otra ronda
        if (jugadores.stream().noneMatch(p -> canicas.getOrDefault(p.getUniqueId(), 0) > 0)) {
            terminar();
        } else {
            fase = 0;
            for (Player p : jugadores) {
                if (canicas.getOrDefault(p.getUniqueId(), 0) > 0) {
                    p.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&7Nueva ronda. Canicas: &e" + canicas.get(p.getUniqueId())));
                }
            }
        }
    }

    @Override
    public void tick() {
        if (!activo) return;
        tiempoJuego++;
        if (tiempoJuego >= getTiempoLimite()) {
            // Eliminar a los que no apostaron
            for (Player p : new ArrayList<>(jugadores)) {
                if (!apuestas.containsKey(p.getUniqueId())) {
                    plugin.getGameManager().eliminarJugador(p);
                }
            }
            terminar();
        }
    }

    @Override
    public void terminar() {
        activo = false;
        if (task != null) task.cancel();
        ArenaBuilder.limpiar(origen, 60, 5, 10);
    }

    @Override
    public void onPlayerQuit(Player player) {
        jugadores.remove(player);
        parejas.remove(player.getUniqueId());
        canicas.remove(player.getUniqueId());
        apuestas.remove(player.getUniqueId());
    }
}
