-- ============================================================
--  L2H5 — Sistema de Apuestas Olimpiada
--  Ejecutar: mysql -u root -p l2jmobiush5 < bets.sql
-- ============================================================

-- Temporadas de apuestas (una por período de olimpiada)
CREATE TABLE IF NOT EXISTS `olympiad_bet_seasons` (
  `id`              INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `name`            VARCHAR(100) NOT NULL DEFAULT 'Temporada Olimpiada',
  `status`          ENUM('open','closed','resolved') DEFAULT 'open',
  `winner_char`     VARCHAR(35)  DEFAULT NULL  COMMENT 'Personaje que se convirtió en Héroe',
  `winner_class_id` SMALLINT     DEFAULT NULL,
  `total_pool`      INT UNSIGNED DEFAULT 0     COMMENT 'Total de monedas apostadas',
  `total_bets`      INT UNSIGNED DEFAULT 0     COMMENT 'Cantidad de apuestas',
  `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `closed_at`       DATETIME     DEFAULT NULL  COMMENT 'Cuando se cerró para nuevas apuestas',
  `resolved_at`     DATETIME     DEFAULT NULL  COMMENT 'Cuando se declaró el ganador',
  INDEX(`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Apuestas individuales
CREATE TABLE IF NOT EXISTS `olympiad_bets` (
  `id`            INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `season_id`     INT UNSIGNED NOT NULL,
  `account_name`  VARCHAR(45)  NOT NULL,
  `char_bet`      VARCHAR(35)  NOT NULL  COMMENT 'Personaje apostado como futuro Héroe',
  `class_id`      SMALLINT     DEFAULT NULL,
  `coins_bet`     TINYINT UNSIGNED DEFAULT 1 COMMENT 'Monedas arriesgadas (1-5)',
  `payout`        TINYINT UNSIGNED DEFAULT 0 COMMENT 'Monedas recibidas si ganó (1-20)',
  `won`           TINYINT(1)   DEFAULT NULL  COMMENT 'NULL=pendiente 1=ganó 0=perdió',
  `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `one_bet_per_season` (`season_id`, `account_name`),
  INDEX(`season_id`),
  INDEX(`account_name`),
  INDEX(`char_bet`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Temporada inicial abierta
INSERT IGNORE INTO `olympiad_bet_seasons` (`id`, `name`, `status`)
VALUES (1, 'Temporada 1 — ¿Quién será el próximo Héroe?', 'open');
