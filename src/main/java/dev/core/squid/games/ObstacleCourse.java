package dev.core.squid.games;

import dev.core.squid.CoreSquid;
import dev.core.squid.utils.ArenaBuilder;
import dev.core.squid.utils.ColorUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

public class ObstacleCourse extends MiniGame {
    private Location origin;
    private List<Player> players;
    private Set<UUID> finished;
    private int gameTime;
    private BukkitTask task;

    public ObstacleCourse(CoreSquid p) { super(p); finished = new HashSet<>(); }
    @Override public String getId() { return "obstacle-course"; }
    @Override public String getName() { return "Carrera de Obstáculos"; }
    @Override public String getDescription() { return plugin.getLang().getRaw("game-desc-obstacle-course"); }
    @Override public int getTimeLimit() { return cfgInt("duration", 120); }

    @Override public void buildArena(Location origin) {
        this.origin = origin.clone();
        int length = cfgInt("length", 50);
        for (int x = 0; x <= length; x++)
            for (int z = -3; z <= 3; z++)
                origin.clone().add(x,-1,z).getBlock().setType(Material.SMOOTH_STONE, false);
        for (int i = 1; i <= 9; i++) {
            int ox = i * 5;
            switch (i % 5) {
                case 0 -> { for (int z=-3;z<=3;z++) origin.clone().add(ox,0,z).getBlock().setType(Material.STONE, false); }
                case 1 -> { for (int z=-3;z<=3;z++) origin.clone().add(ox,-1,z).getBlock().setType(Material.AIR, false); }
                case 2 -> { for (int x2=0;x2<=2;x2++) for (int z=-3;z<=3;z++) origin.clone().add(ox+x2,-1,z).getBlock().setType(Material.ICE, false); }
                case 3 -> { for (int z=-3;z<=3;z++) origin.clone().add(ox,0,z).getBlock().setType(Material.OAK_FENCE, false); }
                case 4 -> { origin.clone().add(ox,-1,-3).getBlock().setType(Material.LAVA, false); origin.clone().add(ox,-1,3).getBlock().setType(Material.LAVA, false); }
            }
        }
        for (int z = -3; z <= 3; z++)
            origin.clone().add(length, 0, z).getBlock().setType(Material.GOLD_BLOCK, false);
    }

    @Override public void start(List<Player> players) {
        this.players = new ArrayList<>(players);
        gameTime = 0; active = true;
        if (origin == null) origin = getOrigin();
        int z = -2;
        for (Player p : players) {
            p.teleport(origin.clone().add(0, 1, z++));
            p.sendTitle(ColorUtils.color("&e&lCARRERA"), ColorUtils.color("&7¡Llega al final!"), 10, 60, 10);
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    @Override public void tick() {
        if (!active) return;
        gameTime++;
        int length = cfgInt("length", 50);
        for (Player p : new ArrayList<>(players)) {
            if (p.getLocation().getBlockX() >= origin.getBlockX() + length && !finished.contains(p.getUniqueId())) {
                finished.add(p.getUniqueId());
                p.sendTitle(ColorUtils.color("&a&lLLEGASTE!"), ColorUtils.color("&7Posición: #" + finished.size()), 10, 60, 10);
                players.remove(p);
            }
        }
        if (players.isEmpty() || gameTime >= getTimeLimit()) {
            for (Player p : new ArrayList<>(players)) plugin.getGameManager().eliminatePlayer(p);
            end();
        }
    }

    @Override public void end() {
        active = false;
        if (task != null) { task.cancel(); task = null; }
        if (arenaData == null && origin != null) ArenaBuilder.clear(origin, 55, 5, 8);
    }
    @Override public void onQuit(Player p) { players.remove(p); }
}
