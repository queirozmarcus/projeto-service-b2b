package com.scopeflow.adapter.in.web.briefing.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Generic paginated response DTO (Spring Boot Page structure).
 *
 * @param <T> content type
 */
@Schema(description = "Paginated response")
public record PageResponse<T>(

        @Schema(description = "Page content (items)")
        List<T> content,

        @Schema(description = "Total number of items matching filters", example = "150")
        long totalElements,

        @Schema(description = "Total pages with current size", example = "8")
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
}
