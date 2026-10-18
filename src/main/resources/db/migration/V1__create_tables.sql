-- Base de endereços prioritários: consultada antes dos provedores externos.
CREATE TABLE priority_address (
    id           BIGSERIAL    PRIMARY KEY,
    cep          VARCHAR(8)   NOT NULL UNIQUE,
    street       VARCHAR(120) NOT NULL,
    complement   VARCHAR(60),
    neighborhood VARCHAR(80)  NOT NULL,
    city         VARCHAR(80)  NOT NULL,
    state        VARCHAR(2)   NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_priority_address_cep CHECK (cep ~ '^[0-9]{8}$')
);

CREATE INDEX idx_priority_address_city ON priority_address (lower(city));

-- Log das consultas, gravado de forma assíncrona pelo consumidor Kafka.
-- event_id único garante idempotência (cada consulta gravada uma única vez).
CREATE TABLE cep_lookup_log (
    id            BIGSERIAL   PRIMARY KEY,
    event_id      UUID        NOT NULL UNIQUE,
    cep           VARCHAR(8)  NOT NULL,
    queried_at    TIMESTAMPTZ NOT NULL,
    source        VARCHAR(20) NOT NULL,
    found         BOOLEAN     NOT NULL,
    response_json TEXT,
    recorded_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_cep_lookup_log_cep_queried ON cep_lookup_log (cep, queried_at DESC);
CREATE INDEX idx_cep_lookup_log_queried ON cep_lookup_log (queried_at DESC);
