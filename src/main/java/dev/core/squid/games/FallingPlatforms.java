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

public class FallingPlatforms extends MiniGame {
    private Location origin;
    private List<Player> players;
    private List<Location> platforms;
    private int gameTime;
    private BukkitTask task;

    public FallingPlatforms(CoreSquid p) { super(p); platforms = new ArrayList<>(); }
    @Override public String getId() { return "falling-platforms"; }
    @Override public String getName() { return "Plataformas que Caen"; }
    @Override public String getDescription() { return plugin.getLang().getRaw("game-desc-falling-platforms"); }
    @Override public int getTimeLimit() { return cfgInt("duration", 90); }

    @Override public void buildArena(Location origin) {
        this.origin = origin.clone();
        Material[] mats = {Material.RED_CONCRETE, Material.ORANGE_CONCRETE, Material.YELLOW_CONCRETE, Material.LIME_CONCRETE};
        int levels = cfgInt("levels", 4);
        for (int nivel = 0; nivel < levels; nivel++) {
            int y = nivel * 5;
            for (int x = -8; x <= 8; x += 3)
                for (int z = -8; z <= 8; z += 3) {
                    Location plat = origin.clone().add(x, y, z);
                    plat.getBlock().setType(mats[nivel % 4], false);
                    platforms.add(plat);
                }
        }
        for (int x = -10; x <= 10; x++)
            for (int z = -10; z <= 10; z++)
                origin.clone().add(x,-2,z).getBlock().setType(Material.LAVA, false);
    }

    @Override public void start(List<Player> players) {
        this.players = new ArrayList<>(players);
        gameTime = 0; active = true;
        if (origin == null) origin = getOrigin();
        int idx = 0;
        for (Player p : players) {
            p.teleport(origin.clone().add((idx%6)*3-7, 16, (idx/6)*3-7));
            idx++;
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    @Override public void tick() {
        if (!active) return;
        gameTime++;
        int interval = cfgInt("fall-interval", 3);
        if (gameTime % interval == 0 && !platforms.isEmpty()) {
            Location plat = platforms.remove(new Random().nextInt(platforms.size()));
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> plat.getBlock().setType(Material.AIR, false), 10L);
            for (Player p : players) SoundUtils.play(p, "platform_fall");
        }
        for (Player p : new ArrayList<>(players)) {
            if (p.getLocation().getY() <= origin.getY() - 1) {
                plugin.getGameManager().eliminatePlayer(p);
                players.remove(p);
            }
        }
        if (players.size() <= 1 || gameTime >= getTimeLimit()) end();
    }

    @Override public void end() {
        active = false;
        if (task != null) { task.cancel(); task = null; }
        if (arenaData == null && origin != null) ArenaBuilder.clear(origin, 20, 25, 20);
    }
    @Override public void onQuit(Player p) { players.remove(p); }
}
