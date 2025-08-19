package com.bucketlist.domain;

public enum SuggestionMode {
    PROVEN("Proven Ideas"),
    CREATIVE("Creative Suggestions");
    
    private final String displayName;
    
    SuggestionMode(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}