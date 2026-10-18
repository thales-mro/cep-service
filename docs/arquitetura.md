# Arquitetura do CEP Service

Os diagramas estão em Mermaid; o GitHub os mostra como imagem.

## 1. Visão geral (contexto e containers)

```mermaid
flowchart LR
    client([Cliente / Swagger UI])

    subgraph docker[Docker Compose]
        app[CEP Service<br/>Java 21 · Spring Boot]
        pg[(PostgreSQL<br/>endereços prioritários<br/>log de consultas)]
        kafka{{Kafka<br/>tópico cep-lookups}}
        prom[Prometheus]
        graf[Grafana]
        kui[Kafka UI]
    end

    viacep[ViaCEP]
    brasil[BrasilAPI]

    client -- "HTTP / JSON" --> app
    app -- "1. busca prioritária" --> pg
    app -- "2. busca externa" --> viacep
    app -. "contingência" .-> brasil
    app -- "3. evento assíncrono" --> kafka
    kafka -- "4. consumidor grava o log" --> app
    app -- "INSERT log" --> pg
    prom -- "coleta /actuator/prometheus" --> app
    graf --> prom
    kui --> kafka
```

## 2. Camadas (portas e adaptadores)

O caso de uso não conhece Spring Data, Kafka nem HTTP: depende só de interfaces (portas).
Cada tecnologia fica num adaptador que implementa uma porta.

```mermaid
flowchart TB
    subgraph api[api · entrada]
        ctrl[Controllers REST]
        dto[DTOs com validação]
        err[GlobalExceptionHandler]
    end

    subgraph application[application · casos de uso]
        lookup[CepLookupService]
        priority[PriorityAddressService]
        logsvc[LookupLogService]
        ports[[Portas:<br/>CepProvider · PriorityAddressRepository<br/>LookupEventPublisher · LookupLogRepository<br/>LookupMetrics]]
    end

    subgraph domain[domain · regras]
        model[Cep · Address · BrazilianState]
        event[CepLookupEvent]
        exc[Exceções de negócio]
    end

    subgraph infra[infrastructure · saída]
        jpa[Adaptadores JPA]
        prov[ViaCepProvider · BrasilApiProvider<br/>FallbackCepProvider]
        kpub[KafkaLookupEventPublisher]
        kcons[LookupLogConsumer]
        metrics[MicrometerLookupMetrics]
    end

    ctrl --> lookup
    ctrl --> priority
    ctrl --> logsvc
    lookup --> ports
    priority --> ports
    logsvc --> ports
    application --> domain
    jpa -. implementa .-> ports
    prov -. implementa .-> ports
    kpub -. implementa .-> ports
    metrics -. implementa .-> ports
    kcons --> logsvc
```

## 3. Fluxo da busca de CEP

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant API as CepController
    participant S as CepLookupService
    participant DB as PostgreSQL
    participant P as FallbackCepProvider
    participant V as ViaCEP
    participant B as BrasilAPI
    participant K as Kafka

    C->>API: GET /api/v1/ceps/01001-000
    API->>S: lookup("01001-000")
    S->>S: Cep.of() valida e normaliza (8 dígitos)
    S->>DB: busca na base prioritária (consulta parametrizada)
    alt encontrado na base prioritária
        DB-->>S: endereço (PRIORITY_BASE)
    else não encontrado
        S->>P: lookup(cep)
        P->>V: GET /ws/01001000/json/
        alt ViaCEP responde
            V-->>P: endereço
        else ViaCEP fora do ar ou timeout
            P->>B: GET /api/cep/v1/01001000
            B-->>P: endereço
        end
        P-->>S: endereço (VIACEP ou BRASILAPI)
    end
    S-)K: publica CepLookupEvent (sem esperar confirmação)
    S-->>API: endereço
    API-->>C: 200 OK
```

## 4. Fluxo do log assíncrono

```mermaid
sequenceDiagram
    autonumber
    participant K as Kafka (cep-lookups)
    participant L as LookupLogConsumer
    participant S as LookupLogService
    participant DB as PostgreSQL
    participant D as Kafka (cep-lookups-dlt)

    K->>L: CepLookupEvent
    L->>S: record(event)
    S->>DB: já existe event_id?
    alt evento repetido (reentrega)
        S-->>L: ignora (idempotência)
    else evento novo
        S->>DB: INSERT cep_lookup_log
    end
    Note over L,DB: Se o INSERT falhar: 3 novas tentativas, a cada 1s
    L--)D: após 3 falhas, a mensagem vai para a DLT para reprocessamento
```

## 5. Modelo de dados

```mermaid
erDiagram
    PRIORITY_ADDRESS {
        bigint id PK
        varchar cep UK "8 dígitos (CHECK)"
        varchar street
        varchar complement
        varchar neighborhood
        varchar city
        varchar state "UF"
        timestamptz created_at
    }
    CEP_LOOKUP_LOG {
        bigint id PK
        uuid event_id UK "idempotência"
        varchar cep
        timestamptz queried_at "horário da consulta"
        varchar source "PRIORITY_BASE, VIACEP, BRASILAPI, NOT_FOUND"
        boolean found
        text response_json "dados retornados"
        timestamptz recorded_at
    }
```

## 6. Decisões e trade-offs

| Decisão | Motivo | Trade-off |
|---|---|---|
| Log via Kafka, fora da requisição | A gravação do log não soma latência à resposta | O log é eventualmente consistente (aparece alguns milissegundos depois) |
| `max.block.ms = 2s` no produtor | Se o Kafka cair, a requisição não fica presa os 60s padrão | Com o Kafka fora, o evento se perde; a falha é contada em métrica |
| Consumidor idempotente (`event_id` único) | O Kafka entrega "pelo menos uma vez"; reentregas não duplicam o log | Uma consulta a mais no banco por evento |
| Retentativas + DLT | Falha temporária do banco não perde eventos nem trava a fila | Exige monitorar a DLT |
| Dois provedores com fallback | ViaCEP fora do ar não derruba o serviço | Formatos diferentes de resposta a mapear |
| Validação por allowlist + consultas parametrizadas | Defesa em camadas contra SQL Injection | Regras de caracteres precisam acompanhar casos reais (ex.: apóstrofo) |

**Evolução natural:** para garantir que nenhum log se perca mesmo com o Kafka fora do ar, o próximo passo seria o padrão *transactional outbox* (gravar o evento numa tabela local e publicar a partir dela).
