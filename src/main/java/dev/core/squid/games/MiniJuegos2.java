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

// ══════════════════════════════════════════════════════════════════
//  TIRA Y JALA
// ══════════════════════════════════════════════════════════════════
class TiraJala extends MiniGame {

    private Location origen;
    private List<Player> equipoA;
    private List<Player> equipoB;
    private List<Player> jugadores;
    private int posicionCuerda; // 0 = centro, negativo = equipo A gana, positivo = equipo B gana
    private int tiempoJuego;
    private Map<UUID, Integer> tirones;
    private BukkitTask task;

    public TiraJala(CoreSquid plugin) {
        super(plugin);
        this.equipoA = new ArrayList<>();
        this.equipoB = new ArrayList<>();
        this.tirones = new HashMap<>();
    }

    @Override public String getNombre() { return "Tira y Jala"; }
    @Override public String getDescripcion() { return "Haz clic rapido para jalar la cuerda!"; }
    @Override public int getTiempoLimite() { return 60; }

    @Override
    public void generarArena(Location origen) {
        this.origen = origen.clone();
        // Plataforma dividida con lava en el centro
        for (int x = -12; x <= 12; x++) {
            for (int z = -3; z <= 3; z++) {
                Material mat = x < 0 ? Material.BLUE_CONCRETE : (x > 0 ? Material.RED_CONCRETE : Material.LAVA);
                origen.clone().add(x, -1, z).getBlock().setType(mat);
            }
        }
        // Lineas de victoria
        for (int z = -3; z <= 3; z++) {
            origen.clone().add(-10, 0, z).getBlock().setType(Material.BLUE_STAINED_GLASS);
            origen.clone().add(10, 0, z).getBlock().setType(Material.RED_STAINED_GLASS);
        }
    }

    @Override
    public void iniciar(List<Player> jugadores) {
        this.jugadores = new ArrayList<>(jugadores);
        this.posicionCuerda = 0;
        this.tiempoJuego = 0;
        this.activo = true;

        // Dividir en equipos
        Collections.shuffle(jugadores);
        for (int i = 0; i < jugadores.size(); i++) {
            if (i % 2 == 0) equipoA.add(jugadores.get(i));
            else equipoB.add(jugadores.get(i));
        }

        // Teleportar
        for (Player p : equipoA) p.teleport(origen.clone().add(-5, 1, 0));
        for (Player p : equipoB) p.teleport(origen.clone().add(5, 1, 0));

        for (Player p : equipoA) p.sendTitle(ColorUtils.color("&9&lEQUIPO AZUL"), ColorUtils.color("&7Haz clic rapido!"), 10, 60, 10);
        for (Player p : equipoB) p.sendTitle(ColorUtils.color("&c&lEQUIPO ROJO"), ColorUtils.color("&7Haz clic rapido!"), 10, 60, 10);

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void jalar(Player player) {
        if (!activo) return;
        boolean esEquipoA = equipoA.contains(player);
        if (esEquipoA) posicionCuerda--;
        else posicionCuerda++;
        tirones.merge(player.getUniqueId(), 1, Integer::sum);
        SoundUtils.play(player, "coresquid.tira_cuerda");

        // Mostrar progreso
        player.sendTitle(ColorUtils.color(esEquipoA ? "&9 <<< " : "&c >>> "),
                ColorUtils.color("&7Posicion: " + posicionCuerda), 0, 10, 5);

        checkVictoria();
    }

    private void checkVictoria() {
        if (posicionCuerda <= -10) {
            // Equipo A gana
            for (Player p : equipoB) plugin.getGameManager().eliminarJugador(p);
            terminar();
        } else if (posicionCuerda >= 10) {
            // Equipo B gana
            for (Player p : equipoA) plugin.getGameManager().eliminarJugador(p);
            terminar();
        }
    }

    @Override public void tick() {
        if (!activo) return;
        tiempoJuego++;

        // Mostrar posicion a todos
        for (Player p : jugadores) {
            p.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&7Cuerda: " +
                    (posicionCuerda < 0 ? "&9<<" : "&c>>") + " " + posicionCuerda));
        }

        if (tiempoJuego >= getTiempoLimite()) {
            // Gana el equipo que tenga la cuerda mas hacia su lado
            if (posicionCuerda <= 0) {
                for (Player p : equipoB) plugin.getGameManager().eliminarJugador(p);
            } else {
                for (Player p : equipoA) plugin.getGameManager().eliminarJugador(p);
            }
            terminar();
        }
    }

    @Override public void terminar() {
        activo = false;
        if (task != null) task.cancel();
        equipoA.clear();
        equipoB.clear();
        ArenaBuilder.limpiar(origen, 28, 5, 8);
    }

    @Override public void onPlayerQuit(Player player) {
        jugadores.remove(player);
        equipoA.remove(player);
        equipoB.remove(player);
    }
}

