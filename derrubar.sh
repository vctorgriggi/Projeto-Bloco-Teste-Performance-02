#!/usr/bin/env bash
#
# derruba tudo o que o ./subir.sh subiu.
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
