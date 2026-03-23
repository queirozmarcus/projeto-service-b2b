package com.scopeflow.core.domain.briefing;

/**
 * ServiceType enum: represents types of services offered.
 */
public enum ServiceType {
    SOCIAL_MEDIA("Social Media Management"),
    LANDING_PAGE("Landing Page Design"),
    WEB_DESIGN("Web Design"),
    BRANDING("Branding"),
    VIDEO_PRODUCTION("Video Production"),
    CONSULTING("Consulting");

    private final String description;

    ServiceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
