package com.scopeflow.user.application.usecase;

import com.scopeflow.user.domain.port.out.UserBlocklist;

import java.time.Duration;
import java.util.Objects;

/**
 * Use case: adds a userId to the blocklist for the given duration.
 *
 * Called after user deactivation/deletion to prevent the still-valid JWT from
 * being accepted until it naturally expires. The TTL should match the access
 * token expiration window (default: 15 minutes).
 */
public class BlockUserUseCase {

    private final UserBlocklist userBlocklist;

    public BlockUserUseCase(UserBlocklist userBlocklist) {
        this.userBlocklist = Objects.requireNonNull(userBlocklist, "UserBlocklist cannot be null");
    }

    /**
     * Blocks the given userId for the specified duration.
     *
     * @param userId the user's UUID as string
     * @param ttl    duration to keep the block (typically equals access token TTL)
     */
    public void execute(String userId, Duration ttl) {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(ttl, "ttl cannot be null");
        userBlocklist.block(userId, ttl);
    }
}
