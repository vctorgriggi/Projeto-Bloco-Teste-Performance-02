#!/usr/bin/env bash
#
# teste de ponta a ponta contra a stack IMPLANTADA -- docker compose, kind ou qualquer
# ambiente que exponha o front (e, por ele, a api).
#
#   scripts/e2e.sh                                  contra http://localhost:8080
#   scripts/e2e.sh http://localhost:30080           contra outro endereco
#   GRAFANA_URL=http://localhost:3000 scripts/e2e.sh
#                                                   confere tambem traces e logs
#
# os testes de cada servico (./mvnw test) provam as pecas; este prova o conjunto: o
# nginx repassando ao monolito, o monolito achando o engajamento pelo eureka, o
# comentario atravessando a fila, o evento de post apagado saindo pelo outbox e voltando
# como contador zerado, o postgres guardando tudo. e o mesmo script que o pipeline roda
# depois de implantar num cluster kind.
#
# precisa de curl e jq.

set -euo pipefail

BASE="${1:-http://localhost:8080}"
API="$BASE/api"
GRAFANA_URL="${GRAFANA_URL:-}"
GRAFANA_AUTH="${GRAFANA_AUTH:-admin:admin}"
SUFIXO="$(date +%s)-$RANDOM"
PASSOS=0

verde()    { printf '\033[32m%s\033[0m\n' "$1"; }
vermelho() { printf '\033[31m%s\033[0m\n' "$1"; }
cinza()    { printf '\033[90m%s\033[0m\n' "$1"; }

ok() { PASSOS=$((PASSOS + 1)); verde "  ok  $1"; }
falha() { vermelho "  falhou  $1"; [ -n "${2:-}" ] && cinza "          $2"; exit 1; }

# faz a requisicao e guarda status e corpo em STATUS e CORPO
req() {
    local metodo="$1" caminho="$2" corpo="${3:-}"
    local saida
    if [ -n "$corpo" ]; then
        saida=$(curl -s -w '\n%{http_code}' -X "$metodo" "$API$caminho" -H 'Content-Type: application/json' -d "$corpo")
    else
        saida=$(curl -s -w '\n%{http_code}' -X "$metodo" "$API$caminho")
    fi
    STATUS="${saida##*$'\n'}"
    CORPO="${saida%$'\n'*}"
}

espera_status() {
    local esperado="$1" descricao="$2"
    [ "$STATUS" = "$esperado" ] || falha "$descricao (esperava $esperado, veio $STATUS)" "$CORPO"
}

# repete uma condicao (expressao jq sobre a resposta de um GET) ate ser verdadeira. e
# assim que se testa o que e assincrono: o comentario que passa pela fila, o contador que
# chega por evento. o limite e generoso porque o relay do outbox roda a cada 2s e o
# consumidor pode estar retentando.
ate() {
    local caminho="$1" condicao="$2" descricao="$3" limite="${4:-60}"
    local i=0
    while [ "$i" -lt "$limite" ]; do
        req GET "$caminho"
        if [ "$STATUS" = "200" ] && echo "$CORPO" | jq -e "$condicao" >/dev/null 2>&1; then
            ok "$descricao (${i}s)"
            return 0
        fi
        sleep 1
        i=$((i + 1))
    done
    falha "$descricao: nao aconteceu em ${limite}s" "ultima resposta ($STATUS): $CORPO"
}

echo
echo "e2e contra $BASE"
echo

# ---- a stack esta pronta ---------------------------------------------------------------
echo "stack"
ate "/engagement/status" '.available and .brokerAvailable and .registeredInstances >= 1' \
    "monolito ve o engajamento pelo registro e o broker esta de pe" 180
req GET "/engagement/status"
cinza "        $(echo "$CORPO" | jq -c '{registeredInstances, pendingEvents}')"

# ---- authoring -------------------------------------------------------------------------
echo "autores e posts"
req POST "/authors" "{\"name\":\"E2E $SUFIXO\",\"email\":\"e2e-$SUFIXO@blog.dev\",\"bio\":\"criado pelo teste\"}"
espera_status 201 "cadastrar autor"
AUTOR=$(echo "$CORPO" | jq -r .id)
ok "autor $AUTOR cadastrado"

req POST "/authors" "{\"name\":\"E2E\",\"email\":\"e2e-$SUFIXO@blog.dev\",\"bio\":\"x\"}"
espera_status 409 "email repetido"
ok "email repetido recusado com 409"

req POST "/posts" "{\"title\":\"post do e2e $SUFIXO\",\"content\":\"texto\",\"authorId\":$AUTOR}"
espera_status 201 "criar post"
POST=$(echo "$CORPO" | jq -r .id)
ok "post $POST criado como rascunho"

req POST "/posts/$POST/publish"
espera_status 200 "publicar"
req POST "/posts/$POST/publish"
espera_status 409 "publicar de novo"
ok "publicado; republicar e recusado com 409"

