package com.scopeflow.adapter.in.web.proposal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Paginated response for proposal list endpoints.
 *
 * Follows the same structure as Spring Page to ease future migration
 * from in-memory to DB-level pagination.
 */
@Schema(description = "Paginated list of proposals")
public record ProposalPageResponse(

        @Schema(description = "Page content (proposals)")
        List<ProposalResponse> content,

        @Schema(description = "Total proposals matching filters", example = "42")
        long totalElements,

        @Schema(description = "Total pages with current size", example = "3")
        int totalPages,

        @Schema(description = "Page size (items per page)", example = "20")
        int size,

        @Schema(description = "Current page number (zero-based)", example = "0")
        int number,

        @Schema(description = "True if this is the first page", example = "true")
        boolean first,

        @Schema(description = "True if this is the last page", example = "false")
        boolean last

) {
    public static ProposalPageResponse of(
            List<ProposalResponse> content,
            long totalElements,
            int totalPages,
            int size,
            int page,
            boolean first,
            boolean last
    ) {
        return new ProposalPageResponse(content, totalElements, totalPages, size, page, first, last);
    }
}
