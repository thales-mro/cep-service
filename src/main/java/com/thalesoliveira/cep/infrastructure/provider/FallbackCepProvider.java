package com.thalesoliveira.cep.infrastructure.provider;

import com.thalesoliveira.cep.application.port.CepProvider;
import com.thalesoliveira.cep.domain.exception.ExternalServiceException;
import com.thalesoliveira.cep.domain.model.Address;
import com.thalesoliveira.cep.domain.model.Cep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Composite de provedores: tenta cada um em ordem e devolve o primeiro que encontrar o CEP.
 * Se um provedor cai, o próximo assume (resiliência). Para adicionar um novo provedor,
 * basta implementar {@link CepProvider} e incluí-lo na lista, sem mudar o caso de uso (Open/Closed).
 */
public class FallbackCepProvider implements CepProvider {

    private static final Logger log = LoggerFactory.getLogger(FallbackCepProvider.class);

    private final List<CepProvider> providers;

    public FallbackCepProvider(List<CepProvider> providers) {
        if (providers.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos um provedor de CEP.");
        }
        this.providers = List.copyOf(providers);
    }

    @Override
    public Optional<Address> lookup(Cep cep) {
        ExternalServiceException lastFailure = null;
        boolean someProviderAnswered = false;

        for (CepProvider provider : providers) {
            try {
                Optional<Address> result = provider.lookup(cep);
                if (result.isPresent()) {
                    return result;
                }
                someProviderAnswered = true;
            } catch (ExternalServiceException e) {
                log.warn("Provedor {} indisponível; tentando o próximo. Causa: {}", provider.name(),
                        e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
                lastFailure = e;
            }
        }

        // Pelo menos um provedor respondeu "não existe": é um CEP inexistente, não uma falha.
        if (someProviderAnswered || lastFailure == null) {
            return Optional.empty();
        }
        throw lastFailure;
    }

    @Override
    public String name() {
        return "fallback";
    }
}
