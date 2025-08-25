package com.bucketlist.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public class PersonSession {
    private final String sessionId;
    private final String personDescription;
    private final LocalDateTime createdAt;
    
    public PersonSession(String personDescription) {
        this.sessionId = UUID.randomUUID().toString();
        this.personDescription = personDescription;
        this.createdAt = LocalDateTime.now();
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public String getPersonDescription() {
        return personDescription;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}