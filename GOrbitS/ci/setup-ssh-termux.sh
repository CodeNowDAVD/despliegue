#!/usr/bin/env bash
# Copia tu clave SSH a Termux para deploy sin contraseña (Mac y Jenkins).
set -euo pipefail

TERMUX_USER="${TERMUX_USER:-u0_a296}"
TERMUX_HOST="${TERMUX_HOST:-192.168.2.4}"
TERMUX_PORT="${TERMUX_PORT:-8022}"

KEY="${SSH_KEY:-$HOME/.ssh/id_ed25519}"
if [[ ! -f "${KEY}" ]]; then
  KEY="$HOME/.ssh/id_rsa"
fi

if [[ ! -f "${KEY}" ]]; then
  echo "Generando clave ${KEY}..."
  ssh-keygen -t ed25519 -f "${KEY}" -N "" -C "gorbits-deploy-mac"
fi

echo "Copiando ${KEY}.pub → ${TERMUX_USER}@${TERMUX_HOST}:${TERMUX_PORT}"
ssh-copy-id -i "${KEY}.pub" -p "${TERMUX_PORT}" "${TERMUX_USER}@${TERMUX_HOST}"

echo ""
echo "Prueba sin password:"
ssh -p "${TERMUX_PORT}" -o BatchMode=yes "${TERMUX_USER}@${TERMUX_HOST}" 'echo SSH OK'

echo ""
echo "Para Jenkins: credencial «gorbits-ssh» = contenido de:"
echo "  ${KEY}"
