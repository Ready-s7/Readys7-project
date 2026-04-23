package com.example.readys7project;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.main.lazy-initialization=true")
@ActiveProfiles("smoke")
class Readys7ProjectApplicationTests {

    @Test
    void contextLoads() {
    }

}
