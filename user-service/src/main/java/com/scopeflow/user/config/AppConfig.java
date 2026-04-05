package com.scopeflow.user.config;

import com.scopeflow.user.application.service.UserService;
import com.scopeflow.user.domain.port.out.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application beans wiring.
 *
 * Connects domain services (pure Java) to Spring-managed ports.
 */
@Configuration
public class AppConfig {

    @Bean
    public UserService userService(UserRepository userRepository) {
        return new UserService(userRepository);
    }
}