// ══════════════════════════════════════════════════════════════════
//  DALGONA
// ══════════════════════════════════════════════════════════════════
class Dalgona extends MiniGame {

    private Location origen;
    private List<Player> jugadores;
    private Map<UUID, Location> figuraJugador;
    private Map<UUID, Set<Location>> bloquesFigura;
    private int tiempoJuego;
    private BukkitTask task;

    private static final Material[][] FIGURAS = {
        {Material.GOLD_BLOCK}, // Circulo simple
        {Material.DIAMOND_BLOCK}, // Triangulo
        {Material.EMERALD_BLOCK}, // Estrella
        {Material.REDSTONE_BLOCK}  // Paraguas
    };

    public Dalgona(CoreSquid plugin) {
        super(plugin);
        this.figuraJugador = new HashMap<>();
        this.bloquesFigura = new HashMap<>();
    }

    @Override public String getNombre() { return "Dalgona"; }
    @Override public String getDescripcion() { return "Rompe la figura sin romper el borde!"; }
    @Override public int getTiempoLimite() { return 120; }

    @Override
    public void generarArena(Location origen) {
        this.origen = origen.clone();
        // Celdas individuales
        for (int i = 0; i < 16; i++) {
            int ox = (i % 4) * 12;
            int oz = (i / 4) * 12;
            // Suelo de la celda
            for (int x = 0; x <= 9; x++)
                for (int z = 0; z <= 9; z++)
                    origen.clone().add(ox + x, -1, oz + z).getBlock().setType(Material.BROWN_CONCRETE);
            // Muro
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
            Location celda = origen.clone().add(ox + 4, 1, oz + 4);
            p.teleport(celda);

            // Generar figura en la celda
            Set<Location> bloques = generarFigura(origen.clone().add(ox + 4, 0, oz + 4), i % 4);
            figuraJugador.put(p.getUniqueId(), celda);
            bloquesFigura.put(p.getUniqueId(), bloques);
        }

        for (Player p : jugadores) {
            p.sendTitle(ColorUtils.color("&e&lDALGONA"),
                    ColorUtils.color("&7Rompe la figura sin romper el borde!"), 10, 80, 10);
            p.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&7Rompe solo los bloques de la FIGURA, no los del borde!"));
        }

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private Set<Location> generarFigura(Location centro, int tipo) {
        Set<Location> bloques = new HashSet<>();
        // Circulo simple
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (x*x + z*z <= 4) {
                    Location loc = centro.clone().add(x, 0, z);
                    loc.getBlock().setType(Material.HONEY_BLOCK);
                    bloques.add(loc);
                }
            }
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
                player.sendTitle(ColorUtils.color("&a&lCOMPLETADO!"),
                        ColorUtils.color("&7Lo lograste!"), 10, 60, 10);
                jugadores.remove(player);
                if (jugadores.isEmpty()) terminar();
            }
        } else {
            // Rompio el borde - eliminado
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
        figuraJugador.remove(player.getUniqueId());
        bloquesFigura.remove(player.getUniqueId());
    }
}

// ══════════════════════════════════════════════════════════════════
//  LABERINTO
// ══════════════════════════════════════════════════════════════════
class Laberinto extends MiniGame {

    private Location origen;
    private List<Player> jugadores;
    private Location meta;
    private int tiempoJuego;
    private BukkitTask task;

    public Laberinto(CoreSquid plugin) { super(plugin); }

    @Override public String getNombre() { return "Laberinto"; }
    @Override public String getDescripcion() { return "Llega a la meta antes que los demas!"; }
    @Override public int getTiempoLimite() { return 120; }

    @Override
    public void generarArena(Location origen) {
        this.origen = origen.clone();
        // Laberinto 20x20
        Random rand = new Random();
        for (int x = 0; x <= 20; x++) {
            for (int z = 0; z <= 20; z++) {
                origen.clone().add(x, -1, z).getBlock().setType(Material.STONE_BRICKS);
                // Muros con patron de laberinto simple
                if (x == 0 || x == 20 || z == 0 || z == 20) {
                    origen.clone().add(x, 0, z).getBlock().setType(Material.STONE_BRICK_WALL);
                    origen.clone().add(x, 1, z).getBlock().setType(Material.STONE_BRICK_WALL);
                } else if (rand.nextInt(4) == 0) {
                    Material wall = rand.nextBoolean() ? Material.STONE_BRICK_WALL : Material.AIR;
                    origen.clone().add(x, 0, z).getBlock().setType(wall);
                    if (wall != Material.AIR)
                        origen.clone().add(x, 1, z).getBlock().setType(wall);
                }
            }
        }
        // Meta
        this.meta = origen.clone().add(19, 0, 19);
        meta.getBlock().setType(Material.BEACON);
        meta.clone().add(0, -1, 0).getBlock().setType(Material.EMERALD_BLOCK);
    }

