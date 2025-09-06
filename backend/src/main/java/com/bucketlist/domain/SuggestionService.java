package com.bucketlist.domain;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SuggestionService {
    
    private static final Logger logger = LoggerFactory.getLogger(SuggestionService.class);
    
    private final ApiKeyService apiKeyService;
    private final PersonSessionService sessionService;
    
    // Store suggestions per session
    private final Map<String, List<BucketListSuggestion>> sessionSuggestions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> acceptedSuggestions = new ConcurrentHashMap<>();
    private final Map<String, Map<String, RejectionFeedback>> rejectedSuggestions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> reviewedSuggestions = new ConcurrentHashMap<>();
    
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
        reviewedSuggestions.computeIfAbsent(sessionId, k -> new HashSet<>()).add(suggestionId);
    }
    
    public void rejectSuggestion(String sessionId, RejectionFeedback feedback) {
        rejectedSuggestions.computeIfAbsent(sessionId, k -> new HashMap<>())
            .put(feedback.getSuggestionId(), feedback);
        reviewedSuggestions.computeIfAbsent(sessionId, k -> new HashSet<>()).add(feedback.getSuggestionId());
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
    
    public String getRejectionReason(String sessionId, String suggestionId) {
        Map<String, RejectionFeedback> rejected = rejectedSuggestions.getOrDefault(sessionId, Map.of());
        RejectionFeedback feedback = rejected.get(suggestionId);
        return feedback != null ? feedback.getReason() : "No reason provided";
    }
    
    public boolean isCustomRejectionReason(String sessionId, String suggestionId) {
        Map<String, RejectionFeedback> rejected = rejectedSuggestions.getOrDefault(sessionId, Map.of());
        RejectionFeedback feedback = rejected.get(suggestionId);
        return feedback != null && feedback.isCustomReason();
    }
    
    public Optional<BucketListSuggestion> getNextUnreviewedSuggestion(String sessionId) {
        List<BucketListSuggestion> suggestions = getSuggestions(sessionId);
        Set<String> reviewed = reviewedSuggestions.getOrDefault(sessionId, Set.of());
        
        return suggestions.stream()
            .filter(s -> !reviewed.contains(s.getId()))
            .findFirst();
    }
    
    public boolean shouldRegenerateWithFeedback(String sessionId) {
        List<BucketListSuggestion> suggestions = getSuggestions(sessionId);
        Set<String> reviewed = reviewedSuggestions.getOrDefault(sessionId, Set.of());
        
        // Regenerate if all current suggestions have been reviewed
        return !suggestions.isEmpty() && reviewed.size() >= suggestions.size();
    }
    
    public List<BucketListSuggestion> regenerateSuggestionsWithFeedback(String sessionId) {
        Optional<PersonSession> sessionOpt = sessionService.getSession(sessionId);
        if (sessionOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid session ID");
        }
        
        if (!apiKeyService.hasValidApiKey()) {
            throw new IllegalStateException("API key not configured");
        }
        
        PersonSession session = sessionOpt.get();
        
        try {
            String prompt = buildRegenerationPrompt(sessionId, session.getPersonDescription());
            String content;
            
            ChatClient chatClient = apiKeyService.getValidatedChatClient();
            if (chatClient != null) {
                BeanOutputConverter<SuggestionResponse> outputConverter = 
                    new BeanOutputConverter<>(SuggestionResponse.class);
                PromptTemplate promptTemplate = new PromptTemplate(prompt + "\n\n{format}");
                Prompt chatPrompt = promptTemplate.create(Map.of("format", outputConverter.getFormat()));
                content = chatClient.prompt(chatPrompt).call().content();
                
                SuggestionResponse suggestionResponse = outputConverter.convert(content);
                List<BucketListSuggestion> suggestions = convertToSuggestions(suggestionResponse);
                
                // Replace old suggestions with new ones and clear review tracking for new batch
                sessionSuggestions.put(sessionId, suggestions);
                reviewedSuggestions.put(sessionId, new HashSet<>());
                
                return suggestions;
            } else {
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
                
                // Replace old suggestions with new ones and clear review tracking for new batch
                sessionSuggestions.put(sessionId, suggestions);
                reviewedSuggestions.put(sessionId, new HashSet<>());
                
                return suggestions;
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to regenerate suggestions: " + e.getMessage(), e);
        }
    }
    
    private String buildSuggestionPrompt(String personDescription) {
        return String.format("""
            You are generating bucket list suggestions for a person based on this description: %s
            
            BUDGET SCALING: Analyze the description for budget level and scale accordingly:
            - HIGH BUDGET: Premium/luxury experiences (%%1000-10000+), include 1-2 stretch goals at 70%%%% budget
            - MEDIUM BUDGET: Balanced aspirational suggestions (%%200-2000), include stretch goals up to %%5000
            - LOW BUDGET: Accessible local experiences (%%25-500), include 1 stretch goal up to %%1200
            
            REQUIREMENTS:
            1. Generate exactly 5 suggestions using different categories: %s
            2. Category field must EXACTLY match display names: %s
            3. Scale pricing to inferred budget level with detailed cost breakdowns
            4. Include 5 rejection reasons per suggestion
            5. Infer currency from location (USD/EUR/GBP/CAD)
            6. Validate all categories are unique and match: %s
            
            {format}
            """, 
            personDescription,
            getDisplayNamesString(),
            getDisplayNamesString(),
            getDisplayNamesString()
        ).replace("%%", "$");
    }
    
    private String getDisplayNamesString() {
        return Arrays.stream(SpendingCategory.values())
                .map(SpendingCategory::getDisplayName)
                .collect(java.util.stream.Collectors.joining(", "));
    }
    
    private String buildRegenerationPrompt(String sessionId, String personDescription) {
        StringBuilder feedbackSection = new StringBuilder();
        
        // Get accepted suggestions
        List<BucketListSuggestion> accepted = getAcceptedSuggestions(sessionId);
        if (!accepted.isEmpty()) {
            feedbackSection.append("PREVIOUSLY ACCEPTED SUGGESTIONS (the user liked these):\n");
            for (BucketListSuggestion suggestion : accepted) {
                feedbackSection.append(String.format("- %s (%s): %s\n", 
                    suggestion.getTitle(), 
                    suggestion.getCategory().getDisplayName(), 
                    suggestion.getDescription()));
            }
            feedbackSection.append("\n");
        }
        
        // Get rejected suggestions with reasons
        List<BucketListSuggestion> rejected = getRejectedSuggestions(sessionId);
        Map<String, RejectionFeedback> rejectionMap = rejectedSuggestions.getOrDefault(sessionId, Map.of());
        if (!rejected.isEmpty()) {
            feedbackSection.append("PREVIOUSLY REJECTED SUGGESTIONS (the user disliked these):\n");
            for (BucketListSuggestion suggestion : rejected) {
                RejectionFeedback feedback = rejectionMap.get(suggestion.getId());
                String reason = feedback != null ? feedback.getReason() : "No reason provided";
                feedbackSection.append(String.format("- %s (%s): %s | REJECTION REASON: %s\n", 
                    suggestion.getTitle(), 
                    suggestion.getCategory().getDisplayName(), 
                    suggestion.getDescription(),
                    reason));
            }
            feedbackSection.append("\n");
        }
        
        return String.format("""
            You are generating NEW bucket list suggestions for a person. This is a REGENERATION based on previous feedback.
            
            PERSON DESCRIPTION: %s
            
            %s
            BUDGET SCALING: Re-analyze description and feedback patterns for budget level:
            - HIGH BUDGET: Premium experiences (%%1000-10000+), stretch goals at 70%%%% budget
            - MEDIUM BUDGET: Balanced suggestions (%%200-2000), stretch goals up to %%5000
            - LOW BUDGET: Accessible experiences (%%25-500), stretch goal up to %%1200
            - Adjust budget assessment based on accepted/rejected pricing patterns
            
            LEARNING: Use feedback to generate better personalized suggestions. Avoid rejected patterns, align with accepted preferences.
            
            REQUIREMENTS:
            1. Generate 5 NEW suggestions using different categories: %s
            2. Category field must EXACTLY match display names: %s  
            3. Scale pricing to budget level with detailed breakdowns
            4. Include 5 rejection reasons per suggestion
            5. Infer currency from location
            6. Validate categories are unique and match: %s
            
            {format}
            """, 
            personDescription,
            feedbackSection.toString(),
            getDisplayNamesString(),
            getDisplayNamesString(),
            getDisplayNamesString()
        ).replace("%%", "$");
    }
    
    private List<BucketListSuggestion> convertToSuggestions(SuggestionResponse response) {
        List<BucketListSuggestion> suggestions = response.getSuggestions().stream()
            .map(this::convertToSuggestion)
            .toList();
            
        validateCategoryDiversity(suggestions);
        return suggestions;
    }
    
    private void validateCategoryDiversity(List<BucketListSuggestion> suggestions) {
        Map<SpendingCategory, Long> categoryCount = suggestions.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                BucketListSuggestion::getCategory,
                java.util.stream.Collectors.counting()
            ));
            
        logger.info("Generated suggestions category distribution: {}", categoryCount);
        
        // Check for duplicates
        List<SpendingCategory> duplicateCategories = categoryCount.entrySet().stream()
            .filter(entry -> entry.getValue() > 1)
            .map(Map.Entry::getKey)
            .toList();
            
        if (!duplicateCategories.isEmpty()) {
            logger.warn("Found duplicate categories in suggestions: {}. This may indicate AI prompt issues.", duplicateCategories);
        }
        
        // Check if we have the expected number of different categories
        if (categoryCount.size() < suggestions.size()) {
            logger.warn("Expected {} different categories but got {}. Category diversity requirement not met.", 
                       suggestions.size(), categoryCount.size());
        } else {
            logger.info("Category diversity validation passed: {} suggestions with {} different categories", 
                       suggestions.size(), categoryCount.size());
        }
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
        if (displayName == null || displayName.trim().isEmpty()) {
            logger.warn("Category display name is null or empty, falling back to SMALL_LUXURY");
            return SpendingCategory.SMALL_LUXURY;
        }
        
        String trimmedDisplayName = displayName.trim();
        logger.debug("Attempting to match category: '{}'", trimmedDisplayName);
        
        // First try exact match (case insensitive)
        for (SpendingCategory category : SpendingCategory.values()) {
            if (category.getDisplayName().equalsIgnoreCase(trimmedDisplayName)) {
                logger.debug("Found exact match: '{}' -> {}", trimmedDisplayName, category);
                return category;
            }
        }
        
        // Try partial matches or common variations
        String lowerDisplayName = trimmedDisplayName.toLowerCase();
        for (SpendingCategory category : SpendingCategory.values()) {
            String categoryLower = category.getDisplayName().toLowerCase();
            if (categoryLower.contains(lowerDisplayName) || lowerDisplayName.contains(categoryLower)) {
                logger.debug("Found partial match: '{}' -> {}", trimmedDisplayName, category);
                return category;
            }
        }
        
        // Log all available categories for debugging
        String availableCategories = getDisplayNamesString();
        logger.warn("No category match found for: '{}'. Available categories: [{}]. Falling back to SMALL_LUXURY", 
                    trimmedDisplayName, availableCategories);
        
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
            logger.error("Failed to parse suggestions JSON", e);
        }
        
        validateCategoryDiversity(suggestions);
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