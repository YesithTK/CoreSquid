package dev.core.squid.managers;

import dev.core.squid.CoreSquid;
import dev.core.squid.utils.ColorUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.Map;

public class LangManager {

    private final CoreSquid plugin;
    private FileConfiguration lang;
    private String currentLang;

    public LangManager(CoreSquid plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        currentLang = plugin.getConfig().getString("language", "es");
        loadLang(currentLang);
    }

    private void loadLang(String code) {
        File langFile = new File(plugin.getDataFolder() + "/lang", code + ".yml");
        if (!langFile.exists()) {
            plugin.saveResource("lang/" + code + ".yml", false);
        }
        if (!langFile.exists()) {
            plugin.getLogger().warning("Lang file not found: " + code + ".yml, falling back to es.yml");
            langFile = new File(plugin.getDataFolder() + "/lang", "es.yml");
            plugin.saveResource("lang/es.yml", false);
        }
        lang = YamlConfiguration.loadConfiguration(langFile);
    }

    /**
     * Gets a message from the lang file with replacements applied.
     * Replacements are pairs: key, value, key, value...
     */
    public String get(String key, String... replacements) {
        String prefix = lang.getString("prefix", "&8[&cCS&8] &r");
        String msg = lang.getString(key, "&c[Missing: " + key + "]");
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return ColorUtils.color(prefix + msg);
    }

    /**
     * Gets a message without the prefix.
     */
    public String getRaw(String key, String... replacements) {
        String msg = lang.getString(key, "&c[Missing: " + key + "]");
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return ColorUtils.color(msg);
    }

    public String getLang() { return currentLang; }
}
