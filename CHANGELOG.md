# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- Invalid email format now returns HTTP 400 (was 500) with RFC 9457 Problem Details
- Created `InvalidValueObjectException` for consistent VO validation across domain layer
- Email validation harmonized across monolith and user-service

### Added
- `InvalidValueObjectException` class for domain value object validation errors
- 11 unit tests for Email VO validation (monolith + user-service)
- 6 integration tests for Email validation at HTTP layer (monolith + user-service)
- RFC 9457 Problem Details handler for VO validation failures

### Changed
- Email VO now throws `InvalidValueObjectException` instead of `IllegalArgumentException` for format errors
- GlobalExceptionHandler maps VO validation to HTTP 400 with error code `VO-001`
