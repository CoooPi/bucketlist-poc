package com.bucketlist.domain;

public enum SpendingCategory {
    TRAVEL_VACATION("Travel & Vacation"),
    LUXURY_THINGS("Luxury Things"),
    HEALTH_WELLNESS("Health & Wellness"),
    SOCIAL_LIFESTYLE("Social & Lifestyle"),
    MENTAL_EMOTIONAL("Mental & Emotional Wellbeing"),
    SMALL_LUXURY("Small Luxury Treats"),
    FREEDOM_COMFORT("Freedom & Comfort"),
    OPTIONAL_ADDONS("Optional Add-ons");
    
    private final String displayName;
    
    SpendingCategory(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}