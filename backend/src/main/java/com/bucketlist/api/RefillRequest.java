package com.bucketlist.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RefillRequest(
    @Min(value = 1, message = "Batch size must be at least 1")
    @Max(value = 10, message = "Batch size must be at most 10")
    Integer batchSize
) {
    public RefillRequest {
        if (batchSize == null) {
            batchSize = 5;
        }
    }
}