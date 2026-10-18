#!/usr/bin/env bash
# Roteiro da demonstração: executa as operações principais em sequência.
# Uso: ./scripts/demo.sh   (com a aplicação no ar: docker compose up --build)
set -euo pipefail

API="${API:-http://localhost:8080/api/v1}"

step() {
  echo
  echo "=== $1"
  read -r -p "(Enter para executar)" _
}

run() {
  echo "\$ $*"
  "$@" -s -w "\nHTTP %{http_code}\n"
}

step "1. Busca de CEP na API externa (ViaCEP)"
run curl "$API/ceps/01001-000"

step "2. Cadastro de endereço prioritário para o mesmo CEP"
run curl -X POST "$API/addresses" -H "Content-Type: application/json" \
  -d '{"cep":"01001-000","street":"Praça da Sé","complement":"Sede","neighborhood":"Sé","city":"São Paulo","state":"SP"}'

step "3. A mesma busca agora vem da base prioritária"
run curl "$API/ceps/01001-000"

step "4. Log das consultas, gravado de forma assíncrona via Kafka"
run curl "$API/lookups?cep=01001000"

step "5. CEP inexistente (404) e CEP inválido (400)"
run curl "$API/ceps/99999-999"
run curl "$API/ceps/abc"

step "6. Tentativa de SQL Injection (400, sem chegar ao banco)"
run curl -X POST "$API/addresses" -H "Content-Type: application/json" \
  -d "{\"cep\":\"01001000'; DROP TABLE priority_address;--\",\"street\":\"Rua A\",\"neighborhood\":\"Centro\",\"city\":\"São Paulo\",\"state\":\"SP\"}"

step "7. Apóstrofo aceito e gravado como texto"
run curl -X POST "$API/addresses" -H "Content-Type: application/json" \
  -d "{\"cep\":\"18035-400\",\"street\":\"Rua D'Ávila\",\"neighborhood\":\"Centro\",\"city\":\"Sorocaba\",\"state\":\"SP\"}"

step "8. Limpeza: remove os endereços cadastrados"
run curl -X DELETE "$API/addresses/01001-000"
run curl -X DELETE "$API/addresses/18035-400"

echo
echo "Fim. Veja os gráficos em http://localhost:3000 e as mensagens em http://localhost:8081"
