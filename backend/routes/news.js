const router = require('express').Router();
const db     = require('../config/db');
const auth   = require('../middleware/auth');

/* Tabla web_news (creada en setup.sql) */

/* ─── GET /api/news ────────────────────────────────────────────── */
router.get('/', async (req, res) => {
  try {
    const limit  = Math.min(parseInt(req.query.limit)  || 10, 50);
    const offset = Math.max(parseInt(req.query.offset) || 0,  0);
    const type   = req.query.type; // 'news' | 'event' | 'update'

    let query = `SELECT id, title, content, type, image_url, author, pinned,
                        created_at, updated_at
                 FROM web_news WHERE active = 1`;
    const params = [];

    if (type) { query += ' AND type = ?'; params.push(type); }
    query += ' ORDER BY pinned DESC, created_at DESC LIMIT ? OFFSET ?';
    params.push(limit, offset);

    const [rows] = await db.execute(query, params);

    const [[{ total }]] = await db.execute(
      `SELECT COUNT(*) AS total FROM web_news WHERE active = 1${type ? ' AND type = ?' : ''}`,
      type ? [type] : []
    );

    res.json({ items: rows, total, limit, offset });
  } catch (err) {
    console.error('[News/GET]', err.message);
    res.status(500).json({ error: 'Error obteniendo noticias' });
  }
});

/* ─── GET /api/news/:id ─────────────────────────────────────────── */
router.get('/:id', async (req, res) => {
  try {
    const [rows] = await db.execute(
      'SELECT * FROM web_news WHERE id = ? AND active = 1',
      [req.params.id]
    );
    if (!rows.length) return res.status(404).json({ error: 'Noticia no encontrada' });
    res.json(rows[0]);
  } catch (err) {
    res.status(500).json({ error: 'Error' });
  }
});

/* ─── POST /api/news  (solo admin) ────────────────────────────── */
router.post('/', auth, async (req, res) => {
  try {
    if (req.user.accessLevel < 100)
      return res.status(403).json({ error: 'Sin permisos de administrador' });

    const { title, content, type = 'news', image_url, pinned = false } = req.body;
    if (!title || !content)
      return res.status(400).json({ error: 'title y content requeridos' });

    const [result] = await db.execute(
      `INSERT INTO web_news (title, content, type, image_url, author, pinned)
       VALUES (?, ?, ?, ?, ?, ?)`,
      [title, content, type, image_url || null, req.user.login, pinned ? 1 : 0]
    );

    res.status(201).json({ id: result.insertId, message: 'Noticia creada' });
  } catch (err) {
    console.error('[News/POST]', err.message);
    res.status(500).json({ error: 'Error creando noticia' });
  }
});

/* ─── PUT /api/news/:id  (solo admin) ──────────────────────────── */
router.put('/:id', auth, async (req, res) => {
  try {
    if (req.user.accessLevel < 100)
      return res.status(403).json({ error: 'Sin permisos' });

    const { title, content, type, image_url, pinned, active } = req.body;
    await db.execute(
      `UPDATE web_news SET title=?, content=?, type=?, image_url=?, pinned=?, active=?
       WHERE id=?`,
      [title, content, type, image_url, pinned ? 1 : 0, active !== false ? 1 : 0, req.params.id]
    );
    res.json({ message: 'Noticia actualizada' });
  } catch (err) {
    res.status(500).json({ error: 'Error' });
  }
});

module.exports = router;
