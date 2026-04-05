package com.scopeflow.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * User Service -- Authentication and Authorization microservice.
 *
 * First extraction from the ScopeFlow monolith via Strangler Fig pattern.
 *
 * Responsibilities:
 *   - JWT authentication (login, register, refresh token)
 *   - User management (lookup by email, create invited user)
 *
 * Stack:
 *   - Java 21 (virtual threads, sealed classes, records)
 *   - Spring Boot 3.2+
 *   - PostgreSQL 16 (shared DB with monolith)
 *   - Traefik routing (/api/v1/auth/*)
 */
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
