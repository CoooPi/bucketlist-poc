package com.bucketlist.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RejectedSuggestionDto(
    UUID id,
    String title,
    String description,
    List<BudgetItem> budgetBreakdown,
    String reason,
    Instant rejectedAt
) {}