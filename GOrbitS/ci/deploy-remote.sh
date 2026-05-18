#!/usr/bin/env bash
# Alias: usa el flujo Termux (deploy.sh en el servidor).
# Preferir: ./ci/deploy-to-termux.sh
exec "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/deploy-to-termux.sh" "$@"
