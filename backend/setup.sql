-- ============================================================
--  L2H5 Web Panel - Tablas adicionales (ejecutar en tu BD L2)
--  Compatible con L2JMobius H5
-- ============================================================

-- Noticias / Eventos del servidor
CREATE TABLE IF NOT EXISTS `web_news` (
  `id`          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `title`       VARCHAR(255) NOT NULL,
  `content`     TEXT NOT NULL,
  `type`        ENUM('news','event','update','maintenance') DEFAULT 'news',
  `image_url`   VARCHAR(500) DEFAULT NULL,
  `author`      VARCHAR(50)  DEFAULT 'Admin',
  `pinned`      TINYINT(1)   DEFAULT 0,
  `active`      TINYINT(1)   DEFAULT 1,
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX(`type`), INDEX(`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tienda web (ítems que se pueden comprar con web_coins)
CREATE TABLE IF NOT EXISTS `web_shop_items` (
  `id`          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `name`        VARCHAR(255) NOT NULL,
  `description` TEXT,
  `item_id`     INT UNSIGNED NOT NULL,
  `item_count`  INT UNSIGNED DEFAULT 1,
  `price_coins` INT UNSIGNED DEFAULT 0,
  `price_adena` BIGINT UNSIGNED DEFAULT 0,
  `category`    VARCHAR(50)  DEFAULT 'general',
  `image_url`   VARCHAR(500) DEFAULT NULL,
  `featured`    TINYINT(1)   DEFAULT 0,
  `stock`       INT DEFAULT NULL COMMENT 'NULL = ilimitado',
  `active`      TINYINT(1)   DEFAULT 1,
  INDEX(`category`), INDEX(`active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Historial de compras
CREATE TABLE IF NOT EXISTS `web_shop_history` (
  `id`           INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `account_name` VARCHAR(45) NOT NULL,
  `char_name`    VARCHAR(35) NOT NULL,
  `item_shop_id` INT UNSIGNED,
  `item_name`    VARCHAR(255),
  `price_coins`  INT UNSIGNED DEFAULT 0,
  `item_count`   INT UNSIGNED DEFAULT 1,
  `created_at`   DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX(`account_name`), INDEX(`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Ranking kills por zona PvP rotativa
CREATE TABLE IF NOT EXISTS `pvp_zone_kills` (
  `id`        INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `char_name` VARCHAR(35) NOT NULL,
  `zone_name` VARCHAR(100) DEFAULT 'Zona PvP',
  `kills`     INT UNSIGNED DEFAULT 0,
  `last_kill` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `unique_char_zone` (`char_name`, `zone_name`),
  INDEX(`kills`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Configuración web dinámica
CREATE TABLE IF NOT EXISTS `web_config` (
  `key`   VARCHAR(100) PRIMARY KEY,
  `value` TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO `web_config` (`key`, `value`) VALUES
  ('current_pvp_zone', 'Coliseo de Giran'),
  ('server_rates_xp',  '100x'),
  ('server_rates_sp',  '100x'),
  ('server_rates_adena','50x');

-- Noticias de ejemplo
INSERT IGNORE INTO `web_news` (`id`,`title`,`content`,`type`,`pinned`) VALUES
(1, '¡Bienvenidos al Servidor L2 H5!',
 '¡El servidor ya está disponible! Únete a miles de jugadores en la mejor experiencia de Lineage 2 H5. Disfruta de nuestras zonas PvP rotativas, tienda web exclusiva y eventos únicos.',
 'news', 1),
(2, 'Evento: Guerra de Clanes este Sábado',
 'Este sábado a las 20:00 (GMT-3) tendremos una Gran Guerra de Clanes en el Coliseo de Giran. ¡Los mejores premios para el clan ganador! Inscripciones abiertas.',
 'event', 0),
(3, 'Actualización v1.1 - Zona PvP Rotativa',
 'Hemos implementado el sistema de Zona PvP Rotativa. Cada hora la zona cambia entre el Coliseo de Giran y las Catacumbas de los Sacrificados. ¡Los top killers recibirán recompensas especiales!',
 'update', 0);

-- Notificaciones de recompensa PvP Zona (1 por personaje, expira en 1 día)
CREATE TABLE IF NOT EXISTS `pvp_zone_notifications` (
  `id`            INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `account_name`  VARCHAR(45)  NOT NULL,
  `char_name`     VARCHAR(35)  NOT NULL,
  `coins_awarded` INT UNSIGNED DEFAULT 0  COMMENT 'acumulado desde que se creó la notif',
  `zone_name`     VARCHAR(100) DEFAULT 'Zona PvP',
  `kills_new`     INT UNSIGNED DEFAULT 0,
  `dismissed`     TINYINT(1)   DEFAULT 0,
  `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `expires_at`    DATETIME     NOT NULL,
  UNIQUE KEY `uq_char_name` (`char_name`),
  INDEX(`account_name`),
  INDEX(`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Recompensas PvP Zona — registro de kills ya premiados por personaje
CREATE TABLE IF NOT EXISTS `pvp_zone_reward_log` (
  `char_name`       VARCHAR(35)  NOT NULL,
  `kills_rewarded`  INT UNSIGNED DEFAULT 0  COMMENT 'kills acumulados ya premiados',
  `coins_total`     INT UNSIGNED DEFAULT 0  COMMENT 'total de coins entregados',
  `last_reward_at`  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`char_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Historial de eventos de recompensa (log de auditoría)
CREATE TABLE IF NOT EXISTS `pvp_zone_reward_history` (
  `id`            INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `char_name`     VARCHAR(35)  NOT NULL,
  `account_name`  VARCHAR(45)  NOT NULL,
  `kills_new`     INT UNSIGNED DEFAULT 0,
  `coins_awarded` INT UNSIGNED DEFAULT 0,
  `rewarded_at`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  INDEX(`char_name`), INDEX(`rewarded_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Config PvP Reward (insertar valores por defecto)
INSERT IGNORE INTO `web_config` (`key`, `value`) VALUES
  ('pvpzone_reward_enabled', '0'),
  ('pvpzone_reward_coins',   '5');

-- Ítems de ejemplo en la tienda
INSERT IGNORE INTO `web_shop_items` (`id`,`name`,`description`,`item_id`,`item_count`,`price_coins`,`category`,`featured`) VALUES
(1, 'Blessed Scroll: Enchant Weapon (S)', 'Encanta un arma de grado S de forma segura. No destruye el ítem.', 959, 1, 50, 'scrolls', 1),
(2, 'Blessed Scroll: Enchant Armor (S)', 'Encanta una armadura de grado S de forma segura.', 960, 1, 30, 'scrolls', 0),
(3, 'Giant\'s Codex - Discipline', 'Permite evolucionar una habilidad avanzada.', 8625, 1, 100, 'skills', 1),
(4, 'Adena x1.000.000', '1 millón de Adena directo a tu inventario.', 57, 1000000, 20, 'adena', 0),
(5, 'Premium Account - 30 días', 'Activa el modo Premium con bonuses de XP, SP y Adena durante 30 días.', 13293, 1, 200, 'premium', 1),
(6, 'Mysterious Box', 'Contiene un ítem aleatorio de grado S o superior. ¡Suerte!', 8077, 1, 75, 'boxes', 0);
