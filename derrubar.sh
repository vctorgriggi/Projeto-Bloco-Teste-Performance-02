#!/usr/bin/env bash
#
# derruba tudo o que o ./subir.sh subiu.
#
#   ./derrubar.sh              encerra os servicos e para o broker (as filas ficam no volume)
#   ./derrubar.sh --limpar     idem, e apaga os volumes do compose: as filas do broker e,
#                              se o modo completo ja rodou, o postgres e a observabilidade
#
# encerra pelas portas, e nao apenas pelos pids anotados, porque o spring-boot:run pode
# ter forkado uma jvm filha: matar o processo do maven deixaria o servico de pe e a porta
# ocupada, e a proxima subida falharia sem explicacao obvia.

set -u

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOGS="$RAIZ/.logs"

cinza() { printf '\033[90m%s\033[0m\n' "$1"; }
verde() { printf '\033[32m%s\033[0m\n' "$1"; }

echo

# o -sTCP:LISTEN e essencial: sem ele, o lsof tambem casa as conexoes DE SAIDA para
# aquela porta, e um "kill" na porta 8081 acabaria matando o monolito, que mantem uma
# conexao aberta com o microsservico.
for porta in 5173 8080 8081 8761 8888; do
    pids=$(lsof -ti "tcp:$porta" -sTCP:LISTEN 2>/dev/null)
    if [ -n "$pids" ]; then
        kill $pids 2>/dev/null
        cinza "encerrando o que escutava na porta $porta"
    fi
done

# os processos maven que restaram (o pai que forkou a jvm)
if [ -f "$LOGS/pids" ]; then
    while read -r pid; do
        [ -n "$pid" ] && kill "$pid" 2>/dev/null
    done < "$LOGS/pids"
    rm -f "$LOGS/pids"
fi

# o broker para, mas o container e o volume ficam: filas duraveis e mensagens
# persistentes sobrevivem, e a proxima subida encontra o que estava esperando. o
# --limpar e para quando se quer comecar do zero.
if command -v docker >/dev/null 2>&1 && docker ps -a --format '{{.Names}}' 2>/dev/null | grep -q '^blog-rabbitmq$'; then
    if [ "${1:-}" = "--limpar" ]; then
        ( cd "$RAIZ" && docker compose down -v ) >/dev/null 2>&1
        cinza "broker removido, com os volumes do compose (filas, e o banco e a observabilidade do modo completo)"
    else
        ( cd "$RAIZ" && docker compose stop rabbitmq ) >/dev/null 2>&1
        cinza "broker parado (filas preservadas; ./derrubar.sh --limpar para apagar)"
    fi
fi

sleep 3

restantes=""
for porta in 5173 8080 8081 8761 8888; do
    lsof -ti "tcp:$porta" -sTCP:LISTEN >/dev/null 2>&1 && restantes="$restantes $porta"
done

if [ -n "$restantes" ]; then
    printf '\033[31mainda ocupadas:%s\033[0m\n' "$restantes"
    cinza "se insistir, use: lsof -ti tcp:PORTA -sTCP:LISTEN | xargs kill -9"
else
    verde "tudo encerrado"
fi
echo
