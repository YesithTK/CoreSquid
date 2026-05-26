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

// ══════════════════════════════════════════════════════════════════
//  BOMBA CALIENTE
// ══════════════════════════════════════════════════════════════════
class BombaCaliente extends MiniGame {

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
                ColorUtils.color("&7Pásala haciendo clic en otro jugador!"), 5, 40, 5);
        SoundUtils.play(conBomba, "coresquid.bomba_activa");
    }

    @Override
    public void tick() {
        if (!activo) return;
        tiempoJuego++;
        tiempoBomba--;

        if (conBomba != null) {
            conBomba.sendTitle(ColorUtils.color("&c\u00a7l" + tiempoBomba),
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

    @Override public void terminar() {
        activo = false;
        if (task != null) task.cancel();
        if (conBomba != null) conBomba.getInventory().remove(Material.TNT);
        ArenaBuilder.limpiar(origen, 20, 5, 20);
    }

    @Override public void onPlayerQuit(Player player) {
        jugadores.remove(player);
        if (conBomba == player) { conBomba = null; if (!jugadores.isEmpty()) resetearBomba(); }
    }
}

// ══════════════════════════════════════════════════════════════════
//  MEMORIA DE BLOQUES
// ══════════════════════════════════════════════════════════════════
class MemoriaBloques extends MiniGame {

    private Location origen;
    private List<Player> jugadores;
    private List<Material> secuencia;
    private List<Location> botones;
    private Map<UUID, List<Material>> respuestas;
    private int ronda;
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

        // Crear 6 botones de colores
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
        this.ronda = 1;
        this.tiempoJuego = 0;
        this.activo = true;

        for (Player p : jugadores) {
            p.teleport(origen.clone().add(0, 1, -4));
        }

        mostrarSecuencia();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void mostrarSecuencia() {
        secuencia.add(COLORES[new Random().nextInt(COLORES.length)]);

        // Mostrar secuencia con delay entre cada bloque
        for (int i = 0; i < secuencia.size(); i++) {
            final int idx = i;
            final Material mat = secuencia.get(i);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (Location btn : botones) {
                    if (btn.getBlock().getType() == mat) {
                        btn.getBlock().setType(Material.GLOWSTONE);
                        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                                btn.getBlock().setType(mat), 10L);
                    }
                }
                for (Player p : jugadores) {
                    SoundUtils.play(p, "coresquid.memoria_bloque");
                }
            }, (i + 1) * 15L);
        }

        // Avisar que ya pueden responder
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

        Material clicked = loc.getBlock().getType();
        resp.add(clicked);
        SoundUtils.play(player, "coresquid.memoria_clic");

        if (resp.size() == secuencia.size()) {
            boolean correcto = resp.equals(secuencia);
            if (!correcto) {
                plugin.getGameManager().eliminarJugador(player);
                jugadores.remove(player);
                respuestas.remove(player.getUniqueId());
            } else {
                player.sendTitle(ColorUtils.color("&a&lCORRECTO!"), ColorUtils.color(""), 5, 30, 5);
            }

            // Si todos respondieron, siguiente ronda
            if (respuestas.values().stream().allMatch(r -> r.size() >= secuencia.size())) {
                if (jugadores.size() <= 1) { terminar(); return; }
                ronda++;
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

// ══════════════════════════════════════════════════════════════════
//  PLATAFORMAS QUE CAEN
// ══════════════════════════════════════════════════════════════════
class PlataformasCaen extends MiniGame {

    private Location origen;
    private List<Player> jugadores;
    private List<Location> plataformas;
    private int tiempoJuego;
    private BukkitTask task;

    public PlataformasCaen(CoreSquid plugin) {
        super(plugin);
        this.plataformas = new ArrayList<>();
    }

    @Override public String getNombre() { return "Plataformas que Caen"; }
    @Override public String getDescripcion() { return "No caigas! Las plataformas desaparecen!"; }
    @Override public int getTiempoLimite() { return 90; }

    @Override
    public void generarArena(Location origen) {
        this.origen = origen.clone();
        // 4 niveles de plataformas
        Material[] mats = {Material.RED_CONCRETE, Material.ORANGE_CONCRETE,
                           Material.YELLOW_CONCRETE, Material.LIME_CONCRETE};
        for (int nivel = 0; nivel < 4; nivel++) {
            int y = nivel * 5;
            for (int x = -8; x <= 8; x += 3) {
                for (int z = -8; z <= 8; z += 3) {
                    Location plat = origen.clone().add(x, y, z);
                    plat.getBlock().setType(mats[nivel]);
                    plataformas.add(plat);
                }
            }
        }
        // Lava abajo
        for (int x = -10; x <= 10; x++)
            for (int z = -10; z <= 10; z++)
                origen.clone().add(x, -2, z).getBlock().setType(Material.LAVA);
    }

    @Override
    public void iniciar(List<Player> jugadores) {
        this.jugadores = new ArrayList<>(jugadores);
        this.tiempoJuego = 0;
        this.activo = true;

        // Teleportar al nivel mas alto
        int idx = 0;
        for (Player p : jugadores) {
            p.teleport(origen.clone().add((idx % 6) * 3 - 7, 16, (idx / 6) * 3 - 7));
            idx++;
        }

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    @Override
    public void tick() {
        if (!activo) return;
        tiempoJuego++;

        // Cada 3 segundos quitar una plataforma aleatoria
        if (tiempoJuego % 3 == 0 && !plataformas.isEmpty()) {
            int idx = new Random().nextInt(plataformas.size());
            Location plat = plataformas.remove(idx);
            // Parpadear antes de caer
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                plat.getBlock().setType(Material.AIR), 10L);

            for (Player p : jugadores) {
                SoundUtils.play(p, "coresquid.plataforma_cae");
            }
        }

        // Verificar jugadores caidos (y = origen.y - 1 o menos)
        for (Player p : new ArrayList<>(jugadores)) {
            if (p.getLocation().getY() <= origen.getY() - 1) {
                plugin.getGameManager().eliminarJugador(p);
                jugadores.remove(p);
            }
        }

        if (jugadores.size() <= 1 || tiempoJuego >= getTiempoLimite()) terminar();
    }

    @Override public void terminar() {
        activo = false;
        if (task != null) task.cancel();
        ArenaBuilder.limpiar(origen, 20, 25, 20);
    }

    @Override public void onPlayerQuit(Player player) { jugadores.remove(player); }
}

// ══════════════════════════════════════════════════════════════════
//  ADIVINA EL NUMERO
// ══════════════════════════════════════════════════════════════════
class AdivinaNumero extends MiniGame {

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
                    ColorUtils.color("&7Entre 1 y 100. Escribe /squid adivinar <numero>"), 10, 80, 10);
            p.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&7Tienes &e" + intentosMax + " &7intentos. Escribe: &e/squid adivinar <numero>"));
        }

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void procesarIntento(Player player, int numero) {
        if (!activo) return;
        int intentosUsados = intentos.getOrDefault(player.getUniqueId(), 0) + 1;
        intentos.put(player.getUniqueId(), intentosUsados);

        if (numero == numeroSecreto) {
            player.sendTitle(ColorUtils.color("&a&lCORRECTO!"),
                    ColorUtils.color("&7Era el &e" + numeroSecreto), 10, 60, 10);
            jugadores.remove(player);
            if (jugadores.isEmpty()) terminar();
        } else {
            String pista = numero < numeroSecreto ? "&7El numero es &amayor" : "&7El numero es &cmenor";
            player.sendMessage(ColorUtils.color(plugin.getPrefijo() + pista +
                    " &7. Intentos: &e" + intentosUsados + "&7/&e" + intentosMax));
            if (intentosUsados >= intentosMax) {
                player.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&cAgotaste tus intentos. Era el &e" + numeroSecreto));
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

// ══════════════════════════════════════════════════════════════════
//  ESCONDITE
// ══════════════════════════════════════════════════════════════════
class Escondite extends MiniGame {

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
        // Laberinto simple con muros
        Material[] mats = {Material.OAK_LOG, Material.SPRUCE_LOG, Material.DARK_OAK_LOG};
        for (int x = -15; x <= 15; x++) {
            for (int z = -15; z <= 15; z++) {
                origen.clone().add(x, -1, z).getBlock().setType(Material.GRASS_BLOCK);
                // Muros aleatorios para esconderse
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

        // Elegir buscador
        buscador = jugadores.get(new Random().nextInt(jugadores.size()));
        buscador.teleport(origen.clone().add(0, 1, 0));
        buscador.sendTitle(ColorUtils.color("&c&lERES EL BUSCADOR"),
                ColorUtils.color("&7Espera 10 segundos y busca a todos!"), 10, 80, 10);

        // Teleportar a los demas
        int ang = 0;
        for (Player p : jugadores) {
            if (p == buscador) continue;
            double rad = Math.toRadians(ang);
            p.teleport(origen.clone().add(12 * Math.cos(rad), 1, 12 * Math.sin(rad)));
            ang += 360 / (jugadores.size() - 1);
            p.sendTitle(ColorUtils.color("&e&lESCONDETE!"),
                    ColorUtils.color("&7Tienes 10 segundos!"), 10, 80, 10);
        }

        // Cegar al buscador 10 segundos
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            buscador.sendTitle(ColorUtils.color("&c&lA BUSCAR!"), ColorUtils.color("&7Encuentra a todos!"), 5, 30, 5);
        }, 200L);

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void onGolpear(Player atacante, Player victima) {
        if (!activo || atacante != buscador) return;
        if (encontrados.contains(victima.getUniqueId())) return;
        encontrados.add(victima.getUniqueId());
        plugin.getGameManager().eliminarJugador(victima);
        jugadores.remove(victima);

        for (Player p : jugadores) {
            p.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&e" + victima.getName() + " &7fue encontrado!"));
        }

        // Si todos encontrados, el buscador gana... o el ultimo sobreviviente
        if (jugadores.stream().allMatch(p -> p == buscador || encontrados.contains(p.getUniqueId()))) {
            terminar();
        }
    }

    @Override public void tick() {
        if (!activo) return;
        tiempoJuego++;
        // Tiempo restante al buscador
        if (buscador != null && buscador.isOnline()) {
            int restante = getTiempoLimite() - tiempoJuego;
            buscador.sendTitle(ColorUtils.color("&c" + restante + "s"),
                    ColorUtils.color("&7Faltan &e" + (jugadores.size() - 1 - encontrados.size()) + " &7jugadores"), 0, 25, 5);
        }
        if (tiempoJuego >= getTiempoLimite()) {
            // Los que no fueron encontrados ganan
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

// ══════════════════════════════════════════════════════════════════
//  REY DE LA COLINA
// ══════════════════════════════════════════════════════════════════
class ReyColina extends MiniGame {

    private Location origen;
    private List<Player> jugadores;
    private Player rey;
    private Map<UUID, Integer> puntos;
    private int tiempoJuego;
    private BukkitTask task;
    private static final int PUNTOS_PARA_GANAR = 30;

    public ReyColina(CoreSquid plugin) {
        super(plugin);
        this.puntos = new HashMap<>();
    }

    @Override public String getNombre() { return "Rey de la Colina"; }
    @Override public String getDescripcion() { return "Mantente en la cima el mayor tiempo posible!"; }
    @Override public int getTiempoLimite() { return 120; }

    @Override
    public void generarArena(Location origen) {
        this.origen = origen.clone();
        // Colina piramidal
        for (int nivel = 0; nivel <= 5; nivel++) {
            int radio = 5 - nivel;
            for (int x = -radio; x <= radio; x++)
                for (int z = -radio; z <= radio; z++)
                    if (x*x + z*z <= radio*radio + radio)
                        origen.clone().add(x, nivel - 1, z).getBlock().setType(Material.STONE);
        }
    }

    @Override
    public void iniciar(List<Player> jugadores) {
        this.jugadores = new ArrayList<>(jugadores);
        this.tiempoJuego = 0;
        this.activo = true;
        jugadores.forEach(p -> puntos.put(p.getUniqueId(), 0));

        int ang = 0;
        for (Player p : jugadores) {
            double rad = Math.toRadians(ang);
            p.teleport(origen.clone().add(8 * Math.cos(rad), 1, 8 * Math.sin(rad)));
            ang += 360 / jugadores.size();
        }

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    @Override
    public void tick() {
        if (!activo) return;
        tiempoJuego++;

        // Detectar quien esta en la cima
        Location cima = origen.clone().add(0, 5, 0);
        Player enCima = null;
        for (Player p : jugadores) {
            if (p.getLocation().distance(cima) <= 2) {
                enCima = p;
                break;
            }
        }

        if (enCima != null) {
            rey = enCima;
            puntos.merge(rey.getUniqueId(), 1, Integer::sum);
            rey.sendTitle(ColorUtils.color("&6\u2654 REY"), ColorUtils.color("&e" + puntos.get(rey.getUniqueId()) + "/" + PUNTOS_PARA_GANAR + "s"), 0, 25, 5);

            if (puntos.get(rey.getUniqueId()) >= PUNTOS_PARA_GANAR) {
                // El rey gana, eliminar a los demas
                for (Player p : new ArrayList<>(jugadores)) {
                    if (p != rey) plugin.getGameManager().eliminarJugador(p);
                }
                terminar();
                return;
            }
        }

        if (tiempoJuego >= getTiempoLimite()) {
            // Gana quien mas tiempo estuvo en la cima
            UUID ganadorUUID = puntos.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey).orElse(null);
            for (Player p : new ArrayList<>(jugadores)) {
                if (ganadorUUID == null || !p.getUniqueId().equals(ganadorUUID)) {
                    plugin.getGameManager().eliminarJugador(p);
                }
            }
            terminar();
        }
    }

    @Override public void terminar() {
        activo = false;
        if (task != null) task.cancel();
        ArenaBuilder.limpiar(origen, 15, 10, 15);
    }

    @Override public void onPlayerQuit(Player player) {
        jugadores.remove(player);
        puntos.remove(player.getUniqueId());
        if (player == rey) rey = null;
    }
}

// ══════════════════════════════════════════════════════════════════
//  RULETA RUSA
// ══════════════════════════════════════════════════════════════════
class RuletaRusa extends MiniGame {

    private Location origen;
    private List<Player> jugadores;
    private int turno;
    private int tiempoJuego;
    private BukkitTask task;

    public RuletaRusa(CoreSquid plugin) { super(plugin); }

    @Override public String getNombre() { return "Ruleta Rusa"; }
    @Override public String getDescripcion() { return "Bebe la pocion. Puede que sea la mala!"; }
    @Override public int getTiempoLimite() { return 120; }

    @Override
    public void generarArena(Location origen) {
        this.origen = origen.clone();
        for (int x = -6; x <= 6; x++)
            for (int z = -6; z <= 6; z++)
                if (x*x + z*z <= 36)
                    origen.clone().add(x, -1, z).getBlock().setType(Material.DARK_OAK_PLANKS);
    }

    @Override
    public void iniciar(List<Player> jugadores) {
        this.jugadores = new ArrayList<>(jugadores);
        this.turno = 0;
        this.tiempoJuego = 0;
        this.activo = true;

        int ang = 0;
        for (Player p : jugadores) {
            double rad = Math.toRadians(ang);
            p.teleport(origen.clone().add(4 * Math.cos(rad), 1, 4 * Math.sin(rad)));
            ang += 360 / jugadores.size();
        }

        anunciarTurno();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void anunciarTurno() {
        if (jugadores.isEmpty()) return;
        Player actual = jugadores.get(turno % jugadores.size());
        for (Player p : jugadores) {
            p.sendTitle(ColorUtils.color(p == actual ? "&c&lTU TURNO" : "&7Turno de &e" + actual.getName()),
                    ColorUtils.color("&7Escribe /squid beber"), 5, 40, 5);
        }
        actual.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&7Escribe &e/squid beber &7para beber la pocion."));
    }

    public void beber(Player player) {
        if (!activo) return;
        Player actual = jugadores.get(turno % jugadores.size());
        if (player != actual) {
            player.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&cNo es tu turno!"));
            return;
        }

        // 1 de 6 probabilidades de ser eliminado
        boolean eliminado = new Random().nextInt(6) == 0;
        if (eliminado) {
            player.sendTitle(ColorUtils.color("&c&lPOCION MALA!"), ColorUtils.color("&7Eliminado!"), 5, 60, 5);
            SoundUtils.play(player, "coresquid.ruleta_mala");
            plugin.getGameManager().eliminarJugador(player);
            jugadores.remove(player);
            if (jugadores.size() <= 1) { terminar(); return; }
        } else {
            player.sendTitle(ColorUtils.color("&a&lPOCION BUENA!"), ColorUtils.color("&7Salvado!"), 5, 40, 5);
            SoundUtils.play(player, "coresquid.ruleta_buena");
            turno++;
        }
        anunciarTurno();
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

    @Override public void onPlayerQuit(Player player) { jugadores.remove(player); }
}
