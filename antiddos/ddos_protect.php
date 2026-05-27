<?php
/**
 * ============================================================
 *  L2H5 Anti-DDoS / Rate Limiting Layer (PHP)
 *  Incluir al inicio de index.php o como auto_prepend_file
 *
 *  Protecciones implementadas:
 *   1. Rate limiting por IP (solicitudes/minuto)
 *   2. Bloqueo de User-Agents vacíos / bots conocidos
 *   3. Blacklist de IPs manual
 *   4. Throttle por endpoint sensible (register, login)
 *   5. Detección de floods (>N req en X segundos)
 *   6. Whitelist de IPs administrativas
 * ============================================================
 */

// ── Configuración ────────────────────────────────────────────
define('DDOS_ENABLED',          true);
define('DDOS_RATE_LIMIT',       60);    // Max requests/minuto por IP
define('DDOS_AUTH_RATE_LIMIT',  10);    // Max intentos auth/minuto
define('DDOS_BAN_DURATION',     600);   // Segundos de bloqueo tras flood
define('DDOS_FLOOD_THRESHOLD',  30);    // Requests en FLOOD_WINDOW = ban
define('DDOS_FLOOD_WINDOW',     10);    // Ventana de detección (seg)
define('DDOS_STORAGE_DIR',      __DIR__ . '/data/');
define('DDOS_LOG_FILE',         __DIR__ . '/logs/ddos.log');
define('DDOS_WHITELIST_FILE',   __DIR__ . '/whitelist.txt');
define('DDOS_BLACKLIST_FILE',   __DIR__ . '/blacklist.txt');

// ── Whitelist (siempre permitir) ─────────────────────────────
$WHITELIST = [
    '127.0.0.1',
    '::1',
];

// ── Blacklist manual de User-Agents ─────────────────────────
$BAD_UA_PATTERNS = [
    '/python-requests/i',
    '/curl\//i',
    '/libwww/i',
    '/scrapy/i',
    '/masscan/i',
    '/nmap/i',
    '/nikto/i',
    '/sqlmap/i',
    '/acunetix/i',
    '/havij/i',
    '/zgrab/i',
];

// ── Endpoints sensibles (rate limit extra bajo) ──────────────
$AUTH_ENDPOINTS = ['/register', '/login', '/change-password', '/api/auth'];

// ────────────────────────────────────────────────────────────

if (!DDOS_ENABLED) return;

// 1. Crear directorios necesarios
foreach ([DDOS_STORAGE_DIR, dirname(DDOS_LOG_FILE)]) {
    if (!is_dir($_)) @mkdir($_, 0750, true);
}

// 2. Obtener IP real del visitante
function getClientIP(): string {
    $headers = ['HTTP_CF_CONNECTING_IP', 'HTTP_X_REAL_IP', 'HTTP_X_FORWARDED_FOR', 'REMOTE_ADDR'];
    foreach ($headers as $h) {
        if (!empty($_SERVER[$h])) {
            $ip = trim(explode(',', $_SERVER[$h])[0]);
            if (filter_var($ip, FILTER_VALIDATE_IP, FILTER_FLAG_NO_PRIV_RANGE | FILTER_FLAG_NO_RES_RANGE)) {
                return $ip;
            }
        }
    }
    return $_SERVER['REMOTE_ADDR'] ?? '0.0.0.0';
}

// 3. Bloquear y responder
function ddos_block(string $ip, string $reason, int $code = 429): never {
    $logLine = sprintf("[%s] BLOCKED ip=%s reason=%s ua='%s' uri='%s'\n",
        date('Y-m-d H:i:s'), $ip, $reason,
        $_SERVER['HTTP_USER_AGENT'] ?? '-',
        $_SERVER['REQUEST_URI'] ?? '-'
    );
    @file_put_contents(DDOS_LOG_FILE, $logLine, FILE_APPEND | LOCK_EX);

    http_response_code($code);
    header('Content-Type: application/json');
    header('Retry-After: 60');
    echo json_encode(['error' => 'Demasiadas solicitudes. Intenta más tarde.', 'code' => $code]);
    exit;
}

// 4. Leer/escribir archivo de datos de IP
function ddos_read(string $file): array {
    if (!file_exists($file)) return [];
    $data = @json_decode(@file_get_contents($file), true);
    return is_array($data) ? $data : [];
}
function ddos_write(string $file, array $data): void {
    @file_put_contents($file, json_encode($data), LOCK_EX);
}

