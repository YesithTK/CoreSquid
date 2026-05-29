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

public class Dalgona extends MiniGame {

    private Location origin;
    private List<Player> players;
    private Map<UUID, Set<Location>> figureBlocks;   // Blocks to break (the shape)
    private Map<UUID, Set<Location>> borderBlocks;   // Blocks NOT to break (the border)
    private int gameTime;
    private BukkitTask task;

    public Dalgona(CoreSquid plugin) {
        super(plugin);
        figureBlocks = new HashMap<>();
        borderBlocks = new HashMap<>();
    }

    @Override public String getId() { return "dalgona"; }
    @Override public String getName() { return "Dalgona"; }
    @Override public String getDescription() { return plugin.getLang().getRaw("game-desc-dalgona"); }
    @Override public int getTimeLimit() { return cfgInt("duration", 120); }

    @Override
    public void buildArena(Location origin) {
        this.origin = origin.clone();
        // 4x4 grid of cells, each 12 blocks apart
        for (int i = 0; i < 16; i++) {
            int ox = (i % 4) * 12;
            int oz = (i / 4) * 12;
            // Floor
            for (int x = 0; x <= 9; x++)
                for (int z = 0; z <= 9; z++)
                    origin.clone().add(ox + x, -1, oz + z).getBlock().setType(Material.BROWN_CONCRETE, false);
            // Walls (barrier)
            for (int x = 0; x <= 9; x++) {
                origin.clone().add(ox + x, 0, oz).getBlock().setType(Material.BARRIER, false);
                origin.clone().add(ox + x, 0, oz + 9).getBlock().setType(Material.BARRIER, false);
                origin.clone().add(ox + x, 1, oz).getBlock().setType(Material.BARRIER, false);
                origin.clone().add(ox + x, 1, oz + 9).getBlock().setType(Material.BARRIER, false);
            }
            for (int z = 0; z <= 9; z++) {
                origin.clone().add(ox, 0, oz + z).getBlock().setType(Material.BARRIER, false);
                origin.clone().add(ox + 9, 0, oz + z).getBlock().setType(Material.BARRIER, false);
                origin.clone().add(ox, 1, oz + z).getBlock().setType(Material.BARRIER, false);
                origin.clone().add(ox + 9, 1, oz + z).getBlock().setType(Material.BARRIER, false);
            }
        }
    }

    @Override
    public void start(List<Player> players) {
        this.players = new ArrayList<>(players);
        this.gameTime = 0;
        this.active = true;
        if (origin == null) origin = getOrigin();

        String shape = cfg("shape", "circle");

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            int ox = (i % 4) * 12;
            int oz = (i / 4) * 12;
            Location center = origin.clone().add(ox + 4, 0, oz + 4);
            p.teleport(center.clone().add(0, 1, 0));

            Set<Location> figure = new HashSet<>();
            Set<Location> border = new HashSet<>();
            buildShape(center, shape, figure, border);
            figureBlocks.put(p.getUniqueId(), figure);
            borderBlocks.put(p.getUniqueId(), border);
        }

