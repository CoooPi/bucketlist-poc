package com.bucketlist.domain;

import java.math.BigDecimal;
import java.util.List;

public class PriceBreakdown {
    private final List<LineItem> lineItems;
    private final String currency;
    
    public PriceBreakdown(List<LineItem> lineItems, String currency) {
        this.lineItems = List.copyOf(lineItems);
        this.currency = currency;
    }
    
    public List<LineItem> getLineItems() {
        return lineItems;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public BigDecimal getTotalCost() {
        return lineItems.stream()
            .map(LineItem::getPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}