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

public class GlassBridge extends MiniGame {

    private Location origin;
    private List<Player> players;
    private boolean[] correctSide; // true = left (z=-1), false = right (z=1)
    private int columns;
    private int gameTime;
    private BukkitTask task;
    private Queue<Player> queue;

    public GlassBridge(CoreSquid plugin) { super(plugin); }

    @Override public String getId() { return "glass-bridge"; }
    @Override public String getName() { return "Puente de Cristal"; }
    @Override public String getDescription() { return plugin.getLang().getRaw("game-desc-glass-bridge"); }
    @Override public int getTimeLimit() { return cfgInt("duration", 120); }

    @Override
    public void buildArena(Location origin) {
        this.origin = origin.clone();
        this.columns = cfgInt("columns", 18);
        this.correctSide = new boolean[columns];
        Random rand = new Random();
        for (int i = 0; i < columns; i++) correctSide[i] = rand.nextBoolean();

        // Lava below
        for (int x = -2; x <= columns + 2; x++)
            for (int z = -3; z <= 3; z++)
                origin.clone().add(x, -5, z).getBlock().setType(Material.LAVA, false);

        // Start platform
        for (int z = -2; z <= 2; z++) {
            origin.clone().add(-2, 0, z).getBlock().setType(Material.RED_CONCRETE, false);
            origin.clone().add(-3, 0, z).getBlock().setType(Material.RED_CONCRETE, false);
        }

        // End platform
        for (int z = -2; z <= 2; z++) {
            origin.clone().add(columns + 1, 0, z).getBlock().setType(Material.LIME_CONCRETE, false);
            origin.clone().add(columns + 2, 0, z).getBlock().setType(Material.LIME_CONCRETE, false);
        }

        // Glass panels
        for (int col = 0; col < columns; col++) {
            origin.clone().add(col, 0, -1).getBlock().setType(Material.GLASS, false);
            origin.clone().add(col, 0, 1).getBlock().setType(Material.GLASS, false);
        }
    }

    @Override
    public void start(List<Player> players) {
        this.players = new ArrayList<>(players);
        this.gameTime = 0;
        this.active = true;
        this.queue = new LinkedList<>(players);

        if (origin == null) origin = getOrigin();

        int z = -1;
        for (Player p : players) {
            p.teleport(origin.clone().add(-2, 1, z));
            z = z == -1 ? 1 : -1;
        }
        announceNext();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void announceNext() {
        Player current = queue.peek();
        if (current == null) return;
        for (Player p : players) {
            p.sendTitle(
                ColorUtils.color(p == current ? "&e&lTU TURNO" : "&7Turno de &e" + current.getName()),
                ColorUtils.color("&7Elige un panel de cristal"),
                10, 40, 10
            );
        }
    }

    public void onStepOnGlass(Player player, Location loc) {
        if (!active || queue.peek() != player) return;
        int col = loc.getBlockX() - origin.getBlockX();
        int z = loc.getBlockZ() - origin.getBlockZ();
        if (col < 0 || col >= columns) return;

        boolean choseLeft = (z == -1);
        boolean correct = (choseLeft == correctSide[col]);

        if (correct) {
            SoundUtils.play(player, "glass_correct");
            queue.poll();
            if (col >= columns - 1) {
                player.sendTitle(ColorUtils.color("&a&lCRUZASTE!"), ColorUtils.color(""), 10, 60, 10);
                players.remove(player);
            } else {
                announceNext();
            }
        } else {
            loc.getBlock().setType(Material.AIR, false);
            SoundUtils.play(player, "glass_break");
            queue.poll();
            plugin.getGameManager().eliminatePlayer(player);
            players.remove(player);
            announceNext();
        }

        if (queue.isEmpty()) end();
    }

    @Override
    public void tick() {
        if (!active) return;
        gameTime++;
        if (gameTime >= getTimeLimit()) {
            for (Player p : new ArrayList<>(queue)) plugin.getGameManager().eliminatePlayer(p);
            end();
        }
    }

    @Override
    public void end() {
        active = false;
        if (task != null) { task.cancel(); task = null; }
        if (arenaData == null && origin != null) ArenaBuilder.clear(origin, columns + 5, 8, 5);
    }

    @Override
    public void onQuit(Player player) {
        players.remove(player);
        queue.remove(player);
    }
}
