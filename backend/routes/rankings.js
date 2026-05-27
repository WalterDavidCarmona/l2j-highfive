const router = require('express').Router();
const db     = require('../config/db');

// Mapa de classid → nombre de clase (L2JMobius H5)
const CLASS_NAMES = {
  0:'Humano Fighter',1:'Warrior',2:'Gladiator',3:'Warlord',4:'Human Knight',
  5:'Paladin',6:'Dark Avenger',7:'Rogue',8:'Treasure Hunter',9:'Hawkeye',
  10:'Human Mystic',11:'Human Wizard',12:'Sorcerer',13:'Necromancer',14:'Warlock',
  15:'Cleric',16:'Bishop',17:'Prophet',
  18:'Elven Fighter',19:'Elven Knight',20:'Temple Knight',21:'Swordsinger',
  22:'Elven Scout',23:'Plainswalker',24:'Silver Ranger',
  25:'Elven Mystic',26:'Elven Wizard',27:'Spellsinger',28:'Elemental Summoner',
  29:'Elven Oracle',30:'Elven Elder',
  31:'Dark Fighter',32:'Palus Knight',33:'Shillien Knight',34:'Bladedancer',
  35:'Assassin',36:'Abyss Walker',37:'Phantom Ranger',
  38:'Dark Elven Mystic',39:'Dark Wizard',40:'Spellhowler',41:'Phantom Summoner',
  42:'Shillien Oracle',43:'Shillien Elder',
  44:'Orc Fighter',45:'Orc Raider',46:'Destroyer',47:'Monk',48:'Tyrant',
  49:'Orc Mystic',50:'Orc Shaman',51:'Overlord',52:'Warcryer',
  53:'Dwarven Fighter',54:'Scavenger',55:'Bounty Hunter',56:'Artisan',57:'Warsmith',
  88:'Duelist',89:'Dreadnought',90:'Phoenix Knight',91:'Hell Knight',
  92:'Sagittarius',93:'Adventurer',94:'Archmage',95:'Soultaker',
  96:'Arcana Lord',97:'Cardinal',98:'Hierophant',
  99:'Eva\'s Templar',100:'Sword Muse',101:'Wind Rider',102:'Moonlight Sentinel',
  103:'Mystic Muse',104:'Elemental Master',105:'Eva\'s Saint',
  106:'Shillien Templar',107:'Spectral Dancer',108:'Ghost Hunter',
  109:'Ghost Sentinel',110:'Storm Screamer',111:'Spectral Master',112:'Shillien Saint',
  113:'Titan',114:'Grand Khavatari',115:'Dominator',116:'Doomcryer',
  117:'Fortune Seeker',118:'Maestro'
};

const RACE_NAMES = {
  0:'Humano', 1:'Élfico', 2:'Elfo Oscuro', 3:'Orco', 4:'Enano', 5:'Kamael'
};

function enrichCharacter(c) {
  return {
    ...c,
    className:  CLASS_NAMES[c.classid]  || `Clase ${c.classid}`,
    raceName:   RACE_NAMES[c.race]      || `Raza ${c.race}`,
    titleColor: c.title_color ? '#' + c.title_color.toString(16).padStart(6,'0') : '#FFFF77'
  };
}

/* ─── GET /api/rankings/pvp  (Top PvP kills global) ───────────── */
router.get('/pvp', async (req, res) => {
  try {
    const limit = Math.min(parseInt(req.query.limit) || 50, 100);
    const [rows] = await db.execute(
      `SELECT c.charId, c.char_name, c.level, c.race, c.classid,
              c.pvpkills, c.pkkills, c.title, c.title_color,
              c.online, cl.clan_name
       FROM characters c
       LEFT JOIN clan_data cl ON cl.clan_id = c.clanid
       WHERE c.deletetime = 0 AND c.accesslevel >= 0
       ORDER BY c.pvpkills DESC
       LIMIT ?`,
      [limit]
    );
    res.json(rows.map((r, i) => ({ rank: i + 1, ...enrichCharacter(r) })));
  } catch (err) {
    console.error('[Rankings/PvP]', err.message);
    res.status(500).json({ error: 'Error obteniendo ranking PvP' });
  }
});

/* ─── GET /api/rankings/pk  (Top PK kills) ────────────────────── */
router.get('/pk', async (req, res) => {
  try {
    const limit = Math.min(parseInt(req.query.limit) || 50, 100);
    const [rows] = await db.execute(
      `SELECT c.charId, c.char_name, c.level, c.race, c.classid,
              c.pvpkills, c.pkkills, c.title, c.title_color, c.online,
              cl.clan_name
       FROM characters c
       LEFT JOIN clan_data cl ON cl.clan_id = c.clanid
       WHERE c.deletetime = 0 AND c.accesslevel >= 0
       ORDER BY c.pkkills DESC
       LIMIT ?`,
      [limit]
    );
    res.json(rows.map((r, i) => ({ rank: i + 1, ...enrichCharacter(r) })));
  } catch (err) {
    console.error('[Rankings/PK]', err.message);
    res.status(500).json({ error: 'Error obteniendo ranking PK' });
  }
});

