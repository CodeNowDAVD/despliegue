#!/usr/bin/env bash
# Reconstruye Jenkins con Java 21 (imagen lts-jdk21). Ejecutar una vez si falla compile con records/text blocks.
set -euo pipefail
RECURSOS_CI="${RECURSOS_CI:-$HOME/Downloads/RECURSOS/ci-cd}"
echo "==> Parar Jenkins"
cd "${RECURSOS_CI}/jenkins"
docker compose down
echo "==> Rebuild imagen (JDK 21)"
docker compose build --no-cache
docker compose up -d
echo ""
echo "Jenkins: http://localhost:9080"
echo "Tras arrancar (~2 min), vuelve a Build en gorbits-pipeline."
echo "En Console Output debe salir: openjdk version \"21\" ..."
