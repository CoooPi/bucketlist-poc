package com.bucketlist.api;

import java.util.UUID;

public record CreateProfileResponse(
    UUID profileId,
    String profileSummary
) {}