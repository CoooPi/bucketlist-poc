# Bucket List Generator

An AI-powered bucket list generator that creates personalized suggestions based on your profile, preferences, and budget. The system uses OpenAI's ChatGPT to generate creative, budget-appropriate experiences ranging from local adventures to ultra-luxury once-in-a-lifetime experiences.

## Quick Start

### Prerequisites
- Java 21 or later
- Node.js 18 or later
- OpenAI API key

### Run Everything (Recommended)

1. **Clone and navigate to the project:**
   ```bash
   git clone <repository-url>
   cd bucketlist-prototype
   ```

2. **Run the setup script with your OpenAI API key:**
   ```bash
   ./run.sh YOUR_OPENAI_API_KEY_HERE
   ```

3. **Open your browser:**
   - Frontend: http://localhost:5173
   - Backend API: http://localhost:8080

The script will automatically start both backend and frontend servers for you.

### Manual Setup

If you prefer to run things manually:

#### Backend Setup
```bash
cd backend
export OPENAI_API_KEY=your_openai_api_key_here
./gradlew bootRun
```

#### Frontend Setup
```bash
cd frontend
npm install
npm run dev
```

## How It Works

1. **Profile Creation**: Users input basic information (age, gender, budget, personality, preferences, prior experiences)
2. **AI Enhancement**: ChatGPT enhances the user profile with additional insights
3. **Smart Suggestions**: AI generates personalized bucket list items with:
   - Budget-relative pricing (LOW: 0-10%, MEDIUM: 10-40%, HIGH: 40%+ of total budget)
   - Detailed cost breakdowns
   - Global diversity (only 20% Sweden-specific)
   - Extravagance matching budget level
4. **Accept/Reject Flow**: Users can accept suggestions to build their personalized bucket list

## Budget-Relative Pricing

The system intelligently scales suggestions based on your budget:

- **Low Budget (< 100k SEK)**: Local experiences, courses, short trips
- **Medium Budget (100k - 1M SEK)**: European adventures, premium experiences
- **High Budget (> 1M SEK)**: Ultra-luxury experiences like private space tourism, custom superyachts, exclusive access to historical sites
- **Ultra High Budget (> 3M SEK)**: Multi-million SEK once-in-a-lifetime experiences

## Tech Stack

### Backend
- **Java 21** with **Spring Boot 3.5.4**
- **Spring AI** for OpenAI ChatGPT integration
- **Spring Data JPA** with **H2 Database** (in-memory)
- **Lombok** for boilerplate reduction
- **Gradle** for build management

### Frontend
- **React 18** with **TypeScript**
- **Vite** for fast development and building
- **Tailwind CSS** for styling
- **shadcn/ui** component library
- **Lucide React** for icons

### AI Integration
- **OpenAI ChatGPT** for profile enhancement and suggestion generation
- Custom JSON schema validation with fallback error handling
- Advanced prompt engineering for global diversity and budget-appropriate suggestions

## Project Structure

```
bucketlist-prototype/
├── backend/                 # Spring Boot API
│   ├── src/main/java/
│   │   └── com/bucketlist/
│   │       ├── api/         # DTOs and REST controllers
│   │       ├── domain/      # Entities and business logic
│   │       └── infra/       # Repositories and configuration
│   └── build.gradle
├── frontend/                # React TypeScript app
│   ├── src/
│   │   ├── components/      # React components
│   │   ├── services/        # API client
│   │   └── types/           # TypeScript definitions
│   ├── package.json
│   └── tailwind.config.js
├── run.sh                   # Quick start script
└── README.md
```

## API Endpoints

- `POST /api/profiles` - Create enhanced user profile
- `GET /api/suggestions/{profileId}/next` - Get next suggestion
- `POST /api/suggestions/{profileId}/refill` - Generate new suggestions
- `POST /api/feedback` - Submit suggestion feedback
- `GET /api/suggestions/{profileId}/accepted` - Get accepted suggestions

## Features

- **AI-Enhanced Profiles**: ChatGPT analyzes and enriches user input
- **Budget-Relative Pricing**: Suggestions scale appropriately with user budget
- **Global Diversity**: Worldwide experiences, not just local suggestions
- **Real-Time Loading States**: No more frozen UI during suggestion generation
- **Detailed Budget Breakdowns**: Line-by-line cost analysis for each suggestion
- **Deduplication**: Prevents duplicate suggestions using content hashing
- **Dark Mode**: Modern UI with shadcn/ui components

## Development

### Backend Development
```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Frontend Development
```bash
cd frontend
npm run dev
```

### Building for Production
```bash
# Backend
cd backend && ./gradlew build

# Frontend  
cd frontend && npm run build
```

## Configuration

The application uses the following environment variables:

- `OPENAI_API_KEY` - Your OpenAI API key (required)
- `SPRING_PROFILES_ACTIVE` - Spring profile (optional, defaults to default)

## License

This is a prototype project for demonstration purposes.