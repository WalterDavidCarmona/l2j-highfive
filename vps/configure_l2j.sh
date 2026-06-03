#!/bin/bash
# =============================================================================
#  L2J Zona Zero - Configurar servidor con IP del VPS y credenciales DB
#
#  Ejecutar DESPUES de copiar los archivos al VPS:
#    sudo ./configure_l2j.sh
# =============================================================================

set -e
RED='\033[0;31m'; GREEN='\033[0;32m'; CYAN='\033[0;36m'; NC='\033[0m'
ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
info() { echo -e "${CYAN}[INFO]${NC} $1"; }

L2J_HOME="/opt/l2j"
CREDS="$L2J_HOME/.db_credentials"

# Leer credenciales guardadas por install.sh
if [[ -f "$CREDS" ]]; then
    source "$CREDS"
    info "Credenciales DB cargadas desde $CREDS"
else
    read -p "DB_USER: " DB_USER
    read -sp "DB_PASS: " DB_PASS; echo
    DB_NAME="l2jmobiush5"
fi

VPS_IP=$(curl -s ifconfig.me 2>/dev/null || hostname -I | awk '{print $1}')
info "IP del VPS: $VPS_IP"

# ── Database.ini - Game ───────────────────────────────────────────────────────
DB_URL="jdbc:mysql://localhost/${DB_NAME}?useUnicode=true&characterEncoding=utf-8&allowPublicKeyRetrieval=true&useSSL=false&connectTimeout=10000&sessionVariables=wait_timeout=28800,interactive_timeout=28800&autoReconnect=true"

for DB_INI in "$L2J_HOME/game/config/Database.ini" "$L2J_HOME/login/config/Database.ini"; do
    if [[ -f "$DB_INI" ]]; then
        sed -i "s|URL = .*|URL = $DB_URL|g"   "$DB_INI"
        sed -i "s|Login = .*|Login = $DB_USER|g" "$DB_INI"
        sed -i "s|Password = .*|Password = $DB_PASS|g" "$DB_INI"
        # Eliminar ruta Windows de MySql
        sed -i "s|MySqlBinLocation = .*|MySqlBinLocation = /usr/bin/|g" "$DB_INI"
        ok "DB configurada: $DB_INI"
    fi
done

# ── Server.ini - GameServer hostname ─────────────────────────────────────────
GAME_SERVER_INI="$L2J_HOME/game/config/Server.ini"
if [[ -f "$GAME_SERVER_INI" ]]; then
    sed -i "s|GameserverHostname = .*|GameserverHostname = $VPS_IP|g" "$GAME_SERVER_INI"
    ok "GameserverHostname actualizado a $VPS_IP"
fi

# ── LoginServer.ini ───────────────────────────────────────────────────────────
LOGIN_INI="$L2J_HOME/login/config/LoginServer.ini"
if [[ -f "$LOGIN_INI" ]]; then
    sed -i "s|LoginHostname = .*|LoginHostname = $VPS_IP|g" "$LOGIN_INI"
    ok "LoginServer configurado"
fi

# ── Eliminar rutas Windows de backups ─────────────────────────────────────────
for ini in "$L2J_HOME/game/config/Database.ini" "$L2J_HOME/login/config/Database.ini"; do
    if [[ -f "$ini" ]]; then
        sed -i 's|BackupPath = .*|BackupPath = ../backup/|g' "$ini"
    fi
done

# ── Permisos ──────────────────────────────────────────────────────────────────
chown -R l2j:l2j "$L2J_HOME"
chmod +x "$L2J_HOME"/game/*.sh "$L2J_HOME"/login/*.sh 2>/dev/null || true
chmod +x "$L2J_HOME"/scripts/*.sh

ok "Configuracion completada para IP: $VPS_IP"
echo ""
echo "Siguiente paso: importar base de datos"
echo "  sudo ./scripts/import_db.sh"
