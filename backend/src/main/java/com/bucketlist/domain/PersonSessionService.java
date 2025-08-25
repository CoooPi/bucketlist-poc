package com.bucketlist.domain;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class PersonSessionService {
    private final Map<String, PersonSession> sessions = new ConcurrentHashMap<>();
    
    public PersonSession createSession(String personDescription) {
        PersonSession session = new PersonSession(personDescription);
        sessions.put(session.getSessionId(), session);
        return session;
    }
    
    public Optional<PersonSession> getSession(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }
    
    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }
}