package dev.core.squid.managers;

import dev.core.squid.CoreSquid;
import dev.core.squid.model.GameState;
import dev.core.squid.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;

public class ScoreboardManager {

    private final CoreSquid plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    private static final String[] ENTRIES = {
        "\u00A71", "\u00A72", "\u00A73", "\u00A74", "\u00A75",
        "\u00A76", "\u00A77", "\u00A78", "\u00A79", "\u00A7a",
        "\u00A7b", "\u00A7c"
    };

    public ScoreboardManager(CoreSquid plugin) {
        this.plugin = plugin;
    }

    public void update() {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;

        for (Player p : Bukkit.getOnlinePlayers()) {
            List<String> lines = buildLines(p);
            show(p, lines);
        }
    }

    private List<String> buildLines(Player player) {
        GameManager gm = plugin.getGameManager();
        GameState state = gm.getState();
        String serverIp = plugin.getConfig().getString("server-ip", "play.colombiacraft.fun");
        String gameTitle = gm.getCurrentGame() != null ? gm.getCurrentGame().getName() : "Ninguno";

        List<String> rawLines;

        if (gm.isInLobby(player)) {
            // LOBBY lines
            rawLines = plugin.getConfig().getStringList("scoreboard.lobby-lines");
        } else if (gm.isInGame(player) || state == GameState.IN_GAME) {
            // IN GAME lines
            rawLines = plugin.getConfig().getStringList("scoreboard.game-lines");
        } else {
            // GLOBAL lines
            rawLines = plugin.getConfig().getStringList("scoreboard.global-lines");
        }

        int min = plugin.getConfig().getInt("game.min-players", 2);
        int max = plugin.getConfig().getInt("game.max-players", 16);

        // Apply placeholders
        List<String> result = new ArrayList<>();
        for (String line : rawLines) {
            String processed = line
                .replace("{state}", getStateText(state))
                .replace("{playing}", String.valueOf(gm.getActivePlayers().size()))
                .replace("{lobby}", String.valueOf(gm.getLobby().size()))
                .replace("{players}", String.valueOf(gm.getLobby().size()))
                .replace("{max}", String.valueOf(max))
                .replace("{min}", String.valueOf(min))
                .replace("{countdown}", String.valueOf(gm.getLobbyCountdown()))
                .replace("{game}", gameTitle)
                .replace("{alive}", String.valueOf(gm.getActivePlayers().size()))
                .replace("{eliminated}", String.valueOf(gm.getSpectators().size()))
                .replace("{time}", getTimeDisplay())
                .replace("{server-ip}", serverIp);
            result.add(processed);
        }
        return result;
    }

    private String getStateText(GameState state) {
        return switch (state) {
            case INACTIVE -> "&7Inactivo";
            case LOBBY -> "&aLobby";
            case STARTING -> "&eIniciando";
            case IN_GAME -> "&cEn Juego";
            case BETWEEN_GAMES -> "&eEntre Juegos";
            case ENDING -> "&cFinalizando";
        };
    }

    private String getTimeDisplay() {
        MiniGame game = plugin.getGameManager().getCurrentGame();
        if (game == null) return "00:00";
        return "En curso";
    }

    private void show(Player player, List<String> lines) {
        org.bukkit.scoreboard.ScoreboardManager sbm = Bukkit.getScoreboardManager();
        Scoreboard sb = boards.computeIfAbsent(player.getUniqueId(), k -> sbm.getNewScoreboard());

        String title = ColorUtils.color(plugin.getConfig().getString("scoreboard.title", "&c&lSQUID GAME"));
        Objective obj = sb.getObjective("coresquid");
        if (obj == null) {
            obj = sb.registerNewObjective("coresquid", "dummy", title);
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        obj.setDisplayName(title);

        // Clear old entries
        for (String entry : ENTRIES) {
            Team t = sb.getEntryTeam(entry);
            if (t != null) t.removeEntry(entry);
        }

        // Write lines bottom-to-top
        int score = lines.size();
        for (int i = 0; i < lines.size() && i < ENTRIES.length; i++) {
            String entry = ENTRIES[i];
            String text = ColorUtils.color(lines.get(i));

            // Split into prefix (max 16 chars in old API, but use full text with team)
            String prefix = text.length() > 64 ? text.substring(0, 64) : text;
            String suffix = text.length() > 64 ? text.substring(64, Math.min(text.length(), 128)) : "";

            Team team = sb.getTeam("sq_" + i);
            if (team == null) team = sb.registerNewTeam("sq_" + i);
            team.setPrefix(prefix);
            team.setSuffix(suffix);
            if (!team.hasEntry(entry)) team.addEntry(entry);
            obj.getScore(entry).setScore(score--);
        }

        player.setScoreboard(sb);
    }

    public void remove(Player player) {
        boards.remove(player.getUniqueId());
        org.bukkit.scoreboard.ScoreboardManager sbm = Bukkit.getScoreboardManager();
        if (sbm != null) player.setScoreboard(sbm.getMainScoreboard());
    }

    public void removeAll() {
        for (Player p : Bukkit.getOnlinePlayers()) remove(p);
        boards.clear();
    }
}
