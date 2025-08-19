package com.bucketlist.domain;

import com.bucketlist.api.SuggestionDto;
import com.bucketlist.api.BudgetItem;
import com.bucketlist.api.SuggestionFeedbackContext;
import com.bucketlist.api.RejectedSuggestionDto;
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
            // Get feedback history for context
            List<SuggestionFeedbackContext> feedbackHistory = getFeedbackHistoryForProfile(profileId);
            
            String prompt = buildSuggestionPrompt(profile, category, mode, count, feedbackHistory);
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
        
        var suggestions = suggestionRepository.findAllById(suggestionIds);
        var suggestionMap = suggestions.stream()
            .collect(Collectors.toMap(Suggestion::getId, s -> s));
        
        // Create feedback map for ordering by accepted date
        var feedbackMap = acceptedFeedback.stream()
            .collect(Collectors.toMap(SuggestionFeedback::getSuggestionId, f -> f));
        
        return suggestionIds.stream()
            .map(id -> {
                var suggestion = suggestionMap.get(id);
                if (suggestion != null) {
                    return new SuggestionDto(
                        suggestion.getId(),
                        suggestion.getTitle(),
                        suggestion.getDescription(),
                        parseBudgetBreakdown(suggestion.getBudgetBreakdownJson())
                    );
                }
                return null;
            })
            .filter(Objects::nonNull)
            .sorted((a, b) -> {
                var feedbackA = feedbackMap.get(a.id());
                var feedbackB = feedbackMap.get(b.id());
                return feedbackB.getCreatedAt().compareTo(feedbackA.getCreatedAt()); // Most recent first
            })
            .collect(Collectors.toList());
    }
    
    public List<RejectedSuggestionDto> getRejectedSuggestions(UUID profileId) {
        log.debug("Getting rejected suggestions for profile {}", profileId);
        
        var rejectedFeedback = feedbackRepository.findByProfileIdAndVerdict(profileId, Verdict.REJECT);
        
        var suggestionIds = rejectedFeedback.stream()
            .map(SuggestionFeedback::getSuggestionId)
            .collect(Collectors.toSet());
        
        var suggestions = suggestionRepository.findAllById(suggestionIds);
        var suggestionMap = suggestions.stream()
            .collect(Collectors.toMap(Suggestion::getId, s -> s));
        
        // Create feedback map for quick lookup
        var feedbackMap = rejectedFeedback.stream()
            .collect(Collectors.toMap(SuggestionFeedback::getSuggestionId, f -> f));
        
        return suggestionIds.stream()
            .map(id -> {
                var suggestion = suggestionMap.get(id);
                var feedback = feedbackMap.get(id);
                if (suggestion != null && feedback != null) {
                    return new RejectedSuggestionDto(
                        suggestion.getId(),
                        suggestion.getTitle(),
                        suggestion.getDescription(),
                        parseBudgetBreakdown(suggestion.getBudgetBreakdownJson()),
                        feedback.getReason(),
                        feedback.getCreatedAt()
                    );
                }
                return null;
            })
            .filter(Objects::nonNull)
            .sorted((a, b) -> b.rejectedAt().compareTo(a.rejectedAt())) // Most recent first
            .collect(Collectors.toList());
    }
    
    public List<SuggestionFeedbackContext> getFeedbackHistoryForProfile(UUID profileId) {
        log.debug("Retrieving feedback history for profile {}", profileId);
        
        // Get all feedback for this profile, ordered by creation time (most recent first)
        var allFeedback = feedbackRepository.findByProfileIdOrderByCreatedAtDesc(profileId);
        
        // Limit to last 20 feedback items to avoid overwhelming the AI prompt
        var recentFeedback = allFeedback.stream().limit(20).collect(Collectors.toList());
        
        // Get suggestion details for each feedback
        var suggestionIds = recentFeedback.stream()
            .map(SuggestionFeedback::getSuggestionId)
            .collect(Collectors.toSet());
        
        var suggestions = suggestionRepository.findAllById(suggestionIds);
        var suggestionMap = suggestions.stream()
            .collect(Collectors.toMap(Suggestion::getId, s -> s));
        
        // Build feedback context list
        return recentFeedback.stream()
            .map(feedback -> {
                var suggestion = suggestionMap.get(feedback.getSuggestionId());
                if (suggestion != null) {
                    return new SuggestionFeedbackContext(
                        suggestion.getTitle(),
                        suggestion.getDescription(),
                        feedback.getVerdict(),
                        feedback.getReason(),
                        feedback.getCreatedAt()
                    );
                }
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    private String buildSuggestionPrompt(UserProfile profile, SpendingCategory spendingCategory, SuggestionMode suggestionMode, int count, List<SuggestionFeedbackContext> feedbackHistory) {
        String categoryFocus = buildCategoryFocus(spendingCategory);
        String modeInstructions = buildModeInstructions(suggestionMode);
        String feedbackContext = buildFeedbackContextPrompt(feedbackHistory);
        String budgetGuidance = buildBudgetGuidance(profile.getCapital());
        
        return String.format("""
            Generate %d bucket list suggestions for this user profile:
            
            Basic Info:
            - Age: %d
            - Gender: %s  
            - Capital: %s SEK
            
            Personality: %s
            Preferences: %s
            Prior Experiences: %s
            
            %s
            
            %s
            
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
                "budgetBreakdown": [
                  {"category": "Transport", "description": "Round-trip flights", "amount": 8000},
                  {"category": "Accommodation", "description": "4 nights hotel", "amount": 12000},
                  {"category": "Activities", "description": "Tours and experiences", "amount": 3000},
                  {"category": "Food", "description": "Meals and dining", "amount": 2000}
                ]
              }
            ]
            
            CRITICAL: Return only the JSON array, no other text.
            
            LANGUAGE REQUIREMENT: All suggestion titles and descriptions MUST be in English. Do not use Swedish or any other language.
            """,
            count, profile.getAge(), profile.getGender(), profile.getCapital(),
            profile.getPersonalityJson(), profile.getPreferencesJson(), profile.getPriorExperiencesJson(),
            feedbackContext,
            budgetGuidance,
            spendingCategory.getDisplayName(), categoryFocus,
            suggestionMode.getDisplayName(), modeInstructions,
            spendingCategory.getDisplayName(),
            count);
    }
    
    private String buildFeedbackContextPrompt(List<SuggestionFeedbackContext> feedbackHistory) {
        if (feedbackHistory.isEmpty()) {
            return "PREVIOUS FEEDBACK CONTEXT: No previous suggestions available for learning.";
        }
        
        var acceptedSuggestions = feedbackHistory.stream()
            .filter(f -> f.verdict() == Verdict.ACCEPT)
            .collect(Collectors.toList());
        
        var rejectedSuggestions = feedbackHistory.stream()
            .filter(f -> f.verdict() == Verdict.REJECT)
            .collect(Collectors.toList());
        
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("PREVIOUS FEEDBACK CONTEXT:\n");
        
        if (!acceptedSuggestions.isEmpty()) {
            contextBuilder.append("User has previously ACCEPTED these suggestions:\n");
            for (SuggestionFeedbackContext feedback : acceptedSuggestions.stream().limit(5).collect(Collectors.toList())) {
                contextBuilder.append("- \"").append(feedback.title()).append("\": ").append(feedback.description());
                if (feedback.reason() != null && !feedback.reason().trim().isEmpty()) {
                    contextBuilder.append(" - Positive feedback: ").append(feedback.reason());
                }
                contextBuilder.append("\n");
            }
            contextBuilder.append("\n");
        }
        
        if (!rejectedSuggestions.isEmpty()) {
            contextBuilder.append("User has previously REJECTED these suggestions:\n");
            for (SuggestionFeedbackContext feedback : rejectedSuggestions.stream().limit(10).collect(Collectors.toList())) {
                contextBuilder.append("- \"").append(feedback.title()).append("\": ").append(feedback.description());
                if (feedback.reason() != null && !feedback.reason().trim().isEmpty()) {
                    contextBuilder.append(" - Rejection reason: ").append(feedback.reason());
                } else {
                    contextBuilder.append(" - No specific reason provided");
                }
                contextBuilder.append("\n");
            }
            contextBuilder.append("\n");
        }
        
        contextBuilder.append("""
            LEARNING INSTRUCTIONS:
            - Carefully avoid suggesting similar items to rejected suggestions
            - Pay special attention to specific rejection reasons and avoid those patterns
            - Focus on themes and characteristics from accepted suggestions
            - If user rejected expensive suggestions due to cost, consider suggesting more lower price band options
            - If user rejected specific activities, locations, or types of experiences, avoid similar ones
            - If user provided positive feedback on accepted suggestions, incorporate those themes
            - Learn from the patterns - what the user likes and dislikes
            """);
        
        return contextBuilder.toString();
    }
    
    private String buildBudgetGuidance(BigDecimal capital) {
        double capitalAmount = capital.doubleValue();
        
        if (capitalAmount < 50000) {
            return """
                BUDGET SCALING GUIDANCE (Low Budget: %s SEK):
                - Focus on LOCAL and REGIONAL experiences primarily (80%% of suggestions)
                - Occasional national experiences (20%% of suggestions)
                - Base cost range: 1,000-15,000 SEK per suggestion
                - Mix: 40%% low cost (1k-5k), 40%% moderate (5k-10k), 20%% higher (10k-15k)
                - Examples: Weekend trips, local courses, equipment purchases, regional adventures
                - Avoid international travel unless budget-friendly (hostels, low-cost airlines)
                """.formatted(capital);
        } else if (capitalAmount < 200000) {
            return """
                BUDGET SCALING GUIDANCE (Medium Budget: %s SEK):
                - Mix of local (40%%), national (40%%), and international (20%%) experiences
                - Base cost range: 5,000-60,000 SEK per suggestion
                - Mix: 30%% moderate (5k-20k), 50%% higher (20k-40k), 20%% premium (40k-60k)
                - Include some premium experiences to match budget capacity
                - Examples: European trips, quality courses/certifications, premium equipment, week-long adventures
                - Sprinkle in occasional luxury touches (business class, 4-star hotels)
                """.formatted(capital);
        } else if (capitalAmount < 500000) {
            return """
                BUDGET SCALING GUIDANCE (High Budget: %s SEK):
                - Premium and luxury experiences should be the norm (60%% of suggestions)
                - Global travel opportunities with quality accommodations
                - Base cost range: 15,000-150,000 SEK per suggestion
                - Mix: 20%% moderate (15k-40k), 50%% premium (40k-80k), 30%% luxury (80k-150k)
                - Include exclusive experiences: private tours, luxury accommodations, first-class travel
                - Examples: Luxury safaris, private yacht charters, exclusive resorts, high-end courses with celebrity instructors
                - Don't be afraid to suggest expensive experiences - the user has the budget for them
                """.formatted(capital);
        } else {
            return """
                BUDGET SCALING GUIDANCE (Ultra-High Budget: %s SEK):
                - Ultra-luxury and exclusive experiences should dominate (70%% of suggestions)
                - Once-in-a-lifetime opportunities that money can barely buy
                - Base cost range: 50,000-500,000+ SEK per suggestion
                - Mix: 30%% premium (50k-100k), 40%% luxury (100k-200k), 30%% ultra-luxury (200k+)
                - Think: Private islands, space tourism, custom superyacht experiences, private jets
                - Examples: Antarctic expedition with private guide, custom Michelin-starred dining experiences, 
                  exclusive access to historical sites, private concerts with famous artists
                - The sky is the limit - suggest truly extraordinary experiences that justify the budget
                """.formatted(capital);
        }
    }
    
    private List<SuggestionDto> parseSuggestionsFromResponse(String response, UserProfile profile) {
        try {
            JsonNode suggestionsArray = objectMapper.readTree(response.trim());
            List<SuggestionDto> results = new ArrayList<>();
            
            for (JsonNode suggestionNode : suggestionsArray) {
                try {
                    String title = suggestionNode.get("title").asText();
                    String description = suggestionNode.get("description").asText();
                    
                    
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
                    suggestion.setBudgetBreakdownJson(budgetBreakdownJson);
                    suggestion.setContentHash(contentHash);
                    
                    suggestion = suggestionRepository.save(suggestion);
                    
                    // Parse budget breakdown for DTO
                    List<BudgetItem> budgetItems = parseBudgetBreakdown(budgetBreakdownJson);
                    
                    results.add(new SuggestionDto(
                        suggestion.getId(),
                        suggestion.getTitle(),
                        suggestion.getDescription(),
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
    
    
    private String buildCategoryFocus(SpendingCategory category) {
        return switch (category) {
            case TRAVEL_VACATION -> """
                Focus EXCLUSIVELY on travel destinations, vacation experiences, accommodations, transportation, and tourism activities.
                
                MUST INCLUDE examples like:
                - International destinations: Japan cultural tour, African safari, European city breaks, Maldives resort stay
                - Domestic travel: Swedish Lapland northern lights, Gothenburg archipelago island hopping, Skåne countryside retreat
                - Adventure travel: Patagonia hiking expedition, Iceland volcano tour, Nepal mountain trekking
                - Luxury travel: Orient Express journey, private island resort, luxury cruise experiences
                - Cultural travel: Art tours in Italy, wine regions of France, historical sites in Egypt
                
                DO NOT include: Shopping, permanent purchases, courses unrelated to travel, local activities that aren't vacation-focused.
                EMPHASIZE: Transportation, accommodations, guided tours, vacation activities, cultural immersion.""";
            
            case LUXURY_THINGS -> """
                Focus EXCLUSIVELY on high-end products, luxury goods, premium services, and exclusive material possessions.
                
                MUST INCLUDE examples like:
                - Luxury vehicles: Ferrari sports car, custom yacht, private jet shares, vintage classic car
                - High-end jewelry: Rolex watch, diamond jewelry, custom-made pieces, designer collections
                - Premium fashion: Hermès handbags, bespoke suits, designer shoe collections, luxury wardrobe
                - Exclusive services: Private chef, personal stylist, luxury concierge membership, VIP access passes
                - Art & collectibles: Original artwork, rare collectibles, custom furniture, luxury home items
                
                DO NOT include: Experiences, travel, services without tangible luxury goods.
                EMPHASIZE: Quality, exclusivity, craftsmanship, status symbols, permanent acquisitions.""";
            
            case HEALTH_WELLNESS -> """
                Focus EXCLUSIVELY on physical health, mental wellbeing, fitness, medical care, and body/mind optimization.
                
                MUST INCLUDE examples like:
                - Medical & dental: Advanced health screenings, cosmetic procedures, dental implants, preventive treatments
                - Fitness & training: Personal trainer sessions, specialized equipment, gym memberships, sports coaching
                - Spa & wellness: Luxury spa retreats, massage therapy programs, wellness resort stays, meditation retreats
                - Nutrition & diet: Nutritionist consultations, organic meal plans, supplement programs, cooking classes
                - Mental health: Therapy sessions, life coaching, stress management programs, mindfulness training
                
                DO NOT include: General lifestyle improvements, travel (unless specifically wellness-focused), luxury goods.
                EMPHASIZE: Physical improvement, mental clarity, professional healthcare, wellness optimization.""";
            
            case SOCIAL_LIFESTYLE -> """
                Focus EXCLUSIVELY on social activities, entertainment, networking, and lifestyle enhancements involving others.
                
                MUST INCLUDE examples like:
                - Social events: Private party hosting, exclusive club memberships, networking events, social gatherings
                - Entertainment: Concert VIP packages, theater season tickets, exclusive event access, entertainment subscriptions
                - Hobbies & clubs: Golf club membership, wine tasting societies, book clubs, hobby group participation
                - Community activities: Charity event organization, community involvement, local group leadership
                - Lifestyle upgrades: Home entertainment systems, social space improvements, hosting capabilities
                
                DO NOT include: Solo activities, pure travel, health services, individual luxury purchases.
                EMPHASIZE: Social interaction, community building, shared experiences, entertainment, networking.""";
            
            case MENTAL_EMOTIONAL -> """
                Focus EXCLUSIVELY on mental health, emotional development, personal growth, and psychological wellbeing.
                
                MUST INCLUDE examples like:
                - Therapy & counseling: Individual therapy, couples counseling, family therapy, specialized treatment programs
                - Personal development: Life coaching, leadership training, confidence building, communication skills
                - Mindfulness & meditation: Meditation retreats, mindfulness courses, spiritual guidance, contemplative practices
                - Emotional healing: Grief counseling, trauma therapy, emotional intelligence training, stress management
                - Self-discovery: Personality assessments, self-reflection retreats, journaling programs, personal insight work
                
                DO NOT include: Physical health, material purchases, travel (unless specifically mental health focused).
                EMPHASIZE: Inner growth, emotional intelligence, mental clarity, psychological healing, self-awareness.""";
            
            case SMALL_LUXURY -> """
                Focus EXCLUSIVELY on affordable luxury treats, premium everyday items, and accessible indulgences.
                
                MUST INCLUDE examples like:
                - Premium food & drink: Fine wine collections, artisanal coffee subscriptions, gourmet ingredient sets, premium tea
                - Comfort upgrades: High-quality bedding, luxury bath products, premium skincare, comfort accessories
                - Small luxury items: Quality leather goods, premium stationery, artisanal crafts, boutique purchases
                - Daily indulgences: Premium chocolate subscriptions, spa day packages, massage sessions, beauty treatments
                - Quality upgrades: Better versions of everyday items, premium brands, enhanced daily experiences
                
                DO NOT include: Major purchases, expensive travel, large luxury goods, significant investments.
                EMPHASIZE: Daily pleasures, quality over quantity, accessible luxury, everyday improvements.""";
            
            case FREEDOM_COMFORT -> """
                Focus EXCLUSIVELY on purchases and services that increase personal freedom, reduce obligations, and enhance comfort.
                
                MUST INCLUDE examples like:
                - Time-saving services: House cleaning, meal delivery, personal assistant, maintenance services
                - Comfort improvements: Ergonomic furniture, climate control, comfort technology, relaxation equipment
                - Freedom purchases: Debt elimination, financial planning, automated systems, convenience solutions
                - Stress reduction: Organization services, simplification consulting, workflow optimization, peace-of-mind services
                - Convenience upgrades: Smart home technology, transportation solutions, efficiency improvements
                
                DO NOT include: Entertainment, social activities, luxury goods for status, travel experiences.
                EMPHASIZE: Time freedom, reduced stress, increased convenience, personal comfort, life simplification.""";
            
            case OPTIONAL_ADDONS -> """
                Focus EXCLUSIVELY on upgrades, enhancements, add-on services, and premium versions of existing experiences.
                
                MUST INCLUDE examples like:
                - Service upgrades: First-class flights, premium hotel rooms, VIP experiences, enhanced memberships
                - Product enhancements: Extended warranties, premium features, upgrade packages, additional accessories
                - Experience add-ons: Private guides for trips, exclusive access, behind-the-scenes tours, enhanced packages
                - Membership upgrades: Premium tiers, additional benefits, exclusive access levels, enhanced services
                - Optional extras: Insurance upgrades, extended services, bonus features, supplementary experiences
                
                DO NOT include: Entirely new purchases, base-level experiences, standalone products without upgrade context.
                EMPHASIZE: Enhancement of existing, premium versions, value-added services, upgrade opportunities.""";
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