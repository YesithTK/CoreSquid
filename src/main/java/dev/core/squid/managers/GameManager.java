package dev.core.squid.managers;

import dev.core.squid.CoreSquid;
import dev.core.squid.games.*;
import dev.core.squid.model.ArenaData;
import dev.core.squid.model.GameState;
import dev.core.squid.model.PlayerStats;
import dev.core.squid.utils.ColorUtils;
import dev.core.squid.utils.SoundUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class GameManager {

    private final CoreSquid plugin;

    private GameState state;
    private final List<Player> lobby = new ArrayList<>();
    private final List<Player> activePlayers = new ArrayList<>();
    private final List<Player> spectators = new ArrayList<>();

    private MiniGame currentGame;
    private List<MiniGame> gamePool;
    private int gameIndex;

    private int lobbyCountdown;
    private BukkitTask lobbyTask;
    private BukkitTask gameTickTask;

    private Location arenaOrigin;

    public GameManager(CoreSquid plugin) {
        this.plugin = plugin;
        this.state = GameState.INACTIVE;
        buildGamePool();
    }

    // ══════════════════════════════════════════════════════════════
    //  GAME POOL
    // ══════════════════════════════════════════════════════════════

    private void buildGamePool() {
        gamePool = new ArrayList<>();
        addIfEnabled(new RedLightGreenLight(plugin), "red-light-green-light");
        addIfEnabled(new GlassBridge(plugin), "glass-bridge");
        addIfEnabled(new Marbles(plugin), "marbles");
        addIfEnabled(new TugOfWar(plugin), "tug-of-war");
        addIfEnabled(new Dalgona(plugin), "dalgona");
        addIfEnabled(new MusicalChairs(plugin), "musical-chairs");
        addIfEnabled(new Maze(plugin), "maze");
        addIfEnabled(new HotPotato(plugin), "hot-potato");
        addIfEnabled(new MemoryBlocks(plugin), "memory-blocks");
        addIfEnabled(new ObstacleCourse(plugin), "obstacle-course");
        addIfEnabled(new FallingPlatforms(plugin), "falling-platforms");
        addIfEnabled(new GuessNumber(plugin), "guess-number");
        addIfEnabled(new HideSeek(plugin), "hide-seek");
        addIfEnabled(new KingHill(plugin), "king-hill");
        addIfEnabled(new RussianRoulette(plugin), "russian-roulette");
        Collections.shuffle(gamePool);
        gameIndex = 0;
    }

    private void addIfEnabled(MiniGame game, String configKey) {
        if (plugin.getConfig().getBoolean("minigames." + configKey, true)) {
            // Set arena data if custom arena exists
            ArenaData arena = plugin.getArenaManager().getArena(configKey);
            if (arena != null) game.setArenaData(arena);
            gamePool.add(game);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  LOBBY
    // ══════════════════════════════════════════════════════════════

    public boolean joinLobby(Player player) {
        if (state == GameState.IN_GAME || state == GameState.STARTING) return false;
        if (lobby.contains(player) || activePlayers.contains(player)) return false;
        int max = plugin.getConfig().getInt("game.max-players", 16);
        if (lobby.size() >= max) return false;

        lobby.add(player);

        // Save player state
        PlayerStats ps = plugin.getStatsManager().getStats(player.getUniqueId(), player.getName());
        ps.setSavedLocation(player.getLocation());
        ps.setSavedInventory(player.getInventory().getContents().clone());
        ps.setSavedArmor(player.getInventory().getArmorContents().clone());
        ps.setSavedGameMode(player.getGameMode());
        ps.setSavedLevel(player.getLevel());
        ps.setSavedExp(player.getExp());
        ps.setSavedHealth(player.getHealth());
        ps.setSavedFood(player.getFoodLevel());
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);

        // Teleport to lobby
        Location lobbySpawn = plugin.getArenaManager().getLobbySpawn();
        if (lobbySpawn != null) player.teleport(lobbySpawn);

        SoundUtils.play(player, "join_lobby");

        // Broadcast
        String msg = plugin.getLang().get("joined-lobby",
                "current", String.valueOf(lobby.size()),
                "max", String.valueOf(max));
        for (Player p : lobby) p.sendMessage(msg);

        // Start countdown if enough players
        int min = plugin.getConfig().getInt("game.min-players", 2);
        if (lobby.size() >= min && state == GameState.INACTIVE) {
            startCountdown();
        }

        return true;
    }

    public void leaveLobby(Player player) {
        if (!lobby.contains(player)) return;
        lobby.remove(player);
        restorePlayer(player);
        player.sendMessage(plugin.getLang().get("left-lobby"));

        int min = plugin.getConfig().getInt("game.min-players", 2);
        if (lobby.size() < min && state == GameState.LOBBY) {
            cancelCountdown();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  COUNTDOWN
    // ══════════════════════════════════════════════════════════════

    private void startCountdown() {
        state = GameState.LOBBY;
        lobbyCountdown = plugin.getConfig().getInt("game.lobby-countdown", 30);

        lobbyTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            int min = plugin.getConfig().getInt("game.min-players", 2);
            if (lobby.size() < min) { cancelCountdown(); return; }

            lobbyCountdown--;

            // Broadcast at intervals
            if (lobbyCountdown % 10 == 0 || lobbyCountdown <= 5) {
                String msg = plugin.getLang().get("countdown-starting",
                        "seconds", String.valueOf(lobbyCountdown));
                for (Player p : lobby) p.sendMessage(msg);
                SoundUtils.playAll(lobby, "countdown");
            }

            // Show title countdown (3, 2, 1)
            if (lobbyCountdown <= 3 && lobbyCountdown > 0 && plugin.getConfig().getBoolean("game.starting-titles", true)) {
                for (Player p : lobby) {
                    p.sendTitle(
                            ColorUtils.color(plugin.getLang().getRaw("countdown-title", "seconds", String.valueOf(lobbyCountdown))),
                            ColorUtils.color(plugin.getLang().getRaw("countdown-subtitle")),
                            5, 25, 5
                    );
                    SoundUtils.play(p, "countdown");
                }
            }

            if (lobbyCountdown <= 0) startGame();

        }, 20L, 20L);
    }

    private void cancelCountdown() {
        state = GameState.INACTIVE;
        if (lobbyTask != null) { lobbyTask.cancel(); lobbyTask = null; }
        for (Player p : lobby) p.sendMessage(plugin.getLang().get("countdown-cancelled"));
    }

    // ══════════════════════════════════════════════════════════════
    //  START GAME
    // ══════════════════════════════════════════════════════════════

    public void startGame() {
        if (lobbyTask != null) { lobbyTask.cancel(); lobbyTask = null; }
        state = GameState.STARTING;

        activePlayers.clear();
        activePlayers.addAll(lobby);
        lobby.clear();

        // Register game for each player
        for (Player p : activePlayers) {
            PlayerStats ps = plugin.getStatsManager().getStats(p.getUniqueId(), p.getName());
            ps.addGame();
            ps.resetSession();
        }

        // Show teleporting title
        if (plugin.getConfig().getBoolean("game.starting-titles", true)) {
            for (Player p : activePlayers) {
                p.sendTitle(
                        ColorUtils.color(plugin.getLang().getRaw("teleporting-title")),
                        ColorUtils.color(plugin.getLang().getRaw("teleporting-subtitle")),
                        10, 60, 20
                );
            }
        }

        // Broadcast
        for (Player p : activePlayers) p.sendMessage(plugin.getLang().get("game-started"));

        // Calculate arena origin
        String worldName = plugin.getConfig().getString("arenas.world", "world");
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) world = Bukkit.getWorlds().get(0);
        arenaOrigin = new Location(world,
                plugin.getConfig().getInt("arenas.origin-x", 10000),
                plugin.getConfig().getInt("arenas.origin-y", 100),
                plugin.getConfig().getInt("arenas.origin-z", 10000));

        // Rebuild game pool (to reset state of all games)
        buildGamePool();

        state = GameState.IN_GAME;
        startNextGame();
    }

    // ══════════════════════════════════════════════════════════════
    //  MINIGAME CYCLE
    // ══════════════════════════════════════════════════════════════

    private void startNextGame() {
        if (activePlayers.size() <= 1) { endGame(); return; }

        if (gameIndex >= gamePool.size()) {
            Collections.shuffle(gamePool);
            gameIndex = 0;
        }

        currentGame = gamePool.get(gameIndex++);

        // Reload arena data in case it was set after buildGamePool
        ArenaData arena = plugin.getArenaManager().getArena(currentGame.getId());
        if (arena != null) currentGame.setArenaData(arena);

        // Announce game
        if (plugin.getConfig().getBoolean("game.announce-games", true)) {
            for (Player p : activePlayers) {
                p.sendTitle(
                        ColorUtils.color(plugin.getLang().getRaw("game-announce-title",
                                "game", currentGame.getName())),
                        ColorUtils.color(plugin.getLang().getRaw("game-announce-subtitle",
                                "description", currentGame.getDescription())),
                        10, 80, 20
                );
                SoundUtils.play(p, "game_start");
            }
        }

        // Build arena and start after 5 seconds
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (currentGame.getArenaData() == null) {
                currentGame.buildArena(arenaOrigin);
            }
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                currentGame.start(new ArrayList<>(activePlayers));
                // Start tick task
                gameTickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                    if (currentGame.isActive()) {
                        currentGame.tick();
                    } else {
                        gameTickTask.cancel();
                        scheduleNextGame();
                    }
                }, 20L, 20L);
            }, 20L);
        }, 100L);
    }

    private void scheduleNextGame() {
        if (activePlayers.size() <= 1) { endGame(); return; }
        int delay = plugin.getConfig().getInt("game.between-games-delay", 10);
        for (Player p : activePlayers) {
            p.sendMessage(plugin.getLang().get("game-next",
                    "game", gameIndex < gamePool.size() ? gamePool.get(gameIndex).getName() : "?"));
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, this::startNextGame, delay * 20L);
    }

    // ══════════════════════════════════════════════════════════════
    //  ELIMINATE PLAYER
    // ══════════════════════════════════════════════════════════════

    public void eliminatePlayer(Player player) {
        if (!activePlayers.contains(player)) return;
        activePlayers.remove(player);
        spectators.add(player);

        PlayerStats ps = plugin.getStatsManager().getStats(player.getUniqueId(), player.getName());
        ps.setEliminated(true);
        ps.setSpectator(true);
        ps.addElimination();

        player.setGameMode(GameMode.SPECTATOR);
        player.sendTitle(
                ColorUtils.color(plugin.getLang().getRaw("game-eliminated-title")),
                ColorUtils.color(plugin.getLang().getRaw("game-eliminated-subtitle")),
                10, 80, 20
        );
        player.sendMessage(plugin.getLang().get("eliminated"));
        SoundUtils.play(player, "eliminated");

        if (activePlayers.size() <= 1) {
            plugin.getServer().getScheduler().runTaskLater(plugin, this::endGame, 60L);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  END GAME
    // ══════════════════════════════════════════════════════════════

    private void endGame() {
        state = GameState.ENDING;

        if (gameTickTask != null) { gameTickTask.cancel(); gameTickTask = null; }
        if (currentGame != null && currentGame.isActive()) currentGame.end();

        Player winner = activePlayers.isEmpty() ? null : activePlayers.get(0);

        if (winner != null) {
            PlayerStats ps = plugin.getStatsManager().getStats(winner.getUniqueId(), winner.getName());
            ps.addWin();

            String winMsg = plugin.getLang().get("winner", "player", winner.getName());
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(winMsg);
                p.sendTitle(
                        ColorUtils.color(plugin.getLang().getRaw("game-winner-title")),
                        ColorUtils.color(plugin.getLang().getRaw("game-winner-subtitle", "player", winner.getName())),
                        10, 100, 20
                );
            }
            SoundUtils.playAll(Bukkit.getOnlinePlayers(), "winner");
            giveRewards(winner);
        } else {
            for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(plugin.getLang().get("no-winner"));
        }

        plugin.getStatsManager().save();

        // Restore all players after 10 seconds
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player p : new ArrayList<>(activePlayers)) restorePlayer(p);
            for (Player p : new ArrayList<>(spectators)) restorePlayer(p);
            activePlayers.clear();
            spectators.clear();
            state = GameState.INACTIVE;
            currentGame = null;
        }, 200L);
    }

    private void giveRewards(Player winner) {
        if (!plugin.getConfig().getBoolean("rewards.enabled", true)) return;

        // Money
        double money = plugin.getConfig().getDouble("rewards.money", 0);
        Economy eco = plugin.getEconomy();
        if (money > 0 && eco != null) eco.depositPlayer(winner, money);

        // Commands
        for (String cmd : plugin.getConfig().getStringList("rewards.commands")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", winner.getName()));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  RESTORE PLAYER
    // ══════════════════════════════════════════════════════════════

    public void restorePlayer(Player player) {
        PlayerStats ps = plugin.getStatsManager().getStats(player.getUniqueId(), player.getName());
        if (ps == null) return;

        player.getInventory().clear();
        if (ps.getSavedInventory() != null) player.getInventory().setContents(ps.getSavedInventory());
        if (ps.getSavedArmor() != null) player.getInventory().setArmorContents(ps.getSavedArmor());
        if (ps.getSavedGameMode() != null) player.setGameMode(ps.getSavedGameMode());
        player.setLevel(ps.getSavedLevel());
        player.setExp(ps.getSavedExp());
        if (ps.getSavedHealth() > 0) player.setHealth(Math.min(ps.getSavedHealth(), player.getMaxHealth()));
        player.setFoodLevel(Math.max(ps.getSavedFood(), 1));
        if (ps.getSavedLocation() != null) player.teleport(ps.getSavedLocation());

        ps.resetSession();
    }

    // ══════════════════════════════════════════════════════════════
    //  FORCE ADMIN
    // ══════════════════════════════════════════════════════════════

    public void forceStart() {
        if (state == GameState.LOBBY || state == GameState.INACTIVE) startGame();
    }

    public void forceStop() {
        if (gameTickTask != null) { gameTickTask.cancel(); gameTickTask = null; }
        if (lobbyTask != null) { lobbyTask.cancel(); lobbyTask = null; }
        if (currentGame != null && currentGame.isActive()) currentGame.end();
        for (Player p : new ArrayList<>(activePlayers)) restorePlayer(p);
        for (Player p : new ArrayList<>(spectators)) restorePlayer(p);
        for (Player p : new ArrayList<>(lobby)) restorePlayer(p);
        activePlayers.clear(); spectators.clear(); lobby.clear();
        state = GameState.INACTIVE;
        currentGame = null;
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(plugin.getLang().get("game-stopped"));
    }

    // ══════════════════════════════════════════════════════════════
    //  GETTERS
    // ══════════════════════════════════════════════════════════════

    public GameState getState() { return state; }
    public List<Player> getLobby() { return lobby; }
    public List<Player> getActivePlayers() { return activePlayers; }
    public List<Player> getSpectators() { return spectators; }
    public MiniGame getCurrentGame() { return currentGame; }
    public boolean isInLobby(Player p) { return lobby.contains(p); }
    public boolean isInGame(Player p) { return activePlayers.contains(p) || spectators.contains(p); }
    public int getLobbyCountdown() { return lobbyCountdown; }
    public String getPrefix() { return plugin.getLang().getRaw("prefix"); }
}
