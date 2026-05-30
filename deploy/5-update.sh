#!/bin/bash
# ============================================================
#  L2H5 Web Panel — Actualizar desde GitHub (cero downtime)
#  Ejecutar: bash 5-update.sh
# ============================================================
set -e
APP_DIR="/var/www/l2h5"
echo "➜ Descargando últimos cambios..."
cd "$APP_DIR" && git pull origin master
echo "➜ Actualizando dependencias..."
cd "$APP_DIR/backend" && npm install --production --silent
echo "➜ Reiniciando backend..."
pm2 reload l2h5-backend --update-env
echo "✔ Actualización completada sin downtime"
pm2 list
