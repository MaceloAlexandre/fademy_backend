#!/usr/bin/env bash
# Espera todos os checks do último commit do PR terminarem (REST) e imprime o resultado.
# uso: ci.sh <numero-do-pr>
set -euo pipefail
R=repos/${FADEMY_DONO:-MaceloAlexandre}/${FADEMY_REPO:-fademy_backend}
SHA=$(gh api "$R/pulls/$1" -q .head.sha)
until [ "$(gh api "$R/commits/$SHA/check-runs" -q '.total_count')" != 0 ] \
      && [ "$(gh api "$R/commits/$SHA/check-runs" -q '[.check_runs[]|select(.status!="completed")]|length')" = 0 ]; do
  sleep 15
done
gh api "$R/commits/$SHA/check-runs" -q '.check_runs[]|"\(.name): \(.conclusion)"'
