package com.scopeflow.adapter.out.persistence.proposal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.scopeflow.core.domain.proposal.ProposalScope;
import org.springframework.stereotype.Component;

/**
 * Handles serialization/deserialization of ProposalScope to/from JSONB.
 *
 * ProposalScope is a value object stored as a JSON column in proposal_versions.
 */
@Component
public class ProposalScopeJsonMapper {

    private final ObjectMapper objectMapper;

    public ProposalScopeJsonMapper() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public String toJson(ProposalScope scope) {
        if (scope == null) return null;
        try {
            return objectMapper.writeValueAsString(scope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize ProposalScope to JSON", e);
        }
    }

    public ProposalScope fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, ProposalScope.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize ProposalScope from JSON", e);
        }
    }
}
