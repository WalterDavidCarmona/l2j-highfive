# 🚀 Despliegue en Ubuntu — L2H5 Web Panel

## Requisitos
- Ubuntu 22.04 LTS (VPS mínimo: 4GB RAM / 2 vCPU)
- Acceso root por SSH
- Dominio apuntando a la IP del VPS (para HTTPS)

---

## Pasos en orden

### 1. Conectarse al VPS
```bash
ssh root@IP_DEL_VPS
```

### 2. Instalación base (Node, MySQL, Nginx, PM2)
```bash
bash 1-install.sh
```

### 3. Clonar repo y hacer deploy
```bash
bash 2-deploy.sh
```

### 4. Configurar el .env con tus datos reales
```bash
nano /var/www/l2h5/backend/.env
```
Cambiar obligatoriamente:
- `DB_PASSWORD` — contraseña para el usuario MySQL
- `JWT_SECRET` — clave secreta larga y aleatoria
- `NODE_ENV=production`
- `APP_BASE_URL=https://tu-dominio.com`
- `PVP_ZONE_INI_PATH` — ruta al PvpZone.ini del servidor L2
- Credenciales de MercadoPago / PayPal si usás pagos

### 5. Configurar base de datos
```bash
bash 3-setup-db.sh
```

### 6. Habilitar HTTPS (necesitás un dominio apuntando al VPS)
```bash
bash 4-ssl.sh tu-dominio.com
```

---

## Actualizar el panel (después de hacer cambios)
```bash
cd /opt/deploy && bash 5-update.sh
```

---

## Comandos útiles

| Comando | Descripción |
|---------|-------------|
| `pm2 list` | Ver estado del servidor |
| `pm2 logs l2h5-backend` | Ver logs en tiempo real |
| `pm2 restart l2h5-backend` | Reiniciar el backend |
| `pm2 stop l2h5-backend` | Detener el backend |
| `systemctl status nginx` | Estado de Nginx |
| `nginx -t` | Verificar config de Nginx |

---

## Arquitectura en producción

```
Internet (443/80)
      │
   Nginx
   ├── /assets/*  → /var/www/l2h5/frontend  (archivos estáticos)
   └── /api/*     → Node.js :3000
                        │
                     MySQL :3306
                        │
                  L2JMobius :7777
```
