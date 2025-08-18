# Bucket List Generator - Feature Specification

## Executive Summary

A prototype application that generates personalized bucket list suggestions based on user demographics and financial constraints. Users can accept/reject suggestions through a simple interface, building their personal bucket list portfolio.

## Core Demo Flow

1. **Profile Input**: User enters basic info (gender, age, capital)
2. **AI Enhancement**: Backend uses ChatGPT to generate synthetic personality/preferences 
3. **Suggestion Generation**: Backend asks ChatGPT for bucket list suggestions based on full profile
4. **User Selection**: Frontend displays suggestions with ✓/✗ buttons to build bucket list portfolio

This demonstrates AI-powered personalization with minimal user input.

## Current State Analysis

### Backend (Spring Boot)
- **Framework**: Spring Boot 3.5.4 with Java 21
- **Dependencies**: Spring AI (OpenAI integration), JPA, H2 database, Lombok
- **Status**: Basic project structure initialized
- **Missing**: All business logic, entities, controllers, services

### Frontend
- **Status**: Not initialized
- **Required**: React + Vite + TypeScript + Tailwind CSS setup

## Technical Architecture

### Backend Structure
```
backend/src/main/java/com/bucketlist/
├── api/           # REST controllers and DTOs
├── domain/        # Core business entities and services  
├── infra/         # JPA repositories, config, external services
└── BucketListApplication.java
```

### Frontend Structure
```
frontend/
├── src/
│   ├── components/    # Reusable UI components
│   ├── pages/         # Route components
│   ├── hooks/         # Custom React hooks
│   ├── services/      # API client functions
│   ├── types/         # TypeScript interfaces
│   └── utils/         # Helper functions
```

## Data Model

### Core Entities

#### UserProfile
```java
@Entity
public class UserProfile {
    @Id private UUID id;
    @Enumerated(EnumType.STRING) private Gender gender;
    private int age;
    private BigDecimal capital;
    @Enumerated(EnumType.STRING) private Mode mode;
    @Column(columnDefinition = "TEXT") private String personalityJson;
    @Column(columnDefinition = "TEXT") private String preferencesJson;
    @Column(columnDefinition = "TEXT") private String priorExperiencesJson;
    private Instant createdAt;
    private Instant updatedAt;
}
```

#### Suggestion
```java
@Entity
public class Suggestion {
    @Id private UUID id;
    @Column(nullable = false) private UUID profileId;
    @Column(length = 120) private String title;
    @Column(length = 600) private String description;
    @Enumerated(EnumType.STRING) private PriceBand priceBand;
    @Enumerated(EnumType.STRING) private Category category;
    private String sourcePromptHash;
    private String contentHash;
    private Instant createdAt;
}
```

#### SuggestionFeedback
```java
@Entity
public class SuggestionFeedback {
    @Id private UUID id;
    @Column(nullable = false) private UUID suggestionId;
    @Column(nullable = false) private UUID profileId;
    @Enumerated(EnumType.STRING) private Verdict verdict;
    private String reason;
    private Instant createdAt;
}
```

### Enums
```java
public enum Gender { MALE, FEMALE, OTHER, UNSPECIFIED }
public enum Mode { CREATIVE, ALIGNED }
public enum PriceBand { LOW, MEDIUM, HIGH }
public enum Category { TRAVEL, ADVENTURE, LEARNING, WELLNESS, FAMILY, OTHER }
public enum Verdict { ACCEPT, REJECT }
```

## API Design

### Endpoints

#### POST /api/profile
Create user profile and generate AI-enhanced profile data.

**Request:**
```json
{
  "gender": "male",
  "age": 29,
  "capital": 70000,
  "mode": "creative"
}
```

**Response:**
```json
{
  "profileId": "uuid-here",
  "profileSummary": "Adventurous 29-year-old with moderate budget",
  "mode": "creative"
}
```

#### POST /api/suggestions/refill
Generate new suggestion batch for a profile.

**Query Params:** `profileId`
**Request:**
```json
{
  "batchSize": 5,
  "min": 3
}
```

#### GET /api/suggestions/next
Get next unrated suggestion.

