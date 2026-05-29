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

public class RedLightGreenLight extends MiniGame {

    private Location origin;
    private List<Player> players;
    private List<Player> safe;  // Players who reached the goal
    private Map<UUID, Location> lastPositions;

    private boolean greenLight;
    private int gameTime;
    private int lightTimer;
    private BukkitTask task;
    private int goalX;

    public RedLightGreenLight(CoreSquid plugin) {
        super(plugin);
        this.safe = new ArrayList<>();
        this.lastPositions = new HashMap<>();
    }

    @Override public String getId() { return "red-light-green-light"; }
    @Override public String getName() { return plugin.getLang().getRaw("game-desc-red-light-green-light").isEmpty() ? "Luz Roja Luz Verde" : "Luz Roja Luz Verde"; }
    @Override public String getDescription() { return plugin.getLang().getRaw("game-desc-red-light-green-light"); }
    @Override public int getTimeLimit() { return cfgInt("duration", 90); }

    @Override
    public void buildArena(Location origin) {
        this.origin = origin.clone();
        this.goalX = origin.getBlockX() + 40;

        // Floor
        for (int x = 0; x <= 42; x++)
            for (int z = -5; z <= 5; z++)
                origin.clone().add(x, -1, z).getBlock().setType(Material.GREEN_CONCRETE, false);

        // Goal line
        for (int z = -5; z <= 5; z++) {
            origin.clone().add(40, 0, z).getBlock().setType(Material.GOLD_BLOCK, false);
        }

        // Doll representation
        origin.clone().add(42, 0, 0).getBlock().setType(Material.CRYING_OBSIDIAN, false);
        origin.clone().add(42, 1, 0).getBlock().setType(Material.JACK_O_LANTERN, false);
    }

    @Override
    public void start(List<Player> players) {
        this.players = new ArrayList<>(players);
        this.safe = new ArrayList<>();
        this.lastPositions = new HashMap<>();
        this.greenLight = true;
        this.gameTime = 0;
        this.lightTimer = 5;
        this.active = true;

        if (this.origin == null) this.origin = getOrigin();
        if (arenaData != null) this.origin = arenaData.getSpawn();

        // Spread players at start line
        int z = -Math.min(4, players.size() / 2);
        for (Player p : players) {
            p.teleport(origin.clone().add(0, 1, z));
            z = z >= 4 ? -4 : z + 1;
            lastPositions.put(p.getUniqueId(), p.getLocation().clone());
        }

        // Announce
        LangManager lang = plugin.getLang();
        for (Player p : players) {
            p.sendTitle(ColorUtils.color("&a&l" + lang.getRaw("rlgl-green")),
                    ColorUtils.color(lang.getRaw("rlgl-green-sub")), 10, 60, 10);
        }
        SoundUtils.playAll(players, "green_light");

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    @Override
    public void tick() {
        if (!active) return;
        gameTime++;
        lightTimer--;

        // CHECK MOVEMENT ON RED LIGHT
        if (!greenLight) {
            double threshold = cfgDouble("move-threshold", 0.15);
            for (Player p : new ArrayList<>(players)) {
                Location last = lastPositions.get(p.getUniqueId());
                if (last == null) continue;
                double moved = p.getLocation().distance(last);
                if (moved > threshold) {
                    plugin.getGameManager().eliminatePlayer(p);
                    players.remove(p);
                    lastPositions.remove(p.getUniqueId());
                }
            }
        }

        // Update last positions
        for (Player p : players) {
            lastPositions.put(p.getUniqueId(), p.getLocation().clone());
        }

        // CHECK GOAL
        for (Player p : new ArrayList<>(players)) {
            if (p.getLocation().getBlockX() >= goalX) {
                safe.add(p);
                players.remove(p);
                lastPositions.remove(p.getUniqueId());
                LangManager lang = plugin.getLang();
                p.sendTitle(ColorUtils.color("&a&l" + lang.getRaw("rlgl-reached")),
                        ColorUtils.color(lang.getRaw("rlgl-reached-sub")), 10, 60, 10);
            }
        }

        // SWITCH LIGHT
        if (lightTimer <= 0) {
            greenLight = !greenLight;
            lightTimer = greenLight
                ? new Random().nextInt(4) + 3
                : new Random().nextInt(3) + 2;

            LangManager lang = plugin.getLang();
            for (Player p : players) {
                if (greenLight) {
                    p.sendTitle(ColorUtils.color("&a&l" + lang.getRaw("rlgl-green")),
                            ColorUtils.color(lang.getRaw("rlgl-green-sub")), 5, 30, 5);
                    SoundUtils.play(p, "green_light");
                } else {
                    p.sendTitle(ColorUtils.color("&c&l" + lang.getRaw("rlgl-red")),
                            ColorUtils.color(lang.getRaw("rlgl-red-sub")), 5, 30, 5);
                    SoundUtils.play(p, "red_light");
                }
            }
        }

        // TIME OUT or all done
        if (gameTime >= getTimeLimit()) {
            for (Player p : new ArrayList<>(players)) plugin.getGameManager().eliminatePlayer(p);
            end();
            return;
        }

        if (players.isEmpty()) end();
    }

    @Override
    public void end() {
        active = false;
        if (task != null) { task.cancel(); task = null; }
        // DO NOT clear arena blocks if custom arena - only clear if auto-generated
        if (arenaData == null && origin != null) {
            ArenaBuilder.clear(origin, 45, 5, 8);
        }
    }

    @Override
    public void onQuit(Player player) {
        players.remove(player);
        safe.remove(player);
        lastPositions.remove(player.getUniqueId());
    }
}
