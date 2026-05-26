package dev.core.squid.managers;

import dev.core.squid.CoreSquid;
import dev.core.squid.games.*;
import dev.core.squid.model.GameState;
import dev.core.squid.model.PlayerData;
import dev.core.squid.utils.ColorUtils;
import dev.core.squid.utils.SoundUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class GameManager {

    private final CoreSquid plugin;

    private GameState estado;
    private final List<Player> lobby = new ArrayList<>();
    private final List<Player> jugadoresActivos = new ArrayList<>();
    private final List<Player> espectadores = new ArrayList<>();

    private MiniGame juegoActual;
    private List<MiniGame> juegosDisponibles;
    private int indiceJuego;

    private int tiempoLobby;
    private BukkitTask taskLobby;
    private BukkitTask taskEntreJuegos;

    // Origen de las arenas (se genera lejos del spawn)
    private Location origenArenas;

    public GameManager(CoreSquid plugin) {
        this.plugin = plugin;
        this.estado = GameState.INACTIVO;
        inicializarJuegos();
    }

    private void inicializarJuegos() {
        juegosDisponibles = new ArrayList<>();
        juegosDisponibles.add(new LuzRojaLuzVerde(plugin));
        juegosDisponibles.add(new PuenteCristal(plugin));
        juegosDisponibles.add(new Canicas(plugin));
        juegosDisponibles.add(new SillasMusicales(plugin));
        juegosDisponibles.add(new BombaCaliente(plugin));
        juegosDisponibles.add(new MemoriaBloques(plugin));
        juegosDisponibles.add(new PlataformasCaen(plugin));
        juegosDisponibles.add(new AdivinaNumero(plugin));
        juegosDisponibles.add(new Escondite(plugin));
        juegosDisponibles.add(new ReyColina(plugin));
        juegosDisponibles.add(new RuletaRusa(plugin));
        juegosDisponibles.add(new TiraJala(plugin));
        juegosDisponibles.add(new Dalgona(plugin));
        juegosDisponibles.add(new Laberinto(plugin));
        juegosDisponibles.add(new CarreraObstaculos(plugin));

        // Filtrar segun config
        juegosDisponibles.removeIf(j -> {
            String key = j.getNombre().toLowerCase()
                    .replace(" ", "-")
                    .replace("á","a").replace("é","e")
                    .replace("í","i").replace("ó","o").replace("ú","u");
            return !plugin.getConfig().getBoolean("minijuegos." + key, true);
        });

        Collections.shuffle(juegosDisponibles);
        indiceJuego = 0;
    }

    // ══════════════════════════════════════════════════════════════════
    //  LOBBY
    // ══════════════════════════════════════════════════════════════════

    public boolean unirseAlLobby(Player player) {
        if (estado == GameState.EN_JUEGO || estado == GameState.INICIANDO) return false;
        if (lobby.contains(player) || jugadoresActivos.contains(player)) return false;
        int max = plugin.getConfig().getInt("max-jugadores", 16);
        if (lobby.size() >= max) return false;

        lobby.add(player);
        PlayerData pd = plugin.getStatsManager().getData(player.getUniqueId(), player.getName());
        pd.setLocationAntes(player.getLocation());
        pd.setInventarioAntes(player.getInventory().getContents().clone());
        pd.setArmorAntes(player.getInventory().getArmorContents().clone());
        player.getInventory().clear();

        anunciarALobby(ColorUtils.color(plugin.getPrefijo() +
                plugin.getConfig().getString("mensajes.unido-lobby", "&aUniste al lobby.")
                .replace("{actual}", String.valueOf(lobby.size()))
                .replace("{max}", String.valueOf(max))));

        if (estado == GameState.INACTIVO) {
            int min = plugin.getConfig().getInt("min-jugadores", 2);
            if (lobby.size() >= min) iniciarConteo();
        }

        return true;
    }

    public void salirDelLobby(Player player) {
        lobby.remove(player);
        restaurarJugador(player);
        player.sendMessage(ColorUtils.color(plugin.getPrefijo() +
                plugin.getConfig().getString("mensajes.salido-lobby", "&7Saliste del lobby.")));

        int min = plugin.getConfig().getInt("min-jugadores", 2);
        if (lobby.size() < min && estado == GameState.LOBBY) {
            detenerConteo();
        }
    }

    private void iniciarConteo() {
        estado = GameState.LOBBY;
        tiempoLobby = plugin.getConfig().getInt("tiempo-lobby", 30);

        taskLobby = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            tiempoLobby--;
            int min = plugin.getConfig().getInt("min-jugadores", 2);

            if (lobby.size() < min) {
                detenerConteo();
                return;
            }

            if (tiempoLobby % 10 == 0 || tiempoLobby <= 5) {
                anunciarALobby(ColorUtils.color(plugin.getPrefijo() +
                        plugin.getConfig().getString("mensajes.iniciando-en", "&eIniciando en &6{segundos}s")
                        .replace("{segundos}", String.valueOf(tiempoLobby))
                        .replace("{actual}", String.valueOf(lobby.size()))
                        .replace("{min}", String.valueOf(min))));

                for (Player p : lobby) {
                    SoundUtils.play(p, "coresquid.cuenta_regresiva");
                }
            }

            if (tiempoLobby <= 0) iniciarPartida();
        }, 20L, 20L);
    }

    private void detenerConteo() {
        estado = GameState.INACTIVO;
        if (taskLobby != null) { taskLobby.cancel(); taskLobby = null; }
    }

    // ══════════════════════════════════════════════════════════════════
    //  INICIAR PARTIDA
    // ══════════════════════════════════════════════════════════════════

    public void iniciarPartida() {
        if (taskLobby != null) { taskLobby.cancel(); taskLobby = null; }
        estado = GameState.EN_JUEGO;

        jugadoresActivos.clear();
        jugadoresActivos.addAll(lobby);
        lobby.clear();

        // Registrar partida en stats
        for (Player p : jugadoresActivos) {
            PlayerData pd = plugin.getStatsManager().getData(p.getUniqueId(), p.getName());
            pd.addPartida();
            pd.setEliminado(false);
            pd.setEspectador(false);
        }

        // Calcular origen de arenas
        String worldName = plugin.getConfig().getString("mundo-arenas", "world");
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) world = Bukkit.getWorlds().get(0);
        origenArenas = new Location(world, 10000, 100, 10000);

        anunciarATodos(ColorUtils.color(plugin.getPrefijo() +
                plugin.getConfig().getString("mensajes.partida-iniciada", "&aLa partida ha comenzado!")));

        Collections.shuffle(juegosDisponibles);
        indiceJuego = 0;
        iniciarSiguienteJuego();
    }

    // ══════════════════════════════════════════════════════════════════
    //  MINIJUEGOS
    // ══════════════════════════════════════════════════════════════════

    private void iniciarSiguienteJuego() {
        if (jugadoresActivos.size() <= 1) {
            terminarPartida();
            return;
        }

        if (indiceJuego >= juegosDisponibles.size()) {
            Collections.shuffle(juegosDisponibles);
            indiceJuego = 0;
        }

        juegoActual = juegosDisponibles.get(indiceJuego);
        indiceJuego++;

        // Anunciar el juego
        String nombre = juegoActual.getNombre();
        String desc = juegoActual.getDescripcion();

        for (Player p : jugadoresActivos) {
            p.sendTitle(
                    ColorUtils.color(plugin.getConfig().getString("anuncios.juego-titulo", "&c&l{juego}").replace("{juego}", nombre)),
                    ColorUtils.color(desc),
                    10, 60, 10
            );
            SoundUtils.play(p, "coresquid.inicio_juego");
        }

        // Generar arena y empezar en 5 segundos
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            juegoActual.generarArena(origenArenas);
            juegoActual.iniciar(new ArrayList<>(jugadoresActivos));
        }, 100L);
    }

    // ══════════════════════════════════════════════════════════════════
    //  ELIMINACION
    // ══════════════════════════════════════════════════════════════════

    public void eliminarJugador(Player player) {
        if (!jugadoresActivos.contains(player)) return;
        jugadoresActivos.remove(player);
        espectadores.add(player);

        PlayerData pd = plugin.getStatsManager().getData(player.getUniqueId(), player.getName());
        pd.setEliminado(true);
        pd.setEspectador(true);
        pd.addEliminacion();

        // Modo espectador
        player.setGameMode(GameMode.SPECTATOR);
        player.sendTitle(
                ColorUtils.color(plugin.getConfig().getString("anuncios.eliminado-titulo", "&c&lELIMINADO")),
                ColorUtils.color(plugin.getConfig().getString("anuncios.eliminado-subtitulo", "&7Mejor suerte la proxima")),
                10, 60, 20
        );
        SoundUtils.play(player, "coresquid.eliminado");

        player.sendMessage(ColorUtils.color(plugin.getPrefijo() +
                plugin.getConfig().getString("mensajes.eliminado", "&cFuiste eliminado.")));

        // Verificar si queda un ganador
        if (jugadoresActivos.size() <= 1) {
            plugin.getServer().getScheduler().runTaskLater(plugin, this::terminarPartida, 40L);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  FIN DE PARTIDA
    // ══════════════════════════════════════════════════════════════════

    private void terminarPartida() {
        estado = GameState.FIN;

        Player ganador = jugadoresActivos.isEmpty() ? null : jugadoresActivos.get(0);

        if (ganador != null) {
            PlayerData pd = plugin.getStatsManager().getData(ganador.getUniqueId(), ganador.getName());
            pd.addVictoria();

            String nombreGanador = ganador.getName();

            // Anunciar ganador a todos
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle(
                        ColorUtils.color(plugin.getConfig().getString("anuncios.ganador-titulo", "&6&lGANADOR")),
                        ColorUtils.color(plugin.getConfig().getString("anuncios.ganador-subtitulo", "&e{jugador}").replace("{jugador}", nombreGanador)),
                        10, 100, 20
                );
            }

            String chatMsg = ColorUtils.color(plugin.getPrefijo() +
                    plugin.getConfig().getString("mensajes.ganador", "&6{jugador} ha ganado!").replace("{jugador}", nombreGanador));
            Bukkit.broadcastMessage(chatMsg);
            SoundUtils.playToAll(Bukkit.getOnlinePlayers(), "coresquid.ganador");

            // Dar recompensas
            darRecompensas(ganador);
        }

        // Guardar stats
        plugin.getStatsManager().guardar();

        // Restaurar a todos en 10 segundos
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player p : new ArrayList<>(jugadoresActivos)) restaurarJugador(p);
            for (Player p : new ArrayList<>(espectadores)) restaurarJugador(p);
            jugadoresActivos.clear();
            espectadores.clear();
            estado = GameState.INACTIVO;
            inicializarJuegos();
        }, 200L);
    }

    private void darRecompensas(Player ganador) {
        // Dinero
        double dinero = plugin.getConfig().getDouble("recompensas.dinero", 0);
        if (dinero > 0 && plugin.getEconomy() != null) {
            plugin.getEconomy().depositPlayer(ganador, dinero);
        }

        // Comandos
        for (String cmd : plugin.getConfig().getStringList("recompensas.comandos")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{jugador}", ganador.getName()));
        }

        // Items
        List<?> rawItems = plugin.getConfig().getList("recompensas.items");
        if (rawItems != null) {
            for (Object obj : rawItems) {
                if (obj instanceof ItemStack) ganador.getInventory().addItem((ItemStack) obj);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  UTILITIES
    // ══════════════════════════════════════════════════════════════════

    private void restaurarJugador(Player player) {
        PlayerData pd = plugin.getStatsManager().getData(player.getUniqueId(), player.getName());
        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().clear();

        if (pd.getInventarioAntes() != null) player.getInventory().setContents(pd.getInventarioAntes());
        if (pd.getArmorAntes() != null) player.getInventory().setArmorContents(pd.getArmorAntes());
        if (pd.getLocationAntes() != null) player.teleport(pd.getLocationAntes());

        pd.setEliminado(false);
        pd.setEspectador(false);
    }

    private void anunciarALobby(String msg) {
        for (Player p : lobby) p.sendMessage(msg);
    }

    private void anunciarATodos(String msg) {
        for (Player p : jugadoresActivos) p.sendMessage(msg);
        for (Player p : espectadores) p.sendMessage(msg);
    }

    // ══════════════════════════════════════════════════════════════════
    //  GETTERS
    // ══════════════════════════════════════════════════════════════════

    public GameState getEstado() { return estado; }
    public List<Player> getLobby() { return lobby; }
    public List<Player> getJugadoresActivos() { return jugadoresActivos; }
    public List<Player> getEspectadores() { return espectadores; }
    public MiniGame getJuegoActual() { return juegoActual; }
    public boolean estaEnPartida(Player p) { return jugadoresActivos.contains(p) || espectadores.contains(p); }
    public boolean estaEnLobby(Player p) { return lobby.contains(p); }

    public void forzarInicio() {
        if (estado == GameState.LOBBY || estado == GameState.INACTIVO) {
            iniciarPartida();
        }
    }

    public void forzarDetener() {
        if (juegoActual != null && juegoActual.isActivo()) juegoActual.terminar();
        for (Player p : new ArrayList<>(jugadoresActivos)) restaurarJugador(p);
        for (Player p : new ArrayList<>(espectadores)) restaurarJugador(p);
        jugadoresActivos.clear();
        espectadores.clear();
        lobby.clear();
        estado = GameState.INACTIVO;
        inicializarJuegos();
    }

    public String getPrefijo() { return plugin.getPrefijo(); }
    }
