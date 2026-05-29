package dev.core.squid.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class ItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;

    private ItemBuilder(Material mat) {
        item = new ItemStack(mat == null ? Material.PAPER : mat);
        meta = item.getItemMeta();
    }

    public static ItemBuilder of(Material mat) { return new ItemBuilder(mat); }

    public ItemBuilder name(String name) {
        if (meta != null) meta.setDisplayName(ColorUtils.color(name));
        return this;
    }

    public ItemBuilder lore(String... lines) {
        if (meta == null) return this;
        List<String> lore = new ArrayList<>();
        for (String l : lines) lore.add(ColorUtils.color(l));
        meta.setLore(lore);
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    public ItemStack build() {
        if (meta != null) item.setItemMeta(meta);
        return item;
    }
}
