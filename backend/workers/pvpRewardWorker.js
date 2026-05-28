/**
 * pvpRewardWorker.js
 * Premia con WebCoins al TOP KILLER (#1) de cada sesión de Zona PvP.
 * El premio se entrega cuando la zona rota (cambia a la siguiente).
 *
 * Flujo:
 *  1. Cada POLL_INTERVAL se detecta si la zona activa cambió.
 *  2. Si cambió → buscar top killer de la zona que TERMINÓ.
 *  3. Acreditar WebCoins al top killer.
 *  4. Crear notificación en el panel del jugador.
 *  5. Snapshottear kills actuales de la nueva zona como punto de partida
 *     (evita acumular kills de sesiones anteriores del mismo mapa).
 *
 * Config (tabla web_config):
 *   pvpzone_reward_enabled   → '1' activo, '0' desactivado
 *   pvpzone_reward_coins     → WebCoins para el top killer (entero ≥ 0)
 *   pvpzone_current_index    → índice de la zona activa actualmente detectada
 *   pvpzone_current_name     → nombre de la zona activa actualmente
 */

const db             = require('../config/db');
const { getActivePvpZone } = require('../config/pvpZoneUtils');

const POLL_INTERVAL = 15 * 1000; // 15 segundos

/* ─── Helpers: config ─────────────────────────────────────────────── */

async function getConfig() {
  try {
    const [rows] = await db.execute(
      `SELECT \`key\`, \`value\` FROM web_config
       WHERE \`key\` IN (
         'pvpzone_reward_enabled',
         'pvpzone_reward_coins',
         'pvpzone_current_index',
         'pvpzone_current_name'
       )`
    );
    const m = {};
    rows.forEach(r => { m[r.key] = r.value; });
    return {
      enabled:      m['pvpzone_reward_enabled'] === '1',
      coinsTopKiller: Math.max(0, parseInt(m['pvpzone_reward_coins'] || '50', 10)),
      storedIndex:  m['pvpzone_current_index'] != null ? parseInt(m['pvpzone_current_index'], 10) : null,
      storedName:   m['pvpzone_current_name']  || null
    };
  } catch {
    return { enabled: false, coinsTopKiller: 50, storedIndex: null, storedName: null };
  }
}

async function saveCurrentZone(index, name) {
  const pairs = [
    ['pvpzone_current_index', String(index)],
    ['pvpzone_current_name',  name]
  ];
  for (const [k, v] of pairs) {
    await db.execute(
      `INSERT INTO web_config (\`key\`, value) VALUES (?, ?)
       ON DUPLICATE KEY UPDATE value = ?`,
      [k, v, v]
    );
  }
}

/* ─── Helpers: personajes ─────────────────────────────────────────── */

/**
 * Encuentra la cuenta del personaje por su nombre REAL.
 * Usa COALESCE para manejar el caso en que el jugador está en zona PvP
 * (donde char_name fue sobreescrito con el nombre de la profesión).
 */
async function findAccount(charName) {
  const [rows] = await db.execute(
    `SELECT c.account_name
     FROM characters c
     WHERE c.charId = COALESCE(
       (SELECT charId FROM character_variables
        WHERE var = 'PVPZ_REAL_NAME' AND val = ? LIMIT 1),
       (SELECT charId FROM characters
        WHERE char_name = ? AND deletetime = 0 LIMIT 1)
     ) AND c.deletetime = 0
     LIMIT 1`,
    [charName, charName]
  );
  return rows.length ? rows[0].account_name : null;
}

async function addCoins(conn, accountName, amount) {
  const pos = Math.max(0, Math.floor(amount));
  await conn.execute(
    `INSERT INTO account_data (account_name, var, value)
     VALUES (?, 'web_coins', ?)
     ON DUPLICATE KEY UPDATE value = CAST(CAST(value AS UNSIGNED) + ? AS CHAR)`,
    [accountName, pos, pos]
  );
}

/* ─── Snapshot de kills ───────────────────────────────────────────── */

/**
 * Guarda los kills actuales de cada personaje en la zona como punto de partida.
 * Se llama al inicio de cada sesión de zona para aislar los kills de esta sesión.
 */
