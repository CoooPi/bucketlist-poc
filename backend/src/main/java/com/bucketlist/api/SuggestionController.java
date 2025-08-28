package com.bucketlist.api;

import com.bucketlist.domain.BucketListSuggestion;
import com.bucketlist.domain.RejectionFeedback;
import com.bucketlist.domain.SuggestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/suggestions")
@CrossOrigin(origins = "http://localhost:5173")
public class SuggestionController {
    
    private final SuggestionService suggestionService;
    
    @Autowired
    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }
    
    @GetMapping("/{sessionId}")
    public ResponseEntity<SuggestionsResponse> getSuggestions(@PathVariable String sessionId) {
        try {
            List<BucketListSuggestion> suggestions = suggestionService.getSuggestions(sessionId);
            
            if (suggestions.isEmpty()) {
                suggestions = suggestionService.generateSuggestionsForPerson(sessionId);
            }
            
            List<SuggestionDto> dtos = suggestions.stream().map(this::toDto).toList();
            return ResponseEntity.ok(new SuggestionsResponse(dtos));
            
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("API key")) {
                return ResponseEntity.status(401).build(); // Unauthorized - API key required
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Error generating suggestions: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
    
    @PostMapping("/accept")
    public ResponseEntity<Void> acceptSuggestion(@RequestBody AcceptRequest request) {
        suggestionService.acceptSuggestion(request.getSessionId(), request.getSuggestionId());
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/reject")
    public ResponseEntity<Void> rejectSuggestion(@RequestBody RejectRequest request) {
        RejectionFeedback feedback = new RejectionFeedback(
            request.getSuggestionId(), 
            request.getReason(), 
            request.isCustomReason()
        );
        suggestionService.rejectSuggestion(request.getSessionId(), feedback);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/accepted/{sessionId}")
    public ResponseEntity<SuggestionsResponse> getAcceptedSuggestions(@PathVariable String sessionId) {
        List<BucketListSuggestion> suggestions = suggestionService.getAcceptedSuggestions(sessionId);
        List<SuggestionDto> dtos = suggestions.stream().map(this::toDto).toList();
        return ResponseEntity.ok(new SuggestionsResponse(dtos));
    }
    
    @GetMapping("/rejected/{sessionId}")
    public ResponseEntity<SuggestionsResponse> getRejectedSuggestions(@PathVariable String sessionId) {
        List<BucketListSuggestion> suggestions = suggestionService.getRejectedSuggestions(sessionId);
        List<SuggestionDto> dtos = suggestions.stream().map(this::toDto).toList();
        return ResponseEntity.ok(new SuggestionsResponse(dtos));
    }
    
    @GetMapping("/next/{sessionId}")
    public ResponseEntity<SuggestionDto> getNextSuggestion(@PathVariable String sessionId) {
        try {
            Optional<BucketListSuggestion> nextSuggestion = suggestionService.getNextUnreviewedSuggestion(sessionId);
            
            if (nextSuggestion.isEmpty()) {
                // Check if we need to regenerate suggestions with feedback
                if (suggestionService.shouldRegenerateWithFeedback(sessionId)) {
                    List<BucketListSuggestion> newSuggestions = suggestionService.regenerateSuggestionsWithFeedback(sessionId);
                    if (!newSuggestions.isEmpty()) {
                        return ResponseEntity.ok(toDto(newSuggestions.get(0)));
                    }
                }
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(toDto(nextSuggestion.get()));
            
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("API key")) {
                return ResponseEntity.status(401).build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Error generating suggestions: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
    
    @PostMapping("/regenerate")
    public ResponseEntity<SuggestionsResponse> regenerateSuggestions(@RequestBody RegenerateRequest request) {
        try {
            List<BucketListSuggestion> suggestions = suggestionService.regenerateSuggestionsWithFeedback(request.getSessionId());
            List<SuggestionDto> dtos = suggestions.stream().map(this::toDto).toList();
            return ResponseEntity.ok(new SuggestionsResponse(dtos));
            
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("API key")) {
                return ResponseEntity.status(401).build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Error generating suggestions: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
    
    private SuggestionDto toDto(BucketListSuggestion suggestion) {
        PriceBreakdownDto priceDto = new PriceBreakdownDto(
            suggestion.getPriceBreakdown().getLineItems().stream()
                .map(li -> new LineItemDto(li.getName(), li.getPrice(), li.getDescription()))
                .toList(),
            suggestion.getPriceBreakdown().getCurrency(),
            suggestion.getPriceBreakdown().getTotalCost()
        );
        
        return new SuggestionDto(
            suggestion.getId(),
            suggestion.getTitle(),
            suggestion.getDescription(),
            suggestion.getCategory().getDisplayName(),
            priceDto,
            suggestion.getRejectionReasons()
        );
    }
    
    public static class SuggestionDto {
        private String id;
        private String title;
        private String description;
        private String category;
        private PriceBreakdownDto priceBreakdown;
        private List<String> rejectionReasons;
        
        public SuggestionDto(String id, String title, String description, String category, 
                           PriceBreakdownDto priceBreakdown, List<String> rejectionReasons) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.category = category;
            this.priceBreakdown = priceBreakdown;
            this.rejectionReasons = rejectionReasons;
        }
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public PriceBreakdownDto getPriceBreakdown() { return priceBreakdown; }
        public void setPriceBreakdown(PriceBreakdownDto priceBreakdown) { this.priceBreakdown = priceBreakdown; }
        
        public List<String> getRejectionReasons() { return rejectionReasons; }
        public void setRejectionReasons(List<String> rejectionReasons) { this.rejectionReasons = rejectionReasons; }
    }
    
    public static class SuggestionsResponse {
        private List<SuggestionDto> suggestions;
        
        public SuggestionsResponse(List<SuggestionDto> suggestions) {
            this.suggestions = suggestions;
        }
        
        public List<SuggestionDto> getSuggestions() { return suggestions; }
        public void setSuggestions(List<SuggestionDto> suggestions) { this.suggestions = suggestions; }
    }
    
    public static class AcceptRequest {
        private String sessionId;
        private String suggestionId;
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getSuggestionId() { return suggestionId; }
        public void setSuggestionId(String suggestionId) { this.suggestionId = suggestionId; }
    }
    
    public static class PriceBreakdownDto {
        private List<LineItemDto> lineItems;
        private String currency;
        private java.math.BigDecimal totalCost;
        
        public PriceBreakdownDto(List<LineItemDto> lineItems, String currency, java.math.BigDecimal totalCost) {
            this.lineItems = lineItems;
            this.currency = currency;
            this.totalCost = totalCost;
        }
        
        public List<LineItemDto> getLineItems() { return lineItems; }
        public void setLineItems(List<LineItemDto> lineItems) { this.lineItems = lineItems; }
        
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        
        public java.math.BigDecimal getTotalCost() { return totalCost; }
        public void setTotalCost(java.math.BigDecimal totalCost) { this.totalCost = totalCost; }
    }
    
    public static class LineItemDto {
        private String name;
        private java.math.BigDecimal price;
        private String description;
        
        public LineItemDto(String name, java.math.BigDecimal price, String description) {
            this.name = name;
            this.price = price;
            this.description = description;
        }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public java.math.BigDecimal getPrice() { return price; }
        public void setPrice(java.math.BigDecimal price) { this.price = price; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    public static class RejectRequest {
        private String sessionId;
        private String suggestionId;
        private String reason;
        private boolean customReason;
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getSuggestionId() { return suggestionId; }
        public void setSuggestionId(String suggestionId) { this.suggestionId = suggestionId; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public boolean isCustomReason() { return customReason; }
        public void setCustomReason(boolean customReason) { this.customReason = customReason; }
    }
    
    public static class RegenerateRequest {
        private String sessionId;
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }
}