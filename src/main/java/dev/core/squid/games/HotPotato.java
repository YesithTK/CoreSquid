package dev.core.squid.games;

import dev.core.squid.CoreSquid;
import dev.core.squid.utils.ArenaBuilder;
import dev.core.squid.utils.ColorUtils;
import dev.core.squid.utils.SoundUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

public class HotPotato extends MiniGame {
    private Location origin;
    private List<Player> players;
    private Player hasBomb;
    private int bombTimer, gameTime;
    private BukkitTask task;

    public HotPotato(CoreSquid p) { super(p); }
    @Override public String getId() { return "hot-potato"; }
    @Override public String getName() { return "Bomba Caliente"; }
    @Override public String getDescription() { return plugin.getLang().getRaw("game-desc-hot-potato"); }
    @Override public int getTimeLimit() { return cfgInt("duration", 90); }

    @Override public void buildArena(Location origin) {
        this.origin = origin.clone();
        for (int x = -8; x <= 8; x++)
            for (int z = -8; z <= 8; z++)
                if (x*x+z*z<=64)
                    origin.clone().add(x,-1,z).getBlock().setType(Material.CRIMSON_PLANKS, false);
    }

    @Override public void start(List<Player> players) {
        this.players = new ArrayList<>(players);
        gameTime = 0; active = true;
        if (origin == null) origin = getOrigin();
        int ang = 0;
        for (Player p : players) {
            p.teleport(origin.clone().add(5*Math.cos(Math.toRadians(ang)), 1, 5*Math.sin(Math.toRadians(ang))));
            ang += 360/players.size();
        }
        resetBomb();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void resetBomb() {
        if (players.isEmpty()) return;
        hasBomb = players.get(new Random().nextInt(players.size()));
        int minFuse = cfgInt("min-fuse", 5);
        int maxFuse = cfgInt("max-fuse", 15);
        bombTimer = new Random().nextInt(maxFuse - minFuse + 1) + minFuse;
        hasBomb.getInventory().setItem(4, new ItemStack(Material.TNT));
        hasBomb.sendTitle(ColorUtils.color("&c&l" + plugin.getLang().getRaw("hot-potato-got")),
                ColorUtils.color(plugin.getLang().getRaw("hot-potato-got-sub")), 5, 40, 5);
        SoundUtils.play(hasBomb, "bomb_tick");
    }

    public void passBomb(Player from, Player to) {
        if (!active || from != hasBomb || !players.contains(to)) return;
        from.getInventory().remove(Material.TNT);
        hasBomb = to;
        to.getInventory().setItem(4, new ItemStack(Material.TNT));
        to.sendTitle(ColorUtils.color("&c&l" + plugin.getLang().getRaw("hot-potato-got")),
                ColorUtils.color(plugin.getLang().getRaw("hot-potato-got-sub")), 5, 30, 5);
        from.sendMessage(plugin.getLang().get("hot-potato-passed", "player", to.getName()));
        SoundUtils.play(to, "bomb_tick");
    }

    @Override public void tick() {
        if (!active) return;
        gameTime++; bombTimer--;
        if (hasBomb != null)
            hasBomb.sendTitle(ColorUtils.color("&c&l" + bombTimer), ColorUtils.color("&7¡PASA LA BOMBA!"), 0, 25, 5);
        if (bombTimer <= 0) {
            if (hasBomb != null) {
                hasBomb.getInventory().remove(Material.TNT);
                hasBomb.getWorld().createExplosion(hasBomb.getLocation(), 0F, false, false);
                for (Player p : players)
                    p.sendMessage(plugin.getLang().get("hot-potato-exploded", "player", hasBomb.getName()));
                plugin.getGameManager().eliminatePlayer(hasBomb);
                players.remove(hasBomb); hasBomb = null;
            }
            if (players.size() <= 1) { end(); return; }
            resetBomb();
        }
        if (gameTime >= getTimeLimit()) end();
    }

    @Override public void end() {
        active = false;
        if (task != null) { task.cancel(); task = null; }
        if (hasBomb != null) hasBomb.getInventory().remove(Material.TNT);
        if (arenaData == null && origin != null) ArenaBuilder.clear(origin, 20, 5, 20);
    }
    @Override public void onQuit(Player p) {
        players.remove(p);
        if (hasBomb == p) { hasBomb = null; if (!players.isEmpty()) resetBomb(); }
    }
}
