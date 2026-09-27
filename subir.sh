#!/usr/bin/env bash
#
# sobe a stack inteira na ordem certa e espera cada peca responder antes de seguir.
#
# a ordem nao e preferencia, e dependencia: o config server serve as propriedades dos
# servicos de negocio, e o registro precisa existir para eles se encontrarem. subir fora
# de ordem funciona (o cliente de configuracao tem retry, e o monolito responde 503
# enquanto nao ve o engajamento), mas demora e polui o log -- em uma apresentacao, isso
# parece defeito.
#
# o broker (rabbitmq) sobe primeiro, por docker compose. ele e o unico que os servicos
# toleram ver chegar depois -- os consumidores reconectam sozinhos, e o outbox guarda os
# eventos enquanto isso --, mas subir antes evita um log cheio de "connection refused".
#
#   ./subir.sh          sobe tudo, incluindo o front-end
#   ./subir.sh --sem-front   sobe o broker e os quatro servicos
#
# os logs de cada processo ficam em .logs/. para derrubar tudo: ./derrubar.sh

set -u

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOGS="$RAIZ/.logs"
COM_FRONT=1
[ "${1:-}" = "--sem-front" ] && COM_FRONT=0

mkdir -p "$LOGS"

azul()  { printf '\033[36m%s\033[0m\n' "$1"; }
verde() { printf '\033[32m%s\033[0m\n' "$1"; }
vermelho() { printf '\033[31m%s\033[0m\n' "$1"; }
cinza() { printf '\033[90m%s\033[0m\n' "$1"; }

# checa se a porta esta livre antes de tentar usar. sem isso, um processo esquecido de
# uma execucao anterior faria o servico novo morrer com "port already in use" no meio do
# log, e a causa nao fica obvia.
porta_ocupada() {
    lsof -ti "tcp:$1" -sTCP:LISTEN >/dev/null 2>&1
}

# espera uma url responder 200. o timeout e generoso porque o primeiro build de cada
# servico pode ter que baixar dependencias.
espera_http() {
    local url="$1" nome="$2" limite="${3:-180}"
    local i=0
    while [ "$i" -lt "$limite" ]; do
        if [ "$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 "$url" 2>/dev/null)" = "200" ]; then
            verde "   ok  $nome respondendo (${i}s)"
            return 0
        fi
        # se o processo morreu, nao ha sentido em esperar o timeout inteiro: mostra o fim
        # do log na hora, que e onde a causa real esta (porta ocupada, erro de
        # configuracao, falha de compilacao)
        if [ -n "${PID_ATUAL:-}" ] && ! kill -0 "$PID_ATUAL" 2>/dev/null; then
            vermelho "   erro  $nome encerrou antes de responder"
            cinza "         ultimas linhas de $LOGS/$nome.log:"
            tail -15 "$LOGS/$nome.log" | sed 's/^/         /'
            return 1
        fi
        sleep 1
        i=$((i + 1))
    done
    vermelho "   erro  $nome nao respondeu em ${limite}s"
    cinza "         ultimas linhas de $LOGS/$nome.log:"
    tail -15 "$LOGS/$nome.log" | sed 's/^/         /'
    return 1
}

# como espera_http, mas com usuario e senha. o painel do rabbitmq so responde 200 para
# quem se autentica, e e ele que diz que o broker terminou de subir (a porta amqp abre
# antes de o broker estar pronto para aceitar declaracoes).
espera_http_auth() {
    local url="$1" nome="$2" credenciais="$3" limite="${4:-90}"
    local i=0
    while [ "$i" -lt "$limite" ]; do
        if [ "$(curl -s -u "$credenciais" -o /dev/null -w '%{http_code}' --max-time 3 "$url" 2>/dev/null)" = "200" ]; then
            verde "   ok  $nome respondendo (${i}s)"
            return 0
        fi
        sleep 1
        i=$((i + 1))
    done
    vermelho "   erro  $nome nao respondeu em ${limite}s"
    cinza "         veja: docker compose logs rabbitmq"
    return 1
}

