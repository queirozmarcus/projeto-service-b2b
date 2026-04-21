package com.scopeflow.core.domain.user;

/**
 * Marker class representing INACTIVE user status.
 * Used by WorkspaceControllerV2 for status mapping from user-service responses.
 */
public final class UserInactive {
    private UserInactive() {
        throw new UnsupportedOperationException("Marker class — do not instantiate");
    }

    public static final String STATUS = "INACTIVE";
}
