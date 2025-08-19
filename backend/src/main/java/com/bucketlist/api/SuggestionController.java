package com.bucketlist.api;

import com.bucketlist.domain.SuggestionService;
import com.bucketlist.domain.SpendingCategory;
import com.bucketlist.domain.SuggestionMode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/suggestions")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
@Slf4j
public class SuggestionController {
    
    private final SuggestionService suggestionService;
    
    @PostMapping("/refill")
    public ResponseEntity<RefillResponse> refillSuggestions(
            @RequestParam UUID profileId,
            @RequestParam SpendingCategory category,
            @RequestParam(defaultValue = "PROVEN") SuggestionMode mode,
            @Valid @RequestBody(required = false) RefillRequest request) {
        
        int batchSize = (request != null && request.batchSize() != null) ? request.batchSize() : 5;
        
        log.info("Refilling suggestions for profile {} with batch size {}, category {}, mode {}", 
                profileId, batchSize, category, mode);
        
        List<SuggestionDto> suggestions = suggestionService.generateSuggestions(profileId, category, mode, batchSize);
        
        return ResponseEntity.ok(new RefillResponse(suggestions));
    }
    
    @GetMapping("/next")
    public ResponseEntity<SuggestionDto> getNextSuggestion(
            @RequestParam UUID profileId,
            @RequestParam SpendingCategory category,
            @RequestParam(defaultValue = "PROVEN") SuggestionMode mode) {
        
        log.info("Getting next suggestion for profile {} with category {}, mode {}", profileId, category, mode);
        
        return suggestionService.getNextSuggestion(profileId, category, mode)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.noContent().build());
    }
    
    @PostMapping("/feedback")
    public ResponseEntity<Void> submitFeedback(@Valid @RequestBody FeedbackRequest request) {
        log.info("Submitting feedback for suggestion {} from profile {}", 
                request.suggestionId(), request.profileId());
        
        suggestionService.recordFeedback(
            request.profileId(),
            request.suggestionId(),
            request.verdict(),
            request.reason()
        );
        
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/accepted")
    public ResponseEntity<List<SuggestionDto>> getAcceptedSuggestions(@RequestParam UUID profileId) {
        log.info("Getting accepted suggestions for profile {}", profileId);
        
        List<SuggestionDto> accepted = suggestionService.getAcceptedSuggestions(profileId);
        return ResponseEntity.ok(accepted);
    }
    
    @GetMapping("/rejected")
    public ResponseEntity<List<RejectedSuggestionDto>> getRejectedSuggestions(@RequestParam UUID profileId) {
        log.info("Getting rejected suggestions for profile {}", profileId);
        
        List<RejectedSuggestionDto> rejected = suggestionService.getRejectedSuggestions(profileId);
        return ResponseEntity.ok(rejected);
    }
}