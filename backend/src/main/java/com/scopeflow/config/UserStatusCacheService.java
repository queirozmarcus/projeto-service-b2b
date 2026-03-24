package com.scopeflow.config;

import com.scopeflow.core.domain.user.User;
import com.scopeflow.core.domain.user.UserId;
import com.scopeflow.core.domain.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Cache service for user status lookups in the JWT authentication filter.
 *
 * Caches user status (ACTIVE/INACTIVE/DELETED) with a TTL configured via
 * application properties (default 5 minutes via Caffeine spec).
 *
 * This prevents a DB query on every authenticated request (N+1 pattern).
 * Cache is invalidated automatically on TTL expiry.
 *
 * Cache name: "userStatus" — configured in application.properties with Caffeine spec.
 */
@Service
public class UserStatusCacheService {

    private static final Logger log = LoggerFactory.getLogger(UserStatusCacheService.class);

    private final UserRepository userRepository;

    public UserStatusCacheService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Fetch user status from cache or DB.
     *
     * Returns the user's status string ("ACTIVE", "INACTIVE", "DELETED"),
     * or null if user does not exist.
     *
     * @param userId user UUID
     * @return status string or null
     */
    @Cacheable(value = "userStatus", key = "#userId", unless = "#result == null")
    public String getUserStatus(UUID userId) {
        log.debug("Cache miss for userId={} — fetching from DB", userId);
        return userRepository.findById(new UserId(userId))
                .map(User::status)
                .orElse(null);
    }
}
