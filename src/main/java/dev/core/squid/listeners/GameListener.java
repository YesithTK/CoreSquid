package dev.core.squid.listeners;

import dev.core.squid.CoreSquid;
import dev.core.squid.games.*;
import dev.core.squid.managers.GameManager;
import dev.core.squid.model.GameState;
import dev.core.squid.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;

public class GameListener implements Listener {

    private final CoreSquid plugin;
    private final GameManager gm;

    public GameListener(CoreSquid plugin) {
        this.plugin = plugin;
        this.gm = plugin.getGameManager();
    }

    // ── Wand: left click = pos1, right click = pos2 ──────────────
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() != Material.GOLDEN_AXE) return;
        if (!player.hasPermission("coresquid.admin")) return;

        if (event.getAction().name().contains("LEFT_CLICK")) {
            if (event.getClickedBlock() == null) return;
            event.setCancelled(true);
            plugin.getArenaManager().setPos1(player.getUniqueId(), event.getClickedBlock().getLocation());
            player.sendMessage(plugin.getLang().get("wand-pos1",
                    "x", String.valueOf(event.getClickedBlock().getX()),
                    "y", String.valueOf(event.getClickedBlock().getY()),
                    "z", String.valueOf(event.getClickedBlock().getZ())));
        } else if (event.getAction().name().contains("RIGHT_CLICK")) {
            if (event.getClickedBlock() == null) return;
            event.setCancelled(true);
            plugin.getArenaManager().setPos2(player.getUniqueId(), event.getClickedBlock().getLocation());
            player.sendMessage(plugin.getLang().get("wand-pos2",
                    "x", String.valueOf(event.getClickedBlock().getX()),
                    "y", String.valueOf(event.getClickedBlock().getY()),
                    "z", String.valueOf(event.getClickedBlock().getZ())));
        }
    }

    // ── Player quit ───────────────────────────────────────────────
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (gm.isInLobby(p)) gm.leaveLobby(p);
        else if (gm.isInGame(p)) {
            gm.eliminatePlayer(p);
            if (gm.getCurrentGame() != null) gm.getCurrentGame().onQuit(p);
            gm.restorePlayer(p);
        }
    }

    // ── No hunger in game/lobby ───────────────────────────────────
    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (gm.isInGame(p) || gm.isInLobby(p)) event.setCancelled(true);
    }

    // ── No item drop in game/lobby ────────────────────────────────
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player p = event.getPlayer();
        if (gm.isInGame(p) || gm.isInLobby(p)) event.setCancelled(true);
    }

    // ── Damage handling ───────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (gm.isInLobby(p)) { event.setCancelled(true); return; }
        if (!gm.isInGame(p)) return;
        // Most games don't use damage - cancel by default unless specific game
        MiniGame game = gm.getCurrentGame();
        if (game instanceof HideSeek || game instanceof TugOfWar || game instanceof KingHill) return;
        event.setDamage(0);
    }

    // ── PvP: pass bomb / hit in hide seek ─────────────────────────
    @EventHandler
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!gm.isInGame(attacker)) return;

        MiniGame game = gm.getCurrentGame();
        event.setCancelled(true);

        if (game instanceof HotPotato hp) {
            hp.passBomb(attacker, victim);
        } else if (game instanceof HideSeek hs) {
            hs.onHit(attacker, victim);
            event.setCancelled(true);
        } else if (game instanceof TugOfWar tw) {
            tw.pull(attacker);
        }
    }

    // ── Block break: Dalgona ─────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        if (!gm.isInGame(p)) return;
        MiniGame game = gm.getCurrentGame();
        if (game instanceof Dalgona dalgona) {
            event.setCancelled(true);
            dalgona.onBreakBlock(p, event.getBlock().getLocation());
        } else {
            event.setCancelled(true);
        }
    }

    // ── Block place: cancel in most games ─────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        Player p = event.getPlayer();
        if (gm.isInGame(p) || gm.isInLobby(p)) event.setCancelled(true);
    }

    // ── Resource pack ─────────────────────────────────────────────
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if (plugin.getConfig().getBoolean("resourcepack.enabled", false)) {
            String url = plugin.getConfig().getString("resourcepack.url", "");
            if (!url.isEmpty()) p.setResourcePack(url);
        }
    }
}
