package com.scopeflow.core.domain.user;

/**
 * Marker class representing ACTIVE user status.
 * Used by WorkspaceControllerV2 for status mapping from user-service responses.
 */
public final class UserActive {
    private UserActive() {
        throw new UnsupportedOperationException("Marker class — do not instantiate");
    }

    public static final String STATUS = "ACTIVE";
}
