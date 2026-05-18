#!/usr/bin/env bash
# Deploy desde Jenkins en Docker: red del host para alcanzar Termux (Mac).
# Antes del build: ./scripts/termux-ssh-tunnel.sh  (en otra terminal)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="${BACKEND_DIR:-${SCRIPT_DIR}/..}"
REPO_ROOT="$(cd "${BACKEND_DIR}/.." && pwd)"

TERMUX_USER="${TERMUX_USER:-u0_a296}"
TERMUX_HOST="${TERMUX_HOST:-192.168.2.4}"
TERMUX_PORT="${TERMUX_PORT:-8022}"
SSH_KEY_FILE="${SSH_KEY_FILE:-}"

if [[ -z "${SSH_KEY_FILE}" || ! -f "${SSH_KEY_FILE}" ]]; then
  echo "ERROR: SSH_KEY_FILE no definido o no existe" >&2
  exit 1
fi

# Jenkins en Docker → túnel en el Mac (termux-ssh-tunnel.sh)
if [[ -f /.dockerenv ]]; then
  TERMUX_HOST="${JENKINS_DEPLOY_HOST:-host.docker.internal}"
  TERMUX_PORT="${JENKINS_DEPLOY_PORT:-28022}"
  echo "Jenkins (Docker) → ${TERMUX_HOST}:${TERMUX_PORT} (túnel al Termux)"
fi

if ! command -v docker >/dev/null 2>&1; then
  export TERMUX_USER TERMUX_HOST TERMUX_PORT SSH_KEY_FILE BACKEND_DIR
  exec "${SCRIPT_DIR}/deploy-to-termux.sh"
fi

docker run --rm --network host \
  -v "${REPO_ROOT}:${REPO_ROOT}" \
  -v "${SSH_KEY_FILE}:/tmp/deploy_key:ro" \
  -e "BACKEND_DIR=${BACKEND_DIR}" \
  -e "TERMUX_USER=${TERMUX_USER}" \
  -e "TERMUX_HOST=${TERMUX_HOST}" \
  -e "TERMUX_PORT=${TERMUX_PORT}" \
  -e "SSH_KEY_FILE=/tmp/deploy_key" \
  -w "${BACKEND_DIR}" \
  alpine:3.20 sh -c '
    apk add --no-cache openssh-client bash >/dev/null
    chmod 600 /tmp/deploy_key
    exec ./ci/deploy-to-termux.sh
  '
