const router = require('express').Router();
const db     = require('../config/db');

/* ─── GET /api/server/status ───────────────────────────────────── */
router.get('/status', async (req, res) => {
  try {
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

    // Zona PvP activa (desde nuestra tabla de configuración web)
    let pvpZone = { name: 'Coliseo de Giran', active: true };
    try {
      const [[zoneRow]] = await db.execute(
        "SELECT value FROM web_config WHERE `key` = 'current_pvp_zone'"
      );
      if (zoneRow) pvpZone.name = zoneRow.value;
    } catch { /* tabla no existe aún */ }

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
      status: 'online',
      online,
      accounts,
      characters,
      pvpZone,
      topClans,
      castles,
      timestamp: new Date().toISOString()
    });
  } catch (err) {
    console.error('[Server/status]', err.message);
    res.status(500).json({ status: 'offline', error: err.message });
  }
});

/* ─── GET /api/server/pvpzone ──────────────────────────────────── */
router.get('/pvpzone', async (req, res) => {
  try {
    // Información de la zona PvP rotativa actual
    let zoneInfo = {
      currentZone: process.env.PVP_ZONE_1 || 'Coliseo de Giran',
      availableZones: [
        process.env.PVP_ZONE_1 || 'Coliseo de Giran',
        process.env.PVP_ZONE_2 || 'Catacumbas de los Sacrificados'
      ],
      playersInZone: 0,
      nextRotationIn: null
    };

    // Jugadores en zona (aproximación: jugadores online en coordenadas de zona)
    const [[{ pcount }]] = await db.execute(
      'SELECT COUNT(*) AS pcount FROM characters WHERE online=1 AND deletetime=0'
    );
    zoneInfo.playersInZone = Math.floor(pcount * 0.3); // estimación

    res.json(zoneInfo);
  } catch (err) {
    res.status(500).json({ error: 'Error obteniendo info de zona' });
  }
});

module.exports = router;
