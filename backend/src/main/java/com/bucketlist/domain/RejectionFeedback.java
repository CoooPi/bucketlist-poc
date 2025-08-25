package com.bucketlist.domain;

public class RejectionFeedback {
    private final String suggestionId;
    private final String reason;
    private final boolean isCustomReason;
    
    public RejectionFeedback(String suggestionId, String reason, boolean isCustomReason) {
        this.suggestionId = suggestionId;
        this.reason = reason;
        this.isCustomReason = isCustomReason;
    }
    
    public String getSuggestionId() {
        return suggestionId;
    }
    
    public String getReason() {
        return reason;
    }
    
    public boolean isCustomReason() {
        return isCustomReason;
    }
}