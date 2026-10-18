# CEP Service: contexto para sessões de desenvolvimento

Desafio técnico de um processo seletivo para uma vaga de engenharia de software
(integrações de cartão de crédito). O código será apresentado ao vivo e explicado em
entrevista, então clareza e simplicidade valem mais que sofisticação.

## Estado atual

O código foi escrito num ambiente sem acesso ao repositório Maven, então **ainda não foi
compilado nem executado**. A primeira tarefa de qualquer sessão é:

1. `mvn -B verify` e corrigir o que falhar (compilação e testes);
2. `docker compose up --build` e corrigir o que impedir a subida;
3. rodar `./scripts/demo.sh` e conferir cada passo (busca, cadastro, prioridade, log, erros);
4. abrir o Grafana (http://localhost:3000) e confirmar que os painéis mostram dados
   depois de `./scripts/load.sh 60`.

Ao corrigir, mude o mínimo necessário e mantenha o desenho descrito abaixo.

## Requisitos do desafio (não remover nenhum)

- Desenhos de arquitetura da solução (`docs/arquitetura.md`, em Mermaid).
- Busca de CEP em API externa.
- Cadastro de endereços que têm prioridade na busca, se existirem na base.
- Dados higienizados com boas práticas contra SQL Injection.
- Log de cada consulta (horário e dados retornados pela API) gravado no banco a partir de
  um **evento assíncrono**, para não comprometer a performance.
- Conceitos básicos de SOLID.
- Tratamento de exceções sem expor a aplicação por exceções técnicas.
- Repositório público no Git.
- Monitoramento e gráficos; publicação em cloud pública é desejável; uso de IA declarado.
- Stack exigida: Java 21+, banco relacional ou não relacional, Kafka (ou similar), Docker.

## Arquitetura

Portas e adaptadores, em quatro pacotes sob `com.thalesoliveira.cep`:

- `domain`: `Cep` (value object, só 8 dígitos), `Address`, `BrazilianState`,
  `CepLookupEvent` e as exceções de negócio. Sem dependência de framework.
- `application`: casos de uso (`CepLookupService`, `PriorityAddressService`,
  `LookupLogService`) e as portas (interfaces) em `application/port`.
- `infrastructure`: adaptadores. JPA (`persistence`), ViaCEP/BrasilAPI com fallback
  (`provider`), Kafka (`messaging`), Micrometer (`metrics`), `Clock` (`config`).
- `api`: controllers REST, DTOs com Bean Validation e `GlobalExceptionHandler` (RFC 7807).

Regras que o desenho segue:

- Casos de uso dependem só de portas; nenhum caso de uso importa JPA, Kafka ou RestClient.
- Novo provedor de CEP = nova implementação de `CepProvider` incluída no
  `FallbackCepProvider` (em `ProviderConfig`), sem mudar o caso de uso.
- Todo acesso a dados é parametrizado (Spring Data ou JPQL com parâmetros nomeados).
  Nunca montar SQL ou JPQL por concatenação.
- O log nunca derruba a busca: falha de publicação no Kafka é registrada e vira métrica.
- O consumidor é idempotente pelo `event_id` (único no banco); falhas vão para
  `cep-lookups-dlt` após 3 tentativas.
- Respostas de erro nunca incluem stack trace, nome de classe, SQL ou o valor recebido.
  Erro inesperado devolve só um código de referência, que aparece no log do servidor.
- Horário sempre via `Clock` injetado, para os testes serem determinísticos.

## Comandos

```bash
mvn test                       # testes unitários e de API (sem Docker)
docker compose up --build      # app + Postgres + Kafka + Kafka UI + Prometheus + Grafana
./scripts/demo.sh              # roteiro da demonstração, passo a passo
./scripts/load.sh 60           # gera tráfego por 60s para os gráficos
docker compose down -v         # derruba tudo e apaga os dados
```

Portas: API 8080 (Swagger em `/swagger-ui.html`), Kafka UI 8081, Grafana 3000,
Prometheus 9090, Postgres 5432, Kafka (acesso externo) 29092.

## Convenções

- Java 21 com records; injeção por construtor; sem Lombok.
- Mensagens para o usuário e documentação em português; nomes no código em inglês.
- Schema só por migrações Flyway (`src/main/resources/db/migration`); `ddl-auto: none`.
- Testes: JUnit 5, Mockito e AssertJ. Toda mudança de regra vem com teste.
- Nenhuma senha real no repositório: valores locais vêm de `.env` (fora do Git), com
  exemplo em `.env.example`.
- Commits pequenos, com mensagem em português dizendo o que mudou e por quê.

## O que não fazer sem pedido explícito

Adicionar módulos Maven, microserviços, cache distribuído, Kubernetes, autenticação ou
novas dependências pesadas. O escopo é o do desafio; melhorias futuras estão listadas na
seção "Próximos passos" do README e servem de assunto para a entrevista.

## Próximos passos possíveis (se sobrar tempo)

1. Testes de integração com Testcontainers (Postgres e Kafka reais).
2. Publicação em cloud pública (ex.: Render, Railway ou AWS) com o link no README.
3. Padrão transactional outbox para não perder eventos com o Kafka fora do ar.
