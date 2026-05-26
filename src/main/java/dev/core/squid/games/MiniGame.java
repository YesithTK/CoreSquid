package dev.core.squid.games;

import dev.core.squid.CoreSquid;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class MiniGame {

    protected final CoreSquid plugin;
    protected boolean activo;

    public MiniGame(CoreSquid plugin) {
        this.plugin = plugin;
        this.activo = false;
    }

    /** Nombre del minijuego mostrado al jugador */
    public abstract String getNombre();

    /** Descripcion corta del minijuego */
    public abstract String getDescripcion();

    /** Tiempo maximo del minijuego en segundos */
    public abstract int getTiempoLimite();

    /**
     * Genera la arena del minijuego en la ubicacion dada.
     * Construye los bloques necesarios.
     */
    public abstract void generarArena(Location origen);

    /**
     * Inicia el minijuego con la lista de jugadores.
     */
    public abstract void iniciar(List<Player> jugadores);

    /**
     * Llamado cada segundo mientras el minijuego esta activo.
     */
    public abstract void tick();

    /**
     * Termina el minijuego y limpia la arena.
     */
    public abstract void terminar();

    /**
     * Llamado cuando un jugador abandona o pierde conexion.
     */
    public abstract void onPlayerQuit(Player player);

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
