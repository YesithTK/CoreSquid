package dev.core.squid.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    private ItemBuilder(Material mat) {
        this.item = new ItemStack(mat == null ? Material.PAPER : mat);
        this.meta = item.getItemMeta();
    }

    public static ItemBuilder of(Material mat) { return new ItemBuilder(mat); }

    public ItemBuilder nombre(String nombre) {
        if (meta != null) meta.setDisplayName(ColorUtils.color(nombre));
        return this;
    }

    public ItemBuilder lore(String... lineas) {
        if (meta == null) return this;
        List<String> lore = new ArrayList<>();
        for (String l : lineas) lore.add(ColorUtils.color(l));
        meta.setLore(lore);
        return this;
    }

    public ItemBuilder cantidad(int cantidad) {
        item.setAmount(Math.max(1, Math.min(64, cantidad)));
        return this;
    }

    public ItemStack construir() {
        if (meta != null) item.setItemMeta(meta);
        return item;
    }
}
