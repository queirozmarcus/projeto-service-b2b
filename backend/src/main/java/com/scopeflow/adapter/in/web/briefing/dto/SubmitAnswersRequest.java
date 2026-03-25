package com.scopeflow.adapter.in.web.briefing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for submitting multiple answers at once to a briefing session.
 */
public record SubmitAnswersRequest(
        @NotEmpty(message = "answers list must not be empty")
        @Valid
        List<AnswerItem> answers
) {

    /**
     * A single question-answer pair.
     */
    public record AnswerItem(
            @NotNull(message = "questionId must not be null")
            UUID questionId,

            @NotNull(message = "answerText must not be null")
            @jakarta.validation.constraints.NotBlank(message = "answerText must not be blank")
            String answerText
    ) {}
}
