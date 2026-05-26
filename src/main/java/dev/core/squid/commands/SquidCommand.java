package dev.core.squid.commands;

import dev.core.squid.CoreSquid;
import dev.core.squid.managers.GameManager;
import dev.core.squid.model.PlayerData;
import dev.core.squid.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SquidCommand implements CommandExecutor, TabCompleter {

    private final CoreSquid plugin;
    private final GameManager gm;

    public SquidCommand(CoreSquid plugin) {
        this.plugin = plugin;
        this.gm = plugin.getGameManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            enviarAyuda(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            // ── /squid unirse ───────────────────────────────────────
            case "unirse" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Solo jugadores."); return true; }
                if (!p.hasPermission("coresquid.use")) {
                    p.sendMessage(ColorUtils.color(plugin.getPrefijo() +
                            plugin.getConfig().getString("mensajes.sin-permiso", "&cSin permiso.")));
                    return true;
                }
                if (gm.estaEnLobby(p) || gm.estaEnPartida(p)) {
                    p.sendMessage(ColorUtils.color(plugin.getPrefijo() +
                            plugin.getConfig().getString("mensajes.ya-en-partida", "&cYa estas en una partida.")));
                    return true;
                }
                boolean unido = gm.unirseAlLobby(p);
                if (!unido) {
                    p.sendMessage(ColorUtils.color(plugin.getPrefijo() +
                            plugin.getConfig().getString("mensajes.lobby-lleno", "&cEl lobby esta lleno.")));
                }
            }

            // ── /squid salir ────────────────────────────────────────
            case "salir" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Solo jugadores."); return true; }
                if (!gm.estaEnLobby(p)) {
                    p.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&cNo estas en el lobby."));
                    return true;
                }
                gm.salirDelLobby(p);
            }

            // ── /squid iniciar (admin) ──────────────────────────────
            case "iniciar" -> {
                if (!sender.hasPermission("coresquid.admin")) {
                    sender.sendMessage(ColorUtils.color(plugin.getPrefijo() +
                            plugin.getConfig().getString("mensajes.sin-permiso", "&cSin permiso.")));
                    return true;
                }
                gm.forzarInicio();
                sender.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&aPartida iniciada forzosamente."));
            }

            // ── /squid detener (admin) ──────────────────────────────
            case "detener" -> {
                if (!sender.hasPermission("coresquid.admin")) {
                    sender.sendMessage(ColorUtils.color(plugin.getPrefijo() +
                            plugin.getConfig().getString("mensajes.sin-permiso", "&cSin permiso.")));
                    return true;
                }
                gm.forzarDetener();
                sender.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&aPartida detenida."));
            }

            // ── /squid stats [jugador] ──────────────────────────────
            case "stats" -> {
                if (!sender.hasPermission("coresquid.stats")) {
                    sender.sendMessage(ColorUtils.color(plugin.getPrefijo() +
                            plugin.getConfig().getString("mensajes.sin-permiso", "&cSin permiso.")));
                    return true;
                }

                Player objetivo = sender instanceof Player p ? p : null;
                if (args.length >= 2) {
                    objetivo = Bukkit.getPlayer(args[1]);
                    if (objetivo == null) {
                        sender.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&cJugador no encontrado."));
                        return true;
                    }
                }

                if (objetivo == null) { sender.sendMessage("Especifica un jugador."); return true; }

                PlayerData pd = plugin.getStatsManager().getData(objetivo.getUniqueId(), objetivo.getName());
                sender.sendMessage(ColorUtils.color("&8&m                            "));
                sender.sendMessage(ColorUtils.color(plugin.getPrefijo() +
                        plugin.getConfig().getString("mensajes.stats-titulo", "&6&lEstadisticas") + " &7- &e" + objetivo.getName()));
                sender.sendMessage(ColorUtils.color(plugin.getConfig().getString("mensajes.stats-victorias", "&7Victorias: &e{victorias}")
                        .replace("{victorias}", String.valueOf(pd.getVictorias()))));
                sender.sendMessage(ColorUtils.color(plugin.getConfig().getString("mensajes.stats-muertes", "&7Eliminaciones: &e{muertes}")
                        .replace("{muertes}", String.valueOf(pd.getEliminaciones()))));
                sender.sendMessage(ColorUtils.color(plugin.getConfig().getString("mensajes.stats-partidas", "&7Partidas: &e{partidas}")
                        .replace("{partidas}", String.valueOf(pd.getPartidas()))));
                sender.sendMessage(ColorUtils.color("&8&m                            "));
            }

            // ── /squid apostar <par|impar> <cantidad> ──────────────
            case "apostar" -> {
                if (!(sender instanceof Player p)) return true;
                if (args.length < 3) {
                    p.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&cUso: /squid apostar <par|impar> <cantidad>"));
                    return true;
                }
                if (!(gm.getJuegoActual() instanceof dev.core.squid.games.Canicas canicas)) {
                    p.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&cNo estas en el juego de Canicas."));
                    return true;
                }
                boolean esPar = args[1].equalsIgnoreCase("par");
                try {
                    int cantidad = Integer.parseInt(args[2]);
                    canicas.procesarApuesta(p, esPar, cantidad);
                } catch (NumberFormatException e) {
                    p.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&cCantidad invalida."));
                }
            }

            // ── /squid adivinar <numero> ────────────────────────────
            case "adivinar" -> {
                if (!(sender instanceof Player p)) return true;
                if (args.length < 2) {
                    p.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&cUso: /squid adivinar <numero>"));
                    return true;
                }
                if (!(gm.getJuegoActual() instanceof dev.core.squid.games.AdivinaNumero adivina)) {
                    p.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&cNo estas en el juego Adivina el Numero."));
                    return true;
                }
                try {
                    int numero = Integer.parseInt(args[1]);
                    adivina.procesarIntento(p, numero);
                } catch (NumberFormatException e) {
                    p.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&cNumero invalido."));
                }
            }

            // ── /squid beber ────────────────────────────────────────
            case "beber" -> {
                if (!(sender instanceof Player p)) return true;
                if (!(gm.getJuegoActual() instanceof dev.core.squid.games.RuletaRusa ruleta)) {
                    p.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&cNo estas en Ruleta Rusa."));
                    return true;
                }
                ruleta.beber(p);
            }

            // ── /squid recargar ─────────────────────────────────────
            case "recargar" -> {
                if (!sender.hasPermission("coresquid.admin")) {
                    sender.sendMessage(ColorUtils.color(plugin.getPrefijo() +
                            plugin.getConfig().getString("mensajes.sin-permiso", "&cSin permiso.")));
                    return true;
                }
                plugin.reloadConfig();
                sender.sendMessage(ColorUtils.color(plugin.getPrefijo() + "&aConfiguración recargada."));
            }

            default -> enviarAyuda(sender);
        }

        return true;
    }

    private void enviarAyuda(CommandSender sender) {
        sender.sendMessage(ColorUtils.color("&8&m                              "));
        sender.sendMessage(ColorUtils.color("  &c&lCoreSquid &7- Ayuda"));
        sender.sendMessage(ColorUtils.color("&8&m                              "));
        sender.sendMessage(ColorUtils.color("  &e/squid unirse &7- Unirse al lobby"));
        sender.sendMessage(ColorUtils.color("  &e/squid salir &7- Salir del lobby"));
        sender.sendMessage(ColorUtils.color("  &e/squid stats &7[jugador] - Ver estadisticas"));
        if (sender.hasPermission("coresquid.admin")) {
            sender.sendMessage(ColorUtils.color("  &e/squid iniciar &7- Forzar inicio"));
            sender.sendMessage(ColorUtils.color("  &e/squid detener &7- Detener partida"));
            sender.sendMessage(ColorUtils.color("  &e/squid recargar &7- Recargar config"));
        }
        sender.sendMessage(ColorUtils.color("&8&m                              "));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> opciones = new ArrayList<>(List.of("unirse", "salir", "stats", "apostar", "adivinar", "beber"));
            if (sender.hasPermission("coresquid.admin")) opciones.addAll(List.of("iniciar", "detener", "recargar"));
            return filtrar(opciones, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("stats")) {
            List<String> jugadores = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> jugadores.add(p.getName()));
            return filtrar(jugadores, args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("apostar")) {
            return filtrar(List.of("par", "impar"), args[1]);
        }
        return List.of();
    }

    private List<String> filtrar(List<String> lista, String prefijo) {
        List<String> r = new ArrayList<>();
        for (String s : lista) if (s.toLowerCase().startsWith(prefijo.toLowerCase())) r.add(s);
        return r;
    }
  }
            
