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

public class RussianRoulette extends MiniGame {
    private Location origin;
    private List<Player> players;
    private int turn, gameTime;
    private BukkitTask task;

    public RussianRoulette(CoreSquid p) { super(p); }
    @Override public String getId() { return "russian-roulette"; }
    @Override public String getName() { return "Ruleta Rusa"; }
    @Override public String getDescription() { return plugin.getLang().getRaw("game-desc-russian-roulette"); }
    @Override public int getTimeLimit() { return cfgInt("duration", 120); }

    @Override public void buildArena(Location origin) {
        this.origin = origin.clone();
        for (int x=-6;x<=6;x++)
            for (int z=-6;z<=6;z++)
                if (x*x+z*z<=36)
                    origin.clone().add(x,-1,z).getBlock().setType(Material.DARK_OAK_PLANKS, false);
    }

    @Override public void start(List<Player> players) {
        this.players = new ArrayList<>(players);
        turn = 0; gameTime = 0; active = true;
        if (origin == null) origin = getOrigin();
        int ang = 0;
        for (Player p : players) {
            double rad = Math.toRadians(ang);
            p.teleport(origin.clone().add(4*Math.cos(rad),1,4*Math.sin(rad)));
            ang += 360/players.size();
        }
        announceTurn();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void announceTurn() {
        if (players.isEmpty()) return;
        Player current = players.get(turn % players.size());
        for (Player p : players) {
            if (p == current) {
                p.sendTitle(ColorUtils.color("&c&l" + plugin.getLang().getRaw("roulette-your-turn")),
                        ColorUtils.color(plugin.getLang().getRaw("roulette-your-turn-sub")), 5, 40, 5);
            } else {
                p.sendTitle(ColorUtils.color(plugin.getLang().getRaw("roulette-others-turn","player",current.getName())),
                        ColorUtils.color(""), 5, 40, 5);
            }
        }
        current.sendMessage(plugin.getLang().get("roulette-your-turn-sub"));
    }

    public void drink(Player player) {
        if (!active) return;
        Player current = players.get(turn % players.size());
        if (player != current) { player.sendMessage(ColorUtils.color("&cNo es tu turno!")); return; }
        int deathChance = cfgInt("death-chance", 6);
        if (new Random().nextInt(deathChance) == 0) {
            player.sendTitle(ColorUtils.color("&c&l" + plugin.getLang().getRaw("roulette-bad")),
                    ColorUtils.color(plugin.getLang().getRaw("roulette-bad-sub")), 5, 60, 5);
            SoundUtils.play(player, "roulette_bad");
            plugin.getGameManager().eliminatePlayer(player);
            players.remove(player);
            if (players.size() <= 1) { end(); return; }
        } else {
            player.sendTitle(ColorUtils.color("&a&l" + plugin.getLang().getRaw("roulette-good")),
                    ColorUtils.color(plugin.getLang().getRaw("roulette-good-sub")), 5, 40, 5);
            SoundUtils.play(player, "roulette_good");
            turn++;
        }
        announceTurn();
    }

    @Override public void tick() {
        if (!active) return;
        gameTime++;
        if (gameTime >= getTimeLimit()) end();
    }
    @Override public void end() {
        active = false;
        if (task != null) { task.cancel(); task = null; }
        if (arenaData == null && origin != null) ArenaBuilder.clear(origin, 15, 5, 15);
    }
    @Override public void onQuit(Player p) { players.remove(p); }
}
