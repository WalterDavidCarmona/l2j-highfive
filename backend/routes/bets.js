/**
 * /api/bets  — Sistema de Apuestas Olimpiada
 * Aposta por el futuro Héroe de la temporada.
 * Payout: 1–20 monedas según cuán acertada y arriesgada fue tu apuesta.
 */
const router = require('express').Router();
const db     = require('../config/db');
const auth   = require('../middleware/auth');

/* ──────────────────────────────────────────────────────────────────────
   SSE — Clientes conectados para tiempo real
────────────────────────────────────────────────────────────────────── */
const sseClients = new Set();

function broadcastBetUpdate(data) {
  const msg = `data: ${JSON.stringify(data)}\n\n`;
  sseClients.forEach(res => {
    try { res.write(msg); } catch { sseClients.delete(res); }
  });
}

/* GET /api/bets/live  — SSE stream */
router.get('/live', (req, res) => {
  res.setHeader('Content-Type',  'text/event-stream');
  res.setHeader('Cache-Control', 'no-cache');
  res.setHeader('Connection',    'keep-alive');
  res.setHeader('X-Accel-Buffering', 'no');
  res.flushHeaders();

  res.write(': connected\n\n');
  sseClients.add(res);

  // Heartbeat cada 25 s para evitar timeout
  const hb = setInterval(() => {
    try { res.write(': ping\n\n'); } catch { /* ignore */ }
  }, 25000);

  req.on('close', () => {
    clearInterval(hb);
    sseClients.delete(res);
  });
});

/* ──────────────────────────────────────────────────────────────────────
   HELPERS
────────────────────────────────────────────────────────────────────── */

/** Calcula el payout que recibirá un ganador:
 *  - Base = floor(total_bets_in_season / bets_on_winner)
 *  - Multiplicado por las monedas apostadas
 *  - Clamped entre 1 y 20
 */
function calcPayout(totalBets, betsOnWinner, coinsBet) {
  if (!betsOnWinner) return 0;
  const odds = Math.floor(totalBets / betsOnWinner);
  return Math.min(20, Math.max(1, odds * (coinsBet || 1)));
}

/** Acredita monedas (mismo helper que payments.js) */
async function creditCoins(conn, accountName, coins) {
  await conn.execute(
    `INSERT INTO account_data (account_name, var, value)
     VALUES (?, 'web_coins', ?)
     ON DUPLICATE KEY UPDATE value = CAST(CAST(value AS UNSIGNED) + ? AS CHAR)`,
    [accountName, coins, coins]
  );
}

/** Descuenta monedas, lanza error si saldo insuficiente */
async function deductCoins(conn, accountName, coins) {
  const [[row]] = await conn.execute(
    "SELECT value FROM account_data WHERE account_name=? AND var='web_coins' FOR UPDATE",
    [accountName]
  );
  const balance = row ? parseInt(row.value) || 0 : 0;
  if (balance < coins) throw new Error(`Saldo insuficiente (tenés ${balance}, necesitás ${coins})`);
  const newBal = balance - coins;
  await conn.execute(
    `INSERT INTO account_data (account_name, var, value) VALUES (?, 'web_coins', ?)
     ON DUPLICATE KEY UPDATE value = ?`,
    [accountName, newBal, newBal]
  );
  return newBal;
}

