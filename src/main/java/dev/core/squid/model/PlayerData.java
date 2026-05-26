package dev.core.squid.model;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private final String nombre;

    // Estadisticas
    private int victorias;
    private int eliminaciones;
    private int partidas;

    // Estado en partida
    private boolean eliminado;
    private boolean espectador;
    private Location locationAntes;
    private ItemStack[] inventarioAntes;
    private ItemStack[] armorAntes;

    public PlayerData(UUID uuid, String nombre) {
        this.uuid = uuid;
        this.nombre = nombre;
        this.victorias = 0;
        this.eliminaciones = 0;
        this.partidas = 0;
        this.eliminado = false;
        this.espectador = false;
    }

    public UUID getUuid() { return uuid; }
    public String getNombre() { return nombre; }

    public int getVictorias() { return victorias; }
    public void addVictoria() { victorias++; }
    public void setVictorias(int v) { victorias = v; }

    public int getEliminaciones() { return eliminaciones; }
    public void addEliminacion() { eliminaciones++; }
    public void setEliminaciones(int e) { eliminaciones = e; }

    public int getPartidas() { return partidas; }
    public void addPartida() { partidas++; }
    public void setPartidas(int p) { partidas = p; }

    public boolean isEliminado() { return eliminado; }
    public void setEliminado(boolean eliminado) { this.eliminado = eliminado; }

    public boolean isEspectador() { return espectador; }
    public void setEspectador(boolean espectador) { this.espectador = espectador; }

    public Location getLocationAntes() { return locationAntes; }
    public void setLocationAntes(Location loc) { this.locationAntes = loc; }

    public ItemStack[] getInventarioAntes() { return inventarioAntes; }
    public void setInventarioAntes(ItemStack[] inv) { this.inventarioAntes = inv; }

    public ItemStack[] getArmorAntes() { return armorAntes; }
    public void setArmorAntes(ItemStack[] armor) { this.armorAntes = armor; }
}
