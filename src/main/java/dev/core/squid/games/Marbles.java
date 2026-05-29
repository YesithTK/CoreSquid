package dev.core.squid.games;

import dev.core.squid.CoreSquid;
import dev.core.squid.managers.LangManager;
import dev.core.squid.utils.ArenaBuilder;
import dev.core.squid.utils.ColorUtils;
import dev.core.squid.utils.SoundUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class Marbles extends MiniGame {

    private Location origin;
    private List<Player> players;
    private Map<UUID, Player> pairs;
    private Map<UUID, Integer> marbles;
    private Map<UUID, Boolean> bets;       // true = even, false = odd
    private Map<UUID, Integer> betAmount;
    private int gameTime;
    private BukkitTask task;

    public Marbles(CoreSquid plugin) {
        super(plugin);
        pairs = new HashMap<>();
        marbles = new HashMap<>();
        bets = new HashMap<>();
        betAmount = new HashMap<>();
    }

    @Override public String getId() { return "marbles"; }
    @Override public String getName() { return "Canicas"; }
    @Override public String getDescription() { return plugin.getLang().getRaw("game-desc-marbles"); }
    @Override public int getTimeLimit() { return cfgInt("duration", 60); }

    @Override
    public void buildArena(Location origin) {
        this.origin = origin.clone();
        for (int i = 0; i < 8; i++) {
            int ox = i * 7;
            for (int x = 0; x <= 4; x++)
                for (int z = 0; z <= 4; z++)
                    origin.clone().add(ox + x, -1, z).getBlock().setType(Material.GRAY_CONCRETE, false);
        }
    }

    @Override
    public void start(List<Player> players) {
        this.players = new ArrayList<>(players);
        this.gameTime = 0;
        this.active = true;
        if (origin == null) origin = getOrigin();

        int starting = cfgInt("starting-marbles", 10);

        // Pair players
        List<Player> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);
        for (int i = 0; i + 1 < shuffled.size(); i += 2) {
            Player a = shuffled.get(i), b = shuffled.get(i + 1);
            pairs.put(a.getUniqueId(), b);
            pairs.put(b.getUniqueId(), a);
            marbles.put(a.getUniqueId(), starting);
            marbles.put(b.getUniqueId(), starting);
            int cell = i / 2;
            a.teleport(origin.clone().add(cell * 7 + 1, 1, 1));
            b.teleport(origin.clone().add(cell * 7 + 3, 1, 1));
        }

        // Solo player passes automatically
        if (shuffled.size() % 2 != 0) {
            Player solo = shuffled.get(shuffled.size() - 1);
            solo.teleport(origin.clone().add(0, 1, 0));
            solo.sendTitle(ColorUtils.color("&aSin pareja"), ColorUtils.color("&7Pasas automáticamente"), 10, 60, 10);
            this.players.remove(solo);
        }

        LangManager lang = plugin.getLang();
        for (Player p : this.players) {
            p.sendMessage(plugin.getLang().get("marbles-explain",
                    "marbles", String.valueOf(starting)));
        }

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void processBet(Player player, boolean even, int amount) {
        if (!active) return;
        int myMarbles = marbles.getOrDefault(player.getUniqueId(), 0);
        if (amount < 1 || amount > myMarbles) {
            player.sendMessage(ColorUtils.color("&cCantidad inválida. Tienes &e" + myMarbles + " &7canicas."));
            return;
        }
        bets.put(player.getUniqueId(), even);
        betAmount.put(player.getUniqueId(), amount);
        player.sendMessage(ColorUtils.color("&aApuesta: &e" + (even ? "PAR" : "IMPAR") + " &7x" + amount));

        // If all players bet, resolve
        if (bets.size() >= players.size()) resolveRound();
    }

    private void resolveRound() {
        Random rand = new Random();
        Set<UUID> processed = new HashSet<>();

        for (UUID uuid : new HashSet<>(pairs.keySet())) {
            if (processed.contains(uuid)) continue;
            Player a = plugin.getServer().getPlayer(uuid);
            Player b = pairs.get(uuid);
            if (a == null || b == null) continue;
            if (!bets.containsKey(uuid)) continue;

            processed.add(uuid);
            processed.add(b.getUniqueId());

            int hidden = rand.nextInt(marbles.getOrDefault(b.getUniqueId(), 10)) + 1;
            boolean isEven = hidden % 2 == 0;
            boolean guessed = bets.get(uuid) == isEven;
            int amount = betAmount.getOrDefault(uuid, 1);

            if (guessed) {
                marbles.merge(uuid, amount, Integer::sum);
                marbles.merge(b.getUniqueId(), -amount, Integer::sum);
                a.sendTitle(ColorUtils.color("&a&lCORRECTO!"),
                        ColorUtils.color("&7+" + amount + " canicas"), 10, 60, 10);
            } else {
                marbles.merge(uuid, -amount, Integer::sum);
                marbles.merge(b.getUniqueId(), amount, Integer::sum);
                a.sendTitle(ColorUtils.color("&c&lINCORRECTO"),
                        ColorUtils.color("&7-" + amount + " canicas"), 10, 60, 10);
            }

            a.sendMessage(ColorUtils.color("&7Canicas ocultas: &e" + hidden +
                    " &7(" + (isEven ? "PAR" : "IMPAR") + "). Tienes: &e" + marbles.get(uuid)));
            b.sendMessage(ColorUtils.color("&7Tienes: &e" + marbles.get(b.getUniqueId())));
        }

        // Eliminate players with 0 marbles
        for (UUID uuid : new HashSet<>(marbles.keySet())) {
            if (marbles.getOrDefault(uuid, 0) <= 0) {
                Player p = plugin.getServer().getPlayer(uuid);
                if (p != null) {
                    plugin.getGameManager().eliminatePlayer(p);
                    players.remove(p);
                }
            }
        }

        bets.clear();
        betAmount.clear();

        if (players.size() <= 1) { end(); return; }

        // New round instructions
        for (Player p : players) {
            p.sendMessage(plugin.getLang().get("marbles-explain",
                    "marbles", String.valueOf(marbles.getOrDefault(p.getUniqueId(), 0))));
        }
    }

    @Override
    public void tick() {
        if (!active) return;
        gameTime++;
        if (gameTime >= getTimeLimit()) {
            for (Player p : new ArrayList<>(players)) plugin.getGameManager().eliminatePlayer(p);
            end();
        }
    }

    @Override
    public void end() {
        active = false;
        if (task != null) { task.cancel(); task = null; }
        if (arenaData == null && origin != null) ArenaBuilder.clear(origin, 60, 5, 8);
    }

    @Override
    public void onQuit(Player player) {
        players.remove(player);
        pairs.remove(player.getUniqueId());
        marbles.remove(player.getUniqueId());
        bets.remove(player.getUniqueId());
    }
}
