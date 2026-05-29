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

public class MusicalChairs extends MiniGame {
    private Location origin;
    private List<Player> players;
    private List<Location> chairs;
    private boolean musicPlaying;
    private int musicTimer, gameTime;
    private Set<UUID> seated;
    private BukkitTask task;

    public MusicalChairs(CoreSquid p) { super(p); chairs = new ArrayList<>(); seated = new HashSet<>(); }
    @Override public String getId() { return "musical-chairs"; }
    @Override public String getName() { return "Sillas Musicales"; }
    @Override public String getDescription() { return plugin.getLang().getRaw("game-desc-musical-chairs"); }
    @Override public int getTimeLimit() { return cfgInt("duration", 120); }

    @Override public void buildArena(Location origin) {
        this.origin = origin.clone();
        for (int x = -10; x <= 10; x++)
            for (int z = -10; z <= 10; z++)
                if (x*x + z*z <= 100)
                    origin.clone().add(x, -1, z).getBlock().setType(Material.POLISHED_ANDESITE, false);
    }

    private void placeChairs(int count) {
        chairs.clear();
        double angle = 2 * Math.PI / count;
        for (int i = 0; i < count; i++) {
            Location loc = origin.clone().add(6 * Math.cos(angle*i), 0, 6 * Math.sin(angle*i));
            loc.getBlock().setType(Material.OAK_STAIRS, false);
            chairs.add(loc);
        }
    }

    @Override public void start(List<Player> players) {
        this.players = new ArrayList<>(players);
        gameTime = 0; musicPlaying = true;
        int minDur = cfgInt("music-duration-min", 5);
        int maxDur = cfgInt("music-duration-max", 12);
        musicTimer = new Random().nextInt(maxDur - minDur + 1) + minDur;
        active = true;
        if (origin == null) origin = getOrigin();
        placeChairs(players.size() - 1);
        for (Player p : players) {
            p.teleport(origin.clone().add(0, 1, 0));
            p.sendTitle(ColorUtils.color("&e&lSILLAS MUSICALES"),
                    ColorUtils.color("&7¡Siéntate cuando pare la música!"), 10, 60, 10);
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    @Override public void tick() {
        if (!active) return;
        gameTime++; musicTimer--;
        int minDur = cfgInt("music-duration-min", 5);
        int maxDur = cfgInt("music-duration-max", 12);
        if (musicPlaying) {
            for (Player p : players) SoundUtils.play(p, "music_play");
            if (musicTimer <= 0) {
                musicPlaying = false; musicTimer = 5; seated.clear();
                for (Player p : players) {
                    p.sendTitle(ColorUtils.color("&c&lMÚSICA PARADA!"),
                            ColorUtils.color("&7¡Busca una silla!"), 5, 30, 5);
                    SoundUtils.play(p, "music_stop");
                }
            }
        } else {
            for (Player p : players) {
                for (Location chair : chairs) {
                    Location pLoc = p.getLocation().getBlock().getLocation();
                    Location pBelow = p.getLocation().clone().subtract(0,1,0).getBlock().getLocation();
                    if (pLoc.equals(chair) || pBelow.equals(chair)) {
                        seated.add(p.getUniqueId());
                        break;
                    }
                }
            }
            if (musicTimer <= 0) {
                Player out = null;
                for (Player p : players) if (!seated.contains(p.getUniqueId())) { out = p; break; }
                if (out != null) { plugin.getGameManager().eliminatePlayer(out); players.remove(out); }
                if (players.size() <= 1) { end(); return; }
                for (Location c : new ArrayList<>(chairs)) { c.getBlock().setType(Material.AIR, false); chairs.remove(c); break; }
                placeChairs(players.size() - 1);
                musicPlaying = true;
                musicTimer = new Random().nextInt(maxDur - minDur + 1) + minDur;
                seated.clear();
            }
        }
        if (gameTime >= getTimeLimit()) end();
    }

    @Override public void end() {
        active = false;
        if (task != null) { task.cancel(); task = null; }
        if (arenaData == null && origin != null) ArenaBuilder.clear(origin, 25, 5, 25);
    }
    @Override public void onQuit(Player p) { players.remove(p); seated.remove(p.getUniqueId()); }
}
