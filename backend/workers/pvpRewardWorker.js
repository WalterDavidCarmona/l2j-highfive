/**
 * pvpRewardWorker.js
 * Worker que se ejecuta en background cada POLL_INTERVAL segundos.
 * Detecta nuevos kills en pvp_zone_kills y acredita WebCoins al ganador.
 *
 * Config (tabla web_config):
 *   pvpzone_reward_enabled  → '1' activo, '0' desactivado
 *   pvpzone_reward_coins    → WebCoins por kill (entero ≥ 0)
 */

const db = require('../config/db');

const POLL_INTERVAL = 15 * 1000; // 15 segundos

/* ─── Helpers ─────────────────────────────────────────────────────── */

async function getConfig() {
  try {
    const [rows] = await db.execute(
      `SELECT \`key\`, \`value\` FROM web_config
       WHERE \`key\` IN ('pvpzone_reward_enabled','pvpzone_reward_coins')`
    );
    const map = {};
    rows.forEach(r => { map[r.key] = r.value; });
    return {
      enabled:     map['pvpzone_reward_enabled'] === '1',
      coinsPerKill: Math.max(0, parseInt(map['pvpzone_reward_coins'] || '5', 10))
    };
  } catch {
    return { enabled: false, coinsPerKill: 5 };
  }
}

/**
 * Devuelve el account_name para un char_name real (de pvp_zone_kills).
 * Maneja el caso en que el jugador está dentro de la zona PvP y
 * characters.char_name fue sobreescrito con el nombre de la profesión.
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

/* ─── Ciclo principal ─────────────────────────────────────────────── */

async function runCycle() {
  const { enabled, coinsPerKill } = await getConfig();
  if (!enabled || coinsPerKill <= 0) return;

  // Obtener todos los registros de kills en zona pvp
  let killRows;
  try {
    [killRows] = await db.execute(
      `SELECT char_name, kills, zone_name FROM pvp_zone_kills WHERE kills > 0`
    );
  } catch {
    return; // tabla no existe aún
  }

  // Limpiar notificaciones expiradas
  db.execute('DELETE FROM pvp_zone_notifications WHERE expires_at <= NOW()').catch(() => {});

  if (!killRows.length) return;

  for (const row of killRows) {
    const { char_name, kills, zone_name } = row;

    // ¿Cuántos kills ya premiamos?
    const [[logRow]] = await db.execute(
      `SELECT kills_rewarded FROM pvp_zone_reward_log WHERE char_name = ?`,
      [char_name]
    ).catch(() => [[null]]);

    const killsRewarded = logRow ? (logRow.kills_rewarded || 0) : 0;
    const newKills      = kills - killsRewarded;

    if (newKills <= 0) continue;

    const coinsToAward = newKills * coinsPerKill;

    // Encontrar la cuenta
    const accountName = await findAccount(char_name).catch(() => null);
    if (!accountName) {
      // Personaje no encontrado — marcar de todas formas para no re-intentar
      await db.execute(
        `INSERT INTO pvp_zone_reward_log (char_name, kills_rewarded, coins_total)
         VALUES (?, ?, 0)
         ON DUPLICATE KEY UPDATE kills_rewarded = ?`,
        [char_name, kills, kills]
      ).catch(() => {});
      continue;
    }

    const conn = await db.getConnection();
    try {
      await conn.beginTransaction();

      // Acreditar coins
      await addCoins(conn, accountName, coinsToAward);

      // Actualizar log de estado
      await conn.execute(
        `INSERT INTO pvp_zone_reward_log (char_name, kills_rewarded, coins_total)
         VALUES (?, ?, ?)
         ON DUPLICATE KEY UPDATE
           kills_rewarded = ?,
           coins_total    = coins_total + ?`,
        [char_name, kills, coinsToAward, kills, coinsToAward]
      );

      // Insertar en historial
      await conn.execute(
        `INSERT INTO pvp_zone_reward_history
           (char_name, account_name, kills_new, coins_awarded)
         VALUES (?, ?, ?, ?)`,
        [char_name, accountName, newKills, coinsToAward]
      );

      // Crear / actualizar notificación en el panel del jugador (expira en 1 día).
      // Si ya existe una notificación activa para este personaje, acumular coins y kills
      // y renovar el tiempo de expiración desde ahora.
      const zoneName = zone_name || 'Zona PvP';
      await conn.execute(
        `INSERT INTO pvp_zone_notifications
           (account_name, char_name, coins_awarded, zone_name, kills_new, dismissed, expires_at)
         VALUES (?, ?, ?, ?, ?, 0, DATE_ADD(NOW(), INTERVAL 1 DAY))
         ON DUPLICATE KEY UPDATE
           account_name  = VALUES(account_name),
           coins_awarded = coins_awarded + VALUES(coins_awarded),
           zone_name     = VALUES(zone_name),
           kills_new     = kills_new + VALUES(kills_new),
           dismissed     = 0,
           expires_at    = DATE_ADD(NOW(), INTERVAL 1 DAY)`,
        [accountName, char_name, coinsToAward, zoneName, newKills]
      ).catch(err => console.warn('[PvpReward] Notif insert warn:', err.message));

      await conn.commit();
      console.log(`[PvpReward] ✅ ${char_name} (${accountName}) +${coinsToAward} coins (${newKills} kills × ${coinsPerKill})`);
    } catch (err) {
      await conn.rollback();
      console.error(`[PvpReward] ❌ Error recompensando a ${char_name}:`, err.message);
    } finally {
      conn.release();
    }
  }
}

/* ─── Arranque ────────────────────────────────────────────────────── */

function start() {
  console.log(`[PvpReward] Worker iniciado (cada ${POLL_INTERVAL / 1000}s)`);

  // Crear tablas si no existen (idempotente)
  db.execute(`
    CREATE TABLE IF NOT EXISTS pvp_zone_reward_log (
      char_name       VARCHAR(35)  NOT NULL,
      kills_rewarded  INT UNSIGNED DEFAULT 0,
      coins_total     INT UNSIGNED DEFAULT 0,
      last_reward_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      PRIMARY KEY (char_name)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `).catch(err => console.warn('[PvpReward] Tabla reward_log:', err.message));

  db.execute(`
    CREATE TABLE IF NOT EXISTS pvp_zone_reward_history (
      id            INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
      char_name     VARCHAR(35)  NOT NULL,
      account_name  VARCHAR(45)  NOT NULL,
      kills_new     INT UNSIGNED DEFAULT 0,
      coins_awarded INT UNSIGNED DEFAULT 0,
      rewarded_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
      INDEX(char_name), INDEX(rewarded_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `).catch(err => console.warn('[PvpReward] Tabla reward_history:', err.message));

  db.execute(`
    INSERT IGNORE INTO web_config (\`key\`, value) VALUES
      ('pvpzone_reward_enabled', '0'),
      ('pvpzone_reward_coins',   '5')
  `).catch(() => {});

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
  `).catch(err => console.warn('[PvpReward] Tabla notifications:', err.message));

  // Ejecutar ciclo periódico
  setInterval(() => {
    runCycle().catch(err => console.error('[PvpReward] Ciclo error:', err.message));
  }, POLL_INTERVAL);

  // Primera ejecución inmediata (pequeño delay para que la BD esté lista)
  setTimeout(() => {
    runCycle().catch(err => console.error('[PvpReward] Ciclo inicial error:', err.message));
  }, 5000);
}

module.exports = { start };
