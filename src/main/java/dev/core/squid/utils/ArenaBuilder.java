package dev.core.squid.utils;

import org.bukkit.Location;
import org.bukkit.Material;

public class ArenaBuilder {

    /**
     * Limpia un area de bloques reemplazandolos con aire.
     */
    public static void limpiar(Location origen, int dx, int dy, int dz) {
        if (origen == null || origen.getWorld() == null) return;
        for (int x = -2; x <= dx + 2; x++) {
            for (int y = -2; y <= dy + 2; y++) {
                for (int z = -dz - 2; z <= dz + 2; z++) {
                    Location loc = origen.clone().add(x, y - 1, z);
                    if (loc.getBlock().getType() != Material.AIR) {
                        loc.getBlock().setType(Material.AIR);
                    }
                }
            }
        }
    }

    /**
     * Rellena un area con un material.
     */
    public static void rellenar(Location origen, int dx, int dy, int dz, Material material) {
        for (int x = 0; x <= dx; x++)
            for (int y = 0; y <= dy; y++)
                for (int z = 0; z <= dz; z++)
                    origen.clone().add(x, y, z).getBlock().setType(material);
    }

    /**
     * Construye las paredes de una caja hueca.
     */
    public static void caja(Location origen, int dx, int dy, int dz, Material material) {
        for (int x = 0; x <= dx; x++) {
            for (int y = 0; y <= dy; y++) {
                for (int z = 0; z <= dz; z++) {
                    if (x == 0 || x == dx || y == 0 || y == dy || z == 0 || z == dz) {
                        origen.clone().add(x, y, z).getBlock().setType(material);
                    }
                }
            }
        }
    }
}