// ─── INICIO DE PROTECCIONES ──────────────────────────────────
$ip      = getClientIP();
$ipHash  = 'ip_' . md5($ip);
$ipFile  = DDOS_STORAGE_DIR . $ipHash . '.json';
$now     = time();
$uri     = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH);
$ua      = $_SERVER['HTTP_USER_AGENT'] ?? '';

// A) Whitelist permanente (IPs locales y admin)
$whitelist = array_merge($WHITELIST, array_filter(
    file_exists(DDOS_WHITELIST_FILE) ? file(DDOS_WHITELIST_FILE, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES) : [],
    fn($l) => strlen(trim($l)) > 0 && $l[0] !== '#'
));
if (in_array($ip, $whitelist)) goto END_DDOS;

// B) Blacklist manual
$blacklist = file_exists(DDOS_BLACKLIST_FILE)
    ? array_filter(file(DDOS_BLACKLIST_FILE, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES),
        fn($l) => strlen(trim($l)) > 0 && $l[0] !== '#')
    : [];
if (in_array($ip, array_map('trim', $blacklist))) {
    ddos_block($ip, 'manual_blacklist', 403);
}

// C) User-Agent vacío o sospechoso
if (empty($ua)) ddos_block($ip, 'empty_ua');
foreach ($BAD_UA_PATTERNS as $pattern) {
    if (preg_match($pattern, $ua)) ddos_block($ip, 'bad_ua:' . $pattern, 403);
}

// D) Leer datos de la IP
$ipData = ddos_read($ipFile);
if (!isset($ipData['requests'])) $ipData['requests'] = [];
if (!isset($ipData['ban_until'])) $ipData['ban_until'] = 0;
if (!isset($ipData['auth_requests'])) $ipData['auth_requests'] = [];

// E) ¿Está baneada?
if ($ipData['ban_until'] > $now) {
    $remaining = $ipData['ban_until'] - $now;
    ddos_block($ip, "banned_until={$ipData['ban_until']} (${remaining}s remaining)");
}

// F) Limpiar requests viejas (>60s)
$ipData['requests']      = array_filter($ipData['requests'],      fn($t) => $t > $now - 60);
$ipData['auth_requests'] = array_filter($ipData['auth_requests'], fn($t) => $t > $now - 60);

// G) Detección de flood (FLOOD_THRESHOLD requests en FLOOD_WINDOW seg)
$recentRequests = array_filter($ipData['requests'], fn($t) => $t > $now - DDOS_FLOOD_WINDOW);
if (count($recentRequests) >= DDOS_FLOOD_THRESHOLD) {
    $ipData['ban_until'] = $now + DDOS_BAN_DURATION;
    ddos_write($ipFile, $ipData);
    ddos_block($ip, 'flood_detected');
}

// H) Rate limit global
if (count($ipData['requests']) >= DDOS_RATE_LIMIT) {
    ddos_block($ip, 'rate_limit_exceeded');
}

// I) Rate limit para endpoints de autenticación
$isAuthEndpoint = false;
foreach ($AUTH_ENDPOINTS as $ep) {
    if (stripos($uri, $ep) !== false) { $isAuthEndpoint = true; break; }
}
if ($isAuthEndpoint) {
    if (count($ipData['auth_requests']) >= DDOS_AUTH_RATE_LIMIT) {
        $ipData['ban_until'] = $now + DDOS_BAN_DURATION;
        ddos_write($ipFile, $ipData);
        ddos_block($ip, 'auth_rate_limit_exceeded');
    }
    $ipData['auth_requests'][] = $now;
}

// J) Registrar request
$ipData['requests'][] = $now;
ddos_write($ipFile, $ipData);

// ─── Limpieza periódica (1% de probabilidad por request) ─────
if (mt_rand(0, 99) === 0) {
    $files = glob(DDOS_STORAGE_DIR . 'ip_*.json');
    foreach (($files ?: []) as $f) {
        $d = ddos_read($f);
        $isExpiredBan = ($d['ban_until'] ?? 0) <= $now;
        $hasOldReqs   = empty(array_filter($d['requests'] ?? [], fn($t) => $t > $now - 300));
        if ($isExpiredBan && $hasOldReqs) @unlink($f);
    }
}

END_DDOS:
// ─── Script PHP de acceso a la BD para proxy ligero ──────────
// Cuando el sitio usa PHP + Nginx/Apache como proxy para Node.js
