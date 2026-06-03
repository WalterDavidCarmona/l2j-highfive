#!/bin/bash
# =============================================================================
#  L2J Zona Zero - Importar base de datos en Ubuntu VPS
#
#  Uso:
#    sudo ./import_db.sh [dump.sql]
#
#  Si no se pasa dump.sql, instala desde cero con los scripts del proyecto.
# =============================================================================

set -e
GREEN='\033[0;32m'; CYAN='\033[0;36m'; YELLOW='\033[1;33m'; NC='\033[0m'
ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
info() { echo -e "${CYAN}[INFO]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

L2J_HOME="/opt/l2j"
CREDS="$L2J_HOME/.db_credentials"
[[ -f "$CREDS" ]] && source "$CREDS" || { echo "Ejecutar install.sh primero"; exit 1; }

DUMP_FILE=$1

# ── Opcion A: restaurar desde dump existente ──────────────────────────────────
if [[ -n "$DUMP_FILE" && -f "$DUMP_FILE" ]]; then
    info "Restaurando desde dump: $DUMP_FILE"
    mysql -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" < "$DUMP_FILE"
    ok "Dump importado"
    exit 0
fi

# ── Opcion B: instalar desde scripts SQL del proyecto ────────────────────────
SQL_GAME="$L2J_HOME/db_installer/sql/game"
SQL_LOGIN="$L2J_HOME/db_installer/sql/login"

if [[ ! -d "$SQL_GAME" ]]; then
    echo "No se encontraron scripts SQL en $SQL_GAME"
    echo "Alternativa: pasar un dump: ./import_db.sh /tmp/l2j_backup.sql"
    exit 1
fi

info "Instalando base de datos desde scripts SQL..."

# Login tables
if [[ -d "$SQL_LOGIN" ]]; then
    for sql in "$SQL_LOGIN"/*.sql; do
        [[ -f "$sql" ]] || continue
        info "  Import: $(basename $sql)"
        mysql -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" < "$sql" 2>/dev/null || warn "  Advertencia en $(basename $sql)"
    done
fi

# Game tables
for sql in "$SQL_GAME"/*.sql; do
    [[ -f "$sql" ]] || continue
    info "  Import: $(basename $sql)"
    mysql -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" < "$sql" 2>/dev/null || warn "  Advertencia en $(basename $sql)"
done

ok "Base de datos instalada en '$DB_NAME'"
info "Tablas creadas:"
mysql -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "SHOW TABLES;" 2>/dev/null | wc -l | xargs echo "  Total:"
