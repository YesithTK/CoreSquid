package dev.core.squid.listeners;

import dev.core.squid.CoreSquid;
import dev.core.squid.games.*;
import dev.core.squid.managers.GameManager;
import dev.core.squid.model.GameState;
import dev.core.squid.utils.ColorUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GameListener implements Listener {

    private final CoreSquid plugin;
    private final GameManager gm;

    public GameListener(CoreSquid plugin) {
        this.plugin = plugin;
        this.gm = plugin.getGameManager();
    }

    // ── Desconexion ─────────────────────────────────────────────────
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (gm.estaEnLobby(p)) gm.salirDelLobby(p);
        if (gm.estaEnPartida(p)) gm.eliminarJugador(p);
        if (gm.getJuegoActual() != null) gm.getJuegoActual().onPlayerQuit(p);
    }

    // ── Daño en partida ─────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (!gm.estaEnPartida(p)) return;

        // Solo permitir daño en ciertos juegos
        if (gm.getEstado() == GameState.EN_JUEGO) {
            // Dejar que los juegos manejen el daño
            if (gm.getJuegoActual() instanceof Escondite) return;
            // En la mayoria de juegos no hay daño PvP
            event.setDamage(0);
        } else {
            event.setCancelled(true);
        }
    }

    // ── PvP - pasar bomba / golpear en escondite ────────────────────
    @EventHandler
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player atacante)) return;
        if (!(event.getEntity() instanceof Player victima)) return;
        if (!gm.estaEnPartida(atacante)) return;

        MiniGame juego = gm.getJuegoActual();
        if (juego instanceof BombaCaliente bc) {
            event.setCancelled(true);
            bc.pasarBomba(atacante, victima);
        } else if (juego instanceof Escondite esc) {
            esc.onGolpear(atacante, victima);
            event.setCancelled(true);
        } else {
            event.setCancelled(true);
        }
    }

    // ── Hambre ──────────────────────────────────────────────────────
    @EventHandler
    public void onHambre(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (gm.estaEnPartida(p) || gm.estaEnLobby(p)) {
            event.setCancelled(true);
        }
    }

    // ── Drop de items ────────────────────────────────────────────────
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player p = event.getPlayer();
        if (gm.estaEnPartida(p) || gm.estaEnLobby(p)) {
            event.setCancelled(true);
        }
    }

    // ── Romper bloques - Dalgona ─────────────────────────────────────
    @EventHandler
    public void onBreakBlock(BlockBreakEvent event) {
        Player p = event.getPlayer();
        if (!gm.estaEnPartida(p)) return;

        MiniGame juego = gm.getJuegoActual();
        if (juego instanceof Dalgona dalgona) {
            event.setCancelled(true);
            dalgona.onRomperBloque(p, event.getBlock().getLocation());
        } else {
            event.setCancelled(true);
        }
    }

    // ── Movimiento - Luz Roja Luz Verde ─────────────────────────────
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        // Solo verificar posicion, no cancelar (LuzRojaLuzVerde lo maneja internamente)
    }

    // ── Interaccion con entidad - Tira y Jala ────────────────────────
    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player p = event.getPlayer();
        if (!gm.estaEnPartida(p)) return;

        MiniGame juego = gm.getJuegoActual();
        if (juego instanceof TiraJala tj) {
            event.setCancelled(true);
            tj.jalar(p);
        }
    }
}
