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
 * Snapshot de kills al inicio de una sesión de zona.
 *
 * IMPORTANTE: No filtramos por zone_name porque el servidor Java puede
 * escribir en pvp_zone_kills un nombre de zona distinto al que aparece
 * en PvpZone.ini. Sumamos los kills totales de TODOS los zone_name por
 * personaje y los guardamos bajo el marcador especial '__session__'.
 * Así el cálculo del delta es completamente agnóstico al nombre de zona.
 */
async function snapshotCurrentKills() {
  try {
    // Total de kills por personaje (sumando todas las zonas)
    const [totals] = await db.execute(
      `SELECT char_name, SUM(kills) AS total_kills
       FROM pvp_zone_kills
       GROUP BY char_name`
    );

    // Reemplazar snapshot anterior de la sesión activa
    await db.execute(
      `DELETE FROM pvp_zone_session_snapshot WHERE zone_name = '__session__'`
    );

    for (const row of totals) {
      await db.execute(
        `INSERT IGNORE INTO pvp_zone_session_snapshot (zone_name, char_name, kills_at_start)
         VALUES ('__session__', ?, ?)`,
        [row.char_name, row.total_kills]
      );
    }
    console.log(`[PvpReward] 📸 Snapshot sesión — ${totals.length} personajes registrados`);
  } catch (err) {
    console.warn('[PvpReward] Error snapshot:', err.message);
  }
}

/* ─── Top killer ──────────────────────────────────────────────────── */

/**
 * Busca al jugador con más kills durante ESTA sesión de zona.
 * Kills de sesión = total_kills_actual − total_kills_al_inicio_de_sesión
 *
 * No depende de zone_name: suma todos los kills del personaje en cualquier
 * fila de pvp_zone_kills y resta lo que tenía al inicio de la sesión.
 * Esto es robusto aunque Java use un nombre de zona diferente al del INI.
 */
async function findTopKiller() {
  const [rows] = await db.execute(
    `SELECT z.char_name,
            (SUM(z.kills) - COALESCE(s.kills_at_start, 0)) AS session_kills
     FROM pvp_zone_kills z
     LEFT JOIN pvp_zone_session_snapshot s
       ON s.char_name = z.char_name AND s.zone_name = '__session__'
     GROUP BY z.char_name, s.kills_at_start
     HAVING session_kills > 0
     ORDER BY session_kills DESC
     LIMIT 1`
  ).catch(() => [[]]);

  return rows.length ? rows[0] : null;
}

/**
 * Premia al top killer de la sesión que acaba de terminar.
 * @param {string} zoneName  Nombre de la zona según PvpZone.ini (solo para display)
 * @param {number} coinsToAward
 */
async function awardTopKiller(zoneName, coinsToAward) {
  const top = await findTopKiller();

  if (!top) {
    console.log(`[PvpReward] ℹ️  Zona "${zoneName}" — sin kills en esta sesión, sin premio`);
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

  // ── Primera ejecución: registrar zona actual y tomar snapshot inicial ──
  if (storedIndex === null) {
    console.log(`[PvpReward] 🚀 Primera ejecución — zona activa según INI: "${activeZone.name}" (idx ${activeZone.index})`);
    await saveCurrentZone(activeZone.index, activeZone.name);
    await snapshotCurrentKills();
    return;
  }

  // ── Zona sin cambios ───────────────────────────────────────────────
  if (storedIndex === activeZone.index) return;

  // ── Zona cambió → premiar top killer de la sesión que terminó ─────
  // storedName es el nombre legible del INI (solo para mostrar en notif/logs).
  // El cálculo de kills NO depende de ese nombre: usa el snapshot __session__.
  console.log(`[PvpReward] 🔄 Rotación detectada: "${storedName}" → "${activeZone.name}"`);

  if (storedName) {
    await awardTopKiller(storedName, coinsTopKiller);
  }

  // Guardar nueva zona activa y tomar snapshot de kills actuales
  // como punto de partida de la nueva sesión
  await saveCurrentZone(activeZone.index, activeZone.name);
  await snapshotCurrentKills();
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
