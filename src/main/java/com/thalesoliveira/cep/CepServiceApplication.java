package com.thalesoliveira.cep;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CepServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CepServiceApplication.class, args);
    }
}
