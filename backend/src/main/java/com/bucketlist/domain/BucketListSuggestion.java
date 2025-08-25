package com.bucketlist.domain;

import java.util.List;
import java.util.UUID;

public class BucketListSuggestion {
    private final String id;
    private final String title;
    private final String description;
    private final SpendingCategory category;
    private final PriceBreakdown priceBreakdown;
    private final List<String> rejectionReasons;
    
    public BucketListSuggestion(String title, String description, SpendingCategory category, 
                               PriceBreakdown priceBreakdown, List<String> rejectionReasons) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.category = category;
        this.priceBreakdown = priceBreakdown;
        this.rejectionReasons = List.copyOf(rejectionReasons);
    }
    
    public String getId() {
        return id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public SpendingCategory getCategory() {
        return category;
    }
    
    public PriceBreakdown getPriceBreakdown() {
        return priceBreakdown;
    }
    
    public List<String> getRejectionReasons() {
        return rejectionReasons;
    }
}