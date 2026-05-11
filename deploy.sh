#!/usr/bin/env bash
###############################################################################
# deploy.sh — Build, push e deploy do price-tracker
#
# Fluxo:
#   1. Build local do JAR (Maven)
#   2. Build da imagem Docker com tag = git SHA curto
#   3. Push pra ECR
#   4. Atualiza SSM Parameter /pricetracker/app/version
#   5. SSM Send-Command no EC2: docker compose pull + up -d
#
# Uso:
#   ./deploy.sh                  # tag = git rev-parse --short HEAD
#   ./deploy.sh v1.2.3           # tag custom
#
# Pre-reqs: rodar do diretório raiz do app (price-tracker, não infra)
###############################################################################
set -euo pipefail

PROFILE="${AWS_PROFILE:-pessoal}"
REGION="${AWS_REGION:-sa-east-1}"
ECR_REPO="941098798253.dkr.ecr.sa-east-1.amazonaws.com/price-tracker"
APP_TAG="price-tracker-app"
SSM_VERSION_PARAM="/pricetracker/app/version"

# 1. Determinar tag
if [ $# -ge 1 ]; then
  TAG="$1"
  echo ">>> Tag override: $TAG (sem checagem de git status)"
else
  if ! command -v git >/dev/null; then
    echo "ERRO: git não encontrado e nenhum tag passado como argumento" >&2
    exit 1
  fi

  # Working tree precisa estar limpo (sem mudanças não commitadas/staged)
  if ! git diff --quiet || ! git diff --cached --quiet; then
    echo "ERRO: working tree não está limpo." >&2
    echo "      Tem mudanças não commitadas — o git SHA não vai bater com o código." >&2
    echo "      Resolva com: git status / git commit / git stash" >&2
    echo "      Ou force uma tag manual: ./deploy.sh <tag>" >&2
    exit 1
  fi

  # Não pode ter arquivos untracked relevantes (ex: src/, pom.xml)
  UNTRACKED=$(git ls-files --others --exclude-standard -- src pom.xml Dockerfile 2>/dev/null || true)
  if [ -n "$UNTRACKED" ]; then
    echo "ERRO: arquivos untracked que afetam o build:" >&2
    echo "$UNTRACKED" >&2
    echo "      git add + commit antes de deployar." >&2
    exit 1
  fi

  TAG=$(git rev-parse --short HEAD)
fi

echo ">>> Deploy version: $TAG"

# 2. Validar pasta (deve ter pom.xml)
if [ ! -f pom.xml ]; then
  echo "ERRO: rode esse script da raiz do projeto price-tracker (onde está pom.xml)" >&2
  exit 1
fi

# 3. Build JAR local
echo ">>> [1/5] Building JAR localmente (mvn clean package)..."
mvn -q clean package -DskipTests

# 4. Build imagem Docker
echo ">>> [2/5] Building imagem Docker..."
docker build --platform linux/arm64 -t "price-tracker:$TAG" .
docker tag "price-tracker:$TAG" "$ECR_REPO:$TAG"

# 5. Login ECR + push
echo ">>> [3/5] Pushing pra ECR..."
aws ecr get-login-password --region "$REGION" --profile "$PROFILE" \
  | docker login --username AWS --password-stdin "${ECR_REPO%/*}"
docker push "$ECR_REPO:$TAG"

# 6. Atualizar SSM Parameter
echo ">>> [4/5] Atualizando SSM /pricetracker/app/version=$TAG..."
AWS_PROFILE="$PROFILE" aws ssm put-parameter \
  --region "$REGION" \
  --name "$SSM_VERSION_PARAM" \
  --type String \
  --value "$TAG" \
  --overwrite \
  --output text >/dev/null

# 7. Pegar IID + SSM send-command
IID=$(AWS_PROFILE="$PROFILE" aws ec2 describe-instances \
  --region "$REGION" \
  --filters "Name=tag:Name,Values=$APP_TAG" "Name=instance-state-name,Values=running" \
  --query 'Reservations[].Instances[].InstanceId' \
  --output text | awk '{print $1}')

if [ -z "$IID" ]; then
  echo "ERRO: não achei EC2 running com tag Name=$APP_TAG" >&2
  exit 1
fi

echo ">>> [5/5] Restart no EC2 ($IID) via SSM send-command..."

# Sync infra files (bootstrap-env.sh) pro EC2 antes de rodar.
# Garante que o script no EC2 sempre bate com o repo.
if [ ! -f infra/bootstrap-env.sh ]; then
  echo "ERRO: infra/bootstrap-env.sh não encontrado no repo" >&2
  exit 1
fi
BOOTSTRAP_B64=$(base64 -i infra/bootstrap-env.sh | tr -d '\n')

CMD_ID=$(AWS_PROFILE="$PROFILE" aws ssm send-command \
  --region "$REGION" \
  --instance-ids "$IID" \
  --document-name "AWS-RunShellScript" \
  --comment "Deploy price-tracker $TAG" \
  --parameters "commands=[
    'cd /opt/price-tracker',
    'echo $BOOTSTRAP_B64 | base64 -d > bootstrap-env.sh',
    'chmod +x bootstrap-env.sh',
    'NEW_VERSION=\$(aws ssm get-parameter --region $REGION --name $SSM_VERSION_PARAM --query Parameter.Value --output text)',
    'sed -i \"s|price-tracker:.*|price-tracker:\$NEW_VERSION|\" docker-compose.yml',
    './bootstrap-env.sh /opt/price-tracker/.env',
    'aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin ${ECR_REPO%/*}',
    'docker compose pull app',
    'docker compose up -d app',
    'docker compose ps'
  ]" \
  --query 'Command.CommandId' \
  --output text)

echo "Command ID: $CMD_ID"
echo ">>> Aguardando execução..."

# Poll status
for i in $(seq 1 20); do
  STATUS=$(AWS_PROFILE="$PROFILE" aws ssm get-command-invocation \
    --region "$REGION" \
    --command-id "$CMD_ID" \
    --instance-id "$IID" \
    --query 'Status' \
    --output text 2>/dev/null || echo "Pending")
  echo "[$i/20] Status: $STATUS"
  if [ "$STATUS" = "Success" ] || [ "$STATUS" = "Failed" ] || [ "$STATUS" = "Cancelled" ]; then
    break
  fi
  sleep 3
done

echo ""
echo ">>> Output do comando:"
AWS_PROFILE="$PROFILE" aws ssm get-command-invocation \
  --region "$REGION" \
  --command-id "$CMD_ID" \
  --instance-id "$IID" \
  --query 'StandardOutputContent' \
  --output text

if [ "$STATUS" != "Success" ]; then
  echo ""
  echo ">>> Stderr:"
  AWS_PROFILE="$PROFILE" aws ssm get-command-invocation \
    --region "$REGION" \
    --command-id "$CMD_ID" \
    --instance-id "$IID" \
    --query 'StandardErrorContent' \
    --output text
  exit 1
fi

echo ""
echo "✓ Deploy $TAG completo!"
echo "  Validar: curl -s https://littlepricetracker.observer/actuator/health"