async function snapshotKillsForZone(zoneName) {
  try {
    const [kills] = await db.execute(
      `SELECT char_name, kills FROM pvp_zone_kills WHERE zone_name = ?`,
      [zoneName]
    );

    // Borrar snapshot anterior para esta zona
    await db.execute(
      `DELETE FROM pvp_zone_session_snapshot WHERE zone_name = ?`,
      [zoneName]
    );

    // Insertar snapshot actual (puede ser vacío si la zona recién empieza)
    for (const row of kills) {
      await db.execute(
        `INSERT IGNORE INTO pvp_zone_session_snapshot (zone_name, char_name, kills_at_start)
         VALUES (?, ?, ?)`,
        [zoneName, row.char_name, row.kills]
      );
    }
    console.log(`[PvpReward] 📸 Snapshot zona "${zoneName}" — ${kills.length} personajes`);
  } catch (err) {
    console.warn('[PvpReward] Error snapshot:', err.message);
  }
}

/* ─── Top killer ──────────────────────────────────────────────────── */

/**
 * Busca al jugador con más kills durante ESTA sesión de la zona.
 * Kills de sesión = kills_actuales − kills_al_inicio_de_sesión
 */
async function findTopKiller(zoneName) {
  const [rows] = await db.execute(
    `SELECT z.char_name,
            (z.kills - COALESCE(s.kills_at_start, 0)) AS session_kills
     FROM pvp_zone_kills z
     LEFT JOIN pvp_zone_session_snapshot s
       ON s.zone_name = z.zone_name AND s.char_name = z.char_name
     WHERE z.zone_name = ?
       AND (z.kills - COALESCE(s.kills_at_start, 0)) > 0
     ORDER BY session_kills DESC
     LIMIT 1`,
    [zoneName]
  ).catch(() => [[]]);

  return rows.length ? rows[0] : null;
}

/**
 * Premia al top killer de la zona que acaba de terminar.
 */
async function awardTopKiller(zoneName, coinsToAward) {
  const top = await findTopKiller(zoneName);

  if (!top) {
    console.log(`[PvpReward] ℹ️  Zona "${zoneName}" sin kills registrados — sin premio`);
    return;
  }

  const { char_name, session_kills } = top;
  console.log(`[PvpReward] 🏆 Top killer "${zoneName}": ${char_name} (${session_kills} kills)`);

  const accountName = await findAccount(char_name).catch(() => null);
  if (!accountName) {
    console.warn(`[PvpReward] ⚠️  No se encontró cuenta para "${char_name}"`);
    return;
  }

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();

    // 1. Acreditar WebCoins
    await addCoins(conn, accountName, coinsToAward);

    // 2. Historial de recompensas
    await conn.execute(
      `INSERT INTO pvp_zone_reward_history
         (char_name, account_name, zone_name, kills_new, coins_awarded)
       VALUES (?, ?, ?, ?, ?)`,
      [char_name, accountName, zoneName, session_kills, coinsToAward]
    );

    // 3. Log acumulado por personaje
    await conn.execute(
      `INSERT INTO pvp_zone_reward_log (char_name, kills_rewarded, coins_total)
       VALUES (?, ?, ?)
       ON DUPLICATE KEY UPDATE
         kills_rewarded = kills_rewarded + ?,
         coins_total    = coins_total    + ?`,
      [char_name, session_kills, coinsToAward, session_kills, coinsToAward]
    );

    // 4. Notificación en el panel del jugador (expira 1 día)
    await conn.execute(
      `INSERT INTO pvp_zone_notifications
         (account_name, char_name, coins_awarded, zone_name, kills_new, dismissed, expires_at)
       VALUES (?, ?, ?, ?, ?, 0, DATE_ADD(NOW(), INTERVAL 1 DAY))
       ON DUPLICATE KEY UPDATE
         account_name  = VALUES(account_name),
         coins_awarded = coins_awarded + VALUES(coins_awarded),
         zone_name     = VALUES(zone_name),
         kills_new     = kills_new     + VALUES(kills_new),
         dismissed     = 0,
         expires_at    = DATE_ADD(NOW(), INTERVAL 1 DAY)`,
      [accountName, char_name, coinsToAward, zoneName, session_kills]
    ).catch(err => console.warn('[PvpReward] Notif warn:', err.message));

    await conn.commit();
    console.log(`[PvpReward] ✅ Premio entregado: ${char_name} (${accountName}) +${coinsToAward} 🪙 por zona "${zoneName}"`);
  } catch (err) {
    await conn.rollback();
    console.error(`[PvpReward] ❌ Error premiando a ${char_name}:`, err.message);
  } finally {
    conn.release();
  }
}

/* ─── Ciclo principal ─────────────────────────────────────────────── */

