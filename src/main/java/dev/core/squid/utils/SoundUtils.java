package dev.core.squid.utils;

import dev.core.squid.CoreSquid;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtils {

    /**
     * Reproduce un sonido personalizado del resource pack.
     * Si no existe, usa un sonido vanilla como fallback.
     */
    public static void play(Player player, String soundKey) {
        if (!CoreSquid.getInstancia().getConfig().getBoolean("sonidos-activados", true)) return;
        try {
            // Intentar reproducir sonido personalizado del resource pack
            player.playSound(player.getLocation(), soundKey, 1.0f, 1.0f);
        } catch (Exception e) {
            // Fallback a sonido vanilla
            playFallback(player, soundKey);
        }
    }

    public static void playFallback(Player player, String key) {
        Sound sound = switch (key) {
            case "coresquid.luz_verde"        -> Sound.BLOCK_NOTE_BLOCK_PLING;
            case "coresquid.luz_roja"         -> Sound.BLOCK_NOTE_BLOCK_BASS;
            case "coresquid.cristal_correcto" -> Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
            case "coresquid.cristal_roto"     -> Sound.BLOCK_GLASS_BREAK;
            case "coresquid.bomba_activa"     -> Sound.ENTITY_TNT_PRIMED;
            case "coresquid.musica_sillas"    -> Sound.BLOCK_NOTE_BLOCK_HARP;
            case "coresquid.musica_parada"    -> Sound.BLOCK_NOTE_BLOCK_SNARE;
            case "coresquid.plataforma_cae"   -> Sound.BLOCK_STONE_BREAK;
            case "coresquid.memoria_bloque"   -> Sound.BLOCK_NOTE_BLOCK_BIT;
            case "coresquid.memoria_clic"     -> Sound.UI_BUTTON_CLICK;
            case "coresquid.tira_cuerda"      -> Sound.ENTITY_FISHING_BOBBER_THROW;
            case "coresquid.dalgona_rompe"    -> Sound.BLOCK_HONEY_BLOCK_BREAK;
            case "coresquid.ruleta_mala"      -> Sound.ENTITY_GENERIC_EXPLODE;
            case "coresquid.ruleta_buena"     -> Sound.ENTITY_PLAYER_LEVELUP;
            case "coresquid.eliminado"        -> Sound.ENTITY_PLAYER_DEATH;
            case "coresquid.ganador"          -> Sound.UI_TOAST_CHALLENGE_COMPLETE;
            case "coresquid.cuenta_regresiva" -> Sound.BLOCK_NOTE_BLOCK_HAT;
            case "coresquid.inicio_juego"     -> Sound.ENTITY_ENDER_DRAGON_GROWL;
            default                           -> Sound.BLOCK_NOTE_BLOCK_PLING;
        };
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }

    public static void playToAll(Iterable<Player> players, String soundKey) {
        for (Player p : players) play(p, soundKey);
    }
}
