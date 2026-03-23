package com.scopeflow.adapter.in.web.briefing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Request to abandon a briefing session.
 */
@Schema(description = "Request to abandon a briefing session")
public record AbandonBriefingRequest(

        @Size(max = 500, message = "Reason must not exceed 500 characters")
        @Schema(description = "Optional reason for abandoning", example = "Client decided to postpone project")
        String reason

) {
}
