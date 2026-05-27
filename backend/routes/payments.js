/**
 * /api/payments  — Sistema de pagos L2H5
 * Soporta: MercadoPago (ARS) + PayPal (USD)
 */
const router = require('express').Router();
const db     = require('../config/db');
const auth   = require('../middleware/auth');

/* ─── Helpers ──────────────────────────────────────────────────────── */

/** Acredita coins a una cuenta (upsert en account_data) */
async function creditCoins(conn, accountName, coins) {
  await conn.execute(
    `INSERT INTO account_data (account_name, var, value)
     VALUES (?, 'web_coins', ?)
     ON DUPLICATE KEY UPDATE value = CAST(CAST(value AS UNSIGNED) + ? AS CHAR)`,
    [accountName, coins, coins]
  );
}

/** Registra la orden en BD */
async function createOrder(conn, accountName, packageId, coins, amount, currency, provider) {
  const [result] = await conn.execute(
    `INSERT INTO payment_orders
       (account_name, package_id, coins, amount, currency, provider, status)
     VALUES (?, ?, ?, ?, ?, ?, 'pending')`,
    [accountName, packageId, coins, amount, currency, provider]
  );
  return result.insertId;
}

/** Valida que el paquete exista y devuelve sus datos */
async function getPackage(packageId) {
  const [[pkg]] = await db.execute(
    'SELECT * FROM coin_packages WHERE id = ? AND active = 1',
    [packageId]
  );
  return pkg;
}

/* ══════════════════════════════════════════════════════════════════════
   MERCADOPAGO
══════════════════════════════════════════════════════════════════════ */

/* ─── POST /api/payments/mp/create ─────────────────────────────────── */
router.post('/mp/create', auth, async (req, res) => {
  try {
    const { packageId } = req.body;
    if (!packageId) return res.status(400).json({ error: 'packageId requerido' });

    const pkg = await getPackage(packageId);
    if (!pkg) return res.status(404).json({ error: 'Paquete no encontrado' });
    if (!pkg.price_ars) return res.status(400).json({ error: 'Paquete sin precio ARS' });

    const { MercadoPagoConfig, Preference } = require('mercadopago');
    const client = new MercadoPagoConfig({ accessToken: process.env.MP_ACCESS_TOKEN });
    const preference = new Preference(client);

    // Guardar orden pendiente
    const conn = await db.getConnection();
    let orderId;
    try {
      orderId = await createOrder(
        conn, req.user.login, packageId,
        pkg.coins, pkg.price_ars, 'ARS', 'mercadopago'
      );
    } finally { conn.release(); }

    const pref = await preference.create({
      body: {
        items: [{
          id:          String(pkg.id),
          title:       `${pkg.name} — ${pkg.coins} WebCoins`,
          description: pkg.description || '',
          quantity:    1,
          currency_id: 'ARS',
          unit_price:  parseFloat(pkg.price_ars)
        }],
        external_reference: String(orderId),
        back_urls: {
          success: `${process.env.APP_BASE_URL}/payment/success?provider=mp`,
          failure: `${process.env.APP_BASE_URL}/payment/failure?provider=mp`,
          pending: `${process.env.APP_BASE_URL}/payment/pending?provider=mp`
        },
        auto_return:         'approved',
        notification_url:    `${process.env.APP_BASE_URL}/api/payments/mp/webhook`,
        statement_descriptor:'L2H5 WebCoins',
        metadata: { order_id: orderId, account: req.user.login }
      }
    });

    // Guardar preference_id en la orden
    await db.execute(
      'UPDATE payment_orders SET provider_id = ? WHERE id = ?',
      [pref.id, orderId]
    );

    res.json({
      orderId,
      preferenceId: pref.id,
      initPoint:    pref.init_point,       // redirect a MP Checkout
      publicKey:    process.env.MP_PUBLIC_KEY
    });

  } catch (err) {
    console.error('[MP/create]', err.message);
    res.status(500).json({ error: 'Error creando preferencia de pago' });
  }
});

