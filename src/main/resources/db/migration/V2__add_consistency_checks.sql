-- Restrições de consistência: o banco passa a recusar dados que a aplicação já não gera.

-- found, source e response_json dizem a mesma coisa (CEP encontrado ou não).
-- A coluna found fica por conveniência de consulta, mas não pode contradizer as outras duas.
ALTER TABLE cep_lookup_log ADD CONSTRAINT ck_cep_lookup_log_found
    CHECK (found = (source <> 'NOT_FOUND') AND found = (response_json IS NOT NULL));

-- UF por lista fechada, a mesma do enum BrazilianState (um teste garante que não divergem).
ALTER TABLE priority_address ADD CONSTRAINT ck_priority_address_state
    CHECK (state IN ('AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG', 'PA',
                     'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'));
