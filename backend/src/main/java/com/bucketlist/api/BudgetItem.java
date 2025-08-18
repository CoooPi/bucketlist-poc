package com.bucketlist.api;

import java.math.BigDecimal;

public record BudgetItem(
    String category,
    String description,
    BigDecimal amount
) {}