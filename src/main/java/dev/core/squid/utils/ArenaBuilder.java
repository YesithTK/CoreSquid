package dev.core.squid.utils;

import org.bukkit.Location;
import org.bukkit.Material;

public class ArenaBuilder {

    public static void clear(Location origin, int dx, int dy, int dz) {
        if (origin == null || origin.getWorld() == null) return;
        for (int x = -3; x <= dx + 3; x++)
            for (int y = -3; y <= dy + 3; y++)
                for (int z = -dz - 3; z <= dz + 3; z++) {
                    Location loc = origin.clone().add(x, y - 1, z);
                    if (loc.getBlock().getType() != Material.AIR)
                        loc.getBlock().setType(Material.AIR, false);
                }
    }

    public static void fill(Location origin, int dx, int dy, int dz, Material mat) {
        for (int x = 0; x <= dx; x++)
            for (int y = 0; y <= dy; y++)
                for (int z = 0; z <= dz; z++)
                    origin.clone().add(x, y, z).getBlock().setType(mat, false);
    }

    public static void box(Location origin, int dx, int dy, int dz, Material mat) {
        for (int x = 0; x <= dx; x++)
            for (int y = 0; y <= dy; y++)
                for (int z = 0; z <= dz; z++)
                    if (x==0||x==dx||y==0||y==dy||z==0||z==dz)
                        origin.clone().add(x, y, z).getBlock().setType(mat, false);
    }

    public static void floor(Location origin, int dx, int dz, Material mat) {
        for (int x = 0; x <= dx; x++)
            for (int z = 0; z <= dz; z++)
                origin.clone().add(x, 0, z).getBlock().setType(mat, false);
    }
}
