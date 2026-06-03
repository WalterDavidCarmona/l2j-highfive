#!/bin/bash
# =============================================================================
#  L2J Zona Zero - Script de instalacion en Ubuntu VPS
#  Probado en Ubuntu 22.04 LTS / 24.04 LTS
#
#  Uso:
#    chmod +x install.sh
#    sudo ./install.sh
# =============================================================================

set -e

# ── Colores ───────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()    { echo -e "${CYAN}[INFO]${NC} $1"; }
ok()      { echo -e "${GREEN}[OK]${NC} $1"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
error()   { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# ── Configuracion ─────────────────────────────────────────────────────────────
L2J_USER="l2j"
L2J_HOME="/opt/l2j"
DB_NAME="l2jmobiush5"
DB_USER="l2j"
DB_PASS="l2j_$(openssl rand -hex 8)"   # generada al instalar
JAVA_VER="21"

# IPs del servidor (se detectan automaticamente, editar si es necesario)
VPS_IP=$(curl -s ifconfig.me 2>/dev/null || hostname -I | awk '{print $1}')

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║         L2J Zona Zero - Instalador VPS Ubuntu                ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
info "IP detectada del VPS: $VPS_IP"
info "Usuario L2J:          $L2J_USER"
info "Directorio:           $L2J_HOME"
info "Base de datos:        $DB_NAME"
echo ""

# ── Verificar root ────────────────────────────────────────────────────────────
[[ $EUID -ne 0 ]] && error "Ejecutar como root: sudo ./install.sh"

# ── 1. Actualizar sistema ─────────────────────────────────────────────────────
info "Actualizando sistema..."
apt-get update -qq && apt-get upgrade -y -qq
ok "Sistema actualizado"

# ── 2. Instalar dependencias ──────────────────────────────────────────────────
info "Instalando dependencias..."
apt-get install -y -qq \
  openjdk-${JAVA_VER}-jdk \
  mariadb-server \
  mariadb-client \
  ufw \
  curl \
  wget \
  screen \
  unzip \
  htop \
  nano \
  git \
  lsof \
  net-tools \
  nginx

ok "Dependencias instaladas"

# ── 3. Verificar Java ─────────────────────────────────────────────────────────
JAVA_INSTALLED=$(java -version 2>&1 | head -1)
info "Java: $JAVA_INSTALLED"

# ── 4. Crear usuario l2j ──────────────────────────────────────────────────────
if ! id "$L2J_USER" &>/dev/null; then
    useradd -r -m -d "$L2J_HOME" -s /bin/bash "$L2J_USER"
    ok "Usuario '$L2J_USER' creado"
else
    warn "Usuario '$L2J_USER' ya existe"
fi

mkdir -p "$L2J_HOME"
chown "$L2J_USER:$L2J_USER" "$L2J_HOME"

# ── 5. Configurar MariaDB ─────────────────────────────────────────────────────
info "Configurando MariaDB..."
systemctl enable mariadb --quiet
systemctl start mariadb

# Seguridad basica y creacion de DB/usuario
mysql -e "
CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'localhost';
FLUSH PRIVILEGES;
" 2>/dev/null

# Guardar credenciales
cat > "$L2J_HOME/.db_credentials" << EOF
DB_NAME=${DB_NAME}
DB_USER=${DB_USER}
DB_PASS=${DB_PASS}
EOF
chmod 600 "$L2J_HOME/.db_credentials"
chown "$L2J_USER:$L2J_USER" "$L2J_HOME/.db_credentials"

ok "MariaDB configurado | DB: $DB_NAME | User: $DB_USER | Pass: $DB_PASS"

# ── 6. Estructura de directorios ──────────────────────────────────────────────
info "Creando estructura de directorios..."
mkdir -p "$L2J_HOME"/{game,login,libs,backup,logs,scripts}
chown -R "$L2J_USER:$L2J_USER" "$L2J_HOME"
ok "Directorios creados en $L2J_HOME"

# ── 7. Configurar firewall UFW ────────────────────────────────────────────────
info "Configurando firewall..."
ufw --force reset > /dev/null 2>&1
ufw default deny incoming > /dev/null 2>&1
ufw default allow outgoing > /dev/null 2>&1
ufw allow 22/tcp    comment 'SSH'        > /dev/null 2>&1
ufw allow 7777/tcp  comment 'L2J Game'   > /dev/null 2>&1
ufw allow 9014/tcp  comment 'L2J Login'  > /dev/null 2>&1
ufw allow 80/tcp    comment 'HTTP Web'   > /dev/null 2>&1
ufw allow 443/tcp   comment 'HTTPS Web'  > /dev/null 2>&1
ufw --force enable > /dev/null 2>&1
ok "Firewall configurado (SSH:22, Game:7777, Login:9014, Web:80/443)"

# ── 8. Crear scripts de inicio ────────────────────────────────────────────────
info "Creando scripts de inicio..."

# start_login.sh
cat > "$L2J_HOME/scripts/start_login.sh" << 'SCRIPT'
#!/bin/bash
cd /opt/l2j/login
screen -dmS l2j_login java \
  -server -Dfile.encoding=UTF-8 \
  -Dorg.slf4j.simpleLogger.log.com.zaxxer.hikari=warn \
  -XX:+UseZGC -Xms128m -Xmx256m \
  -jar libs/LoginServer.jar
echo "[L2J] LoginServer iniciado en screen 'l2j_login'"
SCRIPT

# start_game.sh
cat > "$L2J_HOME/scripts/start_game.sh" << 'SCRIPT'
#!/bin/bash
cd /opt/l2j/game
screen -dmS l2j_game java \
  -server -Dfile.encoding=UTF-8 \
  -Djava.util.logging.manager=org.l2jmobius.log.ServerLogManager \
  -Dorg.slf4j.simpleLogger.log.com.zaxxer.hikari=warn \
  -XX:+UseZGC -Xmx4g -Xms2g \
  -jar libs/GameServer.jar
echo "[L2J] GameServer iniciado en screen 'l2j_game'"
SCRIPT

# stop_all.sh
cat > "$L2J_HOME/scripts/stop_all.sh" << 'SCRIPT'
#!/bin/bash
echo "[L2J] Deteniendo servidores..."
screen -S l2j_game  -X quit 2>/dev/null && echo "  GameServer detenido"  || echo "  GameServer no estaba activo"
screen -S l2j_login -X quit 2>/dev/null && echo "  LoginServer detenido" || echo "  LoginServer no estaba activo"
SCRIPT

# status.sh
cat > "$L2J_HOME/scripts/status.sh" << 'SCRIPT'
#!/bin/bash
echo "═══════════════════════════════════"
echo "  L2J Zona Zero - Estado"
echo "═══════════════════════════════════"
if screen -list | grep -q "l2j_game"; then
    echo "  GameServer:  ACTIVO  ✓"
else
    echo "  GameServer:  DETENIDO ✗"
fi
if screen -list | grep -q "l2j_login"; then
    echo "  LoginServer: ACTIVO  ✓"
else
    echo "  LoginServer: DETENIDO ✗"
fi
echo "  MariaDB: $(systemctl is-active mariadb)"
echo "  Nginx:   $(systemctl is-active nginx)"
echo ""
echo "  Puertos abiertos:"
ss -tlnp | grep -E ":7777|:9014|:80 |:443 " | awk '{print "    " $4}'
echo "═══════════════════════════════════"
SCRIPT

# restart_all.sh
cat > "$L2J_HOME/scripts/restart_all.sh" << 'SCRIPT'
#!/bin/bash
cd /opt/l2j
./scripts/stop_all.sh
sleep 5
./scripts/start_login.sh
sleep 3
./scripts/start_game.sh
SCRIPT

chmod +x "$L2J_HOME"/scripts/*.sh
chown -R "$L2J_USER:$L2J_USER" "$L2J_HOME/scripts"
ok "Scripts creados en $L2J_HOME/scripts/"

# ── 9. Servicio systemd para auto-inicio ──────────────────────────────────────
info "Configurando servicios systemd..."

# LoginServer service
cat > /etc/systemd/system/l2j-login.service << EOF
[Unit]
Description=L2J Zona Zero - LoginServer
After=network.target mariadb.service
Requires=mariadb.service

[Service]
Type=forking
User=${L2J_USER}
WorkingDirectory=${L2J_HOME}/login
ExecStart=/bin/bash ${L2J_HOME}/scripts/start_login.sh
ExecStop=/bin/bash -c 'screen -S l2j_login -X quit'
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# GameServer service
cat > /etc/systemd/system/l2j-game.service << EOF
[Unit]
Description=L2J Zona Zero - GameServer
After=network.target mariadb.service l2j-login.service
Requires=mariadb.service
Wants=l2j-login.service

[Service]
Type=forking
User=${L2J_USER}
WorkingDirectory=${L2J_HOME}/game
ExecStart=/bin/bash ${L2J_HOME}/scripts/start_game.sh
ExecStop=/bin/bash -c 'screen -S l2j_game -X quit'
Restart=on-failure
RestartSec=15

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable l2j-login l2j-game
ok "Servicios systemd configurados (auto-inicio habilitado)"

# ── 10. Configurar Nginx para web panel ───────────────────────────────────────
info "Configurando Nginx..."

cat > /etc/nginx/sites-available/l2j-panel << EOF
server {
    listen 80;
    server_name ${VPS_IP} _;

    root ${L2J_HOME}/web;
    index index.html;

    location / {
        try_files \$uri \$uri/ =404;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
    }

    # Status del servidor en tiempo real (via server-sent events o JSON)
    location /status.json {
        alias ${L2J_HOME}/web/status.json;
        add_header Cache-Control "no-cache, no-store";
        add_header Content-Type "application/json";
    }
}
EOF

ln -sf /etc/nginx/sites-available/l2j-panel /etc/nginx/sites-enabled/l2j-panel
rm -f /etc/nginx/sites-enabled/default
nginx -t && systemctl enable nginx --quiet && systemctl restart nginx
ok "Nginx configurado en puerto 80"

# ── 11. Cron para actualizar status.json ──────────────────────────────────────
cat > "$L2J_HOME/scripts/update_status.sh" << 'SCRIPT'
#!/bin/bash
GAME_STATUS=$(screen -list | grep -q "l2j_game"  && echo "online" || echo "offline")
LOGIN_STATUS=$(screen -list | grep -q "l2j_login" && echo "online" || echo "offline")
PLAYERS=$(ss -tn | grep -c ":7777" 2>/dev/null || echo "0")
UPTIME=$(uptime -p 2>/dev/null | sed 's/up //')

cat > /opt/l2j/web/status.json << JSON
{
  "game":   "${GAME_STATUS}",
  "login":  "${LOGIN_STATUS}",
  "players": ${PLAYERS},
  "uptime": "${UPTIME}",
  "updated": "$(date '+%Y-%m-%d %H:%M:%S')"
}
JSON
SCRIPT
chmod +x "$L2J_HOME/scripts/update_status.sh"

# Cron cada 30 segundos
(crontab -l -u "$L2J_USER" 2>/dev/null; echo "* * * * * /opt/l2j/scripts/update_status.sh") | crontab -u "$L2J_USER" -
(crontab -l -u "$L2J_USER" 2>/dev/null; echo "* * * * * sleep 30 && /opt/l2j/scripts/update_status.sh") | crontab -u "$L2J_USER" -
ok "Cron configurado para actualizar status cada 30s"

# ── 12. Script de deploy/copia desde Windows ──────────────────────────────────
cat > "$L2J_HOME/scripts/deploy.sh" << 'SCRIPT'
#!/bin/bash
# Uso: ./deploy.sh <ruta_zip_del_proyecto>
# Ejemplo: ./deploy.sh /tmp/l2j-highfive.zip
#
# En Windows (PowerShell), comprimir y enviar al VPS:
#   Compress-Archive -Path "C:\Users\david\OneDrive\Desktop\l2j\*" -DestinationPath "l2j.zip"
#   scp l2j.zip l2j@VPS_IP:/tmp/

ZIP=$1
[[ -z "$ZIP" ]] && echo "Uso: ./deploy.sh <archivo.zip>" && exit 1
[[ ! -f "$ZIP" ]] && echo "Archivo no encontrado: $ZIP" && exit 1

echo "[Deploy] Deteniendo servidores..."
/opt/l2j/scripts/stop_all.sh

echo "[Deploy] Extrayendo $ZIP..."
cd /opt/l2j && unzip -o "$ZIP" -x "*.log" "log/*" "backup/*"

echo "[Deploy] Ajustando permisos..."
chown -R l2j:l2j /opt/l2j

echo "[Deploy] Iniciando servidores..."
sudo -u l2j /opt/l2j/scripts/start_login.sh
sleep 3
sudo -u l2j /opt/l2j/scripts/start_game.sh

echo "[Deploy] Listo."
SCRIPT
chmod +x "$L2J_HOME/scripts/deploy.sh"

# ── Resumen final ─────────────────────────────────────────────────────────────
echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║              INSTALACION COMPLETADA                          ║"
echo "╠══════════════════════════════════════════════════════════════╣"
printf "║  IP del VPS:      %-42s ║\n" "$VPS_IP"
printf "║  Directorio:      %-42s ║\n" "$L2J_HOME"
printf "║  DB Usuario:      %-42s ║\n" "$DB_USER"
printf "║  DB Password:     %-42s ║\n" "$DB_PASS"
printf "║  DB Nombre:       %-42s ║\n" "$DB_NAME"
echo "╠══════════════════════════════════════════════════════════════╣"
echo "║  PROXIMOS PASOS:                                             ║"
echo "║  1. Copiar archivos del proyecto al VPS (ver transfer.ps1)   ║"
echo "║  2. Importar base de datos: ./scripts/import_db.sh           ║"
echo "║  3. Actualizar configs con IP del VPS                        ║"
echo "║  4. systemctl start l2j-login l2j-game                      ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
warn "Guardar credenciales DB en: $L2J_HOME/.db_credentials"
