#!/usr/bin/env bash
set -euo pipefail
RECURSOS_CI="${RECURSOS_CI:-$HOME/Downloads/RECURSOS/ci-cd}"
cd "${RECURSOS_CI}/jenkins" && docker compose down
cd "${RECURSOS_CI}/sonarqube" && docker compose down
echo "Contenedores detenidos. La red network_jenkins se conserva."
