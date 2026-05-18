#!/usr/bin/env bash
# Análisis SonarQube local (contenedor en :9001).
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="${BACKEND_DIR:-${SCRIPT_DIR}/..}"

SONAR_HOST_URL="${SONAR_HOST_URL:-http://localhost:9001}"
SONAR_TOKEN="${SONAR_TOKEN:?Exporta SONAR_TOKEN (Sonar → My Account → Security → Generate Token)}"

cd "${BACKEND_DIR}"
./mvnw clean verify
./mvnw org.sonarsource.scanner.maven:sonar-maven-plugin:3.11.0.3922:sonar \
  -Dsonar.host.url="${SONAR_HOST_URL}" \
  -Dsonar.token="${SONAR_TOKEN}"

echo "Dashboard: ${SONAR_HOST_URL}/dashboard?id=gorbits-gorbits"
