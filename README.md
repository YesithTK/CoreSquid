# CoreSquid v1.0.0
**play.colombiacraft.fun**

Plugin profesional de Squid Game para Paper 1.21+

---

## Instalación

1. Pon `CoreSquid.jar` en `plugins/`
2. Reinicia el servidor
3. Configura `plugins/CoreSquid/config.yml`
4. Configura el lobby con `/squid setlobby`
5. Crea arenas con `/squid wand` + `/squid arena create <juego>`

---

## Comandos

### Jugadores
| Comando | Descripción |
|---|---|
| `/squid join` | Unirse al lobby |
| `/squid leave` | Salir del lobby |
| `/squid stats [jugador]` | Ver estadísticas |
| `/squid bet <par/impar> <n>` | Apostar en Canicas |
| `/squid guess <número>` | Adivinar en Adivina el Número |
| `/squid drink` | Beber en Ruleta Rusa |
| `/squid pull` | Jalar en Tira y Jala |

### Administradores
| Comando | Descripción |
|---|---|
| `/squid start` | Forzar inicio de partida |
| `/squid stop` | Detener partida |
| `/squid wand` | Obtener hacha de selección |
| `/squid arena create <juego>` | Crear arena |
| `/squid arena delete <juego>` | Eliminar arena |
| `/squid arena setspawn <juego>` | Configurar spawn de arena |
| `/squid arena list` | Listar arenas |
| `/squid setlobby` | Configurar spawn del lobby |
| `/squid setspawn` | Configurar spawn global |
| `/squid reload` | Recargar configuración |

---

## Permisos

| Permiso | Default | Descripción |
|---|---|---|
| `coresquid.admin` | op | Acceso total |
| `coresquid.use` | true | Puede jugar |
| `coresquid.stats` | true | Ver estadísticas |

---

## Configurar Arenas

```bash
# 1. Obtener el hacha
/squid wand

# 2. Clic izquierdo en un bloque = Posicion 1
# 3. Clic derecho en un bloque = Posicion 2

# 4. Crear la arena
/squid arena create red-light-green-light
```

### IDs de juegos disponibles
| ID | Juego |
|---|---|
| `red-light-green-light` | Luz Roja Luz Verde |
| `glass-bridge` | Puente de Cristal |
| `marbles` | Canicas |
| `tug-of-war` | Tira y Jala |
| `dalgona` | Dalgona |
| `musical-chairs` | Sillas Musicales |
| `maze` | Laberinto |
| `hot-potato` | Bomba Caliente |
| `memory-blocks` | Memoria de Bloques |
| `obstacle-course` | Carrera de Obstaculos |
| `falling-platforms` | Plataformas que Caen |
| `guess-number` | Adivina el Numero |
| `hide-seek` | Escondite |
| `king-hill` | Rey de la Colina |
| `russian-roulette` | Ruleta Rusa |

---

## Configuracion principal (config.yml)

```yaml
language: es              # es o en
server-ip: "play.colombiacraft.fun"

game:
  min-players: 2
  max-players: 16
  lobby-countdown: 30
  between-games-delay: 10
  starting-titles: true

scoreboard:
  enabled: true
  always-visible: true

rewards:
  enabled: true
  money: 500
  commands:
    - "give {player} diamond 3"
```

---

## Idiomas

Los mensajes están en `plugins/CoreSquid/lang/`:
- `es.yml` — Español
- `en.yml` — Inglés

Cambia el idioma en `config.yml`:
```yaml
language: en
```

---

## Resource Pack

1. Abre la carpeta `resourcepack/` del ZIP
2. Pon tus archivos `.ogg` en `assets/coresquid/sounds/coresquid/`
3. Comprime el contenido en un `.zip`
4. Sube el ZIP a internet (MediaFire, GitHub, etc.)
5. Configura en `config.yml`:

```yaml
resourcepack:
  enabled: true
  url: "https://tu-link-directo/CoreSquid-RP.zip"
```

6. Ejecuta `/squid reload`

---

## Compilar en Termux

```bash
pkg install openjdk-21 maven
git clone https://github.com/TU_USUARIO/CoreSquid.git
cd CoreSquid
mvn clean package -U
cp target/CoreSquid.jar /sdcard/Download/
```

---

## Sistema automatico de partidas

1. Jugador ejecuta `/squid join`
2. Al llegar al minimo de jugadores inicia conteo
3. Cuenta regresiva con titulos en pantalla (3, 2, 1)
4. Inicia automaticamente y recorre los minijuegos
5. Al quedar 1 jugador se anuncia el ganador
6. Se restauran inventarios y ubicaciones

---

Compatible con **Paper 1.21+** | Java 21+ | Vault (opcional)
