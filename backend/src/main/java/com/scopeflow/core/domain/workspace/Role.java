package com.scopeflow.core.domain.workspace;

/**
 * Role enum: workspace member roles.
 */
public enum Role {
    OWNER("Owner — full access, can manage members, billing, delete workspace"),
    ADMIN("Admin — can manage proposals, approvals, members (except owner)"),
    MEMBER("Member — read-only access to proposals and briefings");

    private final String description;

    Role(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
