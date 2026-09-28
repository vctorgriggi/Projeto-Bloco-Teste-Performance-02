#!/usr/bin/env bash
#
# apaga o cluster kind do blog, com tudo o que estava nele (inclusive os volumes do
# postgres e do rabbitmq, que vivem dentro do no).
set -euo pipefail
kind delete cluster --name blog
