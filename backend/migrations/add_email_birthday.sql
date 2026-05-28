-- ============================================================
-- Migración: Agregar email y birthday a la tabla accounts
-- Ejecutar UNA SOLA VEZ en la base de datos L2JMobius H5
-- ============================================================

-- Agregar columna email (única, permite NULL para cuentas antiguas)
ALTER TABLE accounts
  ADD COLUMN IF NOT EXISTS email VARCHAR(100) DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS birthday DATE DEFAULT NULL;

-- Índice único en email (evita duplicados)
-- Si ya existiera: DROP INDEX IF EXISTS idx_accounts_email ON accounts;
CREATE UNIQUE INDEX IF NOT EXISTS idx_accounts_email
  ON accounts (email);

-- Verificar resultado
SELECT
  COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME   = 'accounts'
  AND COLUMN_NAME  IN ('email', 'birthday');