/* ─── POST /api/payments/mp/webhook ────────────────────────────────── */
router.post('/mp/webhook', async (req, res) => {
  // MP envía notificación — respondemos 200 inmediatamente
  res.sendStatus(200);

  try {
    const { type, data } = req.body;
    if (type !== 'payment') return;

    const { MercadoPagoConfig, Payment } = require('mercadopago');
    const client  = new MercadoPagoConfig({ accessToken: process.env.MP_ACCESS_TOKEN });
    const payment = new Payment(client);

    const mpPayment = await payment.get({ id: data.id });
    const orderId   = parseInt(mpPayment.external_reference);
    const status    = mpPayment.status; // approved | rejected | pending | cancelled

    const conn = await db.getConnection();
    try {
      await conn.beginTransaction();

      // Verificar orden
      const [[order]] = await conn.execute(
        'SELECT * FROM payment_orders WHERE id = ? FOR UPDATE',
        [orderId]
      );
      if (!order || order.status !== 'pending') {
        await conn.rollback();
        return;
      }

      // Mapear estado
      const dbStatus = {
        approved:  'approved',
        rejected:  'rejected',
        cancelled: 'cancelled',
        refunded:  'refunded'
      }[status] || 'pending';

      await conn.execute(
        `UPDATE payment_orders
         SET status = ?, provider_id = ?, metadata = ?, updated_at = NOW()
         WHERE id = ?`,
        [dbStatus, String(mpPayment.id), JSON.stringify(mpPayment), orderId]
      );

      // Acreditar coins solo si fue aprobado
      if (dbStatus === 'approved') {
        await creditCoins(conn, order.account_name, order.coins);
        console.log(`✅ MP: +${order.coins} coins → ${order.account_name} (orden #${orderId})`);
      }

      await conn.commit();
    } catch (e) {
      await conn.rollback();
      throw e;
    } finally {
      conn.release();
    }
  } catch (err) {
    console.error('[MP/webhook]', err.message);
  }
});

/* ─── GET /api/payments/mp/status/:orderId ──────────────────────────── */
router.get('/mp/status/:orderId', auth, async (req, res) => {
  try {
    const [[order]] = await db.execute(
      'SELECT id, status, coins, amount, currency, created_at FROM payment_orders WHERE id = ? AND account_name = ?',
      [req.params.orderId, req.user.login]
    );
    if (!order) return res.status(404).json({ error: 'Orden no encontrada' });
    res.json(order);
  } catch (err) {
    console.error('[MP/status]', err.message);
    res.status(500).json({ error: 'Error consultando estado' });
  }
});

/* ══════════════════════════════════════════════════════════════════════
   PAYPAL
══════════════════════════════════════════════════════════════════════ */

function getPaypalClient() {
  const paypal = require('@paypal/checkout-server-sdk');
  const env = process.env.PAYPAL_MODE === 'live'
    ? new paypal.core.LiveEnvironment(process.env.PAYPAL_CLIENT_ID, process.env.PAYPAL_CLIENT_SECRET)
    : new paypal.core.SandboxEnvironment(process.env.PAYPAL_CLIENT_ID, process.env.PAYPAL_CLIENT_SECRET);
  return new paypal.core.PayPalHttpClient(env);
}

/* ─── POST /api/payments/paypal/create ──────────────────────────────── */
router.post('/paypal/create', auth, async (req, res) => {
  try {
    const { packageId } = req.body;
    if (!packageId) return res.status(400).json({ error: 'packageId requerido' });

    const pkg = await getPackage(packageId);
    if (!pkg) return res.status(404).json({ error: 'Paquete no encontrado' });
    if (!pkg.price_usd) return res.status(400).json({ error: 'Paquete sin precio USD' });

    // Guardar orden pendiente
    const conn = await db.getConnection();
    let orderId;
    try {
      orderId = await createOrder(
        conn, req.user.login, packageId,
        pkg.coins, pkg.price_usd, 'USD', 'paypal'
      );
    } finally { conn.release(); }

    const paypal  = require('@paypal/checkout-server-sdk');
    const request = new paypal.orders.OrdersCreateRequest();
    request.prefer('return=representation');
    request.requestBody({
      intent: 'CAPTURE',
      purchase_units: [{
        reference_id:  String(orderId),
        description:   `${pkg.name} — ${pkg.coins} WebCoins (L2H5)`,
        amount: {
          currency_code: 'USD',
          value:         parseFloat(pkg.price_usd).toFixed(2)
        }
      }],
      application_context: {
        brand_name:          'L2H5 Web Panel',
        landing_page:        'BILLING',
        user_action:         'PAY_NOW',
        return_url: `${process.env.APP_BASE_URL}/payment/success?provider=paypal&order=${orderId}`,
        cancel_url: `${process.env.APP_BASE_URL}/payment/failure?provider=paypal&order=${orderId}`
      }
    });

    const order = await getPaypalClient().execute(request);

    // Guardar PayPal order ID
    await db.execute(
      'UPDATE payment_orders SET provider_id = ? WHERE id = ?',
      [order.result.id, orderId]
    );

    // Extraer link de aprobación
    const approveLink = order.result.links.find(l => l.rel === 'approve');

    res.json({
      orderId,
      paypalOrderId: order.result.id,
      approveUrl:    approveLink?.href
    });

  } catch (err) {
    console.error('[PayPal/create]', err.message);
    res.status(500).json({ error: 'Error creando orden PayPal' });
  }
});

