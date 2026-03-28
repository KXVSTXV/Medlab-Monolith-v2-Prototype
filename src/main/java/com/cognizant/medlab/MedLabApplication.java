package com.cognizant.medlab;

 import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MedLab — Medical Laboratory System  v2.0
 * Spring Boot Monolith · Cognizant Team 5
 *
 * Upgrade path: v1 (CLI/JDBC) → v2 (this) → v3 (Microservices)
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableScheduling
public class MedLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedLabApplication.class, args);
    }
}
