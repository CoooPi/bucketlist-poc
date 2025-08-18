package com.bucketlist.domain;

import com.bucketlist.api.CreateProfileRequest;
import com.bucketlist.api.CreateProfileResponse;
import com.bucketlist.infra.UserProfileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {
    
    private final UserProfileRepository userProfileRepository;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    
    public CreateProfileResponse createProfile(CreateProfileRequest request) {
        log.info("Creating profile for user: age={}, gender={}, capital={}, mode={}", 
                request.age(), request.gender(), request.capital(), request.mode());
        
        // Create basic profile
        var profile = new UserProfile();
        profile.setGender(request.gender());
        profile.setAge(request.age());
        profile.setCapital(request.capital());
        profile.setMode(request.mode());
        
        // Generate AI-enhanced profile data
        try {
            String enhancedProfileJson = generateEnhancedProfile(request);
            log.debug("Generated enhanced profile: {}", enhancedProfileJson);
            
            // Parse and store the JSON components
            JsonNode profileData = objectMapper.readTree(enhancedProfileJson);
            profile.setPersonalityJson(profileData.get("personality").toString());
            profile.setPreferencesJson(profileData.get("preferences").toString());
            profile.setPriorExperiencesJson(profileData.get("priorExperiences").toString());
            
        } catch (Exception e) {
            log.error("Failed to generate enhanced profile, using defaults", e);
            // Set minimal defaults if AI fails
            profile.setPersonalityJson("{\"openness\":\"medium\",\"extraversion\":\"medium\",\"riskTolerance\":\"medium\"}");
            profile.setPreferencesJson("{\"themes\":[\"travel\",\"culture\"],\"travelStyle\":\"mid\",\"timeWindows\":[\"weekend\"]}");
            profile.setPriorExperiencesJson("[]");
        }
        
        // Save profile
        profile = userProfileRepository.save(profile);
        log.info("Profile created with ID: {}", profile.getId());
        
        // Extract summary for response
        String summary = extractSummary(profile);
        
        return new CreateProfileResponse(
            profile.getId(),
            summary,
            profile.getMode()
        );
    }
    
    private String generateEnhancedProfile(CreateProfileRequest request) {
        String prompt = buildProfilePrompt(request);
        
        return chatClient.prompt()
            .user(prompt)
            .call()
            .content();
    }
    
    private String buildProfilePrompt(CreateProfileRequest request) {
        return String.format("""
            Input:
            - Gender: %s
            - Age: %d
            - Capital: %s SEK (leisure budget)
            - Mode: %s
            
            Generate a realistic Swedish user profile as valid JSON only:
            {
              "personality": {
                "openness": "low|medium|high",
                "extraversion": "low|medium|high",
                "riskTolerance": "low|medium|high"
              },
              "preferences": {
                "themes": ["outdoors","culture","learning","sports","wellness","family","food","tech"],
                "travelStyle": "budget|mid|premium",
                "timeWindows": ["weekend","1-week","2-weeks+"]
              },
              "priorExperiences": [
                {"title": "Example experience", "category": "Adventure", "year": 2023}
              ],
              "summary": "1-2 sentences in Swedish describing this person"
            }
            
            Rules:
            - Make personality realistic for age %d and capital %s SEK
            - If capital is low (<20k), use "budget" travelStyle
            - If capital is high (>80k), can use "premium" travelStyle  
            - Include 2-4 relevant prior experiences
            - Keep themes array to 3-5 items
            - Return only valid JSON, no other text
            """, 
            request.gender(), request.age(), request.capital(), request.mode(),
            request.age(), request.capital());
    }
    
    private String extractSummary(UserProfile profile) {
        try {
            JsonNode profileData = objectMapper.readTree(profile.getPersonalityJson());
            return profileData.has("summary") ? profileData.get("summary").asText() : 
                   "Profile created successfully";
        } catch (Exception e) {
            return "Profile created successfully";
        }
    }
}