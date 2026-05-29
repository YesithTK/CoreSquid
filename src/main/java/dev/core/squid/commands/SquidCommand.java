package dev.core.squid.commands;

import dev.core.squid.CoreSquid;
import dev.core.squid.managers.GameManager;
import dev.core.squid.model.ArenaData;
import dev.core.squid.model.GameState;
import dev.core.squid.model.PlayerStats;
import dev.core.squid.games.*;
import dev.core.squid.utils.ColorUtils;
import dev.core.squid.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class SquidCommand implements CommandExecutor, TabCompleter {

    private final CoreSquid plugin;
    private final GameManager gm;

    public SquidCommand(CoreSquid plugin) {
        this.plugin = plugin;
        this.gm = plugin.getGameManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {

            // ── /squid join ─────────────────────────────────────────
            case "join", "unirse" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
                if (!p.hasPermission("coresquid.use")) { p.sendMessage(plugin.getLang().get("no-permission")); return true; }
                if (gm.isInLobby(p) || gm.isInGame(p)) { p.sendMessage(plugin.getLang().get("already-in-lobby")); return true; }
                if (gm.getState() == GameState.IN_GAME) { p.sendMessage(plugin.getLang().get("game-in-progress")); return true; }
                if (gm.getLobby().size() >= plugin.getConfig().getInt("game.max-players", 16)) { p.sendMessage(plugin.getLang().get("lobby-full", "max", String.valueOf(plugin.getConfig().getInt("game.max-players", 16)))); return true; }
                gm.joinLobby(p);
            }

            // ── /squid leave ────────────────────────────────────────
            case "leave", "salir" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
                if (!gm.isInLobby(p)) { p.sendMessage(plugin.getLang().get("not-in-lobby")); return true; }
                gm.leaveLobby(p);
            }

            // ── /squid start ────────────────────────────────────────
            case "start", "iniciar" -> {
                if (!sender.hasPermission("coresquid.admin")) { sender.sendMessage(plugin.getLang().get("no-permission")); return true; }
                gm.forceStart();
                sender.sendMessage(ColorUtils.color("&aPartida iniciada."));
            }

            // ── /squid stop ─────────────────────────────────────────
            case "stop", "detener" -> {
                if (!sender.hasPermission("coresquid.admin")) { sender.sendMessage(plugin.getLang().get("no-permission")); return true; }
                gm.forceStop();
            }

            // ── /squid stats [player] ────────────────────────────────
            case "stats" -> {
                if (!sender.hasPermission("coresquid.stats")) { sender.sendMessage(plugin.getLang().get("no-permission")); return true; }
                Player target = sender instanceof Player p ? p : null;
                if (args.length >= 2) { target = Bukkit.getPlayer(args[1]); if (target == null) { sender.sendMessage(plugin.getLang().get("player-not-found", "player", args[1])); return true; } }
                if (target == null) { sender.sendMessage("Specify a player."); return true; }
                PlayerStats ps = plugin.getStatsManager().getStats(target.getUniqueId(), target.getName());
                sender.sendMessage(ColorUtils.color("&8&m                            "));
                sender.sendMessage(plugin.getLang().get("stats-title", "player", target.getName()));
                sender.sendMessage(plugin.getLang().get("stats-wins", "wins", String.valueOf(ps.getWins())));
                sender.sendMessage(plugin.getLang().get("stats-losses", "losses", String.valueOf(ps.getEliminations())));
                sender.sendMessage(plugin.getLang().get("stats-games", "games", String.valueOf(ps.getGamesPlayed())));
                sender.sendMessage(plugin.getLang().get("stats-winrate", "rate", String.valueOf(ps.getWinRate())));
                sender.sendMessage(ColorUtils.color("&8&m                            "));
            }

            // ── /squid wand ──────────────────────────────────────────
            case "wand" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
                if (!p.hasPermission("coresquid.admin")) { p.sendMessage(plugin.getLang().get("no-permission")); return true; }
                p.getInventory().addItem(ItemBuilder.of(Material.GOLDEN_AXE)
                        .name("&6Hacha de Selección CoreSquid")
                        .lore("&7Clic izq: &ePosición 1", "&7Clic der: &ePosición 2", "", "&8CoreSquid")
                        .build());
                p.sendMessage(plugin.getLang().get("wand-received"));
            }

            // ── /squid arena <create|delete|list|setspawn> ──────────
            case "arena" -> {
                if (!sender.hasPermission("coresquid.admin")) { sender.sendMessage(plugin.getLang().get("no-permission")); return true; }
                handleArena(sender, args);
            }

            // ── /squid setlobby ──────────────────────────────────────
            case "setlobby" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
                if (!p.hasPermission("coresquid.admin")) { p.sendMessage(plugin.getLang().get("no-permission")); return true; }
                plugin.getArenaManager().setLobbySpawn(p.getLocation());
                p.sendMessage(plugin.getLang().get("lobby-set"));
            }

            // ── /squid setspawn ──────────────────────────────────────
            case "setspawn" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
                if (!p.hasPermission("coresquid.admin")) { p.sendMessage(plugin.getLang().get("no-permission")); return true; }
                plugin.getArenaManager().setGameSpawn(p.getLocation());
                p.sendMessage(plugin.getLang().get("spawn-set"));
            }

            // ── /squid reload ────────────────────────────────────────
            case "reload", "recargar" -> {
                if (!sender.hasPermission("coresquid.admin")) { sender.sendMessage(plugin.getLang().get("no-permission")); return true; }
                plugin.reloadConfig();
                plugin.getLang().reload();
                plugin.getArenaManager().load();
                sender.sendMessage(plugin.getLang().get("reloaded"));
            }

            // ── In-game sub-commands ─────────────────────────────────

            case "bet", "apostar" -> {
                if (!(sender instanceof Player p)) return true;
                if (!(gm.getCurrentGame() instanceof Marbles m)) { p.sendMessage(ColorUtils.color("&cNo estás en el juego de Canicas.")); return true; }
                if (args.length < 3) { p.sendMessage(ColorUtils.color("&cUso: /squid bet <par|impar> <cantidad>")); return true; }
                boolean even = args[1].equalsIgnoreCase("par") || args[1].equalsIgnoreCase("even");
                try { m.processBet(p, even, Integer.parseInt(args[2])); } catch (NumberFormatException e) { p.sendMessage(ColorUtils.color("&cCantidad inválida.")); }
            }

            case "guess", "adivinar" -> {
                if (!(sender instanceof Player p)) return true;
                if (!(gm.getCurrentGame() instanceof GuessNumber gn)) { p.sendMessage(ColorUtils.color("&cNo estás en Adivina el Número.")); return true; }
                if (args.length < 2) { p.sendMessage(ColorUtils.color("&cUso: /squid guess <número>")); return true; }
                try { gn.processGuess(p, Integer.parseInt(args[1])); } catch (NumberFormatException e) { p.sendMessage(ColorUtils.color("&cNúmero inválido.")); }
            }

            case "drink", "beber" -> {
                if (!(sender instanceof Player p)) return true;
                if (!(gm.getCurrentGame() instanceof RussianRoulette rr)) { p.sendMessage(ColorUtils.color("&cNo estás en Ruleta Rusa.")); return true; }
                rr.drink(p);
            }

            case "pull", "jalar" -> {
                if (!(sender instanceof Player p)) return true;
                if (!(gm.getCurrentGame() instanceof TugOfWar tw)) { p.sendMessage(ColorUtils.color("&cNo estás en Tira y Jala.")); return true; }
                tw.pull(p);
            }

            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleArena(CommandSender sender, String[] args) {
        if (args.length < 2) { sendArenaHelp(sender); return; }

        switch (args[1].toLowerCase()) {

            case "create", "crear" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return; }
                if (args.length < 3) { p.sendMessage(ColorUtils.color("&cUso: /squid arena create <juego>")); return; }
                String gameId = args[2].toLowerCase();
                if (!plugin.getArenaManager().hasSelection(p.getUniqueId())) { p.sendMessage(plugin.getLang().get("arena-select-first")); return; }
                if (plugin.getArenaManager().hasArena(gameId)) { p.sendMessage(plugin.getLang().get("arena-exists", "game", gameId)); return; }
                ArenaData arena = plugin.getArenaManager().createArena(gameId, plugin.getArenaManager().getPos1(p.getUniqueId()), plugin.getArenaManager().getPos2(p.getUniqueId()));
                plugin.getArenaManager().clearSelection(p.getUniqueId());
                p.sendMessage(plugin.getLang().get("arena-created", "game", gameId));
            }

            case "delete", "eliminar" -> {
                if (args.length < 3) { sender.sendMessage(ColorUtils.color("&cUso: /squid arena delete <juego>")); return; }
                String gameId = args[2].toLowerCase();
                if (!plugin.getArenaManager().hasArena(gameId)) { sender.sendMessage(plugin.getLang().get("arena-not-found", "game", gameId)); return; }
                plugin.getArenaManager().deleteArena(gameId);
                sender.sendMessage(plugin.getLang().get("arena-deleted"));
            }

            case "list", "lista" -> {
                Map<String, ArenaData> arenas = plugin.getArenaManager().getAll();
                if (arenas.isEmpty()) { sender.sendMessage(plugin.getLang().get("arena-list-empty")); return; }
                sender.sendMessage(plugin.getLang().get("arena-list", "list", String.join(", ", arenas.keySet())));
            }

            case "setspawn" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return; }
                if (args.length < 3) { p.sendMessage(ColorUtils.color("&cUso: /squid arena setspawn <juego>")); return; }
                String gameId = args[2].toLowerCase();
                ArenaData arena = plugin.getArenaManager().getArena(gameId);
                if (arena == null) { p.sendMessage(plugin.getLang().get("arena-not-found", "game", gameId)); return; }
                arena.setSpawn(p.getLocation());
                plugin.getArenaManager().save();
                p.sendMessage(plugin.getLang().get("spawn-set"));
            }

            default -> sendArenaHelp(sender);
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtils.color("&8&m                                  "));
        sender.sendMessage(ColorUtils.color("  &c&lCoreSquid &7- Comandos"));
        sender.sendMessage(ColorUtils.color("&8&m                                  "));
        sender.sendMessage(ColorUtils.color("  &e/squid join &7- Unirse al lobby"));
        sender.sendMessage(ColorUtils.color("  &e/squid leave &7- Salir del lobby"));
        sender.sendMessage(ColorUtils.color("  &e/squid stats &7[jugador] - Estadísticas"));
        if (sender.hasPermission("coresquid.admin")) {
            sender.sendMessage(ColorUtils.color("  &6/squid start &7- Forzar inicio"));
            sender.sendMessage(ColorUtils.color("  &6/squid stop &7- Detener partida"));
            sender.sendMessage(ColorUtils.color("  &6/squid wand &7- Hacha de selección"));
            sender.sendMessage(ColorUtils.color("  &6/squid arena &7<create|delete|list|setspawn>"));
            sender.sendMessage(ColorUtils.color("  &6/squid setlobby &7- Configurar spawn del lobby"));
            sender.sendMessage(ColorUtils.color("  &6/squid reload &7- Recargar configuración"));
        }
        sender.sendMessage(ColorUtils.color("&8&m                                  "));
        sender.sendMessage(ColorUtils.color("  &7Comandos en partida:"));
        sender.sendMessage(ColorUtils.color("  &e/squid bet <par|impar> <n> &7- Canicas"));
        sender.sendMessage(ColorUtils.color("  &e/squid guess <n> &7- Adivina el número"));
        sender.sendMessage(ColorUtils.color("  &e/squid drink &7- Ruleta rusa"));
        sender.sendMessage(ColorUtils.color("  &e/squid pull &7- Tira y jala"));
        sender.sendMessage(ColorUtils.color("&8&m                                  "));
    }

    private void sendArenaHelp(CommandSender sender) {
        sender.sendMessage(ColorUtils.color("&6Arenas:"));
        sender.sendMessage(ColorUtils.color("  &e/squid wand &7- Obtener hacha"));
        sender.sendMessage(ColorUtils.color("  &e/squid arena create &6<juego> &7- Crear arena"));
        sender.sendMessage(ColorUtils.color("  &e/squid arena delete &6<juego> &7- Eliminar arena"));
        sender.sendMessage(ColorUtils.color("  &e/squid arena setspawn &6<juego> &7- Spawn del juego"));
        sender.sendMessage(ColorUtils.color("  &e/squid arena list &7- Listar arenas"));
        sender.sendMessage(ColorUtils.color("  &7Juegos: red-light-green-light, glass-bridge, marbles, tug-of-war, dalgona, musical-chairs, maze, hot-potato, memory-blocks, obstacle-course, falling-platforms, guess-number, hide-seek, king-hill, russian-roulette"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> opts = new ArrayList<>(Arrays.asList("join", "leave", "stats", "bet", "guess", "drink", "pull"));
            if (sender.hasPermission("coresquid.admin")) opts.addAll(Arrays.asList("start", "stop", "wand", "arena", "setlobby", "setspawn", "reload"));
            return filter(opts, args[0]);
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("stats")) {
                List<String> players = new ArrayList<>(); Bukkit.getOnlinePlayers().forEach(p -> players.add(p.getName())); return filter(players, args[1]);
            }
            if (args[0].equalsIgnoreCase("arena")) return filter(Arrays.asList("create", "delete", "list", "setspawn"), args[1]);
            if (args[0].equalsIgnoreCase("bet")) return filter(Arrays.asList("par", "impar"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("arena")) {
            return filter(Arrays.asList("red-light-green-light","glass-bridge","marbles","tug-of-war","dalgona","musical-chairs","maze","hot-potato","memory-blocks","obstacle-course","falling-platforms","guess-number","hide-seek","king-hill","russian-roulette"), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> list, String prefix) {
        List<String> out = new ArrayList<>();
        for (String s : list) if (s.toLowerCase().startsWith(prefix.toLowerCase())) out.add(s);
        return out;
    }
}
