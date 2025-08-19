package com.bucketlist.api;

import com.bucketlist.domain.PriceBand;

import java.util.List;
import java.util.UUID;

public record SuggestionDto(
    UUID id,
    String title,
    String description,
    PriceBand priceBand,
    List<BudgetItem> budgetBreakdown
) {}