async function runCycle() {
  const { enabled, coinsTopKiller, storedIndex, storedName } = await getConfig();

  // Limpiar notificaciones expiradas (tarea de mantenimiento)
  db.execute('DELETE FROM pvp_zone_notifications WHERE expires_at <= NOW()').catch(() => {});

  if (!enabled || coinsTopKiller <= 0) return;

  // Zona activa según PvpZone.ini + rotación
  const activeZone = getActivePvpZone();
  if (!activeZone.enabled || !activeZone.name || activeZone.name === 'Sin zona') return;

  // ── Primera ejecución: sólo registrar zona actual y snapshottear ──
  if (storedIndex === null) {
    console.log(`[PvpReward] 🚀 Primera ejecución — zona actual: "${activeZone.name}" (idx ${activeZone.index})`);
    await saveCurrentZone(activeZone.index, activeZone.name);
    await snapshotKillsForZone(activeZone.name);
    return;
  }

  // ── Zona sin cambios ───────────────────────────────────────────────
  if (storedIndex === activeZone.index) return;

  // ── Zona cambió → premiar top killer de la zona que terminó ───────
  console.log(`[PvpReward] 🔄 Rotación detectada: "${storedName}" → "${activeZone.name}"`);

  if (storedName) {
    await awardTopKiller(storedName, coinsTopKiller);
  }

  // Actualizar zona activa y snapshottear punto de partida de la nueva zona
  await saveCurrentZone(activeZone.index, activeZone.name);
  await snapshotKillsForZone(activeZone.name);
}

/* ─── Arranque ────────────────────────────────────────────────────── */

function start() {
  console.log(`[PvpReward] Worker iniciado (detección de rotación cada ${POLL_INTERVAL / 1000}s)`);

  // Crear tablas necesarias (idempotente)
  db.execute(`
    CREATE TABLE IF NOT EXISTS pvp_zone_reward_log (
      char_name       VARCHAR(35)  NOT NULL,
      kills_rewarded  INT UNSIGNED DEFAULT 0,
      coins_total     INT UNSIGNED DEFAULT 0,
      last_reward_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      PRIMARY KEY (char_name)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `).catch(err => console.warn('[PvpReward] reward_log:', err.message));

  db.execute(`
    CREATE TABLE IF NOT EXISTS pvp_zone_reward_history (
      id            INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
      char_name     VARCHAR(35)  NOT NULL,
      account_name  VARCHAR(45)  NOT NULL,
      zone_name     VARCHAR(100) DEFAULT 'Zona PvP',
      kills_new     INT UNSIGNED DEFAULT 0,
      coins_awarded INT UNSIGNED DEFAULT 0,
      rewarded_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
      INDEX(char_name), INDEX(rewarded_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `).catch(err => console.warn('[PvpReward] reward_history:', err.message));

  db.execute(`
    CREATE TABLE IF NOT EXISTS pvp_zone_session_snapshot (
      zone_name       VARCHAR(100) NOT NULL,
      char_name       VARCHAR(35)  NOT NULL,
      kills_at_start  INT UNSIGNED DEFAULT 0,
      snapshot_at     DATETIME     DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (zone_name, char_name)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `).catch(err => console.warn('[PvpReward] session_snapshot:', err.message));

  db.execute(`
    CREATE TABLE IF NOT EXISTS pvp_zone_notifications (
      id            INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
      account_name  VARCHAR(45)  NOT NULL,
      char_name     VARCHAR(35)  NOT NULL,
      coins_awarded INT UNSIGNED DEFAULT 0,
      zone_name     VARCHAR(100) DEFAULT 'Zona PvP',
      kills_new     INT UNSIGNED DEFAULT 0,
      dismissed     TINYINT(1)   DEFAULT 0,
      created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP,
      expires_at    DATETIME     NOT NULL,
      UNIQUE KEY uq_char_name (char_name),
      INDEX(account_name),
      INDEX(expires_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `).catch(err => console.warn('[PvpReward] notifications:', err.message));

  db.execute(`
    INSERT IGNORE INTO web_config (\`key\`, value) VALUES
      ('pvpzone_reward_enabled', '0'),
      ('pvpzone_reward_coins',   '50')
  `).catch(() => {});

  // Ejecutar ciclo periódico
  setInterval(() => {
    runCycle().catch(err => console.error('[PvpReward] Ciclo error:', err.message));
  }, POLL_INTERVAL);

  // Primera ejecución con pequeño delay para que la BD esté lista
  setTimeout(() => {
    runCycle().catch(err => console.error('[PvpReward] Ciclo inicial error:', err.message));
  }, 5000);
}

module.exports = { start };
