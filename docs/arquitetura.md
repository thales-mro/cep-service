# Arquitetura do CEP Service

Os diagramas estão em Mermaid; o GitHub os mostra como imagem.

As cores indicam o papel de cada peça, em todos os diagramas:
🟦 nosso serviço · 🟩 dados (PostgreSQL) · 🟧 mensageria (Kafka) · 🟪 APIs externas ·
🟥 monitoramento · ⬛ cliente. Os tópicos do Kafka aparecem como cilindros deitados (o
símbolo de fila/stream) e o caminho assíncrono é a linha laranja grossa.

## 1. Visão geral (contexto e containers)

```mermaid
flowchart LR
    client([Cliente / Swagger UI])

    subgraph docker[Docker Compose]
        subgraph svc[CEP Service · Java 21 · Spring Boot]
            api[API REST<br/>busca e cadastro]
            consumer[Consumidor<br/>do log]
        end
        pg[(PostgreSQL<br/>endereços prioritários<br/>log de consultas)]
        topic@{ shape: h-cyl, label: "Kafka<br/>cep-lookups" }
        dlt@{ shape: h-cyl, label: "Kafka<br/>cep-lookups-dlt" }
        prom[Prometheus]
        graf[Grafana]
        kui[Kafka UI]
    end

    viacep[ViaCEP]
    brasil[BrasilAPI]

    client -- "HTTP / JSON" --> api
    api -- "① busca prioritária" --> pg
    api -- "② busca externa" --> viacep
    api -. "contingência" .-> brasil
    api == "③ evento assíncrono" ==> topic
    topic == "④ consome" ==> consumer
    consumer -- "grava o log" --> pg
    consumer -. "após 3 falhas" .-> dlt
    prom -- "coleta /actuator/prometheus" --> svc
    graf --> prom
    kui --> topic

    classDef client fill:#475569,stroke:#1e293b,color:#fff
    classDef app fill:#2563eb,stroke:#1e3a8a,color:#fff
    classDef data fill:#0f766e,stroke:#134e4a,color:#fff
    classDef messaging fill:#ea580c,stroke:#7c2d12,color:#fff
    classDef external fill:#7c3aed,stroke:#4c1d95,color:#fff
    classDef observability fill:#be185d,stroke:#831843,color:#fff
    class client client
    class api,consumer app
    class pg data
    class topic,dlt,kui messaging
    class viacep,brasil external
    class prom,graf observability
    style docker fill:none,stroke:#94a3b8,stroke-dasharray:6 4
    style svc fill:#2563eb1a,stroke:#2563eb
    linkStyle 4,5 stroke:#ea580c,stroke-width:3px
    linkStyle 7 stroke:#ea580c
```

## 2. Camadas (portas e adaptadores)

O caso de uso não conhece Spring Data, Kafka nem HTTP: depende só de interfaces (portas).
Cada tecnologia fica num adaptador que implementa uma porta.

```mermaid
flowchart LR
    subgraph api[api · entrada]
        direction TB
        ctrl[Controllers REST]
        dto[DTOs com validação]
        err[GlobalExceptionHandler]
    end

    subgraph core[núcleo da aplicação]
        direction TB
        subgraph application[application · casos de uso]
            direction TB
            lookup[CepLookupService]
            priority[PriorityAddressService]
            logsvc[LookupLogService]
            ports[[Portas:<br/>CepProvider · PriorityAddressRepository<br/>LookupEventPublisher · LookupLogRepository<br/>LookupMetrics]]
        end
        subgraph domain[domain · regras]
            direction LR
            model[Cep · Address · BrazilianState]
            event[CepLookupEvent]
            exc[Exceções de negócio]
        end
    end

    subgraph infra[infrastructure · saída]
        direction TB
        jpa[Adaptadores JPA]
        prov[ViaCepProvider · BrasilApiProvider<br/>FallbackCepProvider]
        kpub[KafkaLookupEventPublisher]
        metrics[MicrometerLookupMetrics]
        kcons[LookupLogConsumer]
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

    classDef entry fill:#2563eb,stroke:#1e3a8a,color:#fff
    classDef usecase fill:#0f766e,stroke:#134e4a,color:#fff
    classDef port fill:#fef3c7,stroke:#b45309,color:#78350f,stroke-width:2px
    classDef model fill:#7c3aed,stroke:#4c1d95,color:#fff
    classDef adapter fill:#ea580c,stroke:#7c2d12,color:#fff
    class ctrl,dto,err entry
    class lookup,priority,logsvc usecase
    class ports port
    class model,event,exc model
    class jpa,prov,kpub,kcons,metrics adapter
    style core fill:none,stroke:#94a3b8,stroke-dasharray:6 4
    style api fill:#2563eb1a,stroke:#2563eb
    style application fill:#0f766e1a,stroke:#0f766e
    style domain fill:#7c3aed1a,stroke:#7c3aed
    style infra fill:#ea580c1a,stroke:#ea580c
```

Neste diagrama as cores seguem as camadas: 🟦 entrada · 🟩 casos de uso · 🟨 portas ·
🟪 domínio · 🟧 adaptadores. Repare que todas as setas apontam para o núcleo:
entrada e saída dependem dele, e ele não depende de nenhuma tecnologia.

## 3. Fluxo da busca de CEP

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    box rgba(37, 99, 235, 0.15) CEP Service
        participant API as CepController
        participant S as CepLookupService
        participant P as FallbackCepProvider
    end
    box rgba(15, 118, 110, 0.18) Dados
        participant DB as PostgreSQL
    end
    box rgba(124, 58, 237, 0.15) APIs externas
        participant V as ViaCEP
        participant B as BrasilAPI
    end
    box rgba(234, 88, 12, 0.18) Mensageria
        participant K as Kafka
    end

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
    rect rgba(234, 88, 12, 0.12)
        S-)K: publica CepLookupEvent (sem esperar confirmação)
    end
    S-->>API: endereço
    API-->>C: 200 OK
```

## 4. Fluxo do log assíncrono

```mermaid
sequenceDiagram
    autonumber
    box rgba(234, 88, 12, 0.18) Mensageria
        participant K as Kafka (cep-lookups)
    end
    box rgba(37, 99, 235, 0.15) CEP Service
        participant L as LookupLogConsumer
        participant S as LookupLogService
    end
    box rgba(15, 118, 110, 0.18) Dados
        participant DB as PostgreSQL
    end
    box rgba(234, 88, 12, 0.18) Mensageria
        participant D as Kafka (cep-lookups-dlt)
    end

    K->>L: CepLookupEvent
    L->>S: record(event)
    S->>DB: já existe event_id?
    alt evento repetido (reentrega)
        S-->>L: ignora (idempotência)
    else evento novo
        S->>DB: INSERT cep_lookup_log
    end
    rect rgba(220, 38, 38, 0.12)
        Note over L,DB: Se o INSERT falhar: 3 novas tentativas, a cada 1s
        L--)D: após 3 falhas, a mensagem vai para a DLT para reprocessamento
    end
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
        varchar state "UF (CHECK: lista fechada)"
        timestamptz created_at
    }
    CEP_LOOKUP_LOG {
        bigint id PK
        uuid event_id UK "idempotência"
        varchar cep
        timestamptz queried_at "horário da consulta"
        varchar source "PRIORITY_BASE, VIACEP, BRASILAPI, NOT_FOUND"
        boolean found "CHECK: coerente com source"
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
