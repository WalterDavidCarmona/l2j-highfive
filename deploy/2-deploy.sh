#!/bin/bash
# ============================================================
#  L2H5 Web Panel — Deploy del código desde GitHub
#  Ejecutar como root: bash 2-deploy.sh
# ============================================================
set -e

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
ok()  { echo -e "${GREEN}✔ $1${NC}"; }
info(){ echo -e "${CYAN}➜ $1${NC}"; }
warn(){ echo -e "${YELLOW}⚠ $1${NC}"; }
err() { echo -e "${RED}✘ $1${NC}"; exit 1; }

APP_DIR="/var/www/l2h5"
REPO="https://github.com/WalterDavidCarmona/l2j-highfive.git"

echo -e "${CYAN}"
echo "╔══════════════════════════════════════════════════════╗"
echo "║        L2H5 Web Panel — Deploy desde GitHub          ║"
echo "╚══════════════════════════════════════════════════════╝"
echo -e "${NC}"

# ── 1. Clonar / actualizar repo ───────────────────────────────
info "Descargando código desde GitHub..."
if [ -d "$APP_DIR/.git" ]; then
    cd "$APP_DIR" && git pull origin master
    ok "Código actualizado"
else
    git clone "$REPO" "$APP_DIR"
    ok "Repositorio clonado"
fi

# ── 2. Instalar dependencias Node ─────────────────────────────
info "Instalando dependencias del backend..."
cd "$APP_DIR/backend"
npm install --production --silent
ok "Dependencias instaladas"

# ── 3. Crear .env si no existe ───────────────────────────────
if [ ! -f "$APP_DIR/backend/.env" ]; then
    warn ".env no encontrado — creando desde plantilla..."
    cp "$APP_DIR/backend/.env.example" "$APP_DIR/backend/.env"
    warn "IMPORTANTE: Editá $APP_DIR/backend/.env con tus datos reales"
    warn "Luego ejecutá: bash 3-setup-db.sh"
else
    ok ".env ya existe"
fi

# ── 4. Permisos ───────────────────────────────────────────────
chown -R www-data:www-data "$APP_DIR/frontend" 2>/dev/null || true
chmod -R 755 "$APP_DIR/frontend"
ok "Permisos configurados"

# ── 5. Configurar Nginx ───────────────────────────────────────
info "Configurando Nginx..."
DOMAIN=$(grep "APP_BASE_URL" "$APP_DIR/backend/.env" | cut -d'/' -f3 | cut -d':' -f1)
[ -z "$DOMAIN" ] && DOMAIN="localhost"

cat > /etc/nginx/sites-available/l2h5 << NGINX
server {
    listen 80;
    server_name $DOMAIN;

    # Frontend estático
    root /var/www/l2h5/frontend;
    index index.html;

    # Archivos estáticos directamente
    location /assets/ {
        expires 7d;
        add_header Cache-Control "public, immutable";
    }

    # API → Node.js
    location /api/ {
        proxy_pass         http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header   Upgrade \$http_upgrade;
        proxy_set_header   Connection 'upgrade';
        proxy_set_header   Host \$host;
        proxy_set_header   X-Real-IP \$remote_addr;
        proxy_set_header   X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto \$scheme;
        proxy_cache_bypass \$http_upgrade;
        proxy_read_timeout 60s;
    }

    # SPA fallback
    location / {
        try_files \$uri \$uri/ /index.html;
    }
}
NGINX

ln -sf /etc/nginx/sites-available/l2h5 /etc/nginx/sites-enabled/l2h5
rm -f /etc/nginx/sites-enabled/default
nginx -t && systemctl reload nginx
ok "Nginx configurado para dominio: $DOMAIN"

# ── 6. PM2 — iniciar backend ─────────────────────────────────
info "Iniciando backend con PM2..."
cd "$APP_DIR/backend"
pm2 delete l2h5-backend 2>/dev/null || true
pm2 start server.js --name "l2h5-backend" \
    --restart-delay=3000 \
    --max-restarts=10 \
    --log "$APP_DIR/backend/server.log" \
    --error "$APP_DIR/backend/server-error.log"

pm2 save
pm2 startup systemd -u root --hp /root | tail -1 | bash 2>/dev/null || true
ok "Backend corriendo con PM2"

echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  Deploy completado.${NC}"
echo ""
echo -e "${YELLOW}  Próximos pasos:${NC}"
echo -e "${YELLOW}  1. Editá el .env:  nano $APP_DIR/backend/.env${NC}"
echo -e "${YELLOW}  2. Configurá la BD: bash 3-setup-db.sh${NC}"
echo -e "${YELLOW}  3. HTTPS (opcional): bash 4-ssl.sh tu-dominio.com${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
