#!/usr/bin/env bash
###############################################################################
# floci-bootstrap.sh — Provisiona Cognito local (floci) p/ desenvolvimento
#
# Cria:
#   - User pool "pricetracker-local"
#   - Resource server "pricetracker" com scope "external-api"
#   - App client M2M "local-partner" com client_credentials habilitado
#
# Uso:
#   ./infra/floci-bootstrap.sh             # provisiona e imprime resumo legível
#   ./infra/floci-bootstrap.sh --export    # imprime "export FOO=bar" p/ usar com eval
#
# Pre-reqs: floci rodando em localhost:4566 (docker-compose.dev.yml), awscli v2.
# É idempotente: se o pool já existir, reusa.
###############################################################################
set -euo pipefail

ENDPOINT="${FLOCI_ENDPOINT:-http://localhost:4566}"
REGION="us-east-1"
POOL_NAME="pricetracker-local"
RESOURCE_ID="pricetracker"
SCOPE_NAME="external-api"
CLIENT_NAME="local-partner"

# Credenciais dummy: floci aceita qualquer coisa
export AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-test}"
export AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-test}"
export AWS_DEFAULT_REGION="$REGION"

EXPORT_MODE=false
[[ "${1:-}" == "--export" ]] && EXPORT_MODE=true

aws_floci() {
  aws --endpoint-url "$ENDPOINT" --region "$REGION" "$@"
}

log() {
  $EXPORT_MODE || echo ">>> $*" >&2
}

# 1) Pool (idempotente)
log "Procurando user pool '$POOL_NAME'..."
POOL_ID=$(aws_floci cognito-idp list-user-pools --max-results 60 \
  --query "UserPools[?Name=='$POOL_NAME'].Id | [0]" --output text)

if [[ -z "$POOL_ID" || "$POOL_ID" == "None" ]]; then
  log "Criando pool..."
  POOL_ID=$(aws_floci cognito-idp create-user-pool \
    --pool-name "$POOL_NAME" \
    --query 'UserPool.Id' --output text)
  log "Pool criado: $POOL_ID"
else
  log "Pool já existe: $POOL_ID"
fi

# 2) Resource server + scope (idempotente: create-resource-server pode ser re-chamado)
log "Garantindo resource server '$RESOURCE_ID' com scope '$SCOPE_NAME'..."
aws_floci cognito-idp create-resource-server \
  --user-pool-id "$POOL_ID" \
  --identifier "$RESOURCE_ID" \
  --name "$RESOURCE_ID" \
  --scopes "ScopeName=$SCOPE_NAME,ScopeDescription=External partner API access" \
  >/dev/null 2>&1 || true

# 3) App client (procura por nome; cria se não existir)
log "Procurando app client '$CLIENT_NAME'..."
CLIENT_ID=$(aws_floci cognito-idp list-user-pool-clients \
  --user-pool-id "$POOL_ID" --max-results 60 \
  --query "UserPoolClients[?ClientName=='$CLIENT_NAME'].ClientId | [0]" --output text)

if [[ -z "$CLIENT_ID" || "$CLIENT_ID" == "None" ]]; then
  log "Criando client..."
  CREATE_OUT=$(aws_floci cognito-idp create-user-pool-client \
    --user-pool-id "$POOL_ID" \
    --client-name "$CLIENT_NAME" \
    --generate-secret \
    --allowed-o-auth-flows client_credentials \
    --allowed-o-auth-scopes "$RESOURCE_ID/$SCOPE_NAME" \
    --allowed-o-auth-flows-user-pool-client \
    --output json)
  CLIENT_ID=$(echo "$CREATE_OUT" | python3 -c "import json,sys;print(json.load(sys.stdin)['UserPoolClient']['ClientId'])")
  CLIENT_SECRET=$(echo "$CREATE_OUT" | python3 -c "import json,sys;print(json.load(sys.stdin)['UserPoolClient']['ClientSecret'])")
else
  log "Client já existe: $CLIENT_ID — buscando secret..."
  CLIENT_SECRET=$(aws_floci cognito-idp describe-user-pool-client \
    --user-pool-id "$POOL_ID" --client-id "$CLIENT_ID" \
    --query 'UserPoolClient.ClientSecret' --output text)
fi

ISSUER_URI="$ENDPOINT/$POOL_ID"
# Floci expõe o token endpoint num path fixo (não por pool, como o Cognito real).
TOKEN_URL="$ENDPOINT/cognito-idp/oauth2/token"

if $EXPORT_MODE; then
  cat <<EOF
export COGNITO_ISSUER_URI="$ISSUER_URI"
export COGNITO_USER_POOL_ID="$POOL_ID"
export COGNITO_REGION="$REGION"
export PUBLIC_API_SCOPE="$RESOURCE_ID/$SCOPE_NAME"
export FLOCI_CLIENT_ID="$CLIENT_ID"
export FLOCI_CLIENT_SECRET="$CLIENT_SECRET"
export FLOCI_TOKEN_URL="$TOKEN_URL"
EOF
else
  cat <<EOF >&2

==================================================================
Cognito local provisionado em $ENDPOINT
==================================================================
Pool ID         : $POOL_ID
Issuer URI      : $ISSUER_URI
Token URL       : $TOKEN_URL
Scope           : $RESOURCE_ID/$SCOPE_NAME
Client ID       : $CLIENT_ID
Client Secret   : $CLIENT_SECRET

Para exportar como env vars:
  eval "\$(./infra/floci-bootstrap.sh --export)"

Pegar um token:
  curl -s -u "\$FLOCI_CLIENT_ID:\$FLOCI_CLIENT_SECRET" \\
    -d "grant_type=client_credentials&scope=$RESOURCE_ID/$SCOPE_NAME" \\
    "\$FLOCI_TOKEN_URL"
==================================================================
EOF
fi
