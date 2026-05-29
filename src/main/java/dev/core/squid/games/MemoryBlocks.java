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

public class MemoryBlocks extends MiniGame {
    private Location origin;
    private List<Player> players;
    private List<Material> sequence;
    private List<Location> buttons;
    private Map<UUID, List<Material>> answers;
    private int gameTime;
    private BukkitTask task;
    private static final Material[] COLORS = {
        Material.RED_WOOL, Material.BLUE_WOOL, Material.GREEN_WOOL,
        Material.YELLOW_WOOL, Material.PURPLE_WOOL, Material.ORANGE_WOOL
    };

    public MemoryBlocks(CoreSquid p) {
        super(p);
        sequence = new ArrayList<>();
        buttons = new ArrayList<>();
        answers = new HashMap<>();
    }
    @Override public String getId() { return "memory-blocks"; }
    @Override public String getName() { return "Memoria de Bloques"; }
    @Override public String getDescription() { return plugin.getLang().getRaw("game-desc-memory-blocks"); }
    @Override public int getTimeLimit() { return cfgInt("duration", 120); }

    @Override public void buildArena(Location origin) {
        this.origin = origin.clone();
        for (int x = -6; x <= 6; x++)
            for (int z = -6; z <= 6; z++)
                origin.clone().add(x,-1,z).getBlock().setType(Material.BLACK_CONCRETE, false);
        int[] bx = {-4,-2,0,2,4,-1}, bz = {0,0,0,0,0,2};
        for (int i = 0; i < 6; i++) {
            Location btn = origin.clone().add(bx[i], 0, bz[i]);
            btn.getBlock().setType(COLORS[i], false);
            buttons.add(btn);
        }
    }

    @Override public void start(List<Player> players) {
        this.players = new ArrayList<>(players);
        gameTime = 0; active = true;
        if (origin == null) origin = getOrigin();
        for (Player p : players) p.teleport(origin.clone().add(0,1,-4));
        showSequence();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void showSequence() {
        sequence.add(COLORS[new Random().nextInt(COLORS.length)]);
        for (int i = 0; i < sequence.size(); i++) {
            final Material mat = sequence.get(i);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (Location btn : buttons) {
                    if (btn.getBlock().getType() == mat) {
                        btn.getBlock().setType(Material.GLOWSTONE, false);
                        plugin.getServer().getScheduler().runTaskLater(plugin,
                                () -> btn.getBlock().setType(mat, false), 10L);
                    }
                }
                for (Player p : players) SoundUtils.play(p, "memory_show");
            }, (i+1)*15L);
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            answers.clear();
            for (Player p : players) {
                answers.put(p.getUniqueId(), new ArrayList<>());
                p.sendTitle(ColorUtils.color("&e&lTU TURNO"), ColorUtils.color("&7¡Repite la secuencia!"), 5, 30, 5);
            }
        }, (sequence.size()+1)*15L);
    }

    public void onClickBlock(Player player, Location loc) {
        if (!active) return;
        List<Material> ans = answers.get(player.getUniqueId());
        if (ans == null) return;
        ans.add(loc.getBlock().getType());
        SoundUtils.play(player, "memory_click");
        if (ans.size() == sequence.size()) {
            if (!ans.equals(sequence)) {
                plugin.getGameManager().eliminatePlayer(player);
                players.remove(player);
                answers.remove(player.getUniqueId());
            } else {
                player.sendTitle(ColorUtils.color("&a&lCORRECTO!"), ColorUtils.color(""), 5, 30, 5);
            }
            if (answers.values().stream().allMatch(r -> r.size() >= sequence.size())) {
                if (players.size() <= 1) { end(); return; }
                plugin.getServer().getScheduler().runTaskLater(plugin, this::showSequence, 40L);
            }
        }
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
    @Override public void onQuit(Player p) { players.remove(p); answers.remove(p.getUniqueId()); }
}
