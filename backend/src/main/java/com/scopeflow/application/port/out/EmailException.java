package com.scopeflow.application.port.out;

/**
 * Checked exception for email service failures.
 * Thrown by EmailService implementations when SES or SMTP delivery fails.
 * Triggers RabbitMQ retry when re-thrown from a listener.
 */
public class EmailException extends Exception {

    public EmailException(String message) {
        super(message);
    }

    public EmailException(String message, Throwable cause) {
        super(message, cause);
    }
}
