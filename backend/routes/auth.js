const router   = require('express').Router();
const db       = require('../config/db');
const jwt      = require('jsonwebtoken');
const { hashPassword, verifyPassword } = require('../config/hashUtils');

const JWT_SECRET  = process.env.JWT_SECRET  || 'l2h5_secret';
const JWT_EXPIRES = process.env.JWT_EXPIRES_IN || '7d';
const MAX_CHARS   = parseInt(process.env.MAX_CHARS_PER_ACCOUNT) || 3;

/* ─── Helpers ──────────────────────────────────────────────────── */
function signToken(payload) {
  return jwt.sign(payload, JWT_SECRET, { expiresIn: JWT_EXPIRES });
}

function sanitizeLogin(login) {
  return login.trim().toLowerCase().replace(/[^a-z0-9_]/g, '');
}

function normalizeEmail(email) {
  return (email || '').trim().toLowerCase();
}

/** Valida formato de fecha YYYY-MM-DD y que sea una fecha real pasada */
function isValidBirthday(str) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(str)) return false;
  const d = new Date(str + 'T00:00:00');
  if (isNaN(d.getTime())) return false;
  const now = new Date();
  const minAge = new Date(now.getFullYear() - 100, now.getMonth(), now.getDate());
  if (d < minAge) return false;         // más de 100 años atrás
  if (d >= now)   return false;         // fecha futura
  return true;
}

