package dev.core.squid.games;

import dev.core.squid.CoreSquid;
import dev.core.squid.utils.ArenaBuilder;
import dev.core.squid.utils.ColorUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

public class GuessNumber extends MiniGame {
    private Location origin;
    private List<Player> players;
    private int secret, maxAttempts, gameTime;
    private Map<UUID, Integer> attempts;
    private BukkitTask task;

    public GuessNumber(CoreSquid p) { super(p); attempts = new HashMap<>(); }
    @Override public String getId() { return "guess-number"; }
    @Override public String getName() { return "Adivina el Número"; }
    @Override public String getDescription() { return plugin.getLang().getRaw("game-desc-guess-number"); }
    @Override public int getTimeLimit() { return cfgInt("duration", 60); }

    @Override public void buildArena(Location origin) {
        this.origin = origin.clone();
        for (int x=-5;x<=5;x++) for (int z=-5;z<=5;z++) origin.clone().add(x,-1,z).getBlock().setType(Material.BLUE_CONCRETE, false);
    }

    @Override public void start(List<Player> players) {
        this.players = new ArrayList<>(players);
        secret = new Random().nextInt(cfgInt("max", 100)) + 1;
        maxAttempts = cfgInt("max-attempts", 7);
        gameTime = 0; active = true;
        if (origin == null) origin = getOrigin();
        for (Player p : players) {
            p.teleport(origin.clone().add(0,1,0));
            attempts.put(p.getUniqueId(), 0);
            p.sendMessage(plugin.getLang().get("guess-explain"));
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void processGuess(Player player, int number) {
        if (!active) return;
        int tries = attempts.getOrDefault(player.getUniqueId(), 0) + 1;
        attempts.put(player.getUniqueId(), tries);
        if (number == secret) {
            player.sendTitle(ColorUtils.color("&a&l" + plugin.getLang().getRaw("guess-correct")),
                    ColorUtils.color(plugin.getLang().getRaw("guess-correct-sub", "number", String.valueOf(secret))), 10, 60, 10);
            players.remove(player);
            if (players.isEmpty()) end();
        } else {
            boolean higher = number < secret;
            player.sendMessage(plugin.getLang().get(higher ? "guess-higher" : "guess-lower",
                    "tries", String.valueOf(tries), "max", String.valueOf(maxAttempts)));
            if (tries >= maxAttempts) {
                player.sendMessage(plugin.getLang().get("guess-failed", "number", String.valueOf(secret)));
                plugin.getGameManager().eliminatePlayer(player);
                players.remove(player);
                if (players.isEmpty()) end();
            }
        }
    }

    @Override public void tick() {
        if (!active) return;
        gameTime++;
        if (gameTime >= getTimeLimit()) { for (Player p : new ArrayList<>(players)) plugin.getGameManager().eliminatePlayer(p); end(); }
    }
    @Override public void end() {
        active = false;
        if (task != null) { task.cancel(); task = null; }
        if (arenaData == null && origin != null) ArenaBuilder.clear(origin, 12, 5, 12);
    }
    @Override public void onQuit(Player p) { players.remove(p); attempts.remove(p.getUniqueId()); }
}
