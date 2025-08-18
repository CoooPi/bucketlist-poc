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
public class UserProfile {
    
    @Id
    private UUID id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;
    
    @Column(nullable = false)
    private int age;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal capital;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Mode mode;
    
    @Column(columnDefinition = "TEXT")
    private String personalityJson;
    
    @Column(columnDefinition = "TEXT")
    private String preferencesJson;
    
    @Column(columnDefinition = "TEXT")
    private String priorExperiencesJson;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
    private Instant updatedAt;
    
    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}