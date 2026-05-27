-- ============================================================
--  L2H5 Web Panel - Sistema de Pagos
--  Ejecutar: mysql -u root -p l2jmobiush5 < payments.sql
-- ============================================================

-- Paquetes de coins disponibles para comprar
CREATE TABLE IF NOT EXISTS `coin_packages` (
  `id`           INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `name`         VARCHAR(100) NOT NULL,
  `description`  VARCHAR(255) DEFAULT NULL,
  `coins`        INT UNSIGNED NOT NULL,
  `price_ars`    DECIMAL(10,2) DEFAULT NULL  COMMENT 'Precio en ARS (MercadoPago)',
  `price_usd`    DECIMAL(10,2) DEFAULT NULL  COMMENT 'Precio en USD (PayPal)',
  `bonus_pct`    TINYINT UNSIGNED DEFAULT 0  COMMENT 'Bonus % de coins extra',
  `featured`     TINYINT(1) DEFAULT 0,
  `active`       TINYINT(1) DEFAULT 1,
  `sort_order`   TINYINT UNSIGNED DEFAULT 0,
  INDEX(`active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Historial de pagos / transacciones
CREATE TABLE IF NOT EXISTS `payment_orders` (
  `id`             INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `account_name`   VARCHAR(45) NOT NULL,
  `package_id`     INT UNSIGNED NOT NULL,
  `coins`          INT UNSIGNED NOT NULL,
  `amount`         DECIMAL(10,2) NOT NULL,
  `currency`       VARCHAR(3)  NOT NULL DEFAULT 'ARS',
  `provider`       ENUM('mercadopago','paypal') NOT NULL,
  `provider_id`    VARCHAR(255) DEFAULT NULL  COMMENT 'ID externo del proveedor',
  `status`         ENUM('pending','approved','rejected','cancelled','refunded') DEFAULT 'pending',
  `created_at`     DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `metadata`       JSON DEFAULT NULL  COMMENT 'Datos extra del proveedor',
  INDEX(`account_name`),
  INDEX(`provider_id`),
  INDEX(`status`),
  INDEX(`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Paquetes de ejemplo
INSERT IGNORE INTO `coin_packages` (`id`,`name`,`description`,`coins`,`price_ars`,`price_usd`,`bonus_pct`,`featured`,`sort_order`) VALUES
(1, 'Starter Pack',   '100 WebCoins para comenzar',                    100,   2000.00,  2.00,  0,  0, 1),
(2, 'Aventurero',     '300 WebCoins + 10% bonus',                      330,   5000.00,  5.00, 10,  0, 2),
(3, 'Héroe',          '700 WebCoins + 20% bonus — ¡Más popular!',      840,  10000.00, 10.00, 20,  1, 3),
(4, 'Campeón',        '1500 WebCoins + 33% bonus',                    2000,  20000.00, 20.00, 33,  0, 4),
(5, 'Leyenda',        '4000 WebCoins + 50% bonus — Mejor valor',      6000,  50000.00, 50.00, 50,  1, 5);
