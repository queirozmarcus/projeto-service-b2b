package com.scopeflow.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Internal token used to authorize monolith-to-user-service calls.
 *
 * Requests that include the header X-Internal-Token with this value
 * are treated as trusted internal calls and bypass user-ownership checks.
 *
 * In production, set via INTERNAL_TOKEN env var.
 */
@Component
@ConfigurationProperties(prefix = "app")
public class InternalTokenProperties {

    private String internalToken;

    public String getInternalToken() {
        return internalToken;
    }

    public void setInternalToken(String internalToken) {
        this.internalToken = internalToken;
    }
}
