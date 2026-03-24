package com.scopeflow.adapter.out.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for JpaUser.
 */
public interface JpaUserSpringRepository extends JpaRepository<JpaUser, UUID> {

    Optional<JpaUser> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM JpaUser u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<JpaUser> findByEmailIgnoreCase(@Param("email") String email);
}
