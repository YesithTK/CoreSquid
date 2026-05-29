package dev.core.squid.utils;

import dev.core.squid.CoreSquid;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtils {

    public static void play(Player player, String key) {
        if (!CoreSquid.getInstance().getConfig().getBoolean("sounds.enabled", true)) return;
        boolean custom = CoreSquid.getInstance().getConfig().getBoolean("sounds.use-custom-sounds", false);
        if (custom) {
            try { player.playSound(player.getLocation(), "coresquid." + key, 1f, 1f); return; }
            catch (Exception ignored) {}
        }
        Sound fallback = getFallback(key);
        if (fallback != null) player.playSound(player.getLocation(), fallback, 1f, 1f);
    }

    public static void playAll(Iterable<Player> players, String key) {
        for (Player p : players) play(p, key);
    }

    private static Sound getFallback(String key) {
        return switch (key) {
            case "green_light"       -> Sound.BLOCK_NOTE_BLOCK_PLING;
            case "red_light"         -> Sound.BLOCK_NOTE_BLOCK_BASS;
            case "glass_correct"     -> Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
            case "glass_break"       -> Sound.BLOCK_GLASS_BREAK;
            case "bomb_tick"         -> Sound.BLOCK_NOTE_BLOCK_HAT;
            case "bomb_explode"      -> Sound.ENTITY_GENERIC_EXPLODE;
            case "music_play"        -> Sound.BLOCK_NOTE_BLOCK_HARP;
            case "music_stop"        -> Sound.BLOCK_NOTE_BLOCK_SNARE;
            case "platform_fall"     -> Sound.BLOCK_STONE_BREAK;
            case "memory_show"       -> Sound.BLOCK_NOTE_BLOCK_BIT;
            case "memory_click"      -> Sound.UI_BUTTON_CLICK;
            case "tug_pull"          -> Sound.ENTITY_FISHING_BOBBER_THROW;
            case "dalgona_break"     -> Sound.BLOCK_HONEY_BLOCK_BREAK;
            case "roulette_bad"      -> Sound.ENTITY_GENERIC_EXPLODE;
            case "roulette_good"     -> Sound.ENTITY_PLAYER_LEVELUP;
            case "eliminated"        -> Sound.ENTITY_PLAYER_DEATH;
            case "winner"            -> Sound.UI_TOAST_CHALLENGE_COMPLETE;
            case "countdown"         -> Sound.BLOCK_NOTE_BLOCK_HAT;
            case "game_start"        -> Sound.ENTITY_ENDER_DRAGON_GROWL;
            case "join_lobby"        -> Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
            default                  -> Sound.BLOCK_NOTE_BLOCK_PLING;
        };
    }
}
