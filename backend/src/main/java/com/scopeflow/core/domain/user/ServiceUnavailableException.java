package com.scopeflow.core.domain.user;

/**
 * Domain exception: downstream service is unavailable (circuit breaker open or network failure).
 * Error code: USER-012
 *
 * Thrown by outbound adapters (e.g., UserServiceRestAdapter) when the remote service
 * cannot be reached or is rejecting requests via circuit breaker.
 */
public class ServiceUnavailableException extends RuntimeException {

    private static final String ERROR_CODE = "USER-012";

    public ServiceUnavailableException(String service, Throwable cause) {
        super("Service unavailable: " + service, cause);
    }

    public ServiceUnavailableException(String service) {
        super("Service unavailable: " + service);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
