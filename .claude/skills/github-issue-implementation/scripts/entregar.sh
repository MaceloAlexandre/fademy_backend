#!/usr/bin/env bash
# Entrega uma issue cujo PR já passou no CI: In review → merge (squash) → Done → fecha a issue
# com "Entregue no PR #n" → atualiza a main local.
# uso: entregar.sh <issue> <pr>
# Só use com autorização do usuário para fazer o merge (a regra padrão é o merge ser dele).
set -euo pipefail
S=$(cd "$(dirname "$0")" && pwd); RAIZ=$(cd "$S/../../../.." && pwd)
R=repos/${FADEMY_DONO:-MaceloAlexandre}/${FADEMY_REPO:-fademy_backend}; N=$1; PR=$2
"$S/status.sh" "$N" "In review"
T=$(gh api "$R/pulls/$PR" -q .title); B=$(gh api "$R/pulls/$PR" -q .head.ref)
gh api -X PUT "$R/pulls/$PR/merge" -f merge_method=squash -f commit_title="$T (#$PR)" -q .merged
gh api -X DELETE "$R/git/refs/heads/$B"
[ "$(gh api "$R/pulls/$PR" -q .merged)" = true ] || { echo "PR $PR não foi mergeado"; exit 1; }
"$S/status.sh" "$N" Done
gh api "$R/issues/$N/comments" -f body="Entregue no PR #$PR" -q .id >/dev/null
gh api -X PATCH "$R/issues/$N" -f state=closed -q '"issue #\(.number): \(.state)"'
git -C "$RAIZ" fetch -q --prune && git -C "$RAIZ" checkout -q main && git -C "$RAIZ" pull -q && git -C "$RAIZ" branch -D "$B" -q 2>/dev/null || true
git -C "$RAIZ" log --oneline -1
