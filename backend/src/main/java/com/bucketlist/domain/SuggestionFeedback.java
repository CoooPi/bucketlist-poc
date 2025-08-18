package com.bucketlist.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(
    indexes = {
        @Index(name = "idx_feedback_profile_created", columnList = "profileId,createdAt")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_feedback_suggestion_profile", columnNames = {"suggestionId", "profileId"})
    }
)
public class SuggestionFeedback {
    
    @Id
    private UUID id;
    
    @Column(nullable = false)
    private UUID suggestionId;
    
    @Column(nullable = false)
    private UUID profileId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Verdict verdict;
    
    private String reason;
    
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