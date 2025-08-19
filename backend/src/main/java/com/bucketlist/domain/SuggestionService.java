package com.bucketlist.domain;

import com.bucketlist.api.SuggestionDto;
import com.bucketlist.api.BudgetItem;
import com.bucketlist.infra.SuggestionFeedbackRepository;
import com.bucketlist.infra.SuggestionRepository;
import com.bucketlist.infra.UserProfileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuggestionService {
    
    private final SuggestionRepository suggestionRepository;
    private final SuggestionFeedbackRepository feedbackRepository;
    private final UserProfileRepository userProfileRepository;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    
    public List<SuggestionDto> generateSuggestions(UUID profileId, SpendingCategory category, SuggestionMode mode, int count) {
        log.info("Generating {} suggestions for profile {} with category {} and mode {}", count, profileId, category, mode);
        
        var profile = userProfileRepository.findById(profileId)
            .orElseThrow(() -> new RuntimeException("Profile not found: " + profileId));
        
        try {
            String prompt = buildSuggestionPrompt(profile, category, mode, count);
            String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
            
            log.debug("AI response: {}", response);
            
            return parseSuggestionsFromResponse(response, profile);
            
        } catch (Exception e) {
            log.error("Failed to generate suggestions for profile {}", profileId, e);
            return List.of();
        }
    }
    
    public Optional<SuggestionDto> getNextSuggestion(UUID profileId, SpendingCategory category, SuggestionMode mode) {
        var suggestions = suggestionRepository.findUnratedByProfileId(profileId);
        
        if (suggestions.isEmpty()) {
            // Try to generate more suggestions
            var newSuggestions = generateSuggestions(profileId, category, mode, 5);
            if (!newSuggestions.isEmpty()) {
                return Optional.of(newSuggestions.get(0));
            }
            return Optional.empty();
        }
        
        var suggestion = suggestions.get(0);
        List<BudgetItem> budgetItems = parseBudgetBreakdown(suggestion.getBudgetBreakdownJson());
        
        return Optional.of(new SuggestionDto(
            suggestion.getId(),
            suggestion.getTitle(),
            suggestion.getDescription(),
            suggestion.getPriceBand(),
            budgetItems
        ));
    }
    
    public void recordFeedback(UUID profileId, UUID suggestionId, Verdict verdict, String reason) {
        log.info("Recording feedback: profile={}, suggestion={}, verdict={}", profileId, suggestionId, verdict);
        
        // Check if feedback already exists
        if (feedbackRepository.existsBySuggestionIdAndProfileId(suggestionId, profileId)) {
            log.warn("Feedback already exists for suggestion {} and profile {}", suggestionId, profileId);
            return;
        }
        
        var feedback = new SuggestionFeedback();
        feedback.setProfileId(profileId);
        feedback.setSuggestionId(suggestionId);
        feedback.setVerdict(verdict);
        feedback.setReason(reason);
        
        feedbackRepository.save(feedback);
    }
    
    public List<SuggestionDto> getAcceptedSuggestions(UUID profileId) {
        var acceptedFeedback = feedbackRepository.findByProfileIdAndVerdict(profileId, Verdict.ACCEPT);
        
        var suggestionIds = acceptedFeedback.stream()
            .map(SuggestionFeedback::getSuggestionId)
            .collect(Collectors.toSet());
        
        return suggestionRepository.findAllById(suggestionIds).stream()
            .map(s -> new SuggestionDto(s.getId(), s.getTitle(), s.getDescription(), s.getPriceBand(), 
                     parseBudgetBreakdown(s.getBudgetBreakdownJson())))
            .collect(Collectors.toList());
    }
    
    private String buildSuggestionPrompt(UserProfile profile, SpendingCategory spendingCategory, SuggestionMode suggestionMode, int count) {
        var priceBands = calculateRelativePriceBands(profile.getCapital());
        String categoryFocus = buildCategoryFocus(spendingCategory);
        String modeInstructions = buildModeInstructions(suggestionMode);
        
        return String.format("""
            Generate %d bucket list suggestions for this user profile:
            
            Basic Info:
            - Age: %d
            - Gender: %s  
            - Capital: %s SEK
            
            Personality: %s
            Preferences: %s
            Prior Experiences: %s
            
            SPENDING CATEGORY FOCUS: %s (CRITICAL: ALL SUGGESTIONS MUST BE STRICTLY RELATED TO THIS CATEGORY)
            %s
            
            SUGGESTION MODE: %s
            %s
            
            CRITICAL REQUIREMENT: Every single suggestion MUST be directly related to the selected spending category "%s".
            Do NOT suggest anything outside this category. If the category is "Travel & Vacation", suggest ONLY travel and vacation experiences.
            If the category is "Health & Wellness", suggest ONLY health and wellness activities. Stay strictly within the category boundaries.
            
            Return a JSON array of exactly %d suggestions:
            [
              {
                "title": "Short engaging title (max 70 chars)",
                "description": "Detailed description (max 240 chars)", 
                "priceBand": "LOW|MEDIUM|HIGH",
                "budgetBreakdown": [
                  {"category": "Transport", "description": "Round-trip flights", "amount": 8000},
                  {"category": "Accommodation", "description": "4 nights hotel", "amount": 12000},
                  {"category": "Activities", "description": "Tours and experiences", "amount": 3000},
                  {"category": "Food", "description": "Meals and dining", "amount": 2000}
                ]
              }
            ]
            
            Price band rules (relative to user budget of %s SEK):
            - LOW: < %s SEK (up to 10%% of budget)
            - MEDIUM: %s - %s SEK (10-40%% of budget)
            - HIGH: > %s SEK (40%%+ of budget - BE BOLD!)
            
            BUDGET CALCULATION RULES:
            - budgetBreakdown total must be realistic and match the priceBand
            - Include relevant categories: Transport, Accommodation, Activities, Food, Equipment, Other, etc.
            - For local activities, focus on Activities/Equipment costs
            - For travel, include flights/transport from Sweden
            - Scale suggestions to user's budget - higher budgets should get more luxurious/exclusive experiences
            - Ensure budgetBreakdown amounts add up to appropriate totals for the price band
            
            CRITICAL: Return only the JSON array, no other text.
            
            LANGUAGE REQUIREMENT: All suggestion titles and descriptions MUST be in English. Do not use Swedish or any other language.
            """,
            count, profile.getAge(), profile.getGender(), profile.getCapital(),
            profile.getPersonalityJson(), profile.getPreferencesJson(), profile.getPriorExperiencesJson(),
            spendingCategory.getDisplayName(), categoryFocus,
            suggestionMode.getDisplayName(), modeInstructions,
            spendingCategory.getDisplayName(),
            count, profile.getCapital(), priceBands.lowThreshold(), priceBands.mediumLow(), 
            priceBands.mediumHigh(), priceBands.highThreshold());
    }
    
    private List<SuggestionDto> parseSuggestionsFromResponse(String response, UserProfile profile) {
        try {
            JsonNode suggestionsArray = objectMapper.readTree(response.trim());
            List<SuggestionDto> results = new ArrayList<>();
            
            for (JsonNode suggestionNode : suggestionsArray) {
                try {
                    String title = suggestionNode.get("title").asText();
                    String description = suggestionNode.get("description").asText();
                    
                    // Parse price band with fallback
                    PriceBand priceBand;
                    try {
                        priceBand = PriceBand.valueOf(suggestionNode.get("priceBand").asText());
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid price band: {}, defaulting to MEDIUM", suggestionNode.get("priceBand").asText());
                        priceBand = PriceBand.MEDIUM;
                    }
                    
                    
                    // Parse budget information
                    String budgetBreakdownJson = suggestionNode.has("budgetBreakdown") ?
                        suggestionNode.get("budgetBreakdown").toString() :
                        "[]";
                    
                    // Create content hash for deduplication
                    String contentHash = createContentHash(title);
                    
                    // Skip if duplicate
                    if (suggestionRepository.existsByContentHash(contentHash)) {
                        log.debug("Skipping duplicate suggestion: {}", title);
                        continue;
                    }
                    
                    // Save suggestion
                    var suggestion = new Suggestion();
                    suggestion.setProfileId(profile.getId());
                    suggestion.setTitle(title);
                    suggestion.setDescription(description);
                    suggestion.setPriceBand(priceBand);
                    suggestion.setBudgetBreakdownJson(budgetBreakdownJson);
                    suggestion.setContentHash(contentHash);
                    
                    suggestion = suggestionRepository.save(suggestion);
                    
                    // Parse budget breakdown for DTO
                    List<BudgetItem> budgetItems = parseBudgetBreakdown(budgetBreakdownJson);
                    
                    results.add(new SuggestionDto(
                        suggestion.getId(),
                        suggestion.getTitle(),
                        suggestion.getDescription(),
                        suggestion.getPriceBand(),
                        budgetItems
                    ));
                    
                } catch (Exception e) {
                    log.warn("Failed to parse suggestion: {}", suggestionNode, e);
                }
            }
            
            return results;
            
        } catch (Exception e) {
            log.error("Failed to parse suggestions response: {}", response, e);
            return List.of();
        }
    }
    
    
    private String createContentHash(String title) {
        try {
            String content = title.toLowerCase().trim();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    private List<BudgetItem> parseBudgetBreakdown(String budgetBreakdownJson) {
        if (budgetBreakdownJson == null || budgetBreakdownJson.trim().isEmpty()) {
            return List.of();
        }
        
        try {
            JsonNode budgetArray = objectMapper.readTree(budgetBreakdownJson);
            List<BudgetItem> items = new ArrayList<>();
            
            for (JsonNode itemNode : budgetArray) {
                String category = itemNode.get("category").asText();
                String description = itemNode.get("description").asText();
                BigDecimal amount = BigDecimal.valueOf(itemNode.get("amount").asDouble());
                
                items.add(new BudgetItem(category, description, amount));
            }
            
            return items;
        } catch (Exception e) {
            log.warn("Failed to parse budget breakdown: {}", budgetBreakdownJson, e);
            return List.of();
        }
    }
    
    private record PriceBands(
        BigDecimal lowThreshold,
        BigDecimal mediumLow,
        BigDecimal mediumHigh,
        BigDecimal highThreshold
    ) {}
    
    private PriceBands calculateRelativePriceBands(BigDecimal totalBudget) {
        // Calculate relative price bands based on percentage of total budget
        // LOW: 0-10% of budget (for high budgets, this allows more room for medium/high)
        // MEDIUM: 10-40% of budget  
        // HIGH: 40%+ of budget (for truly expensive experiences)
        
        BigDecimal lowThreshold = totalBudget.multiply(BigDecimal.valueOf(0.10));
        BigDecimal mediumLow = lowThreshold;
        BigDecimal mediumHigh = totalBudget.multiply(BigDecimal.valueOf(0.40));
        BigDecimal highThreshold = mediumHigh;
        
        return new PriceBands(lowThreshold, mediumLow, mediumHigh, highThreshold);
    }
    
    private String buildCategoryFocus(SpendingCategory category) {
        return switch (category) {
            case TRAVEL_VACATION -> """
                Focus on travel destinations, vacation experiences, hotels, resorts, flights, and tourism.
                Include both international destinations and unique travel experiences within Sweden.
                Emphasize journeys, cultural immersion, and vacation memories.""";
            
            case LUXURY_THINGS -> """
                Focus on high-end products, luxury goods, premium services, and exclusive experiences.
                Include luxury cars, watches, jewelry, designer items, premium memberships, and elite services.
                Emphasize quality, exclusivity, and status symbols.""";
            
            case HEALTH_WELLNESS -> """
                Focus on physical health, mental wellbeing, fitness, nutrition, and self-care.
                Include spa treatments, medical procedures, fitness programs, wellness retreats, and health coaching.
                Emphasize improving quality of life and personal wellbeing.""";
            
            case SOCIAL_LIFESTYLE -> """
                Focus on social activities, entertainment, lifestyle enhancements, and experiences with others.
                Include parties, events, social clubs, networking, lifestyle upgrades, and community activities.
                Emphasize social connections and lifestyle improvements.""";
            
            case MENTAL_EMOTIONAL -> """
                Focus on mental health, personal development, therapy, coaching, and emotional wellbeing.
                Include meditation retreats, therapy sessions, personal coaching, mindfulness programs, and stress relief.
                Emphasize inner peace, mental clarity, and emotional growth.""";
            
            case SMALL_LUXURY -> """
                Focus on affordable luxury treats, small indulgences, and everyday pleasures.
                Include premium food, wine, small luxury items, comfort upgrades, and accessible treats.
                Emphasize quality over quantity and everyday luxury moments.""";
            
            case FREEDOM_COMFORT -> """
                Focus on experiences and purchases that provide freedom, comfort, and convenience.
                Include time-saving services, comfort improvements, freedom from obligations, and peace of mind.
                Emphasize reducing stress and increasing personal freedom.""";
            
            case OPTIONAL_ADDONS -> """
                Focus on supplementary experiences, add-on services, upgrades, and extra features.
                Include premium versions of existing things, service upgrades, bonus experiences, and enhancements.
                Emphasize improving existing experiences rather than entirely new ones.""";
        };
    }
    
    private String buildModeInstructions(SuggestionMode mode) {
        return switch (mode) {
            case PROVEN -> """
                Generate POPULAR and WELL-KNOWN bucket list items that most people would enjoy and find appealing.
                Focus on tried-and-true experiences that have broad appeal and are commonly desired.
                Examples: Visit famous landmarks, take popular courses, try well-known adventures.
                Avoid overly unique or niche suggestions - stick to crowd-pleasers and classic experiences.""";
            
            case CREATIVE -> """
                Generate UNIQUE and UNCOMMON bucket list items that people generally wouldn't think of on their own.
                Focus on surprising, creative, and imaginative experiences that stand out from typical bucket lists.
                Examples: Sleep in unusual accommodations, try obscure skills, create custom experiences.
                Be bold and creative - suggest things that would make people say "I never thought of that!".""";
        };
    }
}