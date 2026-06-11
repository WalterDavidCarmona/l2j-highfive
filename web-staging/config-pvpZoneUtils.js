/**
 * pvpZoneUtils.js
 * Parseo de PvpZone.ini y detección de jugadores dentro de la zona activa.
 */
const fs   = require('fs');
const path = require('path');

const PVP_ZONE_INI = process.env.PVP_ZONE_INI_PATH ||
  'C:\\Users\\david\\OneDrive\\Desktop\\l2j\\game\\config\\Custom\\PvpZone.ini';
const GS_LOG_PATH  = process.env.GS_LOG_PATH ||
  'C:\\Users\\david\\OneDrive\\Desktop\\l2j\\game\\log\\java0.log';

/* ─── Parseo del .ini ─────────────────────────────────────────── */
function parsePvpZoneIni() {
  try {
    const raw = fs.readFileSync(PVP_ZONE_INI, 'utf8');
    const get = (key) => {
      const m = raw.match(new RegExp(`^\\s*${key}\\s*=\\s*(.+)$`, 'im'));
      return m ? m[1].trim() : null;
    };
    const enabled         = get('Enabled') !== 'False';
    const rotationMinutes = parseInt(get('RotationMinutes') || '60', 10);
    const zonesRaw        = get('Zones') || '';

    // Parsea cada zona: "NombreZona;x1,y1,z1;x2,y2,z2;..."
    const zones = zonesRaw.split('|').map(z => {
      z = z.trim();
      const parts  = z.split(';').map(s => s.trim());
      const name   = parts[0];
      const points = parts.slice(1).map(p => {
        const coords = p.split(',').map(n => parseInt(n.trim(), 10));
        return { x: coords[0], y: coords[1], z: coords[2] };
      }).filter(p => !isNaN(p.x));
      return { name, points };
    }).filter(z => z.name && z.points.length > 0);

    return { enabled, rotationMinutes, zones };
  } catch (err) {
    console.warn('[pvpZoneUtils] No se pudo leer PvpZone.ini:', err.message);
    return { enabled: true, rotationMinutes: 60, zones: [] };
  }
}

/* ─── Tiempo de arranque del PvpZone (desde java0.log) ─────────── */
function getPvpZoneStartTime() {
  try {
    const raw   = fs.readFileSync(GS_LOG_PATH, 'utf8');
    const regex = /^(\d{4}\.\d{2}\.\d{2} \d{2}:\d{2}:\d{2}),\d+\t[^\t]+\t[^\t]+\t[^\t]+\tPvpZone: Loaded \d+ zones\./gm;
    let match, last;
    while ((match = regex.exec(raw)) !== null) { last = match; }
    if (!last) return null;
    const [datePart, timePart] = last[1].split(' ');
    const [y, mo, d] = datePart.split('.');
    const [h, mi, s] = timePart.split(':');
    return new Date(+y, +mo - 1, +d, +h, +mi, +s, 0);
  } catch {
    return null;
  }
}

/* ─── Zona activa actual ──────────────────────────────────────── */
function getActivePvpZone() {
  const { enabled, rotationMinutes, zones } = parsePvpZoneIni();
  if (!enabled || zones.length === 0) {
    return { name: 'Sin zona', index: 0, nextRotationIn: 0, allZones: [], points: [], enabled };
  }
  const rotationMs = rotationMinutes * 60 * 1000;
  const now        = Date.now();
  let   startTime  = getPvpZoneStartTime();
  let   syncSource = 'log';
  if (!startTime) { startTime = new Date(0); syncSource = 'epoch'; }

  const elapsed        = now - startTime.getTime();
  const elapsedInCycle = ((elapsed % rotationMs) + rotationMs) % rotationMs;
  const index          = Math.floor(elapsed / rotationMs) % zones.length;
  const nextRotationIn = Math.ceil((rotationMs - elapsedInCycle) / 1000);

  return {
    name:            zones[index].name,
    index,
    points:          zones[index].points,
    nextRotationIn,
    allZones:        zones.map(z => z.name),
    rotationMinutes,
    enabled,
    syncSource,
    serverStartTime: startTime.toISOString()
  };
}

/* ─── Point-in-polygon (ray casting) ─────────────────────────── */
function pointInPolygon(px, py, polygon) {
  let inside = false;
  for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
    const xi = polygon[i].x, yi = polygon[i].y;
    const xj = polygon[j].x, yj = polygon[j].y;
    const intersect = ((yi > py) !== (yj > py)) &&
      (px < (xj - xi) * (py - yi) / (yj - yi) + xi);
    if (intersect) inside = !inside;
  }
  return inside;
}

/* ─── Bounding box de una zona ────────────────────────────────── */
function getZoneBBox(points) {
  const xs = points.map(p => p.x);
  const ys = points.map(p => p.y);
  return {
    minX: Math.min(...xs), maxX: Math.max(...xs),
    minY: Math.min(...ys), maxY: Math.max(...ys)
  };
}

module.exports = {
  parsePvpZoneIni,
  getActivePvpZone,
  pointInPolygon,
  getZoneBBox
};
