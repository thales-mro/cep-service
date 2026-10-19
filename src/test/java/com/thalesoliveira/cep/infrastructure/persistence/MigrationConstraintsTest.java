package com.thalesoliveira.cep.infrastructure.persistence;

import com.thalesoliveira.cep.domain.model.BrazilianState;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationConstraintsTest {

    private static final Pattern STATE_CHECK = Pattern.compile("CHECK \\(state IN \\(([^)]*)\\)\\)");

    @Test
    void stateCheckListsExactlyTheEnumStates() throws IOException {
        String sql = new ClassPathResource("db/migration/V2__add_consistency_checks.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        Matcher matcher = STATE_CHECK.matcher(sql);
        assertThat(matcher.find()).as("restrição de UF na migração").isTrue();

        String[] statesInSql = Arrays.stream(matcher.group(1).split(","))
                .map(state -> state.strip().replace("'", ""))
                .toArray(String[]::new);
        String[] statesInEnum = Arrays.stream(BrazilianState.values())
                .map(Enum::name)
                .toArray(String[]::new);

        assertThat(statesInSql).containsExactlyInAnyOrder(statesInEnum);
    }
}
