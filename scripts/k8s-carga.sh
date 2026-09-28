#!/usr/bin/env bash
#
# roda o teste de carga (scripts/carga.js) num pod k6 dentro do cluster, contra o front,
# e acompanha o autoescalonamento do engajamento enquanto ele roda. o k6 fica dentro do
# cluster para a carga nao depender da rede da maquina nem do mapeamento de portas do kind.
set -euo pipefail
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

kubectl -n blog delete pod k6 --ignore-not-found >/dev/null
kubectl -n blog run k6 --image=grafana/k6:0.55.0 --restart=Never -i --quiet \
    --labels=app.kubernetes.io/part-of=blog-carga -- run --quiet - < "$RAIZ/scripts/carga.js" &
K6=$!

# o que o hpa esta vendo e decidindo, a cada 15s, enquanto a carga roda
while kill -0 "$K6" 2>/dev/null; do
    printf '%s  cpu media %4s%% do request   replicas desejadas %s   prontas %s\n' \
        "$(date +%H:%M:%S)" \
        "$(kubectl -n blog get hpa engagement-service -o jsonpath='{.status.currentMetrics[0].resource.current.averageUtilization}' 2>/dev/null)" \
        "$(kubectl -n blog get hpa engagement-service -o jsonpath='{.status.desiredReplicas}')" \
        "$(kubectl -n blog get deploy engagement-service -o jsonpath='{.status.readyReplicas}')"
    sleep 15
done
wait "$K6"
kubectl -n blog delete pod k6 --ignore-not-found >/dev/null
