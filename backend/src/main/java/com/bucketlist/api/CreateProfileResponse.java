package com.bucketlist.api;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProfileResponse(
    UUID profileId,
    String profileSummary,
    BigDecimal capital
) {}