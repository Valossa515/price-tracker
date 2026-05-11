#!/usr/bin/env bash
###############################################################################
# bootstrap-env.sh — gera /opt/price-tracker/.env a partir do SSM Parameter Store
#
# Lê todos os parâmetros sob /pricetracker/prod/ (SecureString descriptografada
# via KMS) e escreve no arquivo .env com perm 600.
#
# Roda no EC2 (instance role 'price-tracker-ec2-app' já tem ssm:GetParametersByPath
# + kms:Decrypt). Idempotente.
#
# Uso:
#   sudo bootstrap-env.sh                  # escreve em /opt/price-tracker/.env
#   sudo bootstrap-env.sh /tmp/test.env    # escreve em destino custom (dry-run)
###############################################################################
set -euo pipefail

REGION="${AWS_REGION:-sa-east-1}"
PREFIX="${SSM_PREFIX:-/pricetracker/prod}"
DEST="${1:-/opt/price-tracker/.env}"
TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT

echo ">>> Lendo $PREFIX (region=$REGION)..."

# AWS CLI auto-pagina (todos os params em uma única resposta consolidada).
# --with-decryption descriptografa SecureString via KMS.
RESP=$(aws ssm get-parameters-by-path \
  --region "$REGION" \
  --path "$PREFIX" \
  --with-decryption \
  --output json)

# Extrai KEY=VAL (sem aspas — docker compose env_file as trata como literais).
# Ignora Value vazio (placeholders). Linhas com newline embutido são rejeitadas.
echo "$RESP" | python3 -c "
import json, sys
d = json.load(sys.stdin)
for p in d.get('Parameters', []):
    name = p['Name'].rsplit('/', 1)[-1]
    val = p['Value']
    if val.strip() == '':
        continue
    if '\n' in val or '\r' in val:
        sys.stderr.write(f'SKIP {name}: contém quebra de linha\n')
        continue
    print(f'{name}={val}')
" >> "$TMP"

LINES=$(wc -l < "$TMP" | tr -d ' ')
if [ "$LINES" -eq 0 ]; then
  echo "ERRO: nenhum parâmetro encontrado em $PREFIX" >&2
  exit 1
fi

# Escreve atomicamente
install -m 600 "$TMP" "$DEST"
echo ">>> Escrito $DEST com $LINES variáveis (perm 600)"
