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

public class TugOfWar extends MiniGame {

    private Location origin;
    private List<Player> teamA, teamB, allPlayers;
    private int ropePosition; // negative = A wins, positive = B wins
    private int gameTime;
    private BukkitTask task;

    public TugOfWar(CoreSquid plugin) {
        super(plugin);
        teamA = new ArrayList<>();
        teamB = new ArrayList<>();
    }

    @Override public String getId() { return "tug-of-war"; }
    @Override public String getName() { return "Tira y Jala"; }
    @Override public String getDescription() { return plugin.getLang().getRaw("game-desc-tug-of-war"); }
    @Override public int getTimeLimit() { return cfgInt("duration", 60); }

    @Override
    public void buildArena(Location origin) {
        this.origin = origin.clone();
        for (int x = -12; x <= 12; x++)
            for (int z = -3; z <= 3; z++) {
                Material mat = x < 0 ? Material.BLUE_CONCRETE : x > 0 ? Material.RED_CONCRETE : Material.LAVA;
                origin.clone().add(x, -1, z).getBlock().setType(mat, false);
            }
        for (int z = -3; z <= 3; z++) {
            origin.clone().add(-10, 0, z).getBlock().setType(Material.BLUE_STAINED_GLASS, false);
            origin.clone().add(10, 0, z).getBlock().setType(Material.RED_STAINED_GLASS, false);
        }
    }

    @Override
    public void start(List<Player> players) {
        this.allPlayers = new ArrayList<>(players);
        this.ropePosition = 0;
        this.gameTime = 0;
        this.active = true;
        if (origin == null) origin = getOrigin();

        Collections.shuffle(allPlayers);
        for (int i = 0; i < allPlayers.size(); i++) {
            if (i % 2 == 0) teamA.add(allPlayers.get(i));
            else teamB.add(allPlayers.get(i));
        }

        for (Player p : teamA) {
            p.teleport(origin.clone().add(-5, 1, 0));
            p.sendTitle(ColorUtils.color("&9&lEQUIPO AZUL"), ColorUtils.color("&7¡Haz clic rápido!"), 10, 60, 10);
        }
        for (Player p : teamB) {
            p.teleport(origin.clone().add(5, 1, 0));
            p.sendTitle(ColorUtils.color("&c&lEQUIPO ROJO"), ColorUtils.color("&7¡Haz clic rápido!"), 10, 60, 10);
        }

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void pull(Player player) {
        if (!active) return;
        if (teamA.contains(player)) ropePosition--;
        else if (teamB.contains(player)) ropePosition++;
        else return;
        SoundUtils.play(player, "tug_pull");
        checkWin();
    }

    private void checkWin() {
        int winDist = cfgInt("win-distance", 10);
        if (ropePosition <= -winDist) {
            for (Player p : new ArrayList<>(teamB)) plugin.getGameManager().eliminatePlayer(p);
            end();
        } else if (ropePosition >= winDist) {
            for (Player p : new ArrayList<>(teamA)) plugin.getGameManager().eliminatePlayer(p);
            end();
        }
    }

    @Override
    public void tick() {
        if (!active) return;
        gameTime++;
        // Broadcast rope position every 5s
        if (gameTime % 5 == 0) {
            String progress = plugin.getLang().getRaw("tug-progress",
                    "a", String.valueOf(-ropePosition),
                    "b", String.valueOf(ropePosition));
            for (Player p : allPlayers) p.sendMessage(ColorUtils.color(progress));
        }
        if (gameTime >= getTimeLimit()) {
            if (ropePosition <= 0) for (Player p : new ArrayList<>(teamB)) plugin.getGameManager().eliminatePlayer(p);
            else for (Player p : new ArrayList<>(teamA)) plugin.getGameManager().eliminatePlayer(p);
            end();
        }
    }

    @Override
    public void end() {
        active = false;
        if (task != null) { task.cancel(); task = null; }
        teamA.clear(); teamB.clear();
        if (arenaData == null && origin != null) ArenaBuilder.clear(origin, 28, 5, 8);
    }

    @Override
    public void onQuit(Player player) {
        allPlayers.remove(player);
        teamA.remove(player);
        teamB.remove(player);
    }
}
