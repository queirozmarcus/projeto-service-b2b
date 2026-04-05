package com.scopeflow.user.domain.model;

/**
 * Role enum: workspace member roles.
 *
 * Local copy from workspace bounded context.
 * Used only for invited user validation (cannot invite with OWNER role).
 */
public enum Role {
    OWNER("Owner"),
    ADMIN("Admin"),
    MEMBER("Member");

    private final String description;

    Role(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
