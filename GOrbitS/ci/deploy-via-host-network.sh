#!/usr/bin/env bash
# Deploy desde Jenkins en Docker (Mac): SSH vía túnel en el host.
# Antes del build: ./scripts/termux-ssh-tunnel.sh  (terminal aparte)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="${BACKEND_DIR:-${SCRIPT_DIR}/..}"

TERMUX_USER="${TERMUX_USER:-u0_a296}"
TERMUX_HOST="${TERMUX_HOST:-192.168.2.4}"
TERMUX_PORT="${TERMUX_PORT:-8022}"
SSH_KEY_FILE="${SSH_KEY_FILE:-}"

if [[ -z "${SSH_KEY_FILE}" || ! -f "${SSH_KEY_FILE}" ]]; then
  echo "ERROR: SSH_KEY_FILE no definido o no existe" >&2
  exit 1
fi

# Jenkins en Docker → túnel en el Mac (scripts/termux-ssh-tunnel.sh)
if [[ -f /.dockerenv ]]; then
  TERMUX_HOST="${JENKINS_DEPLOY_HOST:-host.docker.internal}"
  TERMUX_PORT="${JENKINS_DEPLOY_PORT:-28022}"
  echo "Jenkins (Docker) → ${TERMUX_HOST}:${TERMUX_PORT} (túnel Mac → Termux)"
  echo "Si falla: en el Mac ejecuta ./scripts/termux-ssh-tunnel.sh y deja esa terminal abierta."
fi

# La clave de Jenkins suele ser solo lectura; copiar a tmp editable
if [[ ! -w "${SSH_KEY_FILE}" ]]; then
  KEY_COPY="$(mktemp)"
  trap 'rm -f "${KEY_COPY}"' EXIT
  cp "${SSH_KEY_FILE}" "${KEY_COPY}"
  chmod 600 "${KEY_COPY}"
  export SSH_KEY_FILE="${KEY_COPY}"
fi

export BACKEND_DIR TERMUX_USER TERMUX_HOST TERMUX_PORT
exec "${SCRIPT_DIR}/deploy-to-termux.sh"
