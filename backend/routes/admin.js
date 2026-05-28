/**
 * /api/admin  — Panel de Administración L2H5
 * Requiere: auth + accessLevel >= 100
 */
const router = require('express').Router();
const db     = require('../config/db');
const auth   = require('../middleware/auth');
const { hashPassword } = require('../config/hashUtils');

/* ─── Guard de administrador ─────────────────────────────────────── */
function adminOnly(req, res, next) {
  if (!req.user || req.user.accessLevel < 100)
    return res.status(403).json({ error: 'Sin permisos de administrador' });
  next();
}

/* ─── Helpers de coins ───────────────────────────────────────────── */

async function getCoins(conn, accountName) {
  const [rows] = await conn.execute(
    `SELECT CAST(COALESCE(value,'0') AS UNSIGNED) AS coins
     FROM account_data WHERE account_name=? AND var='web_coins'`,
    [accountName]
  );
  return rows.length ? (rows[0].coins || 0) : 0;
}

async function setCoins(conn, accountName, amount) {
  const val = Math.max(0, Math.floor(amount)).toString();
  await conn.execute(
    `INSERT INTO account_data (account_name, var, value)
     VALUES (?, 'web_coins', ?)
     ON DUPLICATE KEY UPDATE value = ?`,
    [accountName, val, val]
  );
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

async function subtractCoins(conn, accountName, amount) {
  const current = await getCoins(conn, accountName);
  const newVal  = Math.max(0, current - Math.floor(amount));
  await setCoins(conn, accountName, newVal);
  return newVal;
}

/* ──────────────────────────────────────────────────────────────────
   GET /api/admin/users/search?q=LOGIN
   Busca cuentas por login (LIKE). Max 30 resultados.
─────────────────────────────────────────────────────────────────── */
router.get('/users/search', auth, adminOnly, async (req, res) => {
  const q = (req.query.q || '').trim();
  if (!q) return res.json({ users: [] });

  try {
    const [rows] = await db.execute(
      `SELECT a.login, a.email, a.accessLevel,
              CAST(COALESCE(ad.value,'0') AS UNSIGNED) AS web_coins,
              ban_ad.value AS ban_until,
              FROM_UNIXTIME(a.lastactive) AS last_login
       FROM accounts a
       LEFT JOIN account_data ad     ON ad.account_name = a.login     AND ad.var = 'web_coins'
       LEFT JOIN account_data ban_ad ON ban_ad.account_name = a.login AND ban_ad.var = 'ban_temp'
       WHERE a.login LIKE ?
       ORDER BY a.login
       LIMIT 30`,
      [`%${q}%`]
    );
    res.json({ users: rows });
  } catch (err) {
    console.error('[admin/search]', err);
    res.status(500).json({ error: 'Error al buscar usuarios' });
  }
});

/* ──────────────────────────────────────────────────────────────────
   GET /api/admin/users/:login
   Datos completos de una cuenta
─────────────────────────────────────────────────────────────────── */
router.get('/users/:login', auth, adminOnly, async (req, res) => {
  try {
    const [rows] = await db.execute(
      `SELECT a.login, a.email, a.accessLevel,
              CAST(COALESCE(ad.value,'0') AS UNSIGNED) AS web_coins,
              ban_ad.value AS ban_until,
              FROM_UNIXTIME(a.lastactive) AS last_login
       FROM accounts a
       LEFT JOIN account_data ad     ON ad.account_name = a.login     AND ad.var = 'web_coins'
       LEFT JOIN account_data ban_ad ON ban_ad.account_name = a.login AND ban_ad.var = 'ban_temp'
       WHERE a.login = ?`,
      [req.params.login]
    );
    if (!rows.length) return res.status(404).json({ error: 'Cuenta no encontrada' });
    res.json({ user: rows[0] });
  } catch (err) {
    console.error('[admin/getUser]', err);
    res.status(500).json({ error: 'Error al obtener usuario' });
  }
});

/* ──────────────────────────────────────────────────────────────────
   POST /api/admin/users/:login/coins
   body: { action: 'add'|'subtract'|'set', amount: number }
─────────────────────────────────────────────────────────────────── */
router.post('/users/:login/coins', auth, adminOnly, async (req, res) => {
  const { action, amount } = req.body;
  const login = req.params.login;

  if (!['add', 'subtract', 'set'].includes(action))
    return res.status(400).json({ error: 'Acción inválida (add|subtract|set)' });

  const amt = parseInt(amount);
  if (isNaN(amt) || amt < 0)
    return res.status(400).json({ error: 'Cantidad inválida' });

  // Seguridad: no modificar cuentas admin ajenas
  if (login !== req.user.login) {
    const [chk] = await db.execute(
      'SELECT accessLevel FROM accounts WHERE login=?', [login]
    );
    if (chk.length && chk[0].accessLevel >= 100)
      return res.status(403).json({ error: 'No puedes modificar coins de otra cuenta admin' });
  }

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();
    let newBalance;

    if (action === 'add') {
      await addCoins(conn, login, amt);
      newBalance = await getCoins(conn, login);
    } else if (action === 'subtract') {
      newBalance = await subtractCoins(conn, login, amt);
    } else { // set
      await setCoins(conn, login, amt);
      newBalance = amt;
    }

    await conn.commit();
    res.json({ ok: true, web_coins: newBalance });
  } catch (err) {
    await conn.rollback();
    console.error('[admin/coins]', err);
    res.status(500).json({ error: 'Error al actualizar coins' });
  } finally {
    conn.release();
  }
});

