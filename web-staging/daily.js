const router = require('express').Router();
const db     = require('../config/db');
const auth   = require('../middleware/auth');

// Recompensas 28 días (itemId:cantidad) desde DailyReward.ini
const DAILY_REWARDS = [
  { day: 1,  itemId: 10639, amount: 100,  label: 'Festival Adena' },
  { day: 2,  itemId: 10639, amount: 150,  label: 'Festival Adena' },
  { day: 3,  itemId: 10639, amount: 200,  label: 'Festival Adena' },
  { day: 4,  itemId: 10639, amount: 200,  label: 'Festival Adena' },
  { day: 5,  itemId: 10639, amount: 250,  label: 'Festival Adena' },
  { day: 6,  itemId: 10639, amount: 250,  label: 'Festival Adena' },
  { day: 7,  itemId: 10639, amount: 500,  label: 'Festival Adena' },
  { day: 8,  itemId: 10639, amount: 200,  label: 'Festival Adena' },
  { day: 9,  itemId: 10639, amount: 250,  label: 'Festival Adena' },
  { day: 10, itemId: 10639, amount: 300,  label: 'Festival Adena' },
  { day: 11, itemId: 10639, amount: 300,  label: 'Festival Adena' },
  { day: 12, itemId: 10639, amount: 350,  label: 'Festival Adena' },
  { day: 13, itemId: 10639, amount: 350,  label: 'Festival Adena' },
  { day: 14, itemId: 10639, amount: 750,  label: 'Festival Adena' },
  { day: 15, itemId: 10639, amount: 300,  label: 'Festival Adena' },
  { day: 16, itemId: 10639, amount: 350,  label: 'Festival Adena' },
  { day: 17, itemId: 10639, amount: 400,  label: 'Festival Adena' },
  { day: 18, itemId: 10639, amount: 400,  label: 'Festival Adena' },
  { day: 19, itemId: 10639, amount: 450,  label: 'Festival Adena' },
  { day: 20, itemId: 10639, amount: 450,  label: 'Festival Adena' },
  { day: 21, itemId: 10639, amount: 1000, label: 'Festival Adena' },
  { day: 22, itemId: 10639, amount: 400,  label: 'Festival Adena' },
  { day: 23, itemId: 10639, amount: 450,  label: 'Festival Adena' },
  { day: 24, itemId: 10639, amount: 500,  label: 'Festival Adena' },
  { day: 25, itemId: 10639, amount: 500,  label: 'Festival Adena' },
  { day: 26, itemId: 10639, amount: 550,  label: 'Festival Adena' },
  { day: 27, itemId: 10639, amount: 550,  label: 'Festival Adena' },
  { day: 28, itemId: 10639, amount: 2000, label: 'Festival Adena' },
];

function todayEpochDay() {
  return Math.floor(Date.now() / 86400000);
}

/* ─── GET /api/daily/status ───────────────────────────────────────── */
// Devuelve el estado de recompensa diaria para todos los personajes de la cuenta
router.get('/status', auth, async (req, res) => {
  try {
    const [chars] = await db.execute(
      'SELECT charId, char_name FROM characters WHERE account_name = ? ORDER BY char_name',
      [req.user.login]
    );

    if (!chars.length) return res.json({ rewards: DAILY_REWARDS, characters: [], todayEpochDay: todayEpochDay() });

    const charIds = chars.map(c => c.charId);
    const placeholders = charIds.map(() => '?').join(',');
    const [rows] = await db.execute(
      `SELECT char_id, last_claim, streak_day FROM daily_rewards WHERE char_id IN (${placeholders})`,
      charIds
    );

    const rewardMap = {};
    rows.forEach(r => { rewardMap[r.char_id] = r; });

    const today = todayEpochDay();
    const characters = chars.map(c => {
      const dr = rewardMap[c.charId];
      const lastClaim   = dr ? dr.last_claim   : 0;
      const streakDay   = dr ? dr.streak_day   : 0;
      const claimedToday = lastClaim === today;
      const nextDay     = claimedToday ? streakDay : (streakDay % 28) + 1;
      return {
        charId:       c.charId,
        char_name:    c.char_name,
        streakDay,
        lastClaim,
        claimedToday,
        nextDay,       // día que reclamaría si presiona el botón
      };
    });

    res.json({ rewards: DAILY_REWARDS, characters, todayEpochDay: today });
  } catch (err) {
    console.error('[Daily/status]', err.message);
    res.status(500).json({ error: 'Error obteniendo estado de recompensa' });
  }
});

/* ─── POST /api/daily/claim ───────────────────────────────────────── */
router.post('/claim', auth, async (req, res) => {
  const { charId } = req.body;
  if (!charId) return res.status(400).json({ error: 'charId requerido' });

  try {
    // Verificar que el personaje pertenece a esta cuenta
    const [[char]] = await db.execute(
      'SELECT charId, char_name FROM characters WHERE charId = ? AND account_name = ?',
      [charId, req.user.login]
    );
    if (!char) return res.status(403).json({ error: 'Personaje no encontrado' });

    const today = todayEpochDay();

    // Leer estado actual
    const [[existing]] = await db.execute(
      'SELECT last_claim, streak_day FROM daily_rewards WHERE char_id = ?',
      [charId]
    );

    if (existing && existing.last_claim === today) {
      const reward = DAILY_REWARDS[(existing.streak_day - 1) % 28];
      return res.status(409).json({
        alreadyClaimed: true,
        message: `¡Ya reclamaste la recompensa del Día ${existing.streak_day} hoy! El ítem <strong>${reward.label} x${reward.amount.toLocaleString()}</strong> fue entregado a <strong>${char.char_name}</strong>.`,
        streakDay: existing.streak_day,
      });
    }

    // Calcular nuevo streak_day
    const prevStreak  = existing ? existing.streak_day  : 0;
    const prevClaim   = existing ? existing.last_claim   : 0;
    const yesterday   = today - 1;
    let newStreak;
    if (!existing || prevStreak === 0) {
      newStreak = 1;
    } else if (prevClaim === yesterday) {
      // Día consecutivo
      newStreak = (prevStreak % 28) + 1;
    } else {
      // Se saltó un día → reset
      newStreak = 1;
    }

    const reward = DAILY_REWARDS[newStreak - 1];

    // Guardar en DB
    await db.execute(`
      INSERT INTO daily_rewards (char_id, last_claim, streak_day)
      VALUES (?, ?, ?)
      ON DUPLICATE KEY UPDATE last_claim = VALUES(last_claim), streak_day = VALUES(streak_day)
    `, [charId, today, newStreak]);

    res.json({
      success: true,
      message: `¡Recompensa del Día ${newStreak} reclamada! <strong>${reward.label} x${reward.amount.toLocaleString()}</strong> entregado a <strong>${char.char_name}</strong>. Retirá el ítem en el juego.`,
      streakDay: newStreak,
      reward,
    });
  } catch (err) {
    console.error('[Daily/claim]', err.message);
    res.status(500).json({ error: 'Error al reclamar recompensa' });
  }
});

module.exports = router;