**Query Params:** `profileId`
**Response:** 200 with suggestion or 204 if queue empty

#### POST /api/suggestions/feedback
Record user feedback on a suggestion.

**Request:**
```json
{
  "profileId": "uuid",
  "suggestionId": "uuid", 
  "verdict": "ACCEPT",
  "reason": "Sounds amazing!"
}
```

#### GET /api/suggestions/accepted
List all accepted suggestions for a profile.

**Query Params:** `profileId`

## OpenAI Integration Strategy

### Configuration Options

#### 1. Environment Variables (Recommended for Development)
```properties
# application.properties
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.model=gpt-4o-mini
```

#### 2. Configuration Properties
```properties
# application-dev.properties
spring.ai.openai.api-key=sk-your-key-here
```

#### 3. Runtime Configuration Endpoint (Production)
```java
@RestController
@RequestMapping("/api/admin")
public class ConfigController {
    
    @PostMapping("/config/openai-key")
    public ResponseEntity<Void> setOpenAIKey(@RequestBody String apiKey) {
        // Validate and set API key at runtime
        openAIService.updateApiKey(apiKey);
        return ResponseEntity.ok().build();
    }
}
```

### LLM Integration

#### Profile Generation Prompt
```java
private String buildProfilePrompt(CreateProfileRequest request) {
    return String.format("""
        Input:
        - Gender: %s
        - Age: %d
        - Capital: %s SEK
        - Mode: %s
        
        Generate a Swedish user profile as JSON matching this schema:
        {
          "personality": {
            "openness": "low|medium|high",
            "extraversion": "low|medium|high", 
            "riskTolerance": "low|medium|high"
          },
          "preferences": {
            "themes": ["outdoors","culture","learning"],
            "travelStyle": "budget|mid|premium",
            "timeWindows": ["weekend","1-week","2-weeks+"]
          },
          "priorExperiences": [
            {"title": "...", "category": "Adventure", "year": 2023}
          ],
          "summary": "1-2 sentences in Swedish"
        }
        """, request.gender(), request.age(), request.capital(), request.mode());
}
```

## Implementation Plan

### Phase 1: Backend Foundation (Week 1)

#### Milestone 1.1: Project Structure & Dependencies
- [ ] Refactor package structure to `com.bucketlist`
- [ ] Add validation dependencies (spring-boot-starter-validation)
- [ ] Configure H2 database with file persistence
- [ ] Set up OpenAI API key configuration

#### Milestone 1.2: Core Entities & Repositories
- [ ] Create all JPA entities with proper relationships
- [ ] Implement JPA repositories with custom queries
- [ ] Add database indexes for performance
- [ ] Create database schema migration scripts

#### Milestone 1.3: Basic API Layer
- [ ] Create DTO classes for all endpoints
- [ ] Implement ProfileController with create endpoint
- [ ] Add request validation and error handling
- [ ] Set up basic logging and health checks

### Phase 2: AI Integration (Week 1-2)

#### Milestone 2.1: Profile AI Generation
- [ ] Implement ProfileService with OpenAI integration
- [ ] Create profile generation prompts
- [ ] Add JSON parsing and validation
- [ ] Implement retry logic for failed LLM calls

#### Milestone 2.2: Suggestion Generation
- [ ] Implement SuggestionService with batch generation
- [ ] Create suggestion prompts with context awareness
- [ ] Add content deduplication logic
- [ ] Implement capital-based price band filtering

#### Milestone 2.3: Feedback System
- [ ] Complete SuggestionController endpoints
- [ ] Implement feedback storage and retrieval
- [ ] Add suggestion queue management
- [ ] Create preference learning from feedback

### Phase 3: Frontend Foundation (Week 2)

#### Milestone 3.1: Project Setup
- [ ] Initialize Vite + React + TypeScript project
- [ ] Install and configure Tailwind CSS
- [ ] Set up routing with React Router
- [ ] Create basic project structure

#### Milestone 3.2: API Integration
- [ ] Create TypeScript interfaces for all DTOs
- [ ] Implement API client service functions
- [ ] Add error handling and loading states
- [ ] Set up environment configuration

