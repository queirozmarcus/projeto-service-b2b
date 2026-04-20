package com.scopeflow.user.config;

import com.scopeflow.user.adapter.out.security.BcryptPasswordHasherAdapter;
import com.scopeflow.user.adapter.out.security.JwtTokenIssuerAdapter;
import com.scopeflow.user.application.usecase.AuthenticateUserUseCase;
import com.scopeflow.user.application.usecase.InviteUserUseCase;
import com.scopeflow.user.application.usecase.RefreshTokenUseCase;
import com.scopeflow.user.application.usecase.RegisterUserUseCase;
import com.scopeflow.user.domain.port.out.PasswordHasher;
import com.scopeflow.user.domain.port.out.TokenIssuer;
import com.scopeflow.user.domain.port.out.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Wires use cases and security adapters as Spring beans.
 *
 * Use cases are pure Java — no Spring annotations inside them.
 * Spring is only at the boundary (this config class).
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public PasswordHasher passwordHasher(PasswordEncoder passwordEncoder) {
        return new BcryptPasswordHasherAdapter(passwordEncoder);
    }

    @Bean
    public TokenIssuer tokenIssuer(JwtService jwtService) {
        return new JwtTokenIssuerAdapter(jwtService);
    }

    @Bean
    public RegisterUserUseCase registerUserUseCase(UserRepository userRepository,
                                                   PasswordHasher passwordHasher) {
        return new RegisterUserUseCase(userRepository, passwordHasher);
    }

    @Bean
    public AuthenticateUserUseCase authenticateUserUseCase(UserRepository userRepository,
                                                           PasswordHasher passwordHasher,
                                                           TokenIssuer tokenIssuer) {
        return new AuthenticateUserUseCase(userRepository, passwordHasher, tokenIssuer);
    }

    @Bean
    public RefreshTokenUseCase refreshTokenUseCase(TokenIssuer tokenIssuer,
                                                   UserRepository userRepository) {
        return new RefreshTokenUseCase(tokenIssuer, userRepository);
    }

    @Bean
    public InviteUserUseCase inviteUserUseCase(UserRepository userRepository,
                                               PasswordHasher passwordHasher) {
        return new InviteUserUseCase(userRepository, passwordHasher);
    }
}
