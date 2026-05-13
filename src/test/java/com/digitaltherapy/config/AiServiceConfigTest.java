package com.digitaltherapy.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class AiServiceConfigTest {

    @Test
    @DisplayName("restTemplate returns a non-null RestTemplate instance")
    void restTemplate_ReturnsNonNull() {
        AiServiceConfig config = new AiServiceConfig();
        RestTemplate restTemplate = config.restTemplate();

        assertThat(restTemplate).isNotNull();
        assertThat(restTemplate).isInstanceOf(RestTemplate.class);
    }

    @Test
    @DisplayName("restTemplate creates a new instance each time")
    void restTemplate_CreatesNewInstance() {
        AiServiceConfig config = new AiServiceConfig();
        RestTemplate first = config.restTemplate();
        RestTemplate second = config.restTemplate();

        assertThat(first).isNotSameAs(second);
    }
}
