package com.nouhaila.docmind;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Needs PostgreSQL and RabbitMQ; will be replaced by a Testcontainers integration test")
class DocmindApplicationTests {

    @Test
    void contextLoads() {
    }
}