package com.scopeflow.user.config;

import com.scopeflow.user.adapter.out.cache.RedisUserBlocklistAdapter;
import com.scopeflow.user.application.service.UserService;
import com.scopeflow.user.application.usecase.BlockUserUseCase;
import com.scopeflow.user.domain.port.out.UserBlocklist;
import com.scopeflow.user.domain.port.out.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Application beans wiring.
 *
 * Connects domain services (pure Java) to Spring-managed ports.
 */
@Configuration
public class AppConfig {

    @Bean
    public UserBlocklist userBlocklist(StringRedisTemplate redisTemplate) {
        return new RedisUserBlocklistAdapter(redisTemplate);
    }

    @Bean
    public BlockUserUseCase blockUserUseCase(UserBlocklist userBlocklist) {
        return new BlockUserUseCase(userBlocklist);
    }

    @Bean
    public UserService userService(UserRepository userRepository,
                                   BlockUserUseCase blockUserUseCase,
                                   @Value("${jwt.expiration:900000}") long jwtExpirationMs) {
        return new UserService(userRepository, blockUserUseCase, Duration.ofMillis(jwtExpirationMs));
    }
}