/* ─── POST /api/payments/paypal/capture ────────────────────────────── */
router.post('/paypal/capture', auth, async (req, res) => {
  try {
    const { paypalOrderId, orderId } = req.body;
    if (!paypalOrderId || !orderId)
      return res.status(400).json({ error: 'paypalOrderId y orderId requeridos' });

    const paypal  = require('@paypal/checkout-server-sdk');
    const request = new paypal.orders.OrdersCaptureRequest(paypalOrderId);
    request.requestBody({});

    const capture = await getPaypalClient().execute(request);
    const captureStatus = capture.result.status; // COMPLETED | VOIDED

    const conn = await db.getConnection();
    try {
      await conn.beginTransaction();

      const [[order]] = await conn.execute(
        'SELECT * FROM payment_orders WHERE id = ? AND account_name = ? FOR UPDATE',
        [orderId, req.user.login]
      );
      if (!order) { await conn.rollback(); return res.status(404).json({ error: 'Orden no encontrada' }); }
      if (order.status !== 'pending') { await conn.rollback(); return res.json({ status: order.status }); }

      const dbStatus = captureStatus === 'COMPLETED' ? 'approved' : 'rejected';

      await conn.execute(
        `UPDATE payment_orders
         SET status = ?, metadata = ?, updated_at = NOW()
         WHERE id = ?`,
        [dbStatus, JSON.stringify(capture.result), orderId]
      );

      if (dbStatus === 'approved') {
        await creditCoins(conn, req.user.login, order.coins);
        console.log(`✅ PayPal: +${order.coins} coins → ${req.user.login} (orden #${orderId})`);
      }

      await conn.commit();
      res.json({
        status:  dbStatus,
        coins:   dbStatus === 'approved' ? order.coins : 0,
        message: dbStatus === 'approved'
          ? `¡Pago aprobado! Se acreditaron ${order.coins} WebCoins a tu cuenta.`
          : 'El pago no pudo completarse.'
      });

    } catch (e) {
      await conn.rollback();
      throw e;
    } finally {
      conn.release();
    }
  } catch (err) {
    console.error('[PayPal/capture]', err.message);
    res.status(500).json({ error: 'Error capturando pago PayPal' });
  }
});

/* ══════════════════════════════════════════════════════════════════════
   ENDPOINTS COMPARTIDOS
══════════════════════════════════════════════════════════════════════ */

/* ─── GET /api/payments/packages ────────────────────────────────────── */
router.get('/packages', async (req, res) => {
  try {
    const [rows] = await db.execute(
      `SELECT id, name, description, coins, price_ars, price_usd,
              bonus_pct, featured
       FROM coin_packages
       WHERE active = 1
       ORDER BY sort_order ASC`
    );
    res.json(rows);
  } catch (err) {
    console.error('[Payments/packages]', err.message);
    res.status(500).json({ error: 'Error obteniendo paquetes' });
  }
});

/* ─── GET /api/payments/history ─────────────────────────────────────── */
router.get('/history', auth, async (req, res) => {
  try {
    const [rows] = await db.execute(
      `SELECT po.id, cp.name AS package_name, po.coins, po.amount,
              po.currency, po.provider, po.status, po.created_at
       FROM payment_orders po
       JOIN coin_packages cp ON cp.id = po.package_id
       WHERE po.account_name = ?
       ORDER BY po.created_at DESC
       LIMIT 30`,
      [req.user.login]
    );
    res.json(rows);
  } catch (err) {
    console.error('[Payments/history]', err.message);
    res.status(500).json({ error: 'Error obteniendo historial' });
  }
});

module.exports = router;
