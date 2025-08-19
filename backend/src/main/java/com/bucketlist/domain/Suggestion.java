package com.bucketlist.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = {
    @Index(name = "idx_suggestion_profile_created", columnList = "profileId,createdAt"),
    @Index(name = "idx_suggestion_content_hash", columnList = "contentHash")
})
public class Suggestion {
    
    @Id
    private UUID id;
    
    @Column(nullable = false)
    private UUID profileId;
    
    @Column(length = 120, nullable = false)
    private String title;
    
    @Column(length = 600, nullable = false)
    private String description;
    
    
    
    @Column(length = 64)
    private String sourcePromptHash;
    
    @Column(length = 64)
    private String contentHash;
    
    
    @Column(columnDefinition = "TEXT")
    private String budgetBreakdownJson;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
    }
}