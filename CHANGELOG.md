# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- Invalid email format now returns HTTP 400 (was 500) with RFC 9457 Problem Details
- Created `InvalidValueObjectException` for consistent VO validation across domain layer
- Email validation harmonized across backend monolith and user-service
- Bean Validation `@Email` removed from DTOs — validation now happens exclusively in domain layer
- 5 integration tests corrected (`InvalidEmailValidationIntegrationTest`)

### Added
- `InvalidValueObjectException` class for domain value object validation errors (error code `VO-001`)
- **53 tests total** for Email VO validation synchronization:
  - 21 unit tests (Email VO + Exception + Handler)
  - 22 integration tests (HTTP → Domain layer)
  - 3 contract tests (Spring Cloud Contract — backend ↔ user-service)
  - 7 E2E tests (RestAssured + Testcontainers)
- RFC 9457 Problem Details handler for VO validation failures
- Custom RFC 9457 properties: `error_code`, `error_id` (UUID), `timestamp` (ISO 8601), `vo_type`
- Complete QA documentation:
  - `docs/qa/EMAIL-VALIDATION-SYNC.md` — technical documentation
  - `docs/qa/sprint10-final-audit-report.md` — final audit report
  - `docs/qa/SPRINT-RETROSPECTIVE.md` — retrospective (Sprints 1-10)
  - 5 sprint reports (Sprints 5-9)

### Changed
- Email VO now throws `InvalidValueObjectException` instead of `IllegalArgumentException` for format errors
- GlobalExceptionHandler maps VO validation to HTTP 400 with error code `VO-001`
- `RegisterRequest` DTO: removed `@Email` annotation (validation moved to domain layer)
- Domain model now 100% pure — zero framework dependencies
- Backend and user-service Email VO synchronized (DB-per-service design)

### Technical Details
- **Error code:** `VO-001` (stable across services)
- **RFC 9457 compliant:** type, title, status, detail, instance, custom properties
- **Content-Type:** `application/problem+json`
- **Test coverage:** 100% (domain layer — Email VO + Exception + Handler)
- **Backward compatibility:** Guaranteed via contract tests (Spring Cloud Contract)
- **Quality gates:** All tests passing (53/53), 0 critical issues, code review approved