# o bug do @Lob da segunda entrega: texto maior que 255 caracteres quebrava a auditoria
LONGO=$(printf 'um paragrafo de verdade. %.0s' $(seq 1 80))
req POST "/posts" "{\"title\":\"post longo $SUFIXO\",\"content\":\"$LONGO\",\"authorId\":$AUTOR}"
espera_status 201 "criar post com texto longo"
POST_LONGO=$(echo "$CORPO" | jq -r .id)
req GET "/posts/$POST_LONGO/history"
espera_status 200 "historico do post longo"
[ "$(echo "$CORPO" | jq -r '.[0].content | length')" = "${#LONGO}" ] || falha "historico guardou o texto inteiro"
ok "post de ${#LONGO} caracteres criado e auditado inteiro"

# ---- comentario pela fila --------------------------------------------------------------
echo "comentario (comando na fila)"
req POST "/posts/$POST/comments" '{"authorName":"Carla","content":"recado do e2e"}'
espera_status 202 "enviar comentario"
ENVIO=$(echo "$CORPO" | jq -r .submissionId)
[ "$(echo "$CORPO" | jq -r .status)" = "PENDING" ] || falha "comentario aceito como PENDING" "$CORPO"
ok "aceito com 202 (envio $ENVIO)"
ate "/posts/$POST/comments" "any(.[]; .submissionId == \"$ENVIO\")" \
    "o engajamento consumiu o comando e o comentario aparece na conversa"

req POST "/posts/$POST/comments" '{"authorName":"Carla","content":""}'
espera_status 400 "comentario vazio"
req POST "/posts/999999999/comments" '{"authorName":"Carla","content":"x"}'
espera_status 404 "comentario em post inexistente"
ok "vazio recusado com 400, post inexistente com 404 (antes de ir para a fila)"

# ---- reacao por http -------------------------------------------------------------------
echo "reacao (http sincrono)"
req POST "/posts/$POST/reactions" '{"readerName":"Carla","type":"IDEIA"}'
espera_status 201 "reagir"
req POST "/posts/$POST/reactions" '{"readerName":"Carla","type":"IDEIA"}'
espera_status 409 "reagir de novo"
req POST "/posts/$POST/reactions" '{"readerName":"Carla","type":"APLAUSO"}'
espera_status 400 "tipo inexistente"
ok "201, repetida 409, tipo inexistente 400 -- as regras e mensagens vem do microsservico"

# ---- contadores por evento -------------------------------------------------------------
echo "contadores (eventos do engajamento)"
ate "/engagement/counters" "any(.[]; .postId == $POST and .comments == 1 and .reactions == 1)" \
    "a copia local no monolito recebeu os dois eventos"

# ---- exclusao pelo outbox --------------------------------------------------------------
echo "exclusao (evento pelo outbox)"
req DELETE "/posts/$POST"
espera_status 204 "apagar post"
ok "post apagado"
ate "/engagement/counters" "any(.[]; .postId == $POST and .comments == 0 and .reactions == 0)" \
    "outbox -> broker -> engajamento limpou a conversa -> engagement.purged zerou o contador"
ate "/engagement/status" '.pendingEvents == 0' "outbox vazio de novo"
req DELETE "/posts/$POST_LONGO"

# ---- observabilidade (opcional) --------------------------------------------------------
if [ -n "$GRAFANA_URL" ]; then
    echo "observabilidade ($GRAFANA_URL)"
    # a linha de log do monolito que aceitou o comentario, com o id do trace
    TRACE=""
    for i in $(seq 1 60); do
        TRACE=$(curl -s -u "$GRAFANA_AUTH" -G "$GRAFANA_URL/api/datasources/proxy/uid/loki/loki/api/v1/query_range" \
            --data-urlencode "query={service_name=\"blog-api\"} |= \"$ENVIO\"" \
            --data-urlencode "start=$(( $(date +%s) - 900 ))000000000" \
            | jq -r '.data.result[0].stream.trace_id // empty' 2>/dev/null || true)
        [ -n "$TRACE" ] && break
        sleep 2
    done
    [ -n "$TRACE" ] || falha "o log do envio chegou ao loki com o trace_id"
    ok "log do envio no loki, com trace_id $TRACE"

    SERVICOS=""
    for i in $(seq 1 60); do
        SERVICOS=$(curl -s -u "$GRAFANA_AUTH" "$GRAFANA_URL/api/datasources/proxy/uid/tempo/api/traces/$TRACE" \
            | jq -r '[.batches[].resource.attributes[] | select(.key=="service.name") | .value.stringValue] | unique | join(",")' 2>/dev/null || true)
        [[ "$SERVICOS" == *blog-api* && "$SERVICOS" == *engagement-service* ]] && break
        sleep 2
    done
    [[ "$SERVICOS" == *blog-api* && "$SERVICOS" == *engagement-service* ]] \
        || falha "o trace do comentario atravessa os dois servicos" "servicos no trace: $SERVICOS"
    ok "o trace atravessa a fila: $SERVICOS"
fi

echo
verde "e2e passou: $PASSOS verificacoes"
echo
