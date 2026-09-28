#!/usr/bin/env bash
# Muda o Status de uma issue no Project "Fademy".
# uso: status.sh <numero-da-issue> "<Todo|In progress|In review|Done>"
#
# Usa uma consulta GraphQL mínima (com cache) e uma mutação por mudança. Os comandos
# "gh project ..." fazem consultas pesadas e disparam o limite secundário da API do GitHub.
set -euo pipefail
N=$1; ST=$2
DONO=${FADEMY_DONO:-MaceloAlexandre}; PROJETO=${FADEMY_PROJETO:-2}; REPO=${FADEMY_REPO:-fademy_backend}
C=${TMPDIR:-/tmp}/fademy-projeto-$PROJETO.json
Q="{ user(login:\"$DONO\") { projectV2(number:$PROJETO) { id field(name:\"Status\") { ... on ProjectV2SingleSelectField { id options { id name } } } items(first:100) { nodes { id content { ... on Issue { number repository { name } } } } } } } }"
item() { python3 -c "import json; d=json.load(open('$C'))['data']['user']['projectV2']; print(next((i['id'] for i in d['items']['nodes'] if i['content'] and i['content'].get('number')==$N and i['content']['repository']['name']=='$REPO'),''))"; }
# Item recém-adicionado pode não estar no cache: consulta de novo.
[ -f "$C" ] && [ -n "$(item)" ] || gh api graphql -f query="$Q" > "$C"
read -r PID FID OID < <(python3 -c "import json; d=json.load(open('$C'))['data']['user']['projectV2']; print(d['id'], d['field']['id'], next(o['id'] for o in d['field']['options'] if o['name']=='$ST'))")
IID=$(item); [ -n "$IID" ] || { echo "issue $N não está no Project (se acabou de ser adicionada, tente de novo em alguns segundos)"; exit 1; }
R=$(gh api graphql -f query="mutation { updateProjectV2ItemFieldValue(input:{projectId:\"$PID\", itemId:\"$IID\", fieldId:\"$FID\", value:{singleSelectOptionId:\"$OID\"}}) { projectV2Item { fieldValueByName(name:\"Status\") { ... on ProjectV2ItemFieldSingleSelectValue { name } } } } }" -q '.data.updateProjectV2ItemFieldValue.projectV2Item.fieldValueByName.name')
echo "#$N -> $R"
