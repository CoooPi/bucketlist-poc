package com.bucketlist.domain;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Service
public class ApiKeyService {
    
    private String storedApiKey;
    private ChatClient validatedChatClient;
    
    // Constructor to load API key from environment on startup
    public ApiKeyService() {
        String envApiKey = System.getenv("OPENAI_API_KEY");
        if (envApiKey != null && !envApiKey.trim().isEmpty()) {
            if (validateAndStoreApiKey(envApiKey)) {
                System.out.println("Successfully loaded API key from OPENAI_API_KEY environment variable");
            } else {
                System.err.println("Invalid API key found in OPENAI_API_KEY environment variable");
            }
        }
    }
    
    public boolean validateAndStoreApiKey(String apiKey) {
        try {
            // Validate the API key by making a simple call to OpenAI API
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Content-Type", "application/json");
            
            String requestBody = "{\n" +
                "  \"model\": \"gpt-4o\",\n" +
                "  \"messages\": [{\"role\": \"user\", \"content\": \"Hello\"}],\n" +
                "  \"max_tokens\": 5\n" +
                "}";
            
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                "https://api.openai.com/v1/chat/completions", 
                HttpMethod.POST, 
                entity, 
                String.class
            );
            
            // If we get here without exception, the API key is valid
            if (response.getStatusCode().is2xxSuccessful()) {
                this.storedApiKey = apiKey;
                
                // Create ChatClient dynamically with the validated API key using builder pattern
                try {
                    OpenAiApi openAiApi = OpenAiApi.builder()
                        .apiKey(apiKey)
                        .build();
                    
                    OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                        .model("gpt-4o")
                        .temperature(0.7)
                        .maxTokens(2000)
                        .build();
                    
                    OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                        .openAiApi(openAiApi)
                        .defaultOptions(chatOptions)
                        .build();
                    
                    this.validatedChatClient = ChatClient.create(openAiChatModel);
                } catch (Exception e) {
                    // If ChatClient creation fails, we'll use REST calls directly
                    this.validatedChatClient = null;
                }
                
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            // API key validation failed
            return false;
        }
    }
    
    public boolean hasValidApiKey() {
        return storedApiKey != null;
    }
    
    public void clearApiKey() {
        this.storedApiKey = null;
        this.validatedChatClient = null;
    }
    
    public ChatClient getValidatedChatClient() {
        return validatedChatClient;
    }
    
    public String getStoredApiKey() {
        return storedApiKey;
    }
    
    // Direct API call method for when ChatClient is not available
    public String callOpenAiDirectly(String prompt) throws Exception {
        if (storedApiKey == null) {
            throw new IllegalStateException("No API key stored");
        }
        
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + storedApiKey);
        headers.set("Content-Type", "application/json");
        
        String escapedPrompt = escapeJsonString(prompt);
        String requestBody = String.format("{\n" +
            "  \"model\": \"gpt-4o\",\n" +
            "  \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}],\n" +
            "  \"max_tokens\": 2000\n" +
            "}", escapedPrompt);
        
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(
            "https://api.openai.com/v1/chat/completions", 
            HttpMethod.POST, 
            entity, 
            String.class
        );
        
        if (response.getStatusCode().is2xxSuccessful()) {
            // Parse the JSON response to get the content
            String responseBody = response.getBody();
            // Simple extraction - in production you'd use proper JSON parsing
            int contentStart = responseBody.indexOf("\"content\":\"") + 11;
            int contentEnd = responseBody.indexOf("\"", contentStart);
            return responseBody.substring(contentStart, contentEnd)
                .replace("\\n", "\n")
                .replace("\\\"", "\"");
        } else {
            throw new Exception("OpenAI API call failed: " + response.getStatusCode());
        }
    }
    
    private String escapeJsonString(String input) {
        if (input == null) return null;
        
        return input
            .replace("\\", "\\\\")  // Escape backslashes first
            .replace("\"", "\\\"")  // Escape quotes
            .replace("\n", "\\n")   // Escape newlines
            .replace("\r", "\\r")   // Escape carriage returns
            .replace("\t", "\\t")   // Escape tabs
            .replace("\b", "\\b")   // Escape backspace
            .replace("\f", "\\f");  // Escape form feed
    }
}