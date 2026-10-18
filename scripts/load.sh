#!/usr/bin/env bash
# Gera tráfego para os gráficos do Grafana ganharem movimento.
# Uso: ./scripts/load.sh [segundos]   (padrão: 60)
set -uo pipefail

API="${API:-http://localhost:8080/api/v1}"
DURATION="${1:-60}"
CEPS=(01001000 20040002 30130010 40020000 70040010 80010000 90010000 18035400 99999999 abc)

end=$((SECONDS + DURATION))
count=0
while [ "$SECONDS" -lt "$end" ]; do
  cep="${CEPS[RANDOM % ${#CEPS[@]}]}"
  curl -s -o /dev/null "$API/ceps/$cep" &
  count=$((count + 1))
  sleep 0.2
done
wait
echo "$count requisições enviadas em ${DURATION}s."
