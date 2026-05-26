package dev.core.squid.managers;

import dev.core.squid.CoreSquid;
import dev.core.squid.model.GameState;
import dev.core.squid.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {

    private final CoreSquid plugin;
    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();

    private static final String[] ENTRIES = {
        "\u00A71", "\u00A72", "\u00A73", "\u00A74", "\u00A75",
        "\u00A76", "\u00A77", "\u00A78", "\u00A79", "\u00A7a"
    };

    public ScoreboardManager(CoreSquid plugin) {
        this.plugin = plugin;
    }

    public void actualizar() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            GameState estado = plugin.getGameManager().getEstado();
            if (estado == GameState.INACTIVO) {
                quitarScoreboard(p);
                continue;
            }
            mostrarScoreboard(p);
        }
    }

    private void mostrarScoreboard(Player player) {
        org.bukkit.scoreboard.ScoreboardManager sbm = Bukkit.getScoreboardManager();
        Scoreboard sb = scoreboards.computeIfAbsent(player.getUniqueId(), k -> sbm.getNewScoreboard());

        Objective obj = sb.getObjective("squidgame");
        if (obj == null) {
            obj = sb.registerNewObjective("squidgame", "dummy",
                    ColorUtils.color("&c&lSQUID GAME"));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        obj.setDisplayName(ColorUtils.color("&c&lSQUID GAME"));

        GameManager gm = plugin.getGameManager();
        String juego = gm.getJuegoActual() != null ? gm.getJuegoActual().getNombre() : "Esperando...";
        int activos = gm.getJugadoresActivos().size();
        int lobby = gm.getLobby().size();
        GameState estado = gm.getEstado();

        String[] lineas = {
            "&7",
            "&7Estado: &e" + estadoTexto(estado),
            "&7",
            "&7Juego: &e" + juego,
            "&7Jugadores: &e" + activos,
            "&7Lobby: &e" + lobby,
            "&7",
            "&cplay.tuservidor.net"
        };

        limpiarEntradas(sb);

        for (int i = 0; i < lineas.length && i < ENTRIES.length; i++) {
            String entry = ENTRIES[i];
            String texto = ColorUtils.color(lineas[i]);

            String prefijo = texto.length() > 16 ? texto.substring(0, 16) : texto;
            String sufijo = texto.length() > 16 ? texto.substring(16) : "";
            if (sufijo.length() > 16) sufijo = sufijo.substring(0, 16);

            Team team = sb.getTeam("sq_" + i);
            if (team == null) team = sb.registerNewTeam("sq_" + i);
            team.setPrefix(prefijo);
            team.setSuffix(sufijo);
            team.addEntry(entry);
            obj.getScore(entry).setScore(lineas.length - i);
        }

        player.setScoreboard(sb);
    }

    private void limpiarEntradas(Scoreboard sb) {
        for (String entry : ENTRIES) {
            Team t = sb.getEntryTeam(entry);
            if (t != null) t.removeEntry(entry);
        }
    }

    private String estadoTexto(GameState estado) {
        return switch (estado) {
            case INACTIVO -> "Inactivo";
            case LOBBY -> "Lobby";
            case INICIANDO -> "Iniciando";
            case EN_JUEGO -> "En juego";
            case ENTRE_JUEGOS -> "Entre juegos";
            case FIN -> "Fin";
        };
    }

    public void quitarScoreboard(Player player) {
        scoreboards.remove(player.getUniqueId());
        org.bukkit.scoreboard.ScoreboardManager sbm = Bukkit.getScoreboardManager();
        if (sbm != null) player.setScoreboard(sbm.getMainScoreboard());
    }

    public void limpiarTodo() {
        for (Player p : Bukkit.getOnlinePlayers()) quitarScoreboard(p);
        scoreboards.clear();
    }
}
