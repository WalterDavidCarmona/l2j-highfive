const router        = require('express').Router();
const db            = require('../config/db');
const net           = require('net');
const { getActivePvpZone } = require('../config/pvpZoneUtils');

const GS_HOST    = process.env.GAME_SERVER_HOST || '127.0.0.1';
const GS_PORT    = parseInt(process.env.GAME_SERVER_PORT) || 7777;
const GS_TIMEOUT = 3000;

/* ─── Helpers ──────────────────────────────────────────────────── */

/** Devuelve true si el puerto del gameserver responde */
function checkGameServer() {
  return new Promise((resolve) => {
    const socket = new net.Socket();
    let resolved = false;
    const done = (result) => {
      if (!resolved) { resolved = true; socket.destroy(); resolve(result); }
    };
    socket.setTimeout(GS_TIMEOUT);
    socket.connect(GS_PORT, GS_HOST, () => done(true));
    socket.on('error',   () => done(false));
    socket.on('timeout', () => done(false));
  });
}

/* ─── GET /api/server/status ───────────────────────────────────── */
router.get('/status', async (req, res) => {
  try {
    // Verificar si el gameserver está encendido (conexión TCP real)
    const gameOnline = await checkGameServer();

    // Jugadores online actuales
    const [[{ online }]] = await db.execute(
      'SELECT COUNT(*) AS online FROM characters WHERE online = 1 AND deletetime = 0'
    );

    // Total de cuentas registradas
    const [[{ accounts }]] = await db.execute(
      'SELECT COUNT(*) AS accounts FROM accounts WHERE accessLevel >= 0'
    );

    // Total de personajes
    const [[{ characters }]] = await db.execute(
      'SELECT COUNT(*) AS characters FROM characters WHERE deletetime = 0'
    );

    // Si el gameserver está offline, los jugadores online deben ser 0
    const onlineCount = gameOnline ? online : 0;

    // Zona PvP activa — leída directamente del PvpZone.ini del gameserver
    const pvpZone = getActivePvpZone();

    // Top clanes por reputación
    const [topClans] = await db.execute(
      `SELECT clan_name, reputation_score,
              (SELECT COUNT(*) FROM characters WHERE clanid=clan_data.clan_id AND online=1) AS online_members
       FROM clan_data
       ORDER BY reputation_score DESC
       LIMIT 5`
    );

    // Castillos tomados
    let castles = [];
    try {
      const [castleRows] = await db.execute(
        `SELECT cs.name, cl.clan_name AS owner
         FROM castle cs
         LEFT JOIN clan_data cl ON cl.clan_id = cs.side_data
         LIMIT 9`
      );
      castles = castleRows;
    } catch { /* tabla puede tener nombre diferente */ }

    res.json({
      status:     gameOnline ? 'online' : 'offline',
      gameOnline,
      online:     onlineCount,
      accounts,
      characters,
      pvpZone,
      topClans,
      castles,
      timestamp: new Date().toISOString()
    });
  } catch (err) {
    console.error('[Server/status]', err.message);
    res.status(500).json({ status: 'offline', gameOnline: false, error: err.message });
  }
});

/* ─── GET /api/server/pvpzone ──────────────────────────────────── */
router.get('/pvpzone', async (req, res) => {
  try {
    const zoneInfo = getActivePvpZone();

    // Jugadores online como referencia
    const [[{ pcount }]] = await db.execute(
      'SELECT COUNT(*) AS pcount FROM characters WHERE online=1 AND deletetime=0'
    );

    res.json({
      currentZone:    zoneInfo.name,
      currentIndex:   zoneInfo.index,
      availableZones: zoneInfo.allZones,
      rotationMinutes: zoneInfo.rotationMinutes,
      nextRotationIn: zoneInfo.nextRotationIn, // segundos
      playersOnline:  pcount
    });
  } catch (err) {
    console.error('[Server/pvpzone]', err.message);
    res.status(500).json({ error: 'Error obteniendo info de zona' });
  }
});

module.exports = router;
