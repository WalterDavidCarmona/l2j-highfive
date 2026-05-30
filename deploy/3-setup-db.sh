#!/bin/bash
# ============================================================
#  L2H5 Web Panel — Configuración de base de datos
#  Ejecutar como root: bash 3-setup-db.sh
# ============================================================
set -e

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
ok()  { echo -e "${GREEN}✔ $1${NC}"; }
info(){ echo -e "${CYAN}➜ $1${NC}"; }
warn(){ echo -e "${YELLOW}⚠ $1${NC}"; }
err() { echo -e "${RED}✘ $1${NC}"; exit 1; }

APP_DIR="/var/www/l2h5"
ENV_FILE="$APP_DIR/backend/.env"

[ ! -f "$ENV_FILE" ] && err "No existe .env en $APP_DIR/backend/.env"

# Leer variables del .env
DB_NAME=$(grep  "^DB_NAME="     "$ENV_FILE" | cut -d= -f2)
DB_USER=$(grep  "^DB_USER="     "$ENV_FILE" | cut -d= -f2)
DB_PASS=$(grep  "^DB_PASSWORD=" "$ENV_FILE" | cut -d= -f2)
DB_HOST=$(grep  "^DB_HOST="     "$ENV_FILE" | cut -d= -f2 | tr -d ' ')

echo -e "${CYAN}"
echo "╔══════════════════════════════════════════════════════╗"
echo "║        L2H5 Web Panel — Setup Base de Datos          ║"
echo "╚══════════════════════════════════════════════════════╝"
echo -e "${NC}"
echo "  BD:       $DB_NAME"
echo "  Usuario:  $DB_USER"
echo "  Host:     $DB_HOST"
echo ""

# ── 1. Crear BD y usuario MySQL ───────────────────────────────
info "Creando base de datos y usuario..."
mysql -u root << SQL
CREATE DATABASE IF NOT EXISTS \`$DB_NAME\`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASS';
GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$DB_USER'@'localhost';
FLUSH PRIVILEGES;
SQL
ok "Base de datos '$DB_NAME' y usuario '$DB_USER' creados"

# ── 2. Ejecutar setup.sql (tablas web) ───────────────────────
info "Creando tablas del panel web..."
mysql -u root "$DB_NAME" < "$APP_DIR/backend/setup.sql"
ok "Tablas del panel creadas"

# ── 3. Verificar conexión ─────────────────────────────────────
info "Verificando conexión desde Node.js..."
cd "$APP_DIR/backend"
node -e "
require('dotenv').config();
const mysql = require('mysql2/promise');
mysql.createConnection({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME
}).then(c => { console.log('Conexión OK'); c.end(); process.exit(0); })
  .catch(e => { console.error('Error:', e.message); process.exit(1); });
" && ok "Conexión a MySQL verificada" || err "Fallo la conexión — revisá DB_USER y DB_PASSWORD en .env"

# ── 4. Reiniciar backend ──────────────────────────────────────
info "Reiniciando backend..."
pm2 restart l2h5-backend
ok "Backend reiniciado"

echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  Base de datos configurada correctamente.${NC}"
echo ""
echo -e "${YELLOW}  IMPORTANTE: La BD de L2JMobius (personajes, cuentas,${NC}"
echo -e "${YELLOW}  etc.) debe ser importada por separado.${NC}"
echo -e "${YELLOW}  El panel web solo crea sus propias tablas auxiliares.${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