/* ──────────────────────────────────────────────────────────────────────
   GET /api/bets/season  — Temporada actual + candidatos + mis apuestas
────────────────────────────────────────────────────────────────────── */
router.get('/season', async (req, res) => {
  try {
    // Temporada activa más reciente
    const [[season]] = await db.execute(
      `SELECT * FROM olympiad_bet_seasons
       WHERE status IN ('open','closed')
       ORDER BY id DESC LIMIT 1`
    );

    if (!season) {
      return res.json({ season: null, candidates: [], myBet: null, totals: null });
    }

    // Candidatos: nobles de olimpiada con sus apuestas acumuladas
    const [candidates] = await db.execute(
      `SELECT c.char_name, c.classid, c.level, c.title, c.title_color,
              o.olympiad_points, o.competitions_won, o.competitions_done,
              IF(h.charId IS NOT NULL, 1, 0) AS is_current_hero,
              COUNT(b.id)        AS bets_count,
              SUM(b.coins_bet)   AS coins_pool,
              cl.clan_name
       FROM olympiad_nobles o
       JOIN characters c ON c.charId = o.charId AND c.deletetime = 0
       JOIN accounts a   ON a.login  = c.account_name AND a.accessLevel < 100
       LEFT JOIN heroes h  ON h.charId = c.charId AND h.played = 1
       LEFT JOIN clan_data cl ON cl.clan_id = c.clanid
       LEFT JOIN olympiad_bets b
              ON b.char_bet = c.char_name AND b.season_id = ?
       GROUP BY c.charId
       ORDER BY bets_count DESC, o.olympiad_points DESC
       LIMIT 30`,
      [season.id]
    );

    // Totales globales de la temporada
    const [[totals]] = await db.execute(
      `SELECT COUNT(*) AS total_bets, COALESCE(SUM(coins_bet),0) AS total_pool
       FROM olympiad_bets WHERE season_id = ?`,
      [season.id]
    );

    // Apuesta del usuario autenticado (si hay token válido)
    let myBet = null;
    const token = (req.headers.authorization || '').replace('Bearer ', '');
    if (token) {
      try {
        const jwt = require('jsonwebtoken');
        const payload = jwt.verify(token, process.env.JWT_SECRET || 'l2h5_secret');
        const [[bet]] = await db.execute(
          `SELECT b.*, os.status AS season_status
           FROM olympiad_bets b
           JOIN olympiad_bet_seasons os ON os.id = b.season_id
           WHERE b.season_id = ? AND b.account_name = ?`,
          [season.id, payload.login]
        );
        myBet = bet || null;
      } catch { /* token inválido, ignorar */ }
    }

    res.json({ season, candidates, myBet, totals });
  } catch (err) {
    console.error('[Bets/season]', err.message);
    res.status(500).json({ error: 'Error obteniendo temporada' });
  }
});

/* ──────────────────────────────────────────────────────────────────────
   GET /api/bets/history  — Temporadas resueltas
────────────────────────────────────────────────────────────────────── */
router.get('/history', async (req, res) => {
  try {
    const [seasons] = await db.execute(
      `SELECT s.*, COUNT(b.id) AS total_bets, COALESCE(SUM(b.coins_bet),0) AS total_pool
       FROM olympiad_bet_seasons s
       LEFT JOIN olympiad_bets b ON b.season_id = s.id
       WHERE s.status = 'resolved'
       GROUP BY s.id
       ORDER BY s.resolved_at DESC
       LIMIT 20`
    );
    res.json(seasons);
  } catch (err) {
    console.error('[Bets/history]', err.message);
    res.status(500).json({ error: 'Error obteniendo historial' });
  }
});

/* ──────────────────────────────────────────────────────────────────────
   GET /api/bets/my  — Apuestas del usuario autenticado
────────────────────────────────────────────────────────────────────── */
router.get('/my', auth, async (req, res) => {
  try {
    const [bets] = await db.execute(
      `SELECT b.*, s.name AS season_name, s.status AS season_status, s.winner_char
       FROM olympiad_bets b
       JOIN olympiad_bet_seasons s ON s.id = b.season_id
       WHERE b.account_name = ?
       ORDER BY b.created_at DESC
       LIMIT 30`,
      [req.user.login]
    );
    res.json(bets);
  } catch (err) {
    console.error('[Bets/my]', err.message);
    res.status(500).json({ error: 'Error obteniendo tus apuestas' });
  }
});