        // Send clear explanation with delay
        for (Player p : players) {
            // Title
            p.sendTitle(ColorUtils.color("&6&lDALGONA"),
                    ColorUtils.color("&7¡Lee las instrucciones!"), 10, 100, 10);
            // Chat message explaining the game
            p.sendMessage(ColorUtils.color("&8&m                              "));
            p.sendMessage(ColorUtils.color("  &6&lDALGONA &7- Instrucciones"));
            p.sendMessage(ColorUtils.color("&8&m                              "));
            p.sendMessage(ColorUtils.color("  &7Tienes una &6galleta de caramelo"));
            p.sendMessage(ColorUtils.color("  &7con una &eFIGURA &7grabada en ella."));
            p.sendMessage(ColorUtils.color("  &7"));
            p.sendMessage(ColorUtils.color("  &a► &7Rompe los bloques &6DORADOS &7(la figura)"));
            p.sendMessage(ColorUtils.color("  &c✗ &7NO rompas los bloques &cROJOS &7(el borde)"));
            p.sendMessage(ColorUtils.color("  &7"));
            p.sendMessage(ColorUtils.color("  &7Si rompes el borde: &c¡Eliminado!"));
            p.sendMessage(ColorUtils.color("  &7Si completas la figura: &a¡Pasas!"));
            p.sendMessage(ColorUtils.color("&8&m                              "));
            p.sendMessage(plugin.getLang().get("dalgona-explain",
                    "shape", shape.toUpperCase()));
        }

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void buildShape(Location center, String shape, Set<Location> figure, Set<Location> border) {
        switch (shape.toLowerCase()) {
            case "triangle" -> {
                // Triangle shape
                int[][] points = {{0,0},{-1,1},{0,1},{1,1},{-2,2},{-1,2},{0,2},{1,2},{2,2}};
                for (int[] pt : points) {
                    Location loc = center.clone().add(pt[0], 0, pt[1]);
                    loc.getBlock().setType(Material.GOLD_BLOCK, false);
                    figure.add(loc);
                }
                // Border around it
                for (int x = -3; x <= 3; x++) for (int z = -1; z <= 3; z++) {
                    Location loc = center.clone().add(x, 0, z);
                    if (!figure.contains(loc)) { loc.getBlock().setType(Material.RED_CONCRETE, false); border.add(loc); }
                }
            }
            case "star" -> {
                // Star shape
                int[][] points = {{0,0},{-1,0},{1,0},{0,-1},{0,1},{-1,-1},{1,-1},{-1,1},{1,1},{0,-2},{0,2},{-2,0},{2,0}};
                for (int[] pt : points) {
                    Location loc = center.clone().add(pt[0], 0, pt[1]);
                    loc.getBlock().setType(Material.GOLD_BLOCK, false);
                    figure.add(loc);
                }
                for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
                    Location loc = center.clone().add(x, 0, z);
                    if (!figure.contains(loc)) { loc.getBlock().setType(Material.RED_CONCRETE, false); border.add(loc); }
                }
            }
            case "umbrella" -> {
                // Umbrella shape
                int[][] points = {{0,0},{0,-1},{0,-2},{-1,-2},{1,-2},{-2,-1},{2,-1},{-2,0},{2,0},{-1,1},{0,1},{1,1},{-2,1},{2,1}};
                for (int[] pt : points) {
                    Location loc = center.clone().add(pt[0], 0, pt[1]);
                    loc.getBlock().setType(Material.GOLD_BLOCK, false);
                    figure.add(loc);
                }
                for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
                    Location loc = center.clone().add(x, 0, z);
                    if (!figure.contains(loc)) { loc.getBlock().setType(Material.RED_CONCRETE, false); border.add(loc); }
                }
            }
            default -> { // circle
                for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                    if (x*x + z*z <= 5) {
                        Location loc = center.clone().add(x, 0, z);
                        loc.getBlock().setType(Material.GOLD_BLOCK, false);
                        figure.add(loc);
                    }
                }
                for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
                    Location loc = center.clone().add(x, 0, z);
                    if (!figure.contains(loc)) { loc.getBlock().setType(Material.RED_CONCRETE, false); border.add(loc); }
                }
            }
        }
    }

    public void onBreakBlock(Player player, Location loc) {
        if (!active) return;

        Set<Location> figure = figureBlocks.get(player.getUniqueId());
        Set<Location> border = borderBlocks.get(player.getUniqueId());
        if (figure == null) return;

        if (border != null && border.contains(loc)) {
            // Broke the border - eliminated
            player.sendTitle(ColorUtils.color("&c&l" + plugin.getLang().getRaw("dalgona-fail")),
                    ColorUtils.color(plugin.getLang().getRaw("dalgona-fail-sub")), 10, 60, 10);
            plugin.getGameManager().eliminatePlayer(player);
            players.remove(player);
            if (players.isEmpty()) end();
        } else if (figure.contains(loc)) {
            figure.remove(loc);
            SoundUtils.play(player, "dalgona_break");
            // Show progress
            player.sendTitle(ColorUtils.color("&e" + figure.size() + " &7restantes"),
                    ColorUtils.color("&7¡Sigue rompiendo la figura!"), 0, 20, 5);
            if (figure.isEmpty()) {
                player.sendTitle(ColorUtils.color("&a&l" + plugin.getLang().getRaw("dalgona-correct")),
                        ColorUtils.color(plugin.getLang().getRaw("dalgona-correct-sub")), 10, 80, 10);
                players.remove(player);
                if (players.isEmpty()) end();
            }
        }
    }

    @Override
    public void tick() {
        if (!active) return;
        gameTime++;
        // Show time remaining every 20s
        if (gameTime % 20 == 0) {
            int left = getTimeLimit() - gameTime;
            for (Player p : players)
                p.sendMessage(ColorUtils.color("&7Tiempo restante: &e" + left + "s"));
        }
        if (gameTime >= getTimeLimit()) {
            for (Player p : new ArrayList<>(players)) plugin.getGameManager().eliminatePlayer(p);
            end();
        }
    }

    @Override
    public void end() {
        active = false;
        if (task != null) { task.cancel(); task = null; }
        if (arenaData == null && origin != null) ArenaBuilder.clear(origin, 52, 5, 52);
    }

    @Override
    public void onQuit(Player player) {
        players.remove(player);
        figureBlocks.remove(player.getUniqueId());
        borderBlocks.remove(player.getUniqueId());
    }
}
