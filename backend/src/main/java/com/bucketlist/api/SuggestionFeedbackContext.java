package com.bucketlist.api;

import com.bucketlist.domain.Verdict;

import java.time.Instant;

public record SuggestionFeedbackContext(
    String title,
    String description,
    Verdict verdict,
    String reason,
    Instant createdAt
) {}