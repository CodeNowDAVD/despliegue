#!/usr/bin/env bash
# Sesión 08 — levanta red Docker + SonarQube + Jenkins (recursos del docente).
set -euo pipefail

RECURSOS_CI="${RECURSOS_CI:-$HOME/Downloads/RECURSOS/ci-cd}"

if [[ ! -d "${RECURSOS_CI}/jenkins" ]]; then
  echo "ERROR: no existe ${RECURSOS_CI}/jenkins" >&2
  echo "Ajusta RECURSOS_CI o copia RECURSOS/ci-cd del curso." >&2
  exit 1
fi

echo "==> Red Docker network_jenkins"
docker network inspect network_jenkins >/dev/null 2>&1 \
  || docker network create network_jenkins

echo "==> SonarQube (http://localhost:9001)"
cd "${RECURSOS_CI}/sonarqube"
docker compose up -d

echo "==> Jenkins (http://localhost:9080) — primera vez: password en jenkins_home/secrets/initialAdminPassword"
cd "${RECURSOS_CI}/jenkins"
docker compose up -d --build

echo ""
echo "Espera ~2 min y abre:"
echo "  Jenkins:    http://localhost:9080"
echo "  SonarQube:  http://localhost:9001  (admin / admin, cambiar password)"
echo ""
echo "Siguiente: GOrbitS/CI.md → sección «Configurar Jenkins (UI)»"