/* ─── GET /api/rankings/pvpzone  (Top killer por zona PvP rotativa) */
router.get('/pvpzone', async (req, res) => {
  try {
    const limit = Math.min(parseInt(req.query.limit) || 25, 50);

    // La zona PvP rotativa guarda kills en la tabla pvp_zone_kills (creada por nosotros)
    // Si la tabla no existe, devolvemos ranking basado en pvpkills con filtro activo
    let rows;
    try {
      [rows] = await db.execute(
        `SELECT z.char_name, z.zone_name, z.kills, z.last_kill,
                c.level, c.race, c.classid, c.title, c.title_color,
                c.pvpkills, c.pkkills, c.online,
                cl.clan_name
         FROM pvp_zone_kills z
         JOIN characters c ON c.char_name = z.char_name AND c.deletetime = 0
         LEFT JOIN clan_data cl ON cl.clan_id = c.clanid
         ORDER BY z.kills DESC
         LIMIT ?`,
        [limit]
      );
    } catch {
      // Tabla no existe aún — fallback a pvpkills general de jugadores online/recientes
      [rows] = await db.execute(
        `SELECT c.charId, c.char_name, c.level, c.race, c.classid,
                c.pvpkills AS kills, c.pkkills, c.title, c.title_color,
                c.online, cl.clan_name,
                'Zona PvP Rotativa' AS zone_name,
                NULL AS last_kill
         FROM characters c
         LEFT JOIN clan_data cl ON cl.clan_id = c.clanid
         WHERE c.deletetime = 0 AND c.pvpkills > 0 AND c.accesslevel >= 0
         ORDER BY c.pvpkills DESC
         LIMIT ?`,
        [limit]
      );
    }

    res.json(rows.map((r, i) => ({ rank: i + 1, ...enrichCharacter(r) })));
  } catch (err) {
    console.error('[Rankings/PvPZone]', err.message);
    res.status(500).json({ error: 'Error obteniendo ranking de zona' });
  }
});

/* ─── GET /api/rankings/clans  (Top clanes por puntos/miembros) ─ */
router.get('/clans', async (req, res) => {
  try {
    const limit = Math.min(parseInt(req.query.limit) || 25, 50);
    const [rows] = await db.execute(
      `SELECT cl.clan_id, cl.clan_name, cl.clan_level,
              cl.reputation_score, cl.ally_name,
              COUNT(c.charId) AS member_count,
              SUM(c.pvpkills) AS total_pvp,
              leader.char_name AS leader_name
       FROM clan_data cl
       LEFT JOIN characters c ON c.clanid = cl.clan_id AND c.deletetime = 0
       LEFT JOIN characters leader ON leader.charId = cl.leader_id
       GROUP BY cl.clan_id
       ORDER BY cl.reputation_score DESC, total_pvp DESC
       LIMIT ?`,
      [limit]
    );
    res.json(rows.map((r, i) => ({ rank: i + 1, ...r })));
  } catch (err) {
    console.error('[Rankings/Clans]', err.message);
    res.status(500).json({ error: 'Error obteniendo ranking de clanes' });
  }
});

/* ─── GET /api/rankings/online  (Jugadores conectados) ────────── */
router.get('/online', async (req, res) => {
  try {
    const [rows] = await db.execute(
      `SELECT c.char_name, c.level, c.race, c.classid, c.title, c.title_color,
              cl.clan_name
       FROM characters c
       LEFT JOIN clan_data cl ON cl.clan_id = c.clanid
       WHERE c.online = 1 AND c.deletetime = 0 AND c.accesslevel >= 0
       ORDER BY c.level DESC
       LIMIT 100`
    );
    res.json({ count: rows.length, players: rows.map(r => enrichCharacter(r)) });
  } catch (err) {
    console.error('[Rankings/Online]', err.message);
    res.status(500).json({ error: 'Error obteniendo jugadores online' });
  }
});

/* ─── GET /api/rankings/olympiad  (Top Olympiad) ──────────────── */
router.get('/olympiad', async (req, res) => {
  try {
    const limit = Math.min(parseInt(req.query.limit) || 25, 50);
    const [rows] = await db.execute(
      `SELECT c.char_name, c.level, c.race, c.classid, c.title, c.title_color,
              c.pvpkills, o.olympiad_points, o.competitions_won, o.competitions_lost,
              cl.clan_name,
              CASE WHEN h.charId IS NOT NULL THEN 1 ELSE 0 END AS is_hero
       FROM olympiad_nobles o
       JOIN characters c ON c.charId = o.charId AND c.deletetime = 0
       LEFT JOIN clan_data cl ON cl.clan_id = c.clanid
       LEFT JOIN heroes h ON h.charId = c.charId AND h.played = 1
       ORDER BY o.olympiad_points DESC
       LIMIT ?`,
      [limit]
    );
    res.json(rows.map((r, i) => ({ rank: i + 1, ...enrichCharacter(r) })));
  } catch (err) {
    console.error('[Rankings/Olympiad]', err.message);
    res.status(500).json({ error: 'Error obteniendo ranking olimpiada' });
  }
});

module.exports = router;
