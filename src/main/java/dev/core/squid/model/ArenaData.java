package dev.core.squid.model;

import org.bukkit.Location;

public class ArenaData {

    private final String gameId;
    private final Location pos1;
    private final Location pos2;
    private Location spawn;

    public ArenaData(String gameId, Location pos1, Location pos2) {
        this.gameId = gameId;
        this.pos1 = pos1.clone();
        this.pos2 = pos2.clone();
        // Default spawn at center
        this.spawn = getCenter();
    }

    public String getGameId() { return gameId; }
    public Location getPos1() { return pos1.clone(); }
    public Location getPos2() { return pos2.clone(); }

    public Location getSpawn() { return spawn != null ? spawn.clone() : getCenter(); }
    public void setSpawn(Location s) { spawn = s.clone(); }

    public Location getCenter() {
        return new Location(
            pos1.getWorld(),
            (pos1.getX() + pos2.getX()) / 2,
            Math.min(pos1.getY(), pos2.getY()) + 1,
            (pos1.getZ() + pos2.getZ()) / 2
        );
    }

    public int getMinX() { return Math.min(pos1.getBlockX(), pos2.getBlockX()); }
    public int getMinY() { return Math.min(pos1.getBlockY(), pos2.getBlockY()); }
    public int getMinZ() { return Math.min(pos1.getBlockZ(), pos2.getBlockZ()); }
    public int getMaxX() { return Math.max(pos1.getBlockX(), pos2.getBlockX()); }
    public int getMaxY() { return Math.max(pos1.getBlockY(), pos2.getBlockY()); }
    public int getMaxZ() { return Math.max(pos1.getBlockZ(), pos2.getBlockZ()); }

    public boolean contains(Location loc) {
        if (!loc.getWorld().equals(pos1.getWorld())) return false;
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        return x >= getMinX() && x <= getMaxX()
            && y >= getMinY() && y <= getMaxY()
            && z >= getMinZ() && z <= getMaxZ();
    }
}