    @Override
    public void iniciar(List<Player> jugadores) {
        this.jugadores = new ArrayList<>(jugadores);
        this.tiempoJuego = 0;
        this.activo = true;

        for (Player p : jugadores) {
            p.teleport(origen.clone().add(1, 1, 1));
            p.sendTitle(ColorUtils.color("&e&lLABERINTO"),
                    ColorUtils.color("&7Llega a la meta!"), 10, 60, 10);
        }

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    @Override
    public void tick() {
        if (!activo) return;
        tiempoJuego++;

        // Verificar si alguien llego a la meta
        for (Player p : new ArrayList<>(jugadores)) {
            if (p.getLocation().distance(meta) <= 2) {
                p.sendTitle(ColorUtils.color("&a&lLLEGASTE!"),
                        ColorUtils.color("&7Eres el primero!"), 10, 60, 10);
                jugadores.remove(p);
                // Los demas son eliminados
                for (Player otro : new ArrayList<>(jugadores)) {
                    plugin.getGameManager().eliminarJugador(otro);
                }
                terminar();
                return;
            }
        }

        // Mostrar tiempo
        for (Player p : jugadores) {
            int restante = getTiempoLimite() - tiempoJuego;
            p.sendTitle(ColorUtils.color("&e" + restante + "s"),
                    ColorUtils.color("&7Llega a la &ameta!"), 0, 25, 5);
        }

        if (tiempoJuego >= getTiempoLimite()) {
            for (Player p : new ArrayList<>(jugadores)) plugin.getGameManager().eliminarJugador(p);
            terminar();
        }
    }

    @Override public void terminar() {
        activo = false;
        if (task != null) task.cancel();
        ArenaBuilder.limpiar(origen, 25, 5, 25);
    }

    @Override public void onPlayerQuit(Player player) { jugadores.remove(player); }
}

// ══════════════════════════════════════════════════════════════════
//  CARRERA DE OBSTACULOS
// ══════════════════════════════════════════════════════════════════
class CarreraObstaculos extends MiniGame {

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

        // Piso largo
        for (int x = 0; x <= 50; x++)
            for (int z = -3; z <= 3; z++)
                origen.clone().add(x, -1, z).getBlock().setType(Material.SMOOTH_STONE);

        // Obstaculos variados cada 5 bloques
        for (int i = 1; i <= 9; i++) {
            int ox = i * 5;
            int tipo = i % 5;
            switch (tipo) {
                case 0 -> { // Bloques para saltar
                    for (int z = -3; z <= 3; z++)
                        origen.clone().add(ox, 0, z).getBlock().setType(Material.STONE);
                }
                case 1 -> { // Agujeros (quitar suelo)
                    for (int z = -3; z <= 3; z++)
                        origen.clone().add(ox, -1, z).getBlock().setType(Material.AIR);
                }
                case 2 -> { // Bloques de hielo (resbaladizos)
                    for (int x = 0; x <= 2; x++)
                        for (int z = -3; z <= 3; z++)
                            origen.clone().add(ox + x, -1, z).getBlock().setType(Material.ICE);
                }
                case 3 -> { // Muros bajos para saltar
                    for (int z = -3; z <= 3; z++)
                        origen.clone().add(ox, 0, z).getBlock().setType(Material.FENCE);
                }
                case 4 -> { // Lava lateral
                    origen.clone().add(ox, -1, -3).getBlock().setType(Material.LAVA);
                    origen.clone().add(ox, -1, 3).getBlock().setType(Material.LAVA);
                }
            }
        }

        // Meta
        for (int z = -3; z <= 3; z++) {
            origen.clone().add(50, 0, z).getBlock().setType(Material.GOLD_BLOCK);
        }
    }

    @Override
    public void iniciar(List<Player> jugadores) {
        this.jugadores = new ArrayList<>(jugadores);
        this.tiempoJuego = 0;
        this.activo = true;

        int z = -2;
        for (Player p : jugadores) {
            p.teleport(origen.clone().add(0, 1, z));
            z += 1;
            p.sendTitle(ColorUtils.color("&e&lCARRERA"), ColorUtils.color("&7Llega al final!"), 10, 60, 10);
        }

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    @Override
    public void tick() {
        if (!activo) return;
        tiempoJuego++;

        // Verificar llegadas
        for (Player p : new ArrayList<>(jugadores)) {
            if (p.getLocation().getBlockX() >= origen.getBlockX() + 50 && !llegaron.contains(p.getUniqueId())) {
                llegaron.add(p.getUniqueId());
                p.sendTitle(ColorUtils.color("&a&lLLEGASTE!"),
                        ColorUtils.color("&7Posicion: #" + llegaron.size()), 10, 60, 10);
                jugadores.remove(p);
            }
        }

        // Solo el ultimo es eliminado
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
