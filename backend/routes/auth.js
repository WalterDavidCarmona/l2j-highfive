const router   = require('express').Router();
const db       = require('../config/db');
const jwt      = require('jsonwebtoken');
const { hashPassword, verifyPassword } = require('../config/hashUtils');

const JWT_SECRET  = process.env.JWT_SECRET  || 'l2h5_secret';
const JWT_EXPIRES = process.env.JWT_EXPIRES_IN || '7d';
const MAX_CHARS   = parseInt(process.env.MAX_CHARS_PER_ACCOUNT) || 3;

/* ─── Helpers ─────────────────────────────────────────────────── */
function signToken(payload) {
  return jwt.sign(payload, JWT_SECRET, { expiresIn: JWT_EXPIRES });
}

function sanitizeLogin(login) {
  return login.trim().toLowerCase().replace(/[^a-z0-9_]/g, '');
}

/* ─── POST /api/auth/register ──────────────────────────────────── */
router.post('/register', async (req, res) => {
  try {
    let { login, password, email } = req.body;

    if (!login || !password)
      return res.status(400).json({ error: 'login y password requeridos' });

    login = sanitizeLogin(login);
    if (login.length < 4 || login.length > 14)
      return res.status(400).json({ error: 'login debe tener entre 4 y 14 caracteres' });

    if (password.length < 6)
      return res.status(400).json({ error: 'password mínimo 6 caracteres' });

    // Verificar si ya existe
    const [rows] = await db.execute(
      'SELECT login FROM accounts WHERE login = ?', [login]
    );
    if (rows.length > 0)
      return res.status(409).json({ error: 'Ese nombre de cuenta ya existe' });

    // Normalizar IP (IPv4-mapped IPv6 → IPv4)
    const rawIp  = req.ip || req.connection?.remoteAddress || '0.0.0.0';
    const lastIP = rawIp.replace(/^::ffff:/, '');

    // Verificar límite de cuentas por IP (máximo 5)
    const [[{ ipCount }]] = await db.execute(
      'SELECT COUNT(*) AS ipCount FROM accounts WHERE lastIP = ?', [lastIP]
    );
    if (ipCount >= 5)
      return res.status(403).json({
        error: 'Límite de cuentas alcanzado para tu IP (máximo 5). Contacta al staff si necesitas más.'
      });

    const hashed = hashPassword(password);
    const now    = Date.now();

    await db.execute(
      'INSERT INTO accounts (login, password, lastactive, accessLevel, lastIP) VALUES (?, ?, ?, 0, ?)',
      [login, hashed, now, lastIP]
    );

    const token = signToken({ login, accessLevel: 0 });
    res.status(201).json({ message: 'Cuenta creada exitosamente', token, login });

  } catch (err) {
    console.error('[Register]', err.message);
    res.status(500).json({ error: 'Error interno del servidor' });
  }
});

/* ─── POST /api/auth/login ─────────────────────────────────────── */
router.post('/login', async (req, res) => {
  try {
    let { login, password } = req.body;
    if (!login || !password)
      return res.status(400).json({ error: 'login y password requeridos' });

    login = sanitizeLogin(login);

    const [rows] = await db.execute(
      `SELECT login, password,
              IF(? > value OR value IS NULL, accessLevel, -1) AS accessLevel,
              lastServer
       FROM accounts
       LEFT JOIN account_data
         ON account_data.account_name = accounts.login
         AND account_data.var = 'ban_temp'
       WHERE login = ?`,
      [Date.now(), login]
    );

    if (rows.length === 0)
      return res.status(401).json({ error: 'Cuenta o contraseña incorrecta' });

    const acc = rows[0];

    if (acc.accessLevel < 0)
      return res.status(403).json({ error: 'Cuenta suspendida temporalmente' });

    if (!verifyPassword(password, acc.password))
      return res.status(401).json({ error: 'Cuenta o contraseña incorrecta' });

    // Actualizar lastactive + lastIP
    await db.execute(
      'UPDATE accounts SET lastactive = ?, lastIP = ? WHERE login = ?',
      [Date.now(), req.ip || '0.0.0.0', login]
    );

    const token = signToken({ login, accessLevel: acc.accessLevel });
    res.json({ token, login, accessLevel: acc.accessLevel, lastServer: acc.lastServer });

  } catch (err) {
    console.error('[Login]', err.message);
    res.status(500).json({ error: 'Error interno del servidor' });
  }
});

/* ─── GET /api/auth/me ─────────────────────────────────────────── */
const auth = require('../middleware/auth');
router.get('/me', auth, async (req, res) => {
  try {
    const { login } = req.user;

    const [accRows] = await db.execute(
      'SELECT login, accessLevel, lastactive, lastIP FROM accounts WHERE login = ?',
      [login]
    );
    if (accRows.length === 0)
      return res.status(404).json({ error: 'Cuenta no encontrada' });

    const account = accRows[0];

    // Personajes de la cuenta
    const [chars] = await db.execute(
      `SELECT charId, char_name, level, race, classid, online,
              pvpkills, pkkills, title, title_color, createDate
       FROM characters
       WHERE account_name = ? AND deletetime = 0
       ORDER BY createDate ASC
       LIMIT ${MAX_CHARS}`,
      [login]
    );

    res.json({ account, characters: chars });
  } catch (err) {
    console.error('[Me]', err.message);
    res.status(500).json({ error: 'Error interno' });
  }
});

/* ─── POST /api/auth/change-password ──────────────────────────── */
router.post('/change-password', auth, async (req, res) => {
  try {
    const { login } = req.user;
    const { currentPassword, newPassword } = req.body;

    if (!currentPassword || !newPassword)
      return res.status(400).json({ error: 'Campos requeridos' });

    if (newPassword.length < 6)
      return res.status(400).json({ error: 'Nueva contraseña mínimo 6 caracteres' });

    const [rows] = await db.execute('SELECT password FROM accounts WHERE login = ?', [login]);
    if (!rows.length) return res.status(404).json({ error: 'Cuenta no encontrada' });

    if (!verifyPassword(currentPassword, rows[0].password))
      return res.status(401).json({ error: 'Contraseña actual incorrecta' });

    await db.execute(
      'UPDATE accounts SET password = ? WHERE login = ?',
      [hashPassword(newPassword), login]
    );

    res.json({ message: 'Contraseña actualizada correctamente' });
  } catch (err) {
    console.error('[ChangePass]', err.message);
    res.status(500).json({ error: 'Error interno' });
  }
});

module.exports = router;
