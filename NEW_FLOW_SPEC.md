# New Bucket List Flow Specification

## Overview
Complete rewrite of the onboarding and suggestion generation system. Instead of category selection, users describe a person and receive diverse suggestions across multiple categories with rejection reasons.

## User Flow

### 1. Person Description Page
- Single shadcn text input for describing a person
- Store description in ephemeral session (no permanent storage)
- Submit triggers suggestion generation

### 2. Suggestion Display
- Show 5 AI-generated suggestions across different categories
- Display category alongside each suggestion
- Each suggestion includes 5 pre-generated rejection reasons
- User can accept, reject with provided reason, or reject with custom reason

## Technical Requirements

### Backend Changes

#### New Data Structures
```java
// Session-based person description (ephemeral)
public class PersonSession {
    private String sessionId;
    private String personDescription;
    private LocalDateTime createdAt;
}

// Updated suggestion with category and rejection reasons
public class BucketListSuggestion {
    private String title;
    private String description;
    private String category; // Must be one of predefined categories
    private PriceBand priceBand;
    private List<String> rejectionReasons; // 5 AI-generated reasons
}

// Rejection feedback with reason
public class RejectionFeedback {
    private String suggestionId;
    private String reason; // Either from provided list or custom
    private boolean isCustomReason;
}
```

#### API Endpoints (Complete Rewrite)
```
POST /api/session/create
- Body: { "personDescription": "string" }
- Returns: { "sessionId": "string" }

GET /api/suggestions/{sessionId}
- Returns: { "suggestions": [BucketListSuggestion] }

POST /api/suggestions/accept
- Body: { "sessionId": "string", "suggestionId": "string" }

POST /api/suggestions/reject
- Body: RejectionFeedback + sessionId
```

#### AI Integration Requirements
- **Input**: Person description from session
- **Output**: 5 suggestions across different categories
- **Categories**: Limited to existing predefined categories only
- **Constraints**: 
  - Each suggestion must include category field
  - Each suggestion must include 5 rejection reasons
  - Vary creativity levels across suggestions
  - Distribute across different categories

#### System Prompt Structure
```
You are generating bucket list suggestions for a person based on this description: {personDescription}

STRICT REQUIREMENTS:
1. Generate exactly 5 suggestions
2. Use only these categories: [list of predefined categories]
3. Distribute suggestions across different categories
4. Vary creativity levels (some safe, some adventurous)
5. Include 5 specific rejection reasons for each suggestion

JSON Response Format:
{
  "suggestions": [
    {
      "title": "string",
      "description": "string", 
      "category": "string", // Must match predefined categories
      "priceBand": "LOW|MEDIUM|HIGH",
      "rejectionReasons": ["reason1", "reason2", "reason3", "reason4", "reason5"]
    }
  ]
}
```

### Frontend Changes

#### Components to Delete/Replace
- Current onboarding flow components
- Category selection components
- Current suggestion display components

#### New Components Needed
```
PersonDescriptionInput
├── Single text input (shadcn)
├── Submit button
└── Session management

SuggestionCard
├── Title and description
├── Category badge/tag
├── Accept button
├── Reject dropdown with reasons
└── Custom rejection input

SuggestionGrid
├── Display 5 suggestions
├── Handle acceptance/rejection
└── Session state management
```

#### State Management
- Ephemeral session ID storage
- Current suggestions state
- Accepted/rejected tracking per session
- No persistent user data

### Implementation Strategy

#### Phase 1: Backend Rewrite
1. Delete existing profile/category logic
2. Implement session-based person description storage
3. Rewrite AI integration with new prompt structure
4. Update suggestion data model with categories and rejection reasons
5. Create new API endpoints

#### Phase 2: Frontend Rewrite  
1. Delete current onboarding components
2. Create PersonDescriptionInput component
3. Rewrite suggestion display with categories
4. Implement rejection reason selection
5. Update API service layer

#### Phase 3: Integration & Testing
1. Connect new frontend to new backend
2. Test AI suggestion quality and variety
3. Verify rejection reason functionality
4. Session cleanup and management

## Key Differences from Current System
- **No user profiles**: Ephemeral sessions only
- **No category selection**: AI determines categories
- **Multi-category suggestions**: 5 suggestions across different categories  
- **Rejection reasons**: AI provides 5 reasons, user can add custom
- **Session-based**: No permanent user data storage
- **Complete rewrite**: Start fresh rather than incremental changes

## Success Criteria
- User can describe any person and get relevant suggestions
- Suggestions span multiple categories appropriately
- Rejection feedback system works with provided and custom reasons
- No permanent data storage, fully session-based
- AI respects category constraints and provides quality rejection reasons