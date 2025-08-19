package com.bucketlist.api;

import java.util.List;
import java.util.UUID;

public record SuggestionDto(
    UUID id,
    String title,
    String description,
    List<BudgetItem> budgetBreakdown
) {}