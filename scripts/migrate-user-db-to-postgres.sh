#!/usr/bin/env bash
# migrate-user-db-to-postgres.sh
# Sprint 3 — Migra scopeflow_users de scopeflow-user-db para scopeflow-postgres
# Uso: ./scripts/migrate-user-db-to-postgres.sh

set -euo pipefail

# ---------------------------------------------------------------------------
# Configuração
# ---------------------------------------------------------------------------
SRC_CONTAINER="scopeflow-user-db"
DST_CONTAINER="scopeflow-postgres"
DB_NAME="scopeflow_users"
DB_USER="postgres"
DB_PASS="postgres"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
BACKUP_DIR="$PROJECT_DIR/backups"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP_FILE="$BACKUP_DIR/${DB_NAME}_backup_${TIMESTAMP}.dump"

# ---------------------------------------------------------------------------
# Helpers de log
# ---------------------------------------------------------------------------
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

ok()   { echo -e "${GREEN}[OK]${NC} $*"; }
err()  { echo -e "${RED}[ERRO]${NC} $*"; }
warn() { echo -e "${YELLOW}[AVISO]${NC} $*"; }
step() { echo -e "\n${YELLOW}>>> $*${NC}"; }

# ---------------------------------------------------------------------------
# 1. Pré-requisitos
# ---------------------------------------------------------------------------
step "Verificando pré-requisitos"

if ! docker info > /dev/null 2>&1; then
  err "Docker não está rodando. Suba o Docker Desktop e tente novamente."
  exit 1
fi
ok "Docker está rodando"

for CONTAINER in "$SRC_CONTAINER" "$DST_CONTAINER"; do
  STATUS="$(docker inspect --format='{{.State.Health.Status}}' "$CONTAINER" 2>/dev/null || echo "not_found")"
  if [[ "$STATUS" != "healthy" ]]; then
    err "Container $CONTAINER não está healthy (status: $STATUS)."
    err "Rode: docker compose up -d && aguarde todos ficarem healthy."
    exit 1
  fi
  ok "Container $CONTAINER: healthy"
done

# ---------------------------------------------------------------------------
# 2. Backup de segurança
# ---------------------------------------------------------------------------
step "Criando backup de segurança (origem: $SRC_CONTAINER/$DB_NAME)"

mkdir -p "$BACKUP_DIR"

docker exec "$SRC_CONTAINER" \
  pg_dump -U "$DB_USER" -F c -d "$DB_NAME" \
  > "$BACKUP_FILE"

ok "Backup salvo em: $BACKUP_FILE"

# ---------------------------------------------------------------------------
# 3. Verificar/criar database destino
# ---------------------------------------------------------------------------
step "Verificando database '$DB_NAME' no container destino ($DST_CONTAINER)"

DB_EXISTS="$(docker exec "$DST_CONTAINER" \
  psql -U "$DB_USER" -tAc \
  "SELECT 1 FROM pg_database WHERE datname='$DB_NAME';" 2>/dev/null || echo "")"

if [[ "$DB_EXISTS" == "1" ]]; then
  ok "Database '$DB_NAME' já existe em $DST_CONTAINER (criado pelo init script da Sprint 2)"
else
  warn "Database '$DB_NAME' não encontrado — criando manualmente (fallback)"
  docker exec "$DST_CONTAINER" \
    psql -U "$DB_USER" -c "CREATE DATABASE $DB_NAME;"
  ok "Database '$DB_NAME' criado"
fi

# ---------------------------------------------------------------------------
# 4. Restore
# ---------------------------------------------------------------------------
step "Restaurando dados para $DST_CONTAINER/$DB_NAME"

docker exec -e PGPASSWORD="$DB_PASS" "$DST_CONTAINER" \
  pg_restore -U "$DB_USER" -d "$DB_NAME" --no-owner --no-privileges -c --if-exists \
  < "$BACKUP_FILE" 2>/dev/null || true
# pg_restore retorna exit 1 em warnings (ex: role não existe) — ignoramos warnings

ok "Restore concluído"

# ---------------------------------------------------------------------------
# 5. Validação de integridade
# ---------------------------------------------------------------------------
step "Validando integridade (contagem de tabelas e registros)"

# Tabelas do schema public
SRC_TABLES="$(docker exec "$SRC_CONTAINER" \
  psql -U "$DB_USER" -d "$DB_NAME" -tAc \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE';")"

DST_TABLES="$(docker exec "$DST_CONTAINER" \
  psql -U "$DB_USER" -d "$DB_NAME" -tAc \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE';")"

echo "  Tabelas — origem: $SRC_TABLES | destino: $DST_TABLES"
if [[ "$SRC_TABLES" != "$DST_TABLES" ]]; then
  err "Contagem de tabelas divergente! Verifique o restore."
  exit 1
fi
ok "Contagem de tabelas: OK ($SRC_TABLES tabelas)"

# Contagem de registros por tabela
echo ""
echo "  Registros por tabela:"
TABLES="$(docker exec "$SRC_CONTAINER" \
  psql -U "$DB_USER" -d "$DB_NAME" -tAc \
  "SELECT tablename FROM pg_tables WHERE schemaname='public';")"

ALL_MATCH=true
while IFS= read -r TABLE; do
  [[ -z "$TABLE" ]] && continue
  SRC_COUNT="$(docker exec "$SRC_CONTAINER" \
    psql -U "$DB_USER" -d "$DB_NAME" -tAc "SELECT COUNT(*) FROM \"$TABLE\";")"
  DST_COUNT="$(docker exec "$DST_CONTAINER" \
    psql -U "$DB_USER" -d "$DB_NAME" -tAc "SELECT COUNT(*) FROM \"$TABLE\";" 2>/dev/null || echo "ERRO")"

  if [[ "$SRC_COUNT" == "$DST_COUNT" ]]; then
    ok "  $TABLE: $SRC_COUNT registros"
  else
    err "  $TABLE: origem=$SRC_COUNT | destino=$DST_COUNT — DIVERGENCIA!"
    ALL_MATCH=false
  fi
done <<< "$TABLES"

if [[ "$ALL_MATCH" != "true" ]]; then
  err "Integridade comprometida. Restore incompleto — verifique o backup: $BACKUP_FILE"
  exit 1
fi

# ---------------------------------------------------------------------------
# 6. Resultado final
# ---------------------------------------------------------------------------
echo ""
echo "================================================================"
ok "MIGRACAO CONCLUIDA COM SUCESSO"
echo "================================================================"
echo ""
echo "  Origem:  $SRC_CONTAINER/$DB_NAME"
echo "  Destino: $DST_CONTAINER/$DB_NAME"
echo "  Backup:  $BACKUP_FILE"
echo ""
echo "  Proximo passo: Sprint 4 — atualizar user-service datasource"
echo "  Altere USER_DB_URL no docker-compose.yml / .env para apontar"
echo "  para scopeflow-postgres (porta 5432, database scopeflow_users)"
echo ""
warn "ATENCAO: antes da Sprint 5 (remover user-db), execute:"
warn "  docker compose down -v  (remove volumes, irreversivel)"
warn "  Certifique-se de que o backup acima esta seguro antes disso."
echo ""
