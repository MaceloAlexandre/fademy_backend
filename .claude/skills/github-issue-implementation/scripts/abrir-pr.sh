#!/usr/bin/env bash
# Abre o PR pela API REST e imprime o número.
# uso: abrir-pr.sh <branch> "<titulo>" <arquivo-com-o-corpo>
set -euo pipefail
REPO=${FADEMY_DONO:-MaceloAlexandre}/${FADEMY_REPO:-fademy_backend}
gh api "repos/$REPO/pulls" -f base=main -f head="$1" -f title="$2" -F body=@"$3" -q .number