/* ──────────────────────────────────────────────────────────────────────
   POST /api/bets/place  — Realizar apuesta
────────────────────────────────────────────────────────────────────── */
router.post('/place', auth, async (req, res) => {
  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();

    const { charBet, coinsBet = 1 } = req.body;
    if (!charBet) return res.status(400).json({ error: 'charBet requerido' });

    const coins = parseInt(coinsBet);
    if (isNaN(coins) || coins < 1 || coins > 1000)
      return res.status(400).json({ error: 'Podés apostar entre 1 y 1000 monedas' });

    // Temporada abierta
    const [[season]] = await conn.execute(
      `SELECT * FROM olympiad_bet_seasons WHERE status = 'open' ORDER BY id DESC LIMIT 1`
    );
    if (!season) {
      await conn.rollback();
      return res.status(400).json({ error: 'No hay temporada de apuestas abierta' });
    }

    // ¿Ya apostó este período?
    const [[existing]] = await conn.execute(
      `SELECT id FROM olympiad_bets WHERE season_id = ? AND account_name = ? FOR UPDATE`,
      [season.id, req.user.login]
    );
    if (existing) {
      await conn.rollback();
      return res.status(409).json({ error: 'Ya tenés una apuesta en esta temporada' });
    }

    // Verificar que el personaje apostado exista en olimpiada
    const [[candidate]] = await conn.execute(
      `SELECT c.char_name, c.classid FROM characters c
       JOIN olympiad_nobles o ON o.charId = c.charId
       JOIN accounts a ON a.login = c.account_name AND a.accessLevel < 100
       WHERE c.char_name = ? AND c.deletetime = 0`,
      [charBet]
    );
    if (!candidate) {
      await conn.rollback();
      return res.status(404).json({ error: 'Ese personaje no está en la Olimpiada' });
    }

    // Descontar monedas
    const newBalance = await deductCoins(conn, req.user.login, coins);

    // Guardar apuesta
    await conn.execute(
      `INSERT INTO olympiad_bets (season_id, account_name, char_bet, class_id, coins_bet)
       VALUES (?, ?, ?, ?, ?)`,
      [season.id, req.user.login, candidate.char_name, candidate.classid, coins]
    );

    // Actualizar totales de la temporada
    await conn.execute(
      `UPDATE olympiad_bet_seasons
       SET total_pool = total_pool + ?, total_bets = total_bets + 1
       WHERE id = ?`,
      [coins, season.id]
    );

    await conn.commit();

    // Broadcast SSE
    const [[updated]] = await db.execute(
      `SELECT char_bet, COUNT(*) AS bets_count, SUM(coins_bet) AS coins_pool
       FROM olympiad_bets WHERE season_id = ? AND char_bet = ?`,
      [season.id, candidate.char_name]
    );
    broadcastBetUpdate({
      type:       'new_bet',
      char_bet:   candidate.char_name,
      bets_count: updated?.bets_count || 1,
      coins_pool: updated?.coins_pool || coins,
      season_id:  season.id,
      total_bets: season.total_bets + 1,
      total_pool: season.total_pool + coins
    });

    res.json({
      message:    `¡Apostaste ${coins} 🪙 por ${candidate.char_name}!`,
      newBalance,
      charBet:    candidate.char_name,
      coinsBet:   coins
    });
  } catch (err) {
    await conn.rollback();
    console.error('[Bets/place]', err.message);
    res.status(500).json({ error: err.message });
  } finally {
    conn.release();
  }
});