/* ──────────────────────────────────────────────────────────────────
   PUT /api/admin/users/:login
   body: { password?, email?, ban? }
   ban: null = desbanear | { days: number } = banear (0 = permanente)
─────────────────────────────────────────────────────────────────── */
router.put('/users/:login', auth, adminOnly, async (req, res) => {
  const login = req.params.login;
  const { password, email, ban } = req.body;

  if (!password && !email && ban === undefined)
    return res.status(400).json({ error: 'No hay cambios que aplicar' });

  // Seguridad: no modificar cuentas admin ajenas
  if (login !== req.user.login) {
    const [chk] = await db.execute(
      'SELECT accessLevel FROM accounts WHERE login=?', [login]
    );
    if (!chk.length) return res.status(404).json({ error: 'Cuenta no encontrada' });
    if (chk[0].accessLevel >= 100)
      return res.status(403).json({ error: 'No puedes modificar otra cuenta de admin' });
  }

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();
    const changes = [];

    // Cambiar contraseña
    if (password) {
      if (password.length < 4)
        return res.status(400).json({ error: 'La contraseña debe tener al menos 4 caracteres' });
      const hashed = await hashPassword(password);
      await conn.execute('UPDATE accounts SET password=? WHERE login=?', [hashed, login]);
      changes.push('contraseña');
    }

    // Cambiar email
    if (email !== undefined && email !== null) {
      const emailTrim = email.trim();
      if (emailTrim && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailTrim))
        return res.status(400).json({ error: 'Email inválido' });
      await conn.execute('UPDATE accounts SET email=? WHERE login=?', [emailTrim, login]);
      changes.push('email');
    }

    // Gestión de ban
    if (ban !== undefined) {
      if (ban === null) {
        // Desbanear
        await conn.execute(
          `DELETE FROM account_data WHERE account_name=? AND var='ban_temp'`,
          [login]
        );
        changes.push('ban removido');
      } else {
        // Banear: days=0 => permanente, days>0 => X días
        let banUntil;
        if (!ban.days || ban.days === 0) {
          banUntil = '9999999999'; // permanente
        } else {
          banUntil = String(Math.floor(Date.now() / 1000) + ban.days * 86400);
        }
        await conn.execute(
          `INSERT INTO account_data (account_name, var, value)
           VALUES (?, 'ban_temp', ?)
           ON DUPLICATE KEY UPDATE value=?`,
          [login, banUntil, banUntil]
        );
        changes.push(ban.days ? `ban ${ban.days} días` : 'ban permanente');
      }
    }

    await conn.commit();
    res.json({ ok: true, changes });
  } catch (err) {
    await conn.rollback();
    console.error('[admin/updateUser]', err);
    res.status(500).json({ error: 'Error al actualizar cuenta' });
  } finally {
    conn.release();
  }
});