sobe_servico() {
    local dir="$1" nome="$2" porta="$3" url="$4"

    if porta_ocupada "$porta"; then
        cinza "   ja no ar  $nome (porta $porta ocupada) -- pulando"
        return 0
    fi

    azul "-> $nome (porta $porta)"
    ( cd "$RAIZ/$dir" && exec ./mvnw -q -B spring-boot:run ) > "$LOGS/$nome.log" 2>&1 &
    PID_ATUAL=$!
    echo "$PID_ATUAL" >> "$LOGS/pids"
    espera_http "$url" "$nome" 180 || return 1
}

echo
azul "subindo a stack do blog (logs em .logs/)"
echo

: > "$LOGS/pids"

# 0. o broker de mensagens, em container
if porta_ocupada 5672; then
    cinza "   ja no ar  rabbitmq (porta 5672 ocupada) -- pulando"
elif ! command -v docker >/dev/null 2>&1; then
    vermelho "   aviso  docker nao encontrado: sem broker, comentar responde 503 e os contadores"
    vermelho "          da estante nao se atualizam. o resto do blog funciona."
else
    azul "-> rabbitmq (portas 5672 e 15672, via docker compose)"
    ( cd "$RAIZ" && docker compose up -d rabbitmq ) > "$LOGS/rabbitmq.log" 2>&1
    espera_http_auth "http://localhost:15672/api/overview" rabbitmq guest:guest 90 || exit 1
fi

# 1. configuracao central: os servicos de negocio pedem as propriedades deles aqui
sobe_servico config-server    config-server    8888 "http://localhost:8888/blog-api/default" || exit 1

# 2. registro de servicos: e por ele que o monolito encontra o engajamento
sobe_servico discovery-server discovery-server 8761 "http://localhost:8761/eureka/apps" || exit 1

# 3. o microsservico
sobe_servico engagement-service engagement-service 8081 "http://localhost:8081/api/engagement/ping" || exit 1

# 4. o monolito
sobe_servico backend          backend          8080 "http://localhost:8080/api/posts" || exit 1

# o monolito so passa a ver o engajamento depois de buscar a tabela de instancias no
# registro (a cada 10s, pela configuracao central). esperar por isso aqui e o que evita
# abrir a interface e ver "fora do ar" por alguns segundos sem motivo aparente.
echo
azul "-> aguardando a integracao entre os dois servicos"
i=0
while [ "$i" -lt 60 ]; do
    if curl -s --max-time 3 http://localhost:8080/api/engagement/status 2>/dev/null | grep -q '"available":true'; then
        verde "   ok  monolito e engajamento se encontraram (${i}s)"
        break
    fi
    sleep 2
    i=$((i + 2))
done
[ "$i" -ge 60 ] && vermelho "   aviso  a integracao nao ficou pronta; veja .logs/backend.log"

if [ "$COM_FRONT" = "1" ]; then
    echo
    if porta_ocupada 5173; then
        cinza "   ja no ar  frontend (porta 5173) -- pulando"
    else
        azul "-> frontend (porta 5173)"
        # checa o binario, e nao a existencia do diretorio: um node_modules presente mas
        # vazio (ou incompleto) passaria por um teste de diretorio e o npm run dev
        # morreria com "vite: command not found", que nao aponta para a causa
        if [ ! -x "$RAIZ/frontend/node_modules/.bin/vite" ]; then
            cinza "   instalando dependencias do front-end..."
            ( cd "$RAIZ/frontend" && npm install --silent ) >> "$LOGS/frontend.log" 2>&1
        fi
        ( cd "$RAIZ/frontend" && exec npm run dev ) > "$LOGS/frontend.log" 2>&1 &
        echo $! >> "$LOGS/pids"
        espera_http "http://localhost:5173/" frontend 60 || true
    fi
fi

echo
verde "stack no ar:"
cat <<'ENDERECOS'
   interface .............. http://localhost:5173
   api (monolito) ......... http://localhost:8080/api
   microsservico .......... http://localhost:8081/api
   painel do eureka ....... http://localhost:8761
   painel do rabbitmq ..... http://localhost:15672   (guest / guest)
   config server .......... http://localhost:8888/blog-api/default
   status da integracao ... http://localhost:8080/api/engagement/status
ENDERECOS
echo
cinza "para derrubar tudo:  ./derrubar.sh"
echo
