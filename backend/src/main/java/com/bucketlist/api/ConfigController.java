package com.bucketlist.api;

import com.bucketlist.domain.ApiKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "http://localhost:5173")
public class ConfigController {
    
    private final ApiKeyService apiKeyService;
    
    @Autowired
    public ConfigController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }
    
    @PostMapping("/api-key")
    public ResponseEntity<ApiKeyResponse> setApiKey(@RequestBody ApiKeyRequest request) {
        try {
            boolean isValid = apiKeyService.validateAndStoreApiKey(request.getApiKey());
            
            if (isValid) {
                return ResponseEntity.ok(new ApiKeyResponse(true, "API key validated and stored successfully"));
            } else {
                return ResponseEntity.badRequest().body(new ApiKeyResponse(false, "Invalid API key"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiKeyResponse(false, "API key validation failed: " + e.getMessage()));
        }
    }
    
    @GetMapping("/api-key/status")
    public ResponseEntity<ApiKeyStatusResponse> getApiKeyStatus() {
        boolean hasValidKey = apiKeyService.hasValidApiKey();
        return ResponseEntity.ok(new ApiKeyStatusResponse(hasValidKey));
    }
    
    @DeleteMapping("/api-key")
    public ResponseEntity<ApiKeyResponse> clearApiKey() {
        apiKeyService.clearApiKey();
        return ResponseEntity.ok(new ApiKeyResponse(true, "API key cleared successfully"));
    }
    
    public static class ApiKeyRequest {
        private String apiKey;
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }
    
    public static class ApiKeyResponse {
        private boolean valid;
        private String message;
        
        public ApiKeyResponse(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    public static class ApiKeyStatusResponse {
        private boolean hasValidKey;
        
        public ApiKeyStatusResponse(boolean hasValidKey) {
            this.hasValidKey = hasValidKey;
        }
        
        public boolean isHasValidKey() { return hasValidKey; }
        public void setHasValidKey(boolean hasValidKey) { this.hasValidKey = hasValidKey; }
    }
}