/* ──────────────────────────────────────────────────────────────────
   GET /api/admin/payments?status=&limit=&offset=
   Tabla de compras con filtros
─────────────────────────────────────────────────────────────────── */
router.get('/payments', auth, adminOnly, async (req, res) => {
  const status = req.query.status || 'all';
  const limit  = Math.min(parseInt(req.query.limit)  || 50, 200);
  const offset = parseInt(req.query.offset) || 0;

  const validStatuses = ['pending', 'approved', 'rejected', 'cancelled', 'refunded'];
  const whereStatus = (status !== 'all' && validStatuses.includes(status))
    ? `AND po.status = '${status}'`
    : '';

  try {
    const [rows] = await db.execute(
      `SELECT po.id, po.account_name, po.coins, po.amount, po.currency,
              po.provider, po.status, po.created_at, po.updated_at,
              cp.name AS package_name, cp.bonus_pct
       FROM payment_orders po
       LEFT JOIN coin_packages cp ON cp.id = po.package_id
       WHERE 1=1 ${whereStatus}
       ORDER BY po.created_at DESC
       LIMIT ? OFFSET ?`,
      [limit, offset]
    );

    const [[{ total }]] = await db.execute(
      `SELECT COUNT(*) AS total FROM payment_orders po WHERE 1=1 ${whereStatus}`
    );

    res.json({ payments: rows, total, limit, offset });
  } catch (err) {
    console.error('[admin/payments]', err);
    res.status(500).json({ error: 'Error al obtener pagos' });
  }
});

/* ──────────────────────────────────────────────────────────────────
   GESTIÓN DE TIENDA WEB (web_shop_items)
─────────────────────────────────────────────────────────────────── */

/* GET /api/admin/shop-items — Lista TODOS los items (activos + inactivos) */
router.get('/shop-items', auth, adminOnly, async (req, res) => {
  try {
    const [rows] = await db.execute(
      `SELECT id, name, description, item_id, item_count, price_coins,
              price_adena, category, image_url, featured, stock, active
       FROM web_shop_items
       ORDER BY active DESC, id ASC`
    );
    res.json({ items: rows });
  } catch (err) {
    console.error('[admin/shop-items GET]', err);
    res.status(500).json({ error: 'Error al obtener items de tienda' });
  }
});

/* POST /api/admin/shop-items — Crear nuevo item */
router.post('/shop-items', auth, adminOnly, async (req, res) => {
  const { name, description, item_id, item_count, price_coins,
          price_adena, category, image_url, featured, active, stock } = req.body;

  if (!name?.trim())
    return res.status(400).json({ error: 'El nombre es requerido' });
  if (!item_id || parseInt(item_id) <= 0)
    return res.status(400).json({ error: 'item_id de L2 es requerido y debe ser > 0' });
  if (parseInt(item_count) < 1)
    return res.status(400).json({ error: 'item_count debe ser al menos 1' });
  if (parseInt(price_coins) < 0)
    return res.status(400).json({ error: 'price_coins no puede ser negativo' });

  try {
    const [result] = await db.execute(
      `INSERT INTO web_shop_items
         (name, description, item_id, item_count, price_coins, price_adena,
          category, image_url, featured, active, stock)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        name.trim(),
        description?.trim() || null,
        parseInt(item_id),
        parseInt(item_count) || 1,
        parseInt(price_coins) || 0,
        parseInt(price_adena) || 0,
        category?.trim() || 'general',
        image_url?.trim() || null,
        featured ? 1 : 0,
        active !== false ? 1 : 0,
        stock !== undefined && stock !== '' ? parseInt(stock) : null
      ]
    );
    res.status(201).json({ ok: true, id: result.insertId });
  } catch (err) {
    console.error('[admin/shop-items POST]', err);
    res.status(500).json({ error: 'Error al crear item' });
  }
});

/* PUT /api/admin/shop-items/:id — Editar item existente */
router.put('/shop-items/:id', auth, adminOnly, async (req, res) => {
  const id = parseInt(req.params.id);
  const { name, description, item_id, item_count, price_coins,
          price_adena, category, image_url, featured, active, stock } = req.body;

  if (!name?.trim())
    return res.status(400).json({ error: 'El nombre es requerido' });
  if (!item_id || parseInt(item_id) <= 0)
    return res.status(400).json({ error: 'item_id de L2 es requerido y debe ser > 0' });

  try {
    const [result] = await db.execute(
      `UPDATE web_shop_items SET
         name=?, description=?, item_id=?, item_count=?, price_coins=?,
         price_adena=?, category=?, image_url=?, featured=?, active=?, stock=?
       WHERE id=?`,
      [
        name.trim(),
        description?.trim() || null,
        parseInt(item_id),
        parseInt(item_count) || 1,
        parseInt(price_coins) || 0,
        parseInt(price_adena) || 0,
        category?.trim() || 'general',
        image_url?.trim() || null,
        featured ? 1 : 0,
        active ? 1 : 0,
        stock !== undefined && stock !== '' && stock !== null ? parseInt(stock) : null,
        id
      ]
    );
    if (!result.affectedRows)
      return res.status(404).json({ error: 'Item no encontrado' });
    res.json({ ok: true });
  } catch (err) {
    console.error('[admin/shop-items PUT]', err);
    res.status(500).json({ error: 'Error al actualizar item' });
  }
});

/* DELETE /api/admin/shop-items/:id — Eliminar item permanentemente */
router.delete('/shop-items/:id', auth, adminOnly, async (req, res) => {
  const id = parseInt(req.params.id);
  try {
    const [result] = await db.execute('DELETE FROM web_shop_items WHERE id=?', [id]);
    if (!result.affectedRows)
      return res.status(404).json({ error: 'Item no encontrado' });
    res.json({ ok: true });
  } catch (err) {
    console.error('[admin/shop-items DELETE]', err);
    res.status(500).json({ error: 'Error al eliminar item' });
  }
});

/* ──────────────────────────────────────────────────────────────────
   RECOMPENSAS PvP ZONA
─────────────────────────────────────────────────────────────────── */

/* GET /api/admin/pvpzone-reward — Obtiene configuración + historial */
router.get('/pvpzone-reward', auth, adminOnly, async (req, res) => {
  try {
    // Config
    const [cfgRows] = await db.execute(
      `SELECT \`key\`, \`value\` FROM web_config
       WHERE \`key\` IN ('pvpzone_reward_enabled','pvpzone_reward_coins')`
    );
    const cfg = {};
    cfgRows.forEach(r => { cfg[r.key] = r.value; });

    // Últimas 50 recompensas entregadas
    let history = [];
    try {
      [history] = await db.execute(
        `SELECT char_name, account_name, zone_name, kills_new, coins_awarded, rewarded_at
         FROM pvp_zone_reward_history
         ORDER BY rewarded_at DESC
         LIMIT 50`
      );
    } catch { /* tabla puede no existir aún */ }

    // Totales por personaje (top 20)
    let totals = [];
    try {
      [totals] = await db.execute(
        `SELECT char_name, kills_rewarded, coins_total, last_reward_at
         FROM pvp_zone_reward_log
         ORDER BY coins_total DESC
         LIMIT 20`
      );
    } catch { /* tabla puede no existir aún */ }

    res.json({
      enabled:      cfg['pvpzone_reward_enabled'] === '1',
      coins_per_kill: parseInt(cfg['pvpzone_reward_coins'] || '5', 10),
      history,
      totals
    });
  } catch (err) {
    console.error('[admin/pvpzone-reward GET]', err);
    res.status(500).json({ error: 'Error al obtener configuración' });
  }
});

