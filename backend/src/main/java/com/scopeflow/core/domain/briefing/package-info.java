/**
 * Briefing domain bounded context.
 *
 * Owns the entire AI-assisted discovery flow:
 * - Briefing session lifecycle (creation, progression, completion, abandonment)
 * - Question generation and presentation
 * - Answer collection with gap detection
 * - Follow-up question auto-generation
 * - Completion scoring and readiness validation
 *
 * Architecture:
 * - Sealed classes for type-safe state management
 * - Value objects (records) for domain modeling
 * - Pure domain logic (no framework dependencies)
 * - Repository ports for persistence abstraction
 * - Domain events for async integration (Outbox pattern)
 *
 * Key Invariants:
 * 1. Sequential questions (no skip)
 * 2. No empty answers
 * 3. Max 1 follow-up per question
 * 4. Completion >= 80% + no critical gaps
 * 5. Immutable answers (audit trail)
 * 6. Unique public token per session
 * 7. Single active briefing per client per service type
 *
 * Package dependencies:
 * - com.scopeflow.core.domain.workspace (WorkspaceId)
 * - No Spring framework dependencies
 *
 * @see BriefingSession
 * @see BriefingAnswer
 * @see BriefingService
 * @see BriefingSessionRepository
 */
package com.scopeflow.core.domain.briefing;
