# ADR-006: Value Object Validation Exception Pattern

## Status
Accepted

## Date
2026-04-19

## Context

Value Objects (VOs) in the domain layer enforce business invariants through validation in their constructors. Previously, validation failures threw `IllegalArgumentException`, which was handled by the catch-all exception handler in `GlobalExceptionHandler`, resulting in HTTP 500 responses.

This behavior was problematic because:
- HTTP 500 indicates server errors, not client input errors
- Clients couldn't distinguish between validation failures and actual server failures
- Error responses were inconsistent with our RFC 9457 Problem Details standard
- Debugging required correlating error IDs with logs instead of clear error codes

The issue was discovered during Sprint 10 when testing Email VO validation in the user-service authentication flow.

## Decision

We introduce a domain-specific exception `InvalidValueObjectException` to be thrown by all Value Objects when validation fails. This exception:

1. **Lives in the domain layer** (`core.domain.common` package) — zero framework dependencies
2. **Carries semantic information**: error code `VO-001` and `vo_type` field (e.g., "Email", "PasswordHash")
3. **Maps to HTTP 400** via dedicated handler in `GlobalExceptionHandler`
4. **Follows RFC 9457** Problem Details format with consistent structure

### Implementation Pattern

```java
// Domain VO
public record Email(String value) {
    public Email {
        if (!isValid(value)) {
            throw new InvalidValueObjectException("Email", "Invalid email format: " + value);
        }
    }
}

// Exception
public class InvalidValueObjectException extends RuntimeException {
    private final String voType;
    // Constructor stores voType for error response
}

// Handler
@ExceptionHandler(InvalidValueObjectException.class)
ProblemDetail handleInvalidValueObject(InvalidValueObjectException ex) {
    var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    pd.setProperty("errorCode", "VO-001");
    pd.setProperty("vo_type", ex.getVoType());
    return pd;
}
```

This pattern is applied consistently across both services (backend monolith and user-service).

## Consequences

### Positive

- **Semantically correct HTTP status**: 400 Bad Request for validation failures
- **RFC 9457 compliant**: consistent error format across all endpoints
- **Better observability**: `vo_type` field enables filtering/alerting on specific VO failures
- **Reusable**: works for all domain VOs (Email, PasswordHash, PublicToken, AnswerText, etc.)
- **Clean separation**: domain exceptions stay in domain layer, HTTP mapping in adapter layer
- **Client-friendly**: clear distinction between client errors (400) and server errors (500)

### Negative

- **Additional exception class**: adds one more exception type to maintain in domain layer
- **Developer discipline required**: developers must remember to throw `InvalidValueObjectException` instead of `IllegalArgumentException` in VO constructors
- **Migration effort**: existing VOs need to be updated to use the new exception

### Neutral

- **Pattern replication**: this pattern should be applied to all future Value Objects (consistency benefit, but requires adherence)
- **Error code namespace**: `VO-001` is generic for all VOs; we chose simplicity over per-VO codes (e.g., `EMAIL-001`, `PASSWORD-001`)

## Alternatives Considered

### 1. Bean Validation (@Valid) annotations on DTOs
- **Rejected**: DTO validation doesn't cover direct VO constructor calls or domain logic
- VOs are often constructed from database values or internal flows where DTOs aren't involved
- Would require duplicate validation rules in DTOs and VOs

### 2. Keep IllegalArgumentException + improve catch-all handler
- **Rejected**: `IllegalArgumentException` is too generic; used by JDK and libraries
- Impossible to distinguish between VO validation failures and programming errors
- Would require parsing exception messages (brittle)

### 3. Per-VO exception classes (EmailInvalidException, PasswordInvalidException)
- **Rejected**: excessive boilerplate for 10+ Value Objects
- Single handler per VO exception vs. one unified handler
- `vo_type` field provides same traceability with less code

### 4. Return Result<T, Error> instead of throwing exceptions
- **Considered for future**: functional approach with Railway Oriented Programming
- **Deferred**: would require major refactoring of existing domain model
- Current exception-based approach is idiomatic in Java/Spring ecosystem

## References

- [RFC 9457 - Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457.html)
- Sprint 10 implementation: Email VO validation fix (backend commit e7a1c2d, user-service commit f3b4a8e)
- `InvalidValueObjectException.java` in `core.domain.common` package
- `GlobalExceptionHandler` HTTP 400 mapping in both services
- Related: ADR-003 (Error Handling Strategy) for RFC 9457 adoption decision
