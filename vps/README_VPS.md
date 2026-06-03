# L2J Zona Zero - Migración a VPS Ubuntu

## Archivos en esta carpeta

| Archivo | Descripción |
|---|---|
| `install.sh` | Instalación completa en VPS Ubuntu (Java, MariaDB, Nginx, systemd) |
| `configure_l2j.sh` | Ajusta configs con IP del VPS y credenciales DB |
| `import_db.sh` | Importa la base de datos (desde dump o scripts SQL) |
| `transfer_to_vps.ps1` | Script PowerShell para copiar el proyecto desde Windows |
| `web/index.html` | Página web de estado del servidor |
| `web/status.json` | JSON de estado (actualizado cada 30s por cron) |

---

## Paso a Paso

### 1. Preparar el VPS (Ubuntu 22.04 o 24.04)

```bash
# Conectarse al VPS
ssh root@TU_IP_VPS

# Subir install.sh al VPS
scp vps/install.sh root@TU_IP_VPS:/root/

# Ejecutar instalador
chmod +x /root/install.sh
sudo /root/install.sh
```

### 2. Transferir el proyecto desde Windows (PowerShell)

```powershell
# Transferencia completa
.\vps\transfer_to_vps.ps1 -VpsIP "TU_IP_VPS" -VpsUser "l2j"

# Solo actualizar game data (sin reinstalar todo)
.\vps\transfer_to_vps.ps1 -VpsIP "TU_IP_VPS" -GameOnly

# Solo actualizar configs
.\vps\transfer_to_vps.ps1 -VpsIP "TU_IP_VPS" -ConfigOnly
```

### 3. Configurar en el VPS

```bash
# Desde el VPS como l2j
sudo /opt/l2j/scripts/configure_l2j.sh
```

### 4. Importar base de datos

**Opción A: Exportar desde XAMPP local y subir el dump**
```powershell
# En Windows - exportar DB de XAMPP
& "C:\xampp\mysql\bin\mysqldump.exe" -u root l2jmobiush5 > l2j_backup.sql
scp l2j_backup.sql l2j@TU_IP_VPS:/tmp/
```

```bash
# En el VPS - importar
sudo /opt/l2j/scripts/import_db.sh /tmp/l2j_backup.sql
```

**Opción B: Instalar desde cero con los scripts SQL**
```bash
sudo /opt/l2j/scripts/import_db.sh
```

### 5. Iniciar los servidores

```bash
# Iniciar con systemd (auto-inicio incluido)
sudo systemctl start l2j-login l2j-game

# Verificar estado
sudo /opt/l2j/scripts/status.sh

# Ver logs en vivo
screen -r l2j_game
# Ctrl+A, D para salir sin cerrar
```

---

## Comandos útiles en el VPS

```bash
# Ver estado
/opt/l2j/scripts/status.sh

# Reiniciar todo
/opt/l2j/scripts/restart_all.sh

# Ver log del GameServer en vivo
screen -r l2j_game

# Ver log del LoginServer en vivo
screen -r l2j_login

# Recargar skills Premium sin reiniciar (como GM en el juego)
# bypass npc_OBJID_reloadskills

# Backup de la DB
mysqldump -u l2j -p l2jmobiush5 > /opt/l2j/backup/$(date +%Y%m%d).sql
```

---

## Estructura en el VPS

```
/opt/l2j/
├── game/           ← GameServer (data, config, jars)
├── login/          ← LoginServer (config, jars)
├── libs/           ← GameServer.jar, LoginServer.jar, deps
├── db_installer/   ← Scripts SQL para instalacion inicial
├── backup/         ← Backups automáticos de DB
├── logs/           ← Logs de aplicación
├── scripts/        ← start/stop/status/deploy scripts
├── web/            ← Página web de estado (servida por Nginx)
└── .db_credentials ← Credenciales DB (generadas al instalar)
```

---

## Puertos

| Puerto | Servicio |
|---|---|
| 22 | SSH |
| 80 | Web panel (Nginx) |
| 443 | HTTPS (opcional, configurar certificado) |
| 7777 | GameServer (clientes L2J) |
| 9014 | LoginServer ↔ GameServer |
| 2106 | Login (clientes) - ya incluido en GameServer |
