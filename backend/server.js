require('dotenv').config();
const express    = require('express');
const cors       = require('cors');
const helmet     = require('helmet');
const rateLimit  = require('express-rate-limit');
const path       = require('path');

const app = express();
const PORT = process.env.PORT || 3000;

/* ─── Seguridad ────────────────────────────────────────────────── */
app.use(helmet({ contentSecurityPolicy: false }));
app.use(cors({
  origin: process.env.FRONTEND_URL || '*',
  credentials: true
}));

const limiter = rateLimit({ windowMs: 15 * 60 * 1000, max: 200 });
const authLimiter = rateLimit({ windowMs: 15 * 60 * 1000, max: 20,
  message: { error: 'Demasiados intentos, espera 15 minutos' }
});

app.use(limiter);
app.use(express.json({ limit: '2mb' }));
app.use(express.urlencoded({ extended: true }));

/* ─── Servir frontend estático ─────────────────────────────────── */
app.use(express.static(path.join(__dirname, '..', 'frontend')));

/* ─── Rutas API ─────────────────────────────────────────────────── */
app.use('/api/auth',     authLimiter, require('./routes/auth'));
app.use('/api/rankings', require('./routes/rankings'));
app.use('/api/news',     require('./routes/news'));
app.use('/api/shop',     require('./routes/shop'));
app.use('/api/server',   require('./routes/server'));
app.use('/api/payments', require('./routes/payments'));
app.use('/api/bets',     require('./routes/bets').router);
app.use('/api/admin',    require('./routes/admin'));

/* ─── Health check ─────────────────────────────────────────────── */
app.get('/api/health', (req, res) =>
  res.json({ status: 'ok', timestamp: new Date().toISOString() })
);

/* ─── Cualquier otra ruta → frontend SPA ──────────────────────── */
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, '..', 'frontend', 'index.html'));
});

/* ─── Iniciar ───────────────────────────────────────────────────── */
app.listen(PORT, () => {
  console.log(`\n🗡️  L2H5 Web Server corriendo en http://localhost:${PORT}`);
  console.log(`📡  API disponible en http://localhost:${PORT}/api`);
  console.log(`⚙️  Entorno: ${process.env.NODE_ENV || 'development'}\n`);

  // Iniciar worker de recompensas PvP Zona
  try {
    require('./workers/pvpRewardWorker').start();
  } catch (err) {
    console.error('[PvpReward] No se pudo iniciar el worker:', err.message);
  }
});

module.exports = app;
