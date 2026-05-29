package dev.core.squid.games;

import dev.core.squid.CoreSquid;
import dev.core.squid.utils.ArenaBuilder;
import dev.core.squid.utils.ColorUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

public class KingHill extends MiniGame {
    private Location origin;
    private List<Player> players;
    private Player king;
    private Map<UUID, Integer> points;
    private int gameTime;
    private BukkitTask task;

    public KingHill(CoreSquid p) { super(p); points = new HashMap<>(); }
    @Override public String getId() { return "king-hill"; }
    @Override public String getName() { return "Rey de la Colina"; }
    @Override public String getDescription() { return plugin.getLang().getRaw("game-desc-king-hill"); }
    @Override public int getTimeLimit() { return cfgInt("duration", 120); }

    @Override public void buildArena(Location origin) {
        this.origin = origin.clone();
        for (int nivel=0;nivel<=5;nivel++) {
            int radio = 5-nivel;
            for (int x=-radio;x<=radio;x++)
                for (int z=-radio;z<=radio;z++)
                    if (x*x+z*z<=radio*radio+radio)
                        origin.clone().add(x,nivel-1,z).getBlock().setType(Material.STONE, false);
        }
    }

    @Override public void start(List<Player> players) {
        this.players = new ArrayList<>(players);
        gameTime = 0; active = true;
        if (origin == null) origin = getOrigin();
        players.forEach(p -> points.put(p.getUniqueId(), 0));
        int ang = 0;
        for (Player p : players) {
            double rad = Math.toRadians(ang);
            p.teleport(origin.clone().add(8*Math.cos(rad),1,8*Math.sin(rad)));
            ang += 360/players.size();
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    @Override public void tick() {
        if (!active) return;
        gameTime++;
        Location top = origin.clone().add(0,5,0);
        Player onTop = null;
        for (Player p : players) if (p.getLocation().distance(top) <= 2) { onTop = p; break; }
        if (onTop != null) {
            king = onTop;
            points.merge(king.getUniqueId(), 1, Integer::sum);
            int pts = points.get(king.getUniqueId());
            int toWin = cfgInt("points-to-win", 30);
            king.sendTitle(ColorUtils.color("&6\u2654 REY"), ColorUtils.color("&e"+pts+"/"+toWin+"s"), 0, 25, 5);
            if (pts >= toWin) {
                for (Player p : new ArrayList<>(players)) if (p != king) plugin.getGameManager().eliminatePlayer(p);
                end(); return;
            }
        }
        if (gameTime >= getTimeLimit()) {
            UUID winner = points.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
            for (Player p : new ArrayList<>(players))
                if (winner == null || !p.getUniqueId().equals(winner)) plugin.getGameManager().eliminatePlayer(p);
            end();
        }
    }

    @Override public void end() {
        active = false;
        if (task != null) { task.cancel(); task = null; }
        if (arenaData == null && origin != null) ArenaBuilder.clear(origin, 15, 10, 15);
    }
    @Override public void onQuit(Player p) {
        players.remove(p);
        points.remove(p.getUniqueId());
        if (p == king) king = null;
    }
}
