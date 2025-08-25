package com.bucketlist.domain;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SuggestionService {
    
    private final ApiKeyService apiKeyService;
    private final PersonSessionService sessionService;
    
    // Store suggestions per session
    private final Map<String, List<BucketListSuggestion>> sessionSuggestions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> acceptedSuggestions = new ConcurrentHashMap<>();
    private final Map<String, Map<String, RejectionFeedback>> rejectedSuggestions = new ConcurrentHashMap<>();
    
    @Autowired
    public SuggestionService(ApiKeyService apiKeyService, PersonSessionService sessionService) {
        this.apiKeyService = apiKeyService;
        this.sessionService = sessionService;
    }
    
    public List<BucketListSuggestion> generateSuggestionsForPerson(String sessionId) {
        Optional<PersonSession> sessionOpt = sessionService.getSession(sessionId);
        if (sessionOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid session ID");
        }
        
        if (!apiKeyService.hasValidApiKey()) {
            throw new IllegalStateException("API key not configured");
        }
        
        PersonSession session = sessionOpt.get();
        
        try {
            String prompt = buildSuggestionPrompt(session.getPersonDescription());
            String content;
            
            ChatClient chatClient = apiKeyService.getValidatedChatClient();
            if (chatClient != null) {
                // Use ChatClient if available
                BeanOutputConverter<SuggestionResponse> outputConverter = 
                    new BeanOutputConverter<>(SuggestionResponse.class);
                PromptTemplate promptTemplate = new PromptTemplate(prompt + "\n\n{format}");
                Prompt chatPrompt = promptTemplate.create(Map.of("format", outputConverter.getFormat()));
                content = chatClient.prompt(chatPrompt).call().content();
                
                SuggestionResponse suggestionResponse = outputConverter.convert(content);
                List<BucketListSuggestion> suggestions = convertToSuggestions(suggestionResponse);
                sessionSuggestions.put(sessionId, suggestions);
                return suggestions;
            } else {
                // Fall back to direct API call with simplified JSON parsing
                content = apiKeyService.callOpenAiDirectly(prompt + 
                    "\n\nRespond with valid JSON in this exact format:\n" +
                    "{\n" +
                    "  \"suggestions\": [\n" +
                    "    {\n" +
                    "      \"title\": \"suggestion title\",\n" +
                    "      \"description\": \"detailed description\",\n" +
                    "      \"category\": \"one of the allowed categories\",\n" +
                    "      \"priceBreakdown\": {\n" +
                    "        \"lineItems\": [\n" +
                    "          {\"name\": \"item name\", \"price\": 100.00, \"description\": \"item description\"}\n" +
                    "        ],\n" +
                    "        \"currency\": \"USD\"\n" +
                    "      },\n" +
                    "      \"rejectionReasons\": [\"reason1\", \"reason2\", \"reason3\", \"reason4\", \"reason5\"]\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}");
                
                List<BucketListSuggestion> suggestions = parseSimpleSuggestions(content);
                sessionSuggestions.put(sessionId, suggestions);
                return suggestions;
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate suggestions: " + e.getMessage(), e);
        }
    }
    
    public List<BucketListSuggestion> getSuggestions(String sessionId) {
        return sessionSuggestions.getOrDefault(sessionId, List.of());
    }
    
    public void acceptSuggestion(String sessionId, String suggestionId) {
        acceptedSuggestions.computeIfAbsent(sessionId, k -> new HashSet<>()).add(suggestionId);
    }
    
    public void rejectSuggestion(String sessionId, RejectionFeedback feedback) {
        rejectedSuggestions.computeIfAbsent(sessionId, k -> new HashMap<>())
            .put(feedback.getSuggestionId(), feedback);
    }
    
    public List<BucketListSuggestion> getAcceptedSuggestions(String sessionId) {
        Set<String> accepted = acceptedSuggestions.getOrDefault(sessionId, Set.of());
        return getSuggestions(sessionId).stream()
            .filter(s -> accepted.contains(s.getId()))
            .toList();
    }
    
    public List<BucketListSuggestion> getRejectedSuggestions(String sessionId) {
        Map<String, RejectionFeedback> rejected = rejectedSuggestions.getOrDefault(sessionId, Map.of());
        return getSuggestions(sessionId).stream()
            .filter(s -> rejected.containsKey(s.getId()))
            .toList();
    }
    
    private String buildSuggestionPrompt(String personDescription) {
        return String.format("""
            You are generating bucket list suggestions for a person based on this description: %s
            
            STRICT REQUIREMENTS:
            1. Generate exactly 5 suggestions
            2. Use only these categories: %s
            3. CRITICAL: Each suggestion MUST use a DIFFERENT category - no two suggestions can share the same category
            4. Vary creativity levels (some safe, some adventurous)
            5. Include exactly 5 specific rejection reasons for each suggestion
            6. Each suggestion must be realistic and achievable
            7. PRICING: Provide detailed cost breakdown with line items
            8. CURRENCY: Infer the person's nationality/location from their description and use the appropriate currency (USD for Americans, EUR for Europeans, GBP for British, CAD for Canadians, etc.)
            9. Each price breakdown must include 2-5 realistic line items with specific costs
            10. Line items should be detailed (e.g., "Flight to Paris", "3-night hotel stay", "Museum tickets", "Local meals")
            
            CATEGORY VALIDATION: Before finalizing your response, verify that all 5 suggestions use different categories from: %s
            
            {format}
            """, 
            personDescription,
            Arrays.toString(SpendingCategory.values()),
            Arrays.toString(SpendingCategory.values())
        );
    }
    
    private List<BucketListSuggestion> convertToSuggestions(SuggestionResponse response) {
        return response.getSuggestions().stream()
            .map(this::convertToSuggestion)
            .toList();
    }
    
    private BucketListSuggestion convertToSuggestion(SuggestionResponse.SuggestionItem item) {
        SpendingCategory category = findCategoryByDisplayName(item.getCategory());
        PriceBreakdown priceBreakdown = convertToPriceBreakdown(item.getPriceBreakdown());
        
        return new BucketListSuggestion(
            item.getTitle(),
            item.getDescription(),
            category,
            priceBreakdown,
            item.getRejectionReasons()
        );
    }
    
    private SpendingCategory findCategoryByDisplayName(String displayName) {
        for (SpendingCategory category : SpendingCategory.values()) {
            if (category.getDisplayName().equalsIgnoreCase(displayName)) {
                return category;
            }
        }
        // Fallback to SMALL_LUXURY if no match found
        return SpendingCategory.SMALL_LUXURY;
    }
    
    private PriceBreakdown convertToPriceBreakdown(SuggestionResponse.PriceBreakdownItem item) {
        List<LineItem> lineItems = item.getLineItems().stream()
            .map(li -> new LineItem(li.getName(), li.getPrice(), li.getDescription()))
            .toList();
        return new PriceBreakdown(lineItems, item.getCurrency());
    }
    
    private List<BucketListSuggestion> parseSimpleSuggestions(String jsonContent) {
        // Simple JSON parsing for fallback - in production use Jackson or Gson
        List<BucketListSuggestion> suggestions = new ArrayList<>();
        
        // This is a simplified parser - replace with proper JSON library in production
        try {
            // Extract suggestions array content between "suggestions": [ and ]
            int startIdx = jsonContent.indexOf("\"suggestions\":");
            if (startIdx == -1) return suggestions;
            
            int arrayStart = jsonContent.indexOf("[", startIdx);
            int arrayEnd = jsonContent.lastIndexOf("]");
            
            if (arrayStart == -1 || arrayEnd == -1) return suggestions;
            
            String arrayContent = jsonContent.substring(arrayStart + 1, arrayEnd);
            
            // Split by object boundaries (simplified approach)
            String[] items = arrayContent.split("\\},\\s*\\{");
            
            for (String item : items) {
                item = item.replace("{", "").replace("}", "");
                
                String title = extractJsonValue(item, "title");
                String description = extractJsonValue(item, "description");
                String category = extractJsonValue(item, "category");
                
                // Extract rejection reasons array
                List<String> rejectionReasons = extractJsonArray(item, "rejectionReasons");
                
                // Create a simple price breakdown with default values for fallback
                List<LineItem> lineItems = List.of(
                    new LineItem("Estimated cost", new java.math.BigDecimal("100.00"), "Approximate cost")
                );
                PriceBreakdown priceBreakdown = new PriceBreakdown(lineItems, "USD");
                
                if (!title.isEmpty() && !description.isEmpty()) {
                    SpendingCategory spendingCategory = findCategoryByDisplayName(category);
                    suggestions.add(new BucketListSuggestion(title, description, spendingCategory, priceBreakdown, rejectionReasons));
                }
            }
        } catch (Exception e) {
            // If parsing fails, return empty list
        }
        
        return suggestions;
    }
    
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "";
    }
    
    private List<String> extractJsonArray(String json, String key) {
        List<String> result = new ArrayList<>();
        String pattern = "\"" + key + "\"\\s*:\\s*\\[([^\\]]+)\\]";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        
        if (m.find()) {
            String arrayContent = m.group(1);
            String[] items = arrayContent.split(",");
            for (String item : items) {
                String cleaned = item.trim().replaceAll("^\"|\"$", "");
                result.add(cleaned);
            }
        }
        
        return result;
    }
    
    // Response class for AI conversion
    public static class SuggestionResponse {
        private List<SuggestionItem> suggestions;
        
        public List<SuggestionItem> getSuggestions() {
            return suggestions;
        }
        
        public void setSuggestions(List<SuggestionItem> suggestions) {
            this.suggestions = suggestions;
        }
        
        public static class SuggestionItem {
            private String title;
            private String description;
            private String category;
            private PriceBreakdownItem priceBreakdown;
            private List<String> rejectionReasons;
            
            public String getTitle() { return title; }
            public void setTitle(String title) { this.title = title; }
            
            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }
            
            public String getCategory() { return category; }
            public void setCategory(String category) { this.category = category; }
            
            public PriceBreakdownItem getPriceBreakdown() { return priceBreakdown; }
            public void setPriceBreakdown(PriceBreakdownItem priceBreakdown) { this.priceBreakdown = priceBreakdown; }
            
            public List<String> getRejectionReasons() { return rejectionReasons; }
            public void setRejectionReasons(List<String> rejectionReasons) { this.rejectionReasons = rejectionReasons; }
        }
        
        public static class PriceBreakdownItem {
            private List<LineItemData> lineItems;
            private String currency;
            
            public List<LineItemData> getLineItems() { return lineItems; }
            public void setLineItems(List<LineItemData> lineItems) { this.lineItems = lineItems; }
            
            public String getCurrency() { return currency; }
            public void setCurrency(String currency) { this.currency = currency; }
        }
        
        public static class LineItemData {
            private String name;
            private java.math.BigDecimal price;
            private String description;
            
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            
            public java.math.BigDecimal getPrice() { return price; }
            public void setPrice(java.math.BigDecimal price) { this.price = price; }
            
            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }
        }
    }
}