/* ─── POST /api/auth/register ─────────────────────────────────── */
router.post('/register', async (req, res) => {
  try {
    let { login, password, email, birthday } = req.body;

    if (!login || !password)
      return res.status(400).json({ error: 'login y password requeridos' });

    login = sanitizeLogin(login);
    if (login.length < 4 || login.length > 14)
      return res.status(400).json({ error: 'login debe tener entre 4 y 14 caracteres' });

    if (password.length < 6)
      return res.status(400).json({ error: 'password mínimo 6 caracteres' });

    // Email obligatorio
    const emailClean = normalizeEmail(email);
    if (!emailClean || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailClean))
      return res.status(400).json({ error: 'Correo electrónico no válido' });

    // Fecha de nacimiento obligatoria
    if (!birthday || !isValidBirthday(birthday))
      return res.status(400).json({ error: 'Fecha de nacimiento no válida' });

    // Verificar si login ya existe
    const [rows] = await db.execute(
      'SELECT login FROM accounts WHERE login = ?', [login]
    );
    if (rows.length > 0)
      return res.status(409).json({ error: 'Ese nombre de cuenta ya existe' });

    // Verificar si el email ya está registrado
    const [emailRows] = await db.execute(
      'SELECT login FROM accounts WHERE email = ?', [emailClean]
    );
    if (emailRows.length > 0)
      return res.status(409).json({ error: 'Ese correo ya está registrado en otra cuenta' });

    // Normalizar IP
    const rawIp  = req.ip || req.connection?.remoteAddress || '0.0.0.0';
    const lastIP = rawIp.replace(/^::ffff:/, '');

    // Límite cuentas por IP
    const [[{ ipCount }]] = await db.execute(
      'SELECT COUNT(*) AS ipCount FROM accounts WHERE lastIP = ?', [lastIP]
    );
    if (ipCount >= 5)
      return res.status(403).json({
        error: 'Límite de cuentas alcanzado para tu IP (máximo 5). Contacta al staff.'
      });

    const hashed = hashPassword(password);
    const now    = Date.now();

    await db.execute(
      `INSERT INTO accounts (login, password, email, birthday, lastactive, accessLevel, lastIP)
       VALUES (?, ?, ?, ?, ?, 0, ?)`,
      [login, hashed, emailClean, birthday, now, lastIP]
    );

    const token = signToken({ login, accessLevel: 0 });
    res.status(201).json({ message: 'Cuenta creada exitosamente', token, login });

  } catch (err) {
    console.error('[Register]', err.message);
    // Error de columna inexistente → guiar al admin
    if (err.code === 'ER_BAD_FIELD_ERROR')
      return res.status(500).json({
        error: 'Error de BD: ejecuta el script de migración (ALTER TABLE accounts ADD email/birthday)'
      });
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
      'SELECT login, accessLevel, lastactive, lastIP, email, birthday FROM accounts WHERE login = ?',
      [login]
    );
    if (accRows.length === 0)
      return res.status(404).json({ error: 'Cuenta no encontrada' });

    const account = accRows[0];
    // Ocultar parte del email para el frontend (privacidad)
    if (account.email) {
      const [user, domain] = account.email.split('@');
      account.emailMasked = user.slice(0, 2) + '***@' + domain;
    }

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

/* ─── POST /api/auth/recover-password ─────────────────────────── */
/* Sin autenticación — verifica email + birthday antes de cambiar  */
router.post('/recover-password', async (req, res) => {
  try {
    const { email, birthday, newPassword } = req.body;

    if (!email || !birthday || !newPassword)
      return res.status(400).json({ error: 'Todos los campos son requeridos' });

    if (newPassword.length < 6)
      return res.status(400).json({ error: 'La nueva contraseña debe tener al menos 6 caracteres' });

    const emailClean = normalizeEmail(email);
    if (!isValidBirthday(birthday))
      return res.status(400).json({ error: 'Formato de fecha inválido' });

    // Buscar cuenta que coincida exactamente con email + birthday
    const [rows] = await db.execute(
      'SELECT login FROM accounts WHERE email = ? AND birthday = ?',
      [emailClean, birthday]
    );

    if (rows.length === 0)
      return res.status(403).json({
        error: 'Los datos no coinciden con ninguna cuenta registrada'
      });

    const login  = rows[0].login;
    const hashed = hashPassword(newPassword);

    await db.execute(
      'UPDATE accounts SET password = ? WHERE login = ?',
      [hashed, login]
    );

    res.json({ message: 'Contraseña actualizada correctamente', login });

  } catch (err) {
    console.error('[RecoverPassword]', err.message);
    res.status(500).json({ error: 'Error interno del servidor' });
  }
});

/* ─── POST /api/auth/change-password ──────────────────────────── */
/* Requiere autenticación + verificación email + birthday + contraseña actual */
router.post('/change-password', auth, async (req, res) => {
  try {
    const { login } = req.user;
    const { currentPassword, email, birthday, newPassword } = req.body;

    if (!currentPassword || !email || !birthday || !newPassword)
      return res.status(400).json({ error: 'Todos los campos son requeridos' });

    if (newPassword.length < 6)
      return res.status(400).json({ error: 'Nueva contraseña mínimo 6 caracteres' });

    const emailClean = normalizeEmail(email);
    if (!isValidBirthday(birthday))
      return res.status(400).json({ error: 'Formato de fecha de nacimiento inválido' });

    const [rows] = await db.execute(
      'SELECT password, email, birthday FROM accounts WHERE login = ?',
      [login]
    );
    if (!rows.length) return res.status(404).json({ error: 'Cuenta no encontrada' });

    const acc = rows[0];

    // Verificar contraseña actual
    if (!verifyPassword(currentPassword, acc.password))
      return res.status(401).json({ error: 'Contraseña actual incorrecta' });

    // La nueva contraseña no puede ser igual a la actual
    if (verifyPassword(newPassword, acc.password))
      return res.status(400).json({ error: 'La nueva contraseña no puede ser igual a la actual' });

    // Verificar email
    if (normalizeEmail(acc.email) !== emailClean)
      return res.status(403).json({ error: 'El correo electrónico no coincide con el registrado' });

    // Verificar birthday (comparar como string YYYY-MM-DD)
    const storedBirthday = acc.birthday instanceof Date
      ? acc.birthday.toISOString().slice(0, 10)
      : String(acc.birthday).slice(0, 10);

    if (storedBirthday !== birthday)
      return res.status(403).json({ error: 'La fecha de nacimiento no coincide con la registrada' });

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
