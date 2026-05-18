#!/usr/bin/env bash
# Reenvía un puerto del Mac hacia Termux para que Jenkins (Docker) pueda hacer SSH.
# Jenkins usa host.docker.internal:LOCAL_PORT → este script → Termux:8022
#
# Uso (dejar corriendo en una terminal antes del build con deploy):
#   ./scripts/termux-ssh-tunnel.sh
#   # o en segundo plano:
#   ./scripts/termux-ssh-tunnel.sh &
#
set -euo pipefail

TERMUX_HOST="${TERMUX_HOST:-192.168.2.4}"
TERMUX_PORT="${TERMUX_PORT:-8022}"
LOCAL_PORT="${LOCAL_PORT:-28022}"

if ! command -v socat >/dev/null 2>&1; then
  echo "Instala socat: brew install socat" >&2
  exit 1
fi

if lsof -i ":${LOCAL_PORT}" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Puerto ${LOCAL_PORT} ya en uso (túnel activo?)."
  echo "Jenkins debe usar: DEPLOY_HOST=host.docker.internal  DEPLOY_SSH_PORT=${LOCAL_PORT}"
  exit 0
fi

echo "Túnel: host.docker.internal:${LOCAL_PORT} → ${TERMUX_HOST}:${TERMUX_PORT}"
echo "Mantén esta terminal abierta durante el build de Jenkins."
echo ""

exec socat "TCP-LISTEN:${LOCAL_PORT},fork,reuseaddr" "TCP:${TERMUX_HOST}:${TERMUX_PORT}"
