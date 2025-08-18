package com.bucketlist.api;

import com.bucketlist.domain.Verdict;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FeedbackRequest(
    @NotNull UUID profileId,
    @NotNull UUID suggestionId,
    @NotNull Verdict verdict,
    String reason
) {}