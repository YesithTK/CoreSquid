package dev.core.squid.games;

import dev.core.squid.CoreSquid;
import dev.core.squid.utils.ArenaBuilder;
import dev.core.squid.utils.ColorUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

public class HideSeek extends MiniGame {
    private Location origin;
    private List<Player> players;
    private Player seeker;
    private Set<UUID> found;
    private int gameTime;
    private BukkitTask task;

    public HideSeek(CoreSquid p) { super(p); found = new HashSet<>(); }
    @Override public String getId() { return "hide-seek"; }
    @Override public String getName() { return "Escondite"; }
    @Override public String getDescription() { return plugin.getLang().getRaw("game-desc-hide-seek"); }
    @Override public int getTimeLimit() { return cfgInt("duration", 90); }

    @Override public void buildArena(Location origin) {
        this.origin = origin.clone();
        Material[] mats = {Material.OAK_LOG, Material.SPRUCE_LOG, Material.DARK_OAK_LOG};
        for (int x=-15;x<=15;x++) for (int z=-15;z<=15;z++) {
            origin.clone().add(x,-1,z).getBlock().setType(Material.GRASS_BLOCK, false);
            if ((x%4==0||z%4==0) && Math.random()<0.4) {
                Material m = mats[new Random().nextInt(3)];
                origin.clone().add(x,0,z).getBlock().setType(m, false);
                origin.clone().add(x,1,z).getBlock().setType(m, false);
            }
        }
    }

    @Override public void start(List<Player> players) {
        this.players = new ArrayList<>(players);
        found = new HashSet<>(); gameTime = 0; active = true;
        if (origin == null) origin = getOrigin();
        seeker = players.get(new Random().nextInt(players.size()));
        int hideTime = cfgInt("hide-time", 10);
        seeker.teleport(origin.clone().add(0,1,0));
        seeker.sendTitle(ColorUtils.color("&c&l" + plugin.getLang().getRaw("hide-seek-seeker")),
                ColorUtils.color(plugin.getLang().getRaw("hide-seek-seeker-sub","seconds",String.valueOf(hideTime))), 10, 80, 10);
        int ang = 0;
        for (Player p : players) {
            if (p == seeker) continue;
            double rad = Math.toRadians(ang);
            p.teleport(origin.clone().add(12*Math.cos(rad),1,12*Math.sin(rad)));
            ang += 360/(players.size()-1);
            p.sendTitle(ColorUtils.color("&e&l" + plugin.getLang().getRaw("hide-seek-hider")),
                    ColorUtils.color(plugin.getLang().getRaw("hide-seek-hider-sub","seconds",String.valueOf(hideTime))), 10, 80, 10);
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (seeker != null && seeker.isOnline())
                seeker.sendTitle(ColorUtils.color("&c&l" + plugin.getLang().getRaw("hide-seek-go")),
                        ColorUtils.color(plugin.getLang().getRaw("hide-seek-go-sub")), 5, 30, 5);
        }, hideTime * 20L);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void onHit(Player attacker, Player victim) {
        if (!active || attacker != seeker || found.contains(victim.getUniqueId())) return;
        found.add(victim.getUniqueId());
        plugin.getGameManager().eliminatePlayer(victim);
        players.remove(victim);
        for (Player p : players) p.sendMessage(plugin.getLang().get("hide-seek-found","player",victim.getName()));
        if (players.stream().allMatch(p -> p==seeker || found.contains(p.getUniqueId()))) end();
    }

    @Override public void tick() {
        if (!active) return;
        gameTime++;
        if (seeker != null && seeker.isOnline())
            seeker.sendTitle(ColorUtils.color("&c"+(getTimeLimit()-gameTime)+"s"),
                    ColorUtils.color("&7Faltan &e"+(players.size()-1-found.size())+" &7jugadores"), 0, 25, 5);
        if (gameTime >= getTimeLimit()) { if (seeker != null) plugin.getGameManager().eliminatePlayer(seeker); end(); }
    }

    @Override public void end() {
        active = false;
        if (task != null) { task.cancel(); task = null; }
        if (arenaData == null && origin != null) ArenaBuilder.clear(origin, 35, 5, 35);
    }
    @Override public void onQuit(Player p) {
        players.remove(p);
        if (p == seeker) seeker = players.isEmpty() ? null : players.get(0);
    }
}
