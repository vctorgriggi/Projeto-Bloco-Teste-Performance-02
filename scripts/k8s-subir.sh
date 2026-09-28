#!/usr/bin/env bash
#
# sobe o blog num cluster kubernetes local (kind) e espera tudo ficar pronto.
#
#   scripts/k8s-subir.sh             cria o cluster (se nao existir), constroi as
#                                    imagens, carrega no cluster e implanta
#   scripts/k8s-subir.sh --sem-build reaproveita as imagens ja construidas
#
# depois: front em http://localhost:30080, grafana em http://localhost:30300 (admin /
# admin), painel do rabbitmq em http://localhost:31567, eureka em http://localhost:30761.
# para derrubar: scripts/k8s-derrubar.sh
#
# precisa de docker, kind e kubectl. o pipeline faz os mesmos passos (veja
# .github/workflows/pipeline.yml), com as imagens que acabou de construir.

set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CLUSTER=blog
IMAGENS=(blog/config-server:dev blog/discovery-server:dev blog/engagement-service:dev blog/blog-api:dev blog/frontend:dev)
METRICS_SERVER_VERSION=v0.7.2

azul()  { printf '\033[36m%s\033[0m\n' "$1"; }
verde() { printf '\033[32m%s\033[0m\n' "$1"; }
cinza() { printf '\033[90m%s\033[0m\n' "$1"; }

cd "$RAIZ"

azul "-> cluster kind \"$CLUSTER\""
if kind get clusters 2>/dev/null | grep -qx "$CLUSTER"; then
    cinza "   ja existe -- reaproveitando"
else
    kind create cluster --config deploy/kind/cluster.yaml --wait 120s
fi
kubectl config use-context "kind-$CLUSTER" >/dev/null

if [ "${1:-}" != "--sem-build" ]; then
    azul "-> construindo as imagens"
    docker compose --profile completo build config-server discovery-server engagement-service blog-api frontend
fi

# o kind nao enxerga as imagens do docker da maquina: elas sao copiadas para dentro do
# no. em producao, o no baixaria do registro (veja deploy/overlays/ghcr).
azul "-> carregando as imagens no cluster"
kind load docker-image --name "$CLUSTER" "${IMAGENS[@]}"

# o hpa precisa saber o uso de cpu dos pods, e quem mede e o metrics-server. no kind o
# kubelet usa certificado autoassinado, dai o --kubelet-insecure-tls (so em cluster local).
azul "-> metrics-server (para o autoescalonamento)"
if ! kubectl -n kube-system get deployment metrics-server >/dev/null 2>&1; then
    kubectl apply -f "https://github.com/kubernetes-sigs/metrics-server/releases/download/$METRICS_SERVER_VERSION/components.yaml" >/dev/null
    kubectl -n kube-system patch deployment metrics-server --type=json \
        -p '[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]' >/dev/null
fi

azul "-> implantando (kubectl apply -k deploy/)"
kubectl apply -k deploy/

# a ordem de prontidao segue as dependencias, como no ./subir.sh: os servicos de negocio
# esperam o config server (com retry) e, sem ele, o kubernetes os reiniciaria ate ele
# aparecer. esperar em ordem so deixa o log mais limpo.
azul "-> esperando cada peca ficar pronta"
kubectl -n blog rollout status statefulset/postgres --timeout=180s
kubectl -n blog rollout status statefulset/rabbitmq --timeout=240s
kubectl -n blog rollout status deployment/lgtm --timeout=240s
kubectl -n blog rollout status deployment/config-server --timeout=240s
kubectl -n blog rollout status deployment/discovery-server --timeout=240s
kubectl -n blog rollout status deployment/engagement-service --timeout=420s
kubectl -n blog rollout status deployment/blog-api --timeout=420s
kubectl -n blog rollout status deployment/frontend --timeout=120s

echo
kubectl -n blog get pods -o wide
echo
verde "blog no ar no kubernetes:"
cat <<'ENDERECOS'
   interface .............. http://localhost:30080
   grafana ................ http://localhost:30300   (admin / admin)
   painel do rabbitmq ..... http://localhost:31567   (blog / rabbit-demo)
   painel do eureka ....... http://localhost:30761
ENDERECOS
echo
cinza "teste de ponta a ponta:  GRAFANA_URL=http://localhost:30300 scripts/e2e.sh http://localhost:30080"
cinza "para derrubar:           scripts/k8s-derrubar.sh"
echo
