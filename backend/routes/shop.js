const router = require('express').Router();
const db     = require('../config/db');
const auth   = require('../middleware/auth');

/* ─── GET /api/shop/items ──────────────────────────────────────── */
router.get('/items', async (req, res) => {
  try {
    const category = req.query.category;
    let query = `SELECT id, name, description, item_id, item_count,
                        price_coins, price_adena, category, image_url,
                        featured, stock, active
                 FROM web_shop_items WHERE active = 1`;
    const params = [];

    if (category) { query += ' AND category = ?'; params.push(category); }
    query += ' ORDER BY featured DESC, id ASC';

    const [rows] = await db.execute(query, params);
    res.json(rows);
  } catch (err) {
    console.error('[Shop/items]', err.message);
    res.status(500).json({ error: 'Error obteniendo ítems' });
  }
});

/* ─── GET /api/shop/balance ─────────────────────────────────────── */
router.get('/balance', auth, async (req, res) => {
  try {
    const [rows] = await db.execute(
      "SELECT value FROM account_data WHERE account_name=? AND var='web_coins'",
      [req.user.login]
    );
    const coins = rows.length ? parseInt(rows[0].value) || 0 : 0;
    res.json({ coins, login: req.user.login });
  } catch (err) {
    console.error('[Shop/balance]', err.message);
    res.status(500).json({ error: 'Error obteniendo balance' });
  }
});

/* ─── POST /api/shop/purchase ──────────────────────────────────── */
router.post('/purchase', auth, async (req, res) => {
  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();

    const { itemShopId, charName } = req.body;
    if (!itemShopId || !charName)
      return res.status(400).json({ error: 'itemShopId y charName requeridos' });

    // 1. Obtener ítem de la tienda
    const [[item]] = await conn.execute(
      'SELECT * FROM web_shop_items WHERE id=? AND active=1',
      [itemShopId]
    );
    if (!item) return res.status(404).json({ error: 'Ítem no disponible' });
    if (item.stock !== null && item.stock <= 0)
      return res.status(400).json({ error: 'Sin stock disponible' });

    // 2. Verificar que el personaje pertenezca a la cuenta
    const [[char]] = await conn.execute(
      'SELECT charId FROM characters WHERE char_name=? AND account_name=? AND deletetime=0',
      [charName, req.user.login]
    );
    if (!char) return res.status(403).json({ error: 'Personaje no pertenece a tu cuenta' });

    // 3. Verificar y descontar coins
    const [[balRow]] = await conn.execute(
      "SELECT value FROM account_data WHERE account_name=? AND var='web_coins' FOR UPDATE",
      [req.user.login]
    );
    const currentCoins = balRow ? parseInt(balRow.value) || 0 : 0;

    if (item.price_coins > 0 && currentCoins < item.price_coins)
      return res.status(400).json({ error: `Coins insuficientes (necesitas ${item.price_coins}, tienes ${currentCoins})` });

    if (item.price_coins > 0) {
      const newBal = currentCoins - item.price_coins;
      await conn.execute(
        `INSERT INTO account_data (account_name, var, value) VALUES (?, 'web_coins', ?)
         ON DUPLICATE KEY UPDATE value = ?`,
        [req.user.login, newBal, newBal]
      );
    }

    // 4. Entregar ítem al personaje (tabla items de L2JMobius H5)
    const objectId = await generateObjectId(conn);
    await conn.execute(
      `INSERT INTO items (object_id, item_id, owner_id, loc, loc_data, count, enchant_level, custom_type1, custom_type2, mana_left, time)
       VALUES (?, ?, ?, 'INVENTORY', 0, ?, 0, 0, 0, -1, -1)`,
      [objectId, item.item_id, char.charId, item.item_count || 1]
    );

    // 5. Registrar en historial
    await conn.execute(
      `INSERT INTO web_shop_history (account_name, char_name, item_shop_id, item_name, price_coins, item_count)
       VALUES (?, ?, ?, ?, ?, ?)`,
      [req.user.login, charName, itemShopId, item.name, item.price_coins, item.item_count]
    );

    // 6. Reducir stock si aplica
    if (item.stock !== null) {
      await conn.execute('UPDATE web_shop_items SET stock=stock-1 WHERE id=?', [itemShopId]);
    }

    await conn.commit();
    res.json({
      message: `¡${item.name} x${item.item_count} entregado a ${charName}!`,
      remainingCoins: currentCoins - (item.price_coins || 0)
    });

  } catch (err) {
    await conn.rollback();
    console.error('[Shop/purchase]', err.message);
    res.status(500).json({ error: 'Error procesando compra: ' + err.message });
  } finally {
    conn.release();
  }
});

/* ─── GET /api/shop/history ─────────────────────────────────────── */
router.get('/history', auth, async (req, res) => {
  try {
    const [rows] = await db.execute(
      `SELECT item_name, char_name, price_coins, item_count, created_at
       FROM web_shop_history
       WHERE account_name = ?
       ORDER BY created_at DESC
       LIMIT 50`,
      [req.user.login]
    );
    res.json(rows);
  } catch (err) {
    console.error('[Shop/history]', err.message);
    res.status(500).json({ error: 'Error obteniendo historial' });
  }
});

/* Helper: generar object_id único */
async function generateObjectId(conn) {
  const [[row]] = await conn.execute('SELECT MAX(object_id) AS maxId FROM items');
  return (row.maxId || 268500000) + 1;
}

module.exports = router;
