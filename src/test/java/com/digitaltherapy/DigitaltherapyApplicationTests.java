package com.digitaltherapy;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.sql.init.mode=never",
    "app.cli.enabled=false",
    "ai.anthropic.api-key=test-key",
    "ai.anthropic.model=claude-3-haiku-20240307",
    "ai.anthropic.enabled=false"
})
@Disabled("Full context load requires compatible springdoc version for Spring Boot 4.x")
class DigitaltherapyApplicationTests {

    @Test
    void contextLoads() {
    }

}
