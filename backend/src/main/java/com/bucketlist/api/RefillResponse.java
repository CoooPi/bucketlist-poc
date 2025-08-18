package com.bucketlist.api;

import java.util.List;

public record RefillResponse(
    List<SuggestionDto> added
) {}