package com.bucketlist.infra;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAIConfig {
    
    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel)
            .defaultSystem("""
                You are a helpful assistant that generates valid JSON responses for a bucket list application.
                
                Rules:
                - Always respond with valid JSON only, no markdown or other text
                - Be realistic and appropriate for Swedish users
                - Consider budget constraints carefully
                - Make responses engaging but practical
                """)
            .build();
    }
}