/* PUT /api/admin/pvpzone-reward — Actualiza configuración */
router.put('/pvpzone-reward', auth, adminOnly, async (req, res) => {
  const { enabled, coins_per_kill } = req.body;

  if (typeof enabled !== 'boolean')
    return res.status(400).json({ error: '"enabled" debe ser true o false' });

  const coins = parseInt(coins_per_kill);
  if (isNaN(coins) || coins < 0)
    return res.status(400).json({ error: '"coins_per_kill" debe ser un número >= 0' });
  if (coins > 10000)
    return res.status(400).json({ error: '"coins_per_kill" no puede superar 10.000' });

  try {
    await db.execute(
      `INSERT INTO web_config (\`key\`, value) VALUES ('pvpzone_reward_enabled', ?)
       ON DUPLICATE KEY UPDATE value = ?`,
      [enabled ? '1' : '0', enabled ? '1' : '0']
    );
    await db.execute(
      `INSERT INTO web_config (\`key\`, value) VALUES ('pvpzone_reward_coins', ?)
       ON DUPLICATE KEY UPDATE value = ?`,
      [String(coins), String(coins)]
    );
    res.json({ ok: true, enabled, coins_per_kill: coins });
  } catch (err) {
    console.error('[admin/pvpzone-reward PUT]', err);
    res.status(500).json({ error: 'Error al guardar configuración' });
  }
});

/* DELETE /api/admin/pvpzone-reward/log — Reinicia el historial (no borra web_config) */
router.delete('/pvpzone-reward/log', auth, adminOnly, async (req, res) => {
  try {
    await db.execute('TRUNCATE TABLE pvp_zone_reward_log').catch(() => {});
    await db.execute('TRUNCATE TABLE pvp_zone_reward_history').catch(() => {});
    res.json({ ok: true, message: 'Historial de recompensas reiniciado' });
  } catch (err) {
    console.error('[admin/pvpzone-reward DELETE]', err);
    res.status(500).json({ error: 'Error al reiniciar historial' });
  }
});

module.exports = router;
