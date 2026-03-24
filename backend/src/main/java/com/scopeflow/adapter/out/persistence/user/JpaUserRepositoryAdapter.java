package com.scopeflow.adapter.out.persistence.user;

import com.scopeflow.core.domain.user.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * JPA adapter implementing UserRepository domain port.
 *
 * Converts between domain User (sealed class) and JpaUser entity.
 */
@Component
public class JpaUserRepositoryAdapter implements UserRepository {

    private final JpaUserSpringRepository springRepo;

    public JpaUserRepositoryAdapter(JpaUserSpringRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public void save(User user) {
        JpaUser entity = fromDomain(user);
        // For update: merge if exists
        springRepo.findById(user.getId().value()).ifPresentOrElse(
                existing -> {
                    existing.setPasswordHash(user.getPasswordHash().value());
                    existing.setFullName(user.getFullName());
                    existing.setPhone(user.getPhone());
                    existing.setStatus(user.status());
                    existing.setUpdatedAt(Instant.now());
                    springRepo.save(existing);
                },
                () -> springRepo.save(entity)
        );
    }

    @Override
    public Optional<User> findById(UserId id) {
        return springRepo.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return springRepo.findByEmailIgnoreCase(email.normalized()).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return springRepo.existsByEmail(email.normalized());
    }

    @Override
    public void delete(UserId id) {
        springRepo.findById(id.value()).ifPresent(entity -> {
            entity.setStatus("DELETED");
            entity.setUpdatedAt(Instant.now());
            springRepo.save(entity);
        });
    }

    // ============ JPA → Domain ============

    private User toDomain(JpaUser entity) {
        UserId id = new UserId(entity.getId());
        Email email = new Email(entity.getEmail());
        PasswordHash hash = new PasswordHash(entity.getPasswordHash());
        return switch (entity.getStatus()) {
            case "ACTIVE" -> new UserActive(
                    id, email, hash, entity.getFullName(), entity.getPhone(),
                    entity.getCreatedAt(), entity.getUpdatedAt()
            );
            case "INACTIVE" -> new UserInactive(
                    id, email, hash, entity.getFullName(), entity.getPhone(),
                    entity.getCreatedAt(), entity.getUpdatedAt()
            );
            case "DELETED" -> new UserDeleted(
                    id, email, hash, entity.getFullName(), entity.getPhone(),
                    entity.getCreatedAt(), entity.getUpdatedAt()
            );
            default -> throw new IllegalStateException("Unknown user status: " + entity.getStatus());
        };
    }

    // ============ Domain → JPA ============

    private JpaUser fromDomain(User user) {
        return new JpaUser(
                user.getId().value(),
                user.getEmail().normalized(),
                user.getPasswordHash().value(),
                user.getFullName(),
                user.getPhone(),
                user.status(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
