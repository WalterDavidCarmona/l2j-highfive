#!/bin/bash
# ============================================================
#  L2H5 Web Panel — Script de instalación automática Ubuntu
#  Ejecutar como root: bash 1-install.sh
# ============================================================
set -e

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
ok()  { echo -e "${GREEN}✔ $1${NC}"; }
info(){ echo -e "${CYAN}➜ $1${NC}"; }
warn(){ echo -e "${YELLOW}⚠ $1${NC}"; }
err() { echo -e "${RED}✘ $1${NC}"; exit 1; }

echo -e "${CYAN}"
echo "╔══════════════════════════════════════════════════════╗"
echo "║        L2H5 Web Panel — Instalador Ubuntu            ║"
echo "╚══════════════════════════════════════════════════════╝"
echo -e "${NC}"

[[ $EUID -ne 0 ]] && err "Ejecutar como root: sudo bash 1-install.sh"

# ── 1. Actualizar sistema ─────────────────────────────────────
info "Actualizando sistema..."
apt-get update -qq && apt-get upgrade -y -qq
ok "Sistema actualizado"

# ── 2. Instalar dependencias base ─────────────────────────────
info "Instalando herramientas base..."
apt-get install -y -qq curl wget git unzip ufw nginx certbot python3-certbot-nginx
ok "Herramientas instaladas"

# ── 3. Node.js 20 LTS ────────────────────────────────────────
info "Instalando Node.js 20 LTS..."
curl -fsSL https://deb.nodesource.com/setup_20.x | bash - > /dev/null 2>&1
apt-get install -y -qq nodejs
ok "Node.js $(node --version) instalado"

# ── 4. PM2 ───────────────────────────────────────────────────
info "Instalando PM2..."
npm install -g pm2 --silent
ok "PM2 $(pm2 --version) instalado"

# ── 5. MySQL 8 ───────────────────────────────────────────────
info "Instalando MySQL 8..."
apt-get install -y -qq mysql-server
systemctl enable mysql --quiet
systemctl start mysql
ok "MySQL instalado y activo"

# ── 6. Firewall ───────────────────────────────────────────────
info "Configurando firewall..."
ufw --force enable > /dev/null
ufw allow OpenSSH    > /dev/null
ufw allow 'Nginx Full' > /dev/null
ufw allow 7777/tcp   > /dev/null  # GameServer
ufw allow 2106/tcp   > /dev/null  # LoginServer
ok "Firewall configurado (SSH, HTTP, HTTPS, L2 puertos)"

# ── 7. Directorio de la app ───────────────────────────────────
APP_DIR="/var/www/l2h5"
mkdir -p "$APP_DIR"
ok "Directorio $APP_DIR creado"

echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  Instalación base completada.${NC}"
echo -e "${GREEN}  Próximo paso: bash 2-deploy.sh${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