#### Milestone 3.3: Core Components
- [ ] Build OnboardingForm component
- [ ] Create SuggestionCard component
- [ ] Implement SwipeControls component
- [ ] Add basic navigation and layout

### Phase 4: User Experience (Week 2-3)

#### Milestone 4.1: Onboarding Flow
- [ ] Complete onboarding form with validation
- [ ] Add loading states during profile creation
- [ ] Implement navigation to bucket list view
- [ ] Add error handling and user feedback

#### Milestone 4.2: Swipe Interface
- [ ] Implement suggestion viewing and swiping
- [ ] Add accept/reject button interactions
- [ ] Create queue management with automatic refill
- [ ] Add loading indicators and empty states

#### Milestone 4.3: Accepted Items View
- [ ] Create accepted suggestions list page
- [ ] Add filtering and sorting capabilities
- [ ] Implement basic categorization view
- [ ] Add export/sharing functionality

### Phase 5: Polish & Testing (Week 3)

#### Milestone 5.1: Backend Testing
- [ ] Unit tests for all services
- [ ] Integration tests for API endpoints
- [ ] Mock OpenAI service for testing
- [ ] Performance testing for suggestion generation

#### Milestone 5.2: Frontend Testing
- [ ] Component testing with React Testing Library
- [ ] End-to-end testing with Playwright
- [ ] API integration testing
- [ ] Accessibility testing and improvements

#### Milestone 5.3: Deployment & Configuration
- [ ] Containerize application with Docker
- [ ] Set up environment-specific configurations
- [ ] Create deployment documentation
- [ ] Implement logging and monitoring

## Configuration Examples

### Development Environment Setup

#### Backend (.env or application-dev.properties)
```properties
OPENAI_API_KEY=sk-your-development-key
spring.datasource.url=jdbc:h2:file:./data/bucketlist
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
logging.level.com.bucketlist=DEBUG
```

#### Frontend (.env.development)
```
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_NAME=Bucket List Generator
```

### Production Considerations

#### Security
- [ ] API key rotation strategy
- [ ] Rate limiting per profile
- [ ] Input validation and sanitization
- [ ] CORS configuration for production domains

#### Performance
- [ ] Database connection pooling
- [ ] LLM response caching
- [ ] Suggestion pre-generation for popular profiles
- [ ] CDN setup for frontend assets

## Success Metrics

### Technical Metrics
- Profile creation success rate > 95%
- Suggestion generation latency < 3 seconds
- JSON validation success rate > 98%
- API response times < 500ms (95th percentile)

### User Experience Metrics
- Onboarding completion rate > 80%
- Average suggestions per session > 10
- Suggestion acceptance rate 20-40%
- Creative mode differentiation clear in user feedback

## Risk Mitigation

### LLM Integration Risks
- **Invalid JSON responses**: Implement repair logic + retry
- **API rate limits**: Add exponential backoff + queuing
- **Cost control**: Set daily spending limits + monitoring
- **Provider outages**: Fallback to pre-generated suggestions

### Data Privacy
- **No PII to LLM**: Hash prompts, abstract user data
- **API key security**: Environment-based configuration
- **Data retention**: Clear policies for profile data

### Performance Risks
- **Database growth**: Partition suggestions by profile
- **Memory usage**: Implement suggestion cleanup policies
- **Frontend bundle size**: Code splitting + lazy loading

## Next Steps

1. **Immediate**: Set up OpenAI API key configuration strategy
2. **Day 1**: Begin backend entity implementation
3. **Day 2**: Start frontend project initialization in parallel
4. **Week 1 End**: Have basic profile creation working end-to-end
5. **Week 2 End**: Complete suggestion generation and swipe interface
6. **Week 3**: Polish, testing, and deployment preparation

## Questions for Clarification

1. **OpenAI API Key**: Prefer environment variable or runtime configuration?
2. **Database**: File-based H2 or in-memory for prototype?
3. **Deployment**: Local development only or prepare for cloud deployment?
4. **Swedish Language**: All UI text in Swedish or English acceptable for prototype?
5. **Budget Ranges**: Confirm SEK thresholds for LOW/MEDIUM/HIGH price bands?