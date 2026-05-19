#!/usr/bin/env bash
# Deploy GOrbitS → servidor vía HTTP (Deploy Receiver /deploy).
# Ver: documentacion_deploy_gorbits.txt
#
# Uso:
#   export DEPLOY_TOKEN="tu_token"
#   ./ci/deploy-http-gorbits.sh
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="${BACKEND_DIR:-${SCRIPT_DIR}/..}"

DEPLOY_URL="${DEPLOY_URL:-https://app.gorbits.xyz/deploy}"
HEALTH_URL="${HEALTH_URL:-https://app.gorbits.xyz/api/actuator/health}"
JAR_FILE="${JAR_FILE:-${BACKEND_DIR}/target/GOrbitS-0.0.1-SNAPSHOT.jar}"

if [[ -z "${DEPLOY_TOKEN:-}" ]]; then
  echo "ERROR: define DEPLOY_TOKEN (Bearer) o usa credencial Jenkins gorbits-deploy-token" >&2
  exit 1
fi

if [[ ! -f "${JAR_FILE}" ]]; then
  echo "ERROR: no existe ${JAR_FILE}. Compila antes: ./mvnw clean package -DskipTests" >&2
  exit 1
fi

echo "======================================"
echo " DEPLOY HTTP → GOrbitS"
echo "======================================"
echo "URL:  ${DEPLOY_URL}"
echo "JAR:  ${JAR_FILE}"
echo ""

RESP=$(curl -sf -S -X POST \
  -H "Authorization: Bearer ${DEPLOY_TOKEN}" \
  -F "file=@${JAR_FILE}" \
  "${DEPLOY_URL}") || {
  echo "ERROR: falló POST a ${DEPLOY_URL}" >&2
  exit 1
}

echo "${RESP}"
echo ""

if ! echo "${RESP}" | grep -qE 'Deploy OK|Deploy exitoso|✅'; then
  echo "ERROR: el servidor no confirmó deploy exitoso" >&2
  exit 1
fi

echo "==> Health público"
HEALTH=$(curl -sf "${HEALTH_URL}" || true)
echo "${HEALTH}"
if echo "${HEALTH}" | grep -q '"status":"UP"'; then
  echo "Deploy y health OK."
else
  echo "AVISO: revisar health manualmente: ${HEALTH_URL}"
fi
