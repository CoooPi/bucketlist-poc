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
    
    public List<SuggestionDto> generateSuggestions(UUID profileId, int count) {
        log.info("Generating {} suggestions for profile {}", count, profileId);
        
        var profile = userProfileRepository.findById(profileId)
            .orElseThrow(() -> new RuntimeException("Profile not found: " + profileId));
        
        try {
            String prompt = buildSuggestionPrompt(profile, count);
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
    
    public Optional<SuggestionDto> getNextSuggestion(UUID profileId) {
        var suggestions = suggestionRepository.findUnratedByProfileId(profileId);
        
        if (suggestions.isEmpty()) {
            // Try to generate more suggestions
            var newSuggestions = generateSuggestions(profileId, 5);
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
            suggestion.getCategory(),
            suggestion.getEstimatedCost(),
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
            .map(s -> new SuggestionDto(s.getId(), s.getTitle(), s.getDescription(), s.getPriceBand(), s.getCategory(), 
                     s.getEstimatedCost(), parseBudgetBreakdown(s.getBudgetBreakdownJson())))
            .collect(Collectors.toList());
    }
    
    private String buildSuggestionPrompt(UserProfile profile, int count) {
        var priceBands = calculateRelativePriceBands(profile.getCapital());
        
        return String.format("""
            Generate %d bucket list suggestions for this Swedish user profile:
            
            Basic Info:
            - Age: %d
            - Gender: %s
            - Capital: %s SEK
            - Mode: %s
            
            Personality: %s
            Preferences: %s
            Prior Experiences: %s
            
            Return a JSON array of exactly %d suggestions:
            [
              {
                "title": "Short engaging title (max 70 chars)",
                "description": "Detailed description (max 240 chars)",
                "priceBand": "LOW|MEDIUM|HIGH",
                "category": "TRAVEL|ADVENTURE|LEARNING|WELLNESS|FAMILY|OTHER",
                "estimatedCost": 25000,
                "budgetBreakdown": [
                  {"category": "Transport", "description": "Round-trip flights", "amount": 8000},
                  {"category": "Accommodation", "description": "4 nights hotel", "amount": 12000},
                  {"category": "Activities", "description": "Tours and experiences", "amount": 3000},
                  {"category": "Food", "description": "Meals and dining", "amount": 2000}
                ]
              }
            ]
            
            IMPORTANT - Valid category values ONLY (use these exact strings):
            - TRAVEL: trips, destinations, journeys, hotels, flights
            - ADVENTURE: outdoor activities, sports, challenges, hiking, climbing  
            - LEARNING: courses, skills, education, workshops, classes
            - WELLNESS: health, fitness, self-care, yoga, meditation
            - FAMILY: activities with loved ones, kids, relatives
            - OTHER: food experiences, culture, art, music, festivals, anything else
            
            CRITICAL MAPPING RULES:
            - Food/culinary activities → OTHER
            - Culture/art/music → OTHER  
            - Museums/galleries → OTHER
            - Festivals/events → OTHER
            
            Price band rules (relative to user budget of %s SEK):
            - LOW: < %s SEK (up to 10%% of budget)
            - MEDIUM: %s - %s SEK (10-40%% of budget)
            - HIGH: > %s SEK (40%%+ of budget - BE BOLD!)
            
            Mode rules:
            - CREATIVE: Include surprising but feasible ideas
            - ALIGNED: Focus on conventional popular activities
            
            GLOBAL DIVERSITY REQUIREMENTS:
            - Suggest experiences from around the world, not just Sweden
            - Include international destinations (Asia, Americas, Africa, Europe, Oceania)
            - Balance local Swedish experiences with global bucket list items
            - Consider worldwide cultural experiences, natural wonders, and adventures
            - Only 20%% of suggestions should be Sweden-specific
            
            BUDGET SCALING REQUIREMENTS:
            - For LOW budgets (< %s SEK): Focus on accessible local experiences, short trips, courses
            - For MEDIUM budgets (%s - %s SEK): Include European trips, longer adventures, premium experiences
            - For HIGH budgets (> %s SEK): GO ABSOLUTELY WILD! Suggest ultra-luxury, exclusive, once-in-a-lifetime experiences
            - High budget examples: Private space tourism (Virgin Galactic/Blue Origin), entire cruise ship charters, 
              buying private islands for vacations, commissioning custom superyachts, private jet world tours with 
              presidential suites, exclusive access to closed historical sites, hiring famous chefs for private dinners,
              custom expedition to Antarctica or Everest base camps, renting entire luxury resorts, private concerts 
              by famous artists, bespoke adventure experiences money can't usually buy
            - For budgets over 1 million SEK: Suggest experiences costing 500K-2M+ SEK without hesitation
            - For budgets over 3 million SEK: Dream bigger - suggest 1M-3M+ SEK experiences
            - Match the extravagance level to the budget - wealthy users should get wealthy suggestions!
            
            BUDGET CALCULATION RULES:
            - estimatedCost must be realistic and match the priceBand
            - budgetBreakdown items must add up to estimatedCost
            - Include relevant categories: Transport, Accommodation, Activities, Food, Equipment, Other, etc.
            - For local activities, focus on Activities/Equipment costs
            - For travel, include flights/transport from Sweden
            - Use "Other" category for miscellaneous expenses like visas, insurance, tips
            - Scale suggestions to user's budget - higher budgets should get more luxurious/exclusive experiences
            
            CRITICAL: Use only the exact category values listed above. Return only the JSON array, no other text.
            """,
            count, profile.getAge(), profile.getGender(), profile.getCapital(), profile.getMode(),
            profile.getPersonalityJson(), profile.getPreferencesJson(), profile.getPriorExperiencesJson(),
            count, profile.getCapital(), priceBands.lowThreshold(), priceBands.mediumLow(), 
            priceBands.mediumHigh(), priceBands.highThreshold(),
            priceBands.lowThreshold(), priceBands.mediumLow(), priceBands.mediumHigh(), priceBands.highThreshold());
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
                    
                    // Parse category with fallback
                    Category category;
                    try {
                        category = Category.valueOf(suggestionNode.get("category").asText());
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid category: {}, defaulting to OTHER", suggestionNode.get("category").asText());
                        category = Category.OTHER;
                    }
                    
                    // Parse budget information
                    BigDecimal estimatedCost = suggestionNode.has("estimatedCost") ? 
                        BigDecimal.valueOf(suggestionNode.get("estimatedCost").asDouble()) : 
                        BigDecimal.ZERO;
                    
                    String budgetBreakdownJson = suggestionNode.has("budgetBreakdown") ?
                        suggestionNode.get("budgetBreakdown").toString() :
                        "[]";
                    
                    // Create content hash for deduplication
                    String contentHash = createContentHash(title, category);
                    
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
                    suggestion.setCategory(category);
                    suggestion.setEstimatedCost(estimatedCost);
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
                        suggestion.getCategory(),
                        suggestion.getEstimatedCost(),
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
    
    
    private String createContentHash(String title, Category category) {
        try {
            String content = (title.toLowerCase().trim() + category.toString()).toLowerCase();
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
}