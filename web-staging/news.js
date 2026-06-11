const router = require('express').Router();
const db     = require('../config/db');
const auth   = require('../middleware/auth');
const multer = require('multer');
const path   = require('path');
const fs     = require('fs');

/* ─── Configuracion de Multer para imagenes ──────────────────────── */
const UPLOAD_DIR = path.join(__dirname, '..', '..', 'frontend', 'uploads', 'news');
if (!fs.existsSync(UPLOAD_DIR)) fs.mkdirSync(UPLOAD_DIR, { recursive: true });

const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, UPLOAD_DIR),
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase();
    const name = `news_${Date.now()}_${Math.random().toString(36).slice(2, 8)}${ext}`;
    cb(null, name);
  }
});

const upload = multer({
  storage,
  limits: { fileSize: 5 * 1024 * 1024 }, // 5MB max
  fileFilter: (req, file, cb) => {
    const allowed = ['.jpg', '.jpeg', '.png', '.gif', '.webp'];
    const ext = path.extname(file.originalname).toLowerCase();
    if (allowed.includes(ext)) cb(null, true);
    else cb(new Error('Solo se permiten imagenes (jpg, png, gif, webp)'));
  }
});

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

/* ─── GET /api/news/admin/all  (solo admin) ───────────────────── */
router.get('/admin/all', auth, async (req, res) => {
  try {
    if (req.user.accessLevel < 100)
      return res.status(403).json({ error: 'Sin permisos' });

    const limit  = Math.min(parseInt(req.query.limit)  || 50, 200);
    const offset = Math.max(parseInt(req.query.offset) || 0,  0);

    const [rows] = await db.execute(
      `SELECT id, title, content, type, image_url, author, pinned, active,
              created_at, updated_at
       FROM web_news
       ORDER BY created_at DESC
       LIMIT ? OFFSET ?`,
      [limit, offset]
    );

    const [[{ total }]] = await db.execute('SELECT COUNT(*) AS total FROM web_news');
    res.json({ items: rows, total, limit, offset });
  } catch (err) {
    console.error('[News/admin/all]', err.message);
    res.status(500).json({ error: 'Error obteniendo noticias' });
  }
});

/* ─── GET /api/news/:id ─────────────────────────────────────────── */
router.get('/:id', async (req, res) => {
  try {
    const [rows] = await db.execute(
      'SELECT * FROM web_news WHERE id = ?',
      [req.params.id]
    );
    if (!rows.length) return res.status(404).json({ error: 'Noticia no encontrada' });
    res.json(rows[0]);
  } catch (err) {
    res.status(500).json({ error: 'Error' });
  }
});

/* ─── POST /api/news/upload  (solo admin, multiple) ──────────── */
router.post('/upload', auth, (req, res) => {
  if (req.user.accessLevel < 100)
    return res.status(403).json({ error: 'Sin permisos de administrador' });

  upload.array('images', 10)(req, res, (err) => {
    if (err) {
      console.error('[News/upload]', err.message);
      return res.status(400).json({ error: err.message });
    }
    if (!req.files || !req.files.length)
      return res.status(400).json({ error: 'No se recibieron imagenes' });

    const urls = req.files.map(f => `/uploads/news/${f.filename}`);
    res.json({ urls });
  });
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
      `UPDATE web_news SET title=?, content=?, type=?, image_url=?, pinned=?, active=?,
              updated_at=NOW()
       WHERE id=?`,
      [title, content, type, image_url || null, pinned ? 1 : 0, active !== false ? 1 : 0, req.params.id]
    );
    res.json({ message: 'Noticia actualizada' });
  } catch (err) {
    res.status(500).json({ error: 'Error actualizando noticia' });
  }
});

/* ─── DELETE /api/news/:id  (solo admin) ──────────────────────── */
router.delete('/:id', auth, async (req, res) => {
  try {
    if (req.user.accessLevel < 100)
      return res.status(403).json({ error: 'Sin permisos' });

    // Obtener imagenes para borrarlas del disco
    const [rows] = await db.execute('SELECT image_url FROM web_news WHERE id=?', [req.params.id]);
    if (rows.length && rows[0].image_url) {
      try {
        const urls = JSON.parse(rows[0].image_url);
        (Array.isArray(urls) ? urls : [urls]).forEach(u => {
          if (u) { const p = path.join(__dirname, '..', '..', 'frontend', u); if (fs.existsSync(p)) fs.unlinkSync(p); }
        });
      } catch {
        const p = path.join(__dirname, '..', '..', 'frontend', rows[0].image_url);
        if (fs.existsSync(p)) fs.unlinkSync(p);
      }
    }

    const [result] = await db.execute('DELETE FROM web_news WHERE id=?', [req.params.id]);
    if (!result.affectedRows) return res.status(404).json({ error: 'Noticia no encontrada' });
    res.json({ message: 'Noticia eliminada' });
  } catch (err) {
    console.error('[News/DELETE]', err.message);
    res.status(500).json({ error: 'Error eliminando noticia' });
  }
});

module.exports = router;
