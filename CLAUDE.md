# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Quick Start
- **Run everything**: `./run.sh YOUR_OPENAI_API_KEY` - Starts both backend and frontend servers
- **Backend**: `cd backend && export OPENAI_API_KEY=your_key && ./gradlew bootRun`
- **Frontend**: `cd frontend && npm install && npm run dev`

### Backend (Spring Boot + Gradle)
- **Build**: `cd backend && ./gradlew build`
- **Run**: `cd backend && ./gradlew bootRun` 
- **Tests**: `cd backend && ./gradlew test`
- **Clean**: `cd backend && ./gradlew clean`

### Frontend (React + Vite)
- **Dev server**: `cd frontend && npm run dev`
- **Build**: `cd frontend && npm run build`
- **Lint**: `cd frontend && npm run lint`
- **Preview build**: `cd frontend && npm run preview`

## Architecture Overview

This is a full-stack AI-powered bucket list generator with clear separation between frontend and backend:

### Backend Architecture (Spring Boot)
- **API Layer** (`com.bucketlist.api`): REST controllers and DTOs
  - `ProfileController`: User profile creation with AI enhancement
  - `SuggestionController`: Suggestion generation, feedback, and retrieval
- **Domain Layer** (`com.bucketlist.domain`): Business logic and entities
  - `ProfileService`: Handles user profile creation and AI enhancement
  - `SuggestionService`: Core suggestion generation and management logic
- **Infrastructure Layer** (`com.bucketlist.infra`): Data persistence and external integrations
  - Repositories for JPA entities
  - OpenAI configuration and integration

### Frontend Architecture (React + TypeScript)
- **Components**: Modular React components with shadcn/ui library
- **Services**: API client layer (`services/api.ts`) for backend communication
- **Types**: Centralized TypeScript definitions
- **State Management**: React state with no external state library

### Key Architectural Patterns
- **Domain-driven design**: Clear separation of API, domain, and infrastructure layers
- **RESTful API**: Standard HTTP methods and status codes
- **OpenAI Integration**: Spring AI framework for ChatGPT integration with structured JSON responses
- **Budget-relative pricing**: AI generates suggestions scaled to user's budget
- **Category-based suggestions**: Users select spending categories before getting suggestions

## Database & Environment

- **Database**: H2 in-memory database (data is lost on restart)
- **Required Environment**: `OPENAI_API_KEY` environment variable
- **Ports**: Backend runs on 8080, Frontend on 5173
- **CORS**: Backend configured for localhost:5173

## API Endpoints

- `POST /api/profile` - Create enhanced user profile
- `GET /api/suggestions/next` - Get next suggestion for category
- `POST /api/suggestions/refill` - Generate new batch of suggestions  
- `POST /api/suggestions/feedback` - Submit accept/reject feedback
- `GET /api/suggestions/accepted` - Get user's accepted suggestions
- `GET /api/suggestions/rejected` - Get user's rejected suggestions

## OpenAI Integration

The system uses Spring AI to integrate with OpenAI's ChatGPT for:
1. **Profile Enhancement**: Enriches user input with additional insights
2. **Suggestion Generation**: Creates budget-appropriate, globally diverse experiences
3. **Structured Responses**: Uses JSON schema validation with fallback error handling

Budget categories scale suggestions appropriately:
- **LOW**: 0-10% of budget (local experiences, courses)
- **MEDIUM**: 10-40% of budget (regional travel, premium experiences)  
- **HIGH**: 40%+ of budget (luxury travel, exclusive experiences)