/* ──────────────────────────────────────────────────────────────────────
   POST /api/bets/admin/resolve  — ADMIN: declarar ganador y pagar
────────────────────────────────────────────────────────────────────── */
router.post('/admin/resolve', auth, async (req, res) => {
  if (req.user.accessLevel < 100)
    return res.status(403).json({ error: 'Solo admins' });

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();

    const { seasonId, winnerChar } = req.body;
    if (!seasonId || !winnerChar)
      return res.status(400).json({ error: 'seasonId y winnerChar requeridos' });

    const [[season]] = await conn.execute(
      `SELECT * FROM olympiad_bet_seasons WHERE id = ? AND status != 'resolved' FOR UPDATE`,
      [seasonId]
    );
    if (!season) {
      await conn.rollback();
      return res.status(404).json({ error: 'Temporada no encontrada o ya resuelta' });
    }

    // Obtener class_id del ganador
    const [[winner]] = await conn.execute(
      `SELECT c.classid FROM characters c WHERE c.char_name = ? AND c.deletetime = 0`,
      [winnerChar]
    );

    // Cantidad de apuestas en el ganador
    const [[winBets]] = await conn.execute(
      `SELECT COUNT(*) AS cnt, SUM(coins_bet) AS pool
       FROM olympiad_bets WHERE season_id = ? AND char_bet = ?`,
      [seasonId, winnerChar]
    );

    const totalBets   = season.total_bets  || 0;
    const betsOnWinner = parseInt(winBets?.cnt) || 0;

    // Pagar a cada ganador
    const [winners] = await conn.execute(
      `SELECT * FROM olympiad_bets WHERE season_id = ? AND char_bet = ?`,
      [seasonId, winnerChar]
    );

    for (const bet of winners) {
      const payout = calcPayout(totalBets, betsOnWinner, bet.coins_bet);
      await creditCoins(conn, bet.account_name, payout);
      await conn.execute(
        `UPDATE olympiad_bets SET won = 1, payout = ? WHERE id = ?`,
        [payout, bet.id]
      );
    }

    // Marcar perdedores
    await conn.execute(
      `UPDATE olympiad_bets SET won = 0, payout = 0
       WHERE season_id = ? AND char_bet != ?`,
      [seasonId, winnerChar]
    );

    // Cerrar temporada
    await conn.execute(
      `UPDATE olympiad_bet_seasons
       SET status = 'resolved', winner_char = ?, winner_class_id = ?,
           resolved_at = NOW()
       WHERE id = ?`,
      [winnerChar, winner?.classid || null, seasonId]
    );

    await conn.commit();

    // Broadcast SSE
    broadcastBetUpdate({
      type:         'season_resolved',
      season_id:    seasonId,
      winner_char:  winnerChar,
      total_winners: betsOnWinner,
      total_bets:   totalBets
    });

    res.json({
      message:       `¡Temporada resuelta! Héroe: ${winnerChar}`,
      winnersCount:  betsOnWinner,
      totalBets,
      payoutFormula: `min(20, max(1, floor(${totalBets}/${betsOnWinner}) * coins_apostadas))`
    });
  } catch (err) {
    await conn.rollback();
    console.error('[Bets/resolve]', err.message);
    res.status(500).json({ error: err.message });
  } finally {
    conn.release();
  }
});

/* ──────────────────────────────────────────────────────────────────────
   POST /api/bets/admin/new-season  — ADMIN: abrir nueva temporada
────────────────────────────────────────────────────────────────────── */
router.post('/admin/new-season', auth, async (req, res) => {
  if (req.user.accessLevel < 100)
    return res.status(403).json({ error: 'Solo admins' });
  try {
    const { name } = req.body;
    // Cerrar temporadas abiertas anteriores
    await db.execute(
      `UPDATE olympiad_bet_seasons SET status='closed', closed_at=NOW()
       WHERE status='open'`
    );
    const [result] = await db.execute(
      `INSERT INTO olympiad_bet_seasons (name, status)
       VALUES (?, 'open')`,
      [name || `Temporada ${Date.now()}`]
    );
    broadcastBetUpdate({ type: 'new_season', season_id: result.insertId });
    res.json({ message: 'Nueva temporada abierta', seasonId: result.insertId });
  } catch (err) {
    console.error('[Bets/new-season]', err.message);
    res.status(500).json({ error: err.message });
  }
});

/* ──────────────────────────────────────────────────────────────────────
   POST /api/bets/admin/close  — ADMIN: cerrar apuestas (sin resolver)
────────────────────────────────────────────────────────────────────── */
router.post('/admin/close', auth, async (req, res) => {
  if (req.user.accessLevel < 100)
    return res.status(403).json({ error: 'Solo admins' });
  try {
    const { seasonId } = req.body;
    await db.execute(
      `UPDATE olympiad_bet_seasons SET status='closed', closed_at=NOW() WHERE id=? AND status='open'`,
      [seasonId]
    );
    broadcastBetUpdate({ type: 'season_closed', season_id: seasonId });
    res.json({ message: 'Apuestas cerradas. Ya no se aceptan nuevas apuestas.' });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = { router, broadcastBetUpdate };
