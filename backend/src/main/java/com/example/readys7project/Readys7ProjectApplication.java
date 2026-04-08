package com.example.readys7project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class Readys7ProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(Readys7ProjectApplication.class, args);
    }

}
