package com.bucketlist.api;

import com.bucketlist.domain.Category;
import com.bucketlist.domain.PriceBand;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SuggestionDto(
    UUID id,
    String title,
    String description,
    PriceBand priceBand,
    Category category,
    BigDecimal estimatedCost,
    List<BudgetItem> budgetBreakdown
) {}