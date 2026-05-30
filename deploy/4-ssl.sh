#!/bin/bash
# ============================================================
#  L2H5 Web Panel — HTTPS con Let's Encrypt (gratis)
#  Uso: bash 4-ssl.sh tu-dominio.com
# ============================================================
set -e

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
ok()  { echo -e "${GREEN}✔ $1${NC}"; }
info(){ echo -e "${CYAN}➜ $1${NC}"; }
err() { echo -e "${RED}✘ $1${NC}"; exit 1; }

DOMAIN="$1"
[ -z "$DOMAIN" ] && err "Uso: bash 4-ssl.sh tu-dominio.com"

echo -e "${CYAN}"
echo "╔══════════════════════════════════════════════════════╗"
echo "║        L2H5 Web Panel — Configurar HTTPS             ║"
echo "╚══════════════════════════════════════════════════════╝"
echo -e "${NC}"
echo "  Dominio: $DOMAIN"
echo ""

# Actualizar nginx config con el dominio correcto
sed -i "s/server_name .*/server_name $DOMAIN www.$DOMAIN;/" /etc/nginx/sites-available/l2h5
nginx -t && systemctl reload nginx
ok "Nginx actualizado con dominio $DOMAIN"

# Obtener certificado SSL
info "Obteniendo certificado Let's Encrypt..."
certbot --nginx -d "$DOMAIN" -d "www.$DOMAIN" --non-interactive --agree-tos --email "admin@$DOMAIN" --redirect
ok "HTTPS habilitado para $DOMAIN"

# Actualizar .env con la URL correcta
APP_DIR="/var/www/l2h5"
sed -i "s|APP_BASE_URL=.*|APP_BASE_URL=https://$DOMAIN|" "$APP_DIR/backend/.env"
pm2 restart l2h5-backend
ok "Backend reiniciado con nueva URL"

# Renovación automática
(crontab -l 2>/dev/null; echo "0 12 * * * /usr/bin/certbot renew --quiet") | crontab -
ok "Renovación automática configurada (cada día a las 12:00)"

echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  HTTPS activo en: https://$DOMAIN${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
