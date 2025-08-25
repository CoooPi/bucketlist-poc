package com.bucketlist.api;

import com.bucketlist.domain.PersonSession;
import com.bucketlist.domain.PersonSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/session")
@CrossOrigin(origins = "http://localhost:5173")
public class SessionController {
    
    private final PersonSessionService sessionService;
    
    @Autowired
    public SessionController(PersonSessionService sessionService) {
        this.sessionService = sessionService;
    }
    
    @PostMapping("/create")
    public ResponseEntity<SessionResponse> createSession(@RequestBody PersonDescriptionRequest request) {
        PersonSession session = sessionService.createSession(request.getPersonDescription());
        return ResponseEntity.ok(new SessionResponse(session.getSessionId()));
    }
    
    public static class PersonDescriptionRequest {
        private String personDescription;
        
        public String getPersonDescription() { return personDescription; }
        public void setPersonDescription(String personDescription) { this.personDescription = personDescription; }
    }
    
    public static class SessionResponse {
        private String sessionId;
        
        public SessionResponse(String sessionId) {
            this.sessionId = sessionId;
        }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }
}