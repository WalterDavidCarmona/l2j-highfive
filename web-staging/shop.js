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

/* ─── POST /api/shop/purchase  (ítem individual — legado) ──────── */
router.post('/purchase', auth, async (req, res) => {
  const { itemShopId, charName, qty = 1 } = req.body;
  if (!itemShopId || !charName)
    return res.status(400).json({ error: 'itemShopId y charName requeridos' });

  const quantity = Math.max(1, parseInt(qty) || 1);

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();
    await processPurchase(conn, req.user.login, charName, [{ id: itemShopId, qty: quantity }]);
    await conn.commit();

    const [[balRow]] = await conn.execute(
      "SELECT CAST(COALESCE(value,'0') AS UNSIGNED) AS coins FROM account_data WHERE account_name=? AND var='web_coins'",
      [req.user.login]
    );
    res.json({
      message: `¡Compra procesada! Revisa tu inventario en el juego en unos segundos.`,
      remainingCoins: balRow ? balRow.coins : 0
    });
  } catch (err) {
    await conn.rollback();
    console.error('[Shop/purchase]', err.message);
    res.status(err.status || 500).json({ error: err.message || 'Error procesando compra' });
  } finally {
    conn.release();
  }
});

/* ─── POST /api/shop/cart-checkout ─────────────────────────────── */
/*  body: {
      charName: "NombrePersonaje",
      items: [ { id: shopItemId, qty: 2 }, { id: shopItemId2, qty: 1 }, ... ]
    }
*/
router.post('/cart-checkout', auth, async (req, res) => {
  const { charName, items } = req.body;

  if (!charName || !Array.isArray(items) || !items.length)
    return res.status(400).json({ error: 'charName e items[] requeridos' });

  if (items.length > 20)
    return res.status(400).json({ error: 'Máximo 20 ítems distintos por compra' });

  // Validar que cada qty sea positivo
  for (const it of items) {
    const q = parseInt(it.qty);
    if (!it.id || isNaN(q) || q < 1 || q > 999)
      return res.status(400).json({ error: `Cantidad inválida para ítem ${it.id}` });
  }

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();
    const { totalCoins, summary } = await processPurchase(conn, req.user.login, charName, items);
    await conn.commit();

    const [[balRow]] = await conn.execute(
      "SELECT CAST(COALESCE(value,'0') AS UNSIGNED) AS coins FROM account_data WHERE account_name=? AND var='web_coins'",
      [req.user.login]
    );

    res.json({
      ok: true,
      message: `¡Compra exitosa! ${summary}. Recibirás los ítems en tu inventario en unos segundos.`,
      spent: totalCoins,
      remainingCoins: balRow ? balRow.coins : 0
    });
  } catch (err) {
    await conn.rollback();
    console.error('[Shop/cart-checkout]', err.message);
    res.status(err.status || 500).json({ error: err.message || 'Error procesando carrito' });
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

/* ══════════════════════════════════════════════════════════════════
   HELPER: processPurchase
   Ejecuta toda la lógica de compra dentro de una transacción existente.
   items = [ { id: shopItemId, qty: número } ]
   Lanza errores con .status para que el caller devuelva el código HTTP correcto.
══════════════════════════════════════════════════════════════════ */
async function processPurchase(conn, accountName, charName, items) {
  // 1. Verificar personaje
  const [[char]] = await conn.execute(
    'SELECT charId FROM characters WHERE char_name=? AND account_name=? AND deletetime=0',
    [charName, accountName]
  );
  if (!char) { const e = new Error('Personaje no pertenece a tu cuenta'); e.status = 403; throw e; }

  // 2. Obtener todos los shop items pedidos
  const ids = [...new Set(items.map(i => parseInt(i.id)))];
  const [shopItems] = await conn.execute(
    `SELECT * FROM web_shop_items WHERE id IN (${ids.map(() => '?').join(',')}) AND active=1`,
    ids
  );
  const shopMap = Object.fromEntries(shopItems.map(s => [s.id, s]));

  // 3. Calcular costo total y validar stock
  let totalCost = 0;
  const lines = []; // para el resumen del mail

  for (const { id, qty } of items) {
    const shopId = parseInt(id);
    const q      = parseInt(qty);
    const s      = shopMap[shopId];
    if (!s) { const e = new Error(`Ítem #${shopId} no disponible`); e.status = 404; throw e; }
    if (s.stock !== null && s.stock < q) {
      const e = new Error(`Stock insuficiente para "${s.name}" (disponible: ${s.stock})`);
      e.status = 400; throw e;
    }
    totalCost += (s.price_coins || 0) * q;
    lines.push({ shopItem: s, qty: q });
  }

  // 4. Verificar balance
  const [[balRow]] = await conn.execute(
    "SELECT value FROM account_data WHERE account_name=? AND var='web_coins' FOR UPDATE",
    [accountName]
  );
  const currentCoins = balRow ? parseInt(balRow.value) || 0 : 0;

  if (totalCost > 0 && currentCoins < totalCost) {
    const e = new Error(`Coins insuficientes (necesitas ${totalCost.toLocaleString()}, tienes ${currentCoins.toLocaleString()})`);
    e.status = 400; throw e;
  }

  // 5. Descontar coins
  if (totalCost > 0) {
    const newBal = currentCoins - totalCost;
    await conn.execute(
      `INSERT INTO account_data (account_name, var, value) VALUES (?, 'web_coins', ?)
       ON DUPLICATE KEY UPDATE value = ?`,
      [accountName, newBal, newBal]
    );
  }

  // 6. Entregar ítems via custom_mail (un correo por compra con todos los ítems)
  //    Formato: "itemId totalCantidad;itemId2 totalCantidad2"
  const mailItemsStr = lines
    .map(({ shopItem, qty }) => `${shopItem.item_id} ${(shopItem.item_count || 1) * qty}`)
    .join(';');

  const summaryText = lines
    .map(({ shopItem, qty }) => `${shopItem.name} x${(shopItem.item_count || 1) * qty}`)
    .join(', ');

  await conn.execute(
    `INSERT INTO custom_mail (receiver, subject, message, items) VALUES (?, ?, ?, ?)`,
    [
      char.charId,
      'Tienda Web — Tu pedido',
      `Muchas gracias por tu compra. Tu pedido incluye: ${summaryText}`,
      mailItemsStr
    ]
  );

  // 7. Historial + stock por cada línea
  for (const { shopItem, qty } of lines) {
    await conn.execute(
      `INSERT INTO web_shop_history (account_name, char_name, item_shop_id, item_name, price_coins, item_count)
       VALUES (?, ?, ?, ?, ?, ?)`,
      [accountName, charName, shopItem.id, shopItem.name,
       (shopItem.price_coins || 0) * qty, (shopItem.item_count || 1) * qty]
    );
    if (shopItem.stock !== null) {
      await conn.execute('UPDATE web_shop_items SET stock=stock-? WHERE id=?', [qty, shopItem.id]);
    }
  }

  return { totalCoins: totalCost, summary: summaryText };
}

module.exports = router;
