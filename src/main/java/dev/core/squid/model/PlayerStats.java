package dev.core.squid.model;

import org.bukkit.Location;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PlayerStats {

    private final UUID uuid;
    private final String name;

    // Persistent stats
    private int wins;
    private int eliminations;
    private int gamesPlayed;

    // Session state
    private boolean eliminated;
    private boolean spectator;
    private Location savedLocation;
    private ItemStack[] savedInventory;
    private ItemStack[] savedArmor;
    private GameMode savedGameMode;
    private int savedLevel;
    private float savedExp;
    private double savedHealth;
    private int savedFood;

    public PlayerStats(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    // ── Persistent ────────────────────────────────────────────────
    public UUID getUuid() { return uuid; }
    public String getName() { return name; }

    public int getWins() { return wins; }
    public void addWin() { wins++; }
    public void setWins(int w) { wins = w; }

    public int getEliminations() { return eliminations; }
    public void addElimination() { eliminations++; }
    public void setEliminations(int e) { eliminations = e; }

    public int getGamesPlayed() { return gamesPlayed; }
    public void addGame() { gamesPlayed++; }
    public void setGamesPlayed(int g) { gamesPlayed = g; }

    public double getWinRate() {
        if (gamesPlayed == 0) return 0;
        return Math.round((wins / (double) gamesPlayed) * 100.0 * 10) / 10.0;
    }

    // ── Session ───────────────────────────────────────────────────
    public boolean isEliminated() { return eliminated; }
    public void setEliminated(boolean e) { eliminated = e; }

    public boolean isSpectator() { return spectator; }
    public void setSpectator(boolean s) { spectator = s; }

    public Location getSavedLocation() { return savedLocation; }
    public void setSavedLocation(Location l) { savedLocation = l; }

    public ItemStack[] getSavedInventory() { return savedInventory; }
    public void setSavedInventory(ItemStack[] i) { savedInventory = i; }

    public ItemStack[] getSavedArmor() { return savedArmor; }
    public void setSavedArmor(ItemStack[] a) { savedArmor = a; }

    public GameMode getSavedGameMode() { return savedGameMode; }
    public void setSavedGameMode(GameMode g) { savedGameMode = g; }

    public int getSavedLevel() { return savedLevel; }
    public void setSavedLevel(int l) { savedLevel = l; }

    public float getSavedExp() { return savedExp; }
    public void setSavedExp(float e) { savedExp = e; }

    public double getSavedHealth() { return savedHealth; }
    public void setSavedHealth(double h) { savedHealth = h; }

    public int getSavedFood() { return savedFood; }
    public void setSavedFood(int f) { savedFood = f; }

    public void resetSession() {
        eliminated = false;
        spectator = false;
    }
}
