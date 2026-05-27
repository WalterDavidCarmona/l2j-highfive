# ⚔️ L2H5 Web Panel — Lineage 2 High Five

Panel web moderno para servidor L2JMobius H5.  
Stack: **Node.js + Express + MySQL + HTML/CSS/JS vanilla**

---

## 📁 Estructura del Proyecto

```
l2-web/
├── backend/
│   ├── server.js          ← Servidor Express principal
│   ├── package.json
│   ├── .env.example       ← Copiar a .env y configurar
│   ├── setup.sql          ← Tablas adicionales para la web
│   ├── config/
│   │   ├── db.js          ← Pool MySQL
│   │   └── hashUtils.js   ← Hash compatible con L2JMobius (SHA1)
│   ├── middleware/
│   │   └── auth.js        ← Verificación JWT
│   └── routes/
│       ├── auth.js        ← Registro, login, perfil
│       ├── rankings.js    ← PvP, PK, Zona, Clanes, Olimpiada
│       ├── news.js        ← Noticias y eventos
│       ├── shop.js        ← Tienda web
│       └── server.js      ← Estado del servidor
├── frontend/
│   ├── index.html         ← SPA principal
│   └── assets/
│       ├── css/style.css  ← Diseño gaming moderno
│       └── js/
│           ├── api.js     ← Cliente de la API
│           └── main.js    ← Lógica de la aplicación
└── antiddos/
    ├── ddos_protect.php   ← Capa anti-DDoS en PHP
    ├── nginx_ddos.conf    ← Configuración Nginx con rate limiting
    └── htaccess_ddos.txt  ← Reglas Apache .htaccess
```

---

## 🚀 Instalación

### 1. Requisitos
- Node.js 18+
- MySQL 8.x (la misma BD de tu L2JMobius)
- npm

### 2. Configurar variables de entorno

```bash
cd backend
cp .env.example .env
```

Editar `.env`:
```env
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=tu_password
DB_NAME=l2jmobius      # nombre exacto de tu BD
JWT_SECRET=clave_secreta_larga_aqui
L2_PASS_HASH=sha1      # sha1 (default L2JMobius)
PORT=3000
```

### 3. Crear tablas adicionales

```bash
mysql -u root -p l2jmobius < backend/setup.sql
```

Esto crea:
- `web_news` — Noticias y eventos
- `web_shop_items` — Ítems de la tienda con ejemplos
- `web_shop_history` — Historial de compras
- `pvp_zone_kills` — Ranking por zona PvP
- `web_config` — Configuración dinámica

### 4. Instalar dependencias y ejecutar

```bash
cd backend
npm install
npm start          # Producción
npm run dev        # Desarrollo (con nodemon)
```

El servidor estará disponible en: **http://localhost:3000**

---

## 🔗 API Endpoints

### Autenticación
| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/api/auth/register` | Crear cuenta (compatible con L2JMobius) |
| `POST` | `/api/auth/login` | Iniciar sesión → JWT token |
| `GET`  | `/api/auth/me` | Perfil + personajes (requiere token) |
| `POST` | `/api/auth/change-password` | Cambiar contraseña |

### Rankings
| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET` | `/api/rankings/pvp` | Top PvP kills global |
| `GET` | `/api/rankings/pk` | Top PK kills |
| `GET` | `/api/rankings/pvpzone` | Top killer por zona PvP rotativa |
| `GET` | `/api/rankings/clans` | Top clanes por reputación |
| `GET` | `/api/rankings/olympiad` | Top Olimpiada |
| `GET` | `/api/rankings/online` | Jugadores conectados |

### Servidor
| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET` | `/api/server/status` | Online, cuentas, personajes, zona PvP |
| `GET` | `/api/server/pvpzone` | Info de la zona PvP rotativa |

### Noticias
| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET` | `/api/news` | Listar noticias (paginado) |
| `POST` | `/api/news` | Crear noticia (admin: `accessLevel >= 100`) |

### Tienda
| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET` | `/api/shop/items` | Listar ítems (por categoría) |
| `GET` | `/api/shop/balance` | Balance de monedas del usuario |
| `POST` | `/api/shop/purchase` | Comprar ítem |
| `GET` | `/api/shop/history` | Historial de compras |

---

## 🛡️ Anti-DDoS

### Capa Node.js (incluida en server.js)
- Rate limiting: 200 req/15min general, 20 req/15min para auth
- Helmet.js para headers de seguridad
- CORS configurado

### Capa PHP (antiddos/ddos_protect.php)
Incluir al inicio del index.php si usas Apache+PHP:
```php
require_once 'antiddos/ddos_protect.php';
```
O configurar en `php.ini`:
```ini
auto_prepend_file = /ruta/antiddos/ddos_protect.php
```
Configuración en el archivo: rate limits, blacklist de UAs, flood detection.

### Capa Nginx (antiddos/nginx_ddos.conf)
Copiar a `/etc/nginx/sites-available/l2h5.conf` y ajustar `server_name`.
Incluye:
- `limit_req_zone` por IP: 30 req/min general, 5 req/min para auth
- Bloqueo de UAs maliciosos
- Headers de seguridad

---

## 🎮 Zona PvP Rotativa

El sistema `RotatingPvpZoneManager` de L2JMobius maneja las zonas en memoria.  
Para que el ranking de zona sea persistente, el servidor debe guardar kills en `pvp_zone_kills`.

Agrega esto a tu script de Java en `RotatingPvpZoneListener` (handleKill):
```java
try (Connection con = DatabaseFactory.getConnection();
     PreparedStatement ps = con.prepareStatement(
       "INSERT INTO pvp_zone_kills (char_name, zone_name, kills) VALUES (?, ?, 1) " +
       "ON DUPLICATE KEY UPDATE kills=kills+1, last_kill=NOW()")) {
    ps.setString(1, killer.getName());
    ps.setString(2, getCurrentZone().getName());
    ps.execute();
} catch (SQLException e) { /* log */ }
```

---

## ⚙️ Configuración Adicional

### Agregar monedas web a un usuario (MySQL)
```sql
INSERT INTO account_data (account_name, var, value)
VALUES ('nombre_cuenta', 'web_coins', '500')
ON DUPLICATE KEY UPDATE value = '500';
```

### Dar acceso de admin (accessLevel 100)
```sql
UPDATE accounts SET accessLevel = 100 WHERE login = 'tu_cuenta';
```

### Agregar ítem a la tienda
```sql
INSERT INTO web_shop_items (name, description, item_id, item_count, price_coins, category, featured)
VALUES ('Nombre del Ítem', 'Descripción...', 1234, 1, 50, 'general', 0);
```

---

## 📦 Dependencias Backend

```json
{
  "express":         "^4.18.2",
  "mysql2":          "^3.6.5",
  "jsonwebtoken":    "^9.0.2",
  "bcryptjs":        "^2.4.3",
  "cors":            "^2.8.5",
  "dotenv":          "^16.3.1",
  "express-rate-limit": "^7.1.5",
  "helmet":          "^7.1.0"
}
```

---

## 🔐 Seguridad

- Contraseñas hasheadas con SHA1 (compatible con L2JMobius)
- JWT con expiración configurable (default 7 días)
- Rate limiting por IP en auth endpoints
- Validación de ownership de personajes antes de compra
- SQL queries parametrizadas (no hay SQL injection)
- Transacciones MySQL para compras de tienda

---

*Compatible con L2JMobius High Five (H5) — Build by David 2026*
