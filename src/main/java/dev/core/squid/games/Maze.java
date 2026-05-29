package dev.core.squid.games;

import dev.core.squid.CoreSquid;
import dev.core.squid.utils.ArenaBuilder;
import dev.core.squid.utils.ColorUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

public class Maze extends MiniGame {
    private Location origin, goal;
    private List<Player> players;
    private int gameTime;
    private BukkitTask task;

    public Maze(CoreSquid p) { super(p); }
    @Override public String getId() { return "maze"; }
    @Override public String getName() { return "Laberinto"; }
    @Override public String getDescription() { return plugin.getLang().getRaw("game-desc-maze"); }
    @Override public int getTimeLimit() { return cfgInt("duration", 120); }

    @Override public void buildArena(Location origin) {
        this.origin = origin.clone();
        int size = cfgInt("size", 20);
        Random rand = new Random();
        for (int x = 0; x <= size; x++) {
            for (int z = 0; z <= size; z++) {
                origin.clone().add(x, -1, z).getBlock().setType(Material.STONE_BRICKS, false);
                if (x==0||x==size||z==0||z==size) {
                    origin.clone().add(x, 0, z).getBlock().setType(Material.STONE_BRICK_WALL, false);
                    origin.clone().add(x, 1, z).getBlock().setType(Material.STONE_BRICK_WALL, false);
                } else if (x%2==0 && z%2==0 && rand.nextBoolean()) {
                    origin.clone().add(x, 0, z).getBlock().setType(Material.STONE_BRICK_WALL, false);
                    origin.clone().add(x, 1, z).getBlock().setType(Material.STONE_BRICK_WALL, false);
                }
            }
        }
        goal = origin.clone().add(size-1, 0, size-1);
        goal.getBlock().setType(Material.BEACON, false);
        goal.clone().subtract(0,1,0).getBlock().setType(Material.EMERALD_BLOCK, false);
    }

    @Override public void start(List<Player> players) {
        this.players = new ArrayList<>(players);
        gameTime = 0; active = true;
        if (origin == null) origin = getOrigin();
        for (Player p : players) {
            p.teleport(origin.clone().add(1, 1, 1));
            p.sendTitle(ColorUtils.color("&e&lLABERINTO"), ColorUtils.color("&7¡Llega a la meta!"), 10, 60, 10);
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    @Override public void tick() {
        if (!active) return;
        gameTime++;
        for (Player p : new ArrayList<>(players)) {
            if (goal != null && p.getLocation().distance(goal) <= 2) {
                p.sendTitle(ColorUtils.color("&a&lLLEGASTE!"), ColorUtils.color("&7¡Eres el primero!"), 10, 80, 10);
                players.remove(p);
                for (Player other : new ArrayList<>(players)) plugin.getGameManager().eliminatePlayer(other);
                end(); return;
            }
        }
        if (gameTime >= getTimeLimit()) {
            for (Player p : new ArrayList<>(players)) plugin.getGameManager().eliminatePlayer(p);
            end();
        }
    }

    @Override public void end() {
        active = false;
        if (task != null) { task.cancel(); task = null; }
        if (arenaData == null && origin != null) ArenaBuilder.clear(origin, 25, 5, 25);
    }
    @Override public void onQuit(Player p) { players.remove(p); }
}
