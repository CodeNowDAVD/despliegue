#!/usr/bin/env bash
# Deploy GOrbitS backend → Termux (mismo flujo que ~/devops/gorbits/deploy-backend-to-termux.sh).
# Ver también: despliegueS/comandos_despliegue_gorbits.txt
#
# Uso (local o Jenkins):
#   ./ci/deploy-to-termux.sh
#   # o: TERMUX_HOST=192.168.2.4 ./ci/deploy-to-termux.sh
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="${BACKEND_DIR:-${SCRIPT_DIR}/..}"

TERMUX_USER="${TERMUX_USER:-u0_a296}"
TERMUX_HOST="${TERMUX_HOST:-192.168.2.4}"
TERMUX_PORT="${TERMUX_PORT:-8022}"

LOCAL_JAR="${LOCAL_JAR:-${BACKEND_DIR}/target/GOrbitS-0.0.1-SNAPSHOT.jar}"
LOCAL_MIGRATIONS_DIR="${LOCAL_MIGRATIONS_DIR:-${BACKEND_DIR}/database/migrations}"
LOCAL_ENV_REQUIRED="${LOCAL_ENV_REQUIRED:-${BACKEND_DIR}/database/env.required.example}"

REMOTE_BASE="${REMOTE_BASE:-~/servers/gorbits}"
REMOTE_RELEASES="${REMOTE_BASE}/releases"
REMOTE_MIGRATIONS="${REMOTE_BASE}/database/migrations"
REMOTE_JAR="${REMOTE_RELEASES}/GOrbitS-new.jar"
REMOTE_DEPLOY_SCRIPT="${REMOTE_BASE}/bin/deploy.sh"

# accept-new: evita "Host key verification failed" en Jenkins/Docker (host.docker.internal)
SSH_COMMON=(-o StrictHostKeyChecking=accept-new -o UserKnownHostsFile=/dev/null -o BatchMode=yes)
if [[ -n "${SSH_KEY_FILE:-}" && -f "${SSH_KEY_FILE}" ]]; then
  SSH_COMMON+=(-i "${SSH_KEY_FILE}")
  chmod 600 "${SSH_KEY_FILE}" 2>/dev/null || true
fi

SSH=(ssh -p "${TERMUX_PORT}" "${SSH_COMMON[@]}" "${TERMUX_USER}@${TERMUX_HOST}")
SCP=(scp -P "${TERMUX_PORT}" "${SSH_COMMON[@]}")

echo "======================================"
echo " DEPLOY GORBITS BACKEND → TERMUX"
echo "======================================"
echo "Backend:  ${BACKEND_DIR}"
echo "Destino:  ${TERMUX_USER}@${TERMUX_HOST}:${TERMUX_PORT}"
echo ""

if [[ ! -f "${LOCAL_JAR}" ]]; then
  echo "ERROR: no existe el JAR: ${LOCAL_JAR}" >&2
  echo "Compila antes: ./mvnw -f GOrbitS/pom.xml clean package -DskipTests" >&2
  exit 1
fi

if [[ ! -d "${LOCAL_MIGRATIONS_DIR}" ]]; then
  echo "ERROR: no existe ${LOCAL_MIGRATIONS_DIR}" >&2
  exit 1
fi

echo "=== SSH ==="
"${SSH[@]}" 'echo "SSH OK"; pwd'

echo ""
echo "=== Preparar carpetas remotas ==="
"${SSH[@]}" "mkdir -p ${REMOTE_RELEASES} ${REMOTE_MIGRATIONS}"

echo ""
echo "=== Copiar JAR → ${REMOTE_JAR} ==="
"${SCP[@]}" "${LOCAL_JAR}" "${TERMUX_USER}@${TERMUX_HOST}:${REMOTE_JAR}"

echo ""
echo "=== Copiar migraciones SQL ==="
if find "${LOCAL_MIGRATIONS_DIR}" -maxdepth 1 -type f -name '*.sql' | grep -q .; then
  "${SCP[@]}" "${LOCAL_MIGRATIONS_DIR}"/*.sql "${TERMUX_USER}@${TERMUX_HOST}:${REMOTE_MIGRATIONS}/"
else
  echo "Sin archivos .sql nuevos."
fi

if [[ -f "${LOCAL_ENV_REQUIRED}" ]]; then
  echo ""
  echo "=== Variables requeridas (revisar .env en Termux) ==="
  cat "${LOCAL_ENV_REQUIRED}"
fi

echo ""
echo "=== Estado migraciones (si existe el script) ==="
"${SSH[@]}" 'test -x ~/servers/gorbits/database/scripts/migrations-status.sh && ~/servers/gorbits/database/scripts/migrations-status.sh || echo "(migrations-status.sh no disponible)"'

echo ""
echo "=== Deploy remoto: bin/deploy.sh ==="
"${SSH[@]}" "${REMOTE_DEPLOY_SCRIPT} ${REMOTE_JAR}"

echo ""
echo "=== Estado final ==="
"${SSH[@]}" '
echo "--- GOrbitS ---"
~/servers/gorbits/bin/status.sh 2>/dev/null || true
echo ""
echo "--- Nginx ---"
~/services/nginx/bin/status.sh 2>/dev/null || true
'

echo ""
echo "Deploy terminado."
echo "  Frontend: http://${TERMUX_HOST}:8088"
echo "  Health:   http://${TERMUX_HOST}:8088/api/actuator/health"
