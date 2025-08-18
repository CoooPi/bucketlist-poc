package com.bucketlist.api;

import com.bucketlist.domain.Mode;

import java.util.UUID;

public record CreateProfileResponse(
    UUID profileId,
    String profileSummary,
    Mode mode
) {}