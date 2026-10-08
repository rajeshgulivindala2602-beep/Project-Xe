package com.xe.ratealerts.rates;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class XeConfig {

    @Bean
    public RestTemplate xeRestTemplate(
            RestTemplateBuilder builder,
            @Value("${xecd.account-id}") String accountId,
            @Value("${xecd.api-key}") String apiKey) {
        return builder
                .basicAuthentication(accountId, apiKey)
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(5))
                .build();
    }
}
