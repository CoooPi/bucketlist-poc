#!/bin/bash

# Bucket List Generator - Quick Start Script
# Usage: ./run.sh [YOUR_OPENAI_API_KEY]
# Note: API key is now optional - you can provide it through the frontend

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# API key is now optional
OPENAI_API_KEY=${1:-""}

echo -e "${BLUE}🚀 Starting Bucket List Generator...${NC}"
echo ""

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

# Check Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java is not installed. Please install Java 21 or later.${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt "21" ]; then
    echo -e "${RED}❌ Java 21 or later is required. Current version: $JAVA_VERSION${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Java $JAVA_VERSION found${NC}"

# Check Node.js
if ! command -v node &> /dev/null; then
    echo -e "${RED}❌ Node.js is not installed. Please install Node.js 18 or later.${NC}"
    exit 1
fi

NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
if [ "$NODE_VERSION" -lt "18" ]; then
    echo -e "${RED}❌ Node.js 18 or later is required. Current version: $NODE_VERSION${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Node.js v$NODE_VERSION found${NC}"

# Check npm
if ! command -v npm &> /dev/null; then
    echo -e "${RED}❌ npm is not installed.${NC}"
    exit 1
fi
echo -e "${GREEN}✅ npm found${NC}"

echo ""

# Function to kill background processes on exit
cleanup() {
    echo -e "\n${YELLOW}🛑 Shutting down servers...${NC}"
    if [ ! -z "$BACKEND_PID" ]; then
        kill $BACKEND_PID 2>/dev/null || true
    fi
    if [ ! -z "$FRONTEND_PID" ]; then
        kill $FRONTEND_PID 2>/dev/null || true
    fi
    echo -e "${GREEN}✅ Cleanup complete${NC}"
}

trap cleanup EXIT

# Install frontend dependencies
echo -e "${YELLOW}📦 Installing frontend dependencies...${NC}"
cd frontend
if [ ! -d "node_modules" ]; then
    npm install
else
    echo -e "${GREEN}✅ Dependencies already installed${NC}"
fi

# Start backend
echo -e "${YELLOW}🔧 Starting backend server...${NC}"
cd ../backend
if [ ! -z "$OPENAI_API_KEY" ]; then
    export OPENAI_API_KEY=$OPENAI_API_KEY
    echo -e "${GREEN}✅ Using provided API key${NC}"
else
    echo -e "${YELLOW}ℹ️  No API key provided - will be set through frontend${NC}"
fi

# Start backend in background
./gradlew bootRun > backend.log 2>&1 &
BACKEND_PID=$!

# Wait for backend to start
echo -e "${BLUE}⏳ Waiting for backend to start...${NC}"
for i in {1..30}; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Backend server started successfully${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}❌ Backend failed to start within 30 seconds${NC}"
        echo "Check backend.log for details:"
        tail -20 backend.log
        exit 1
    fi
    sleep 1
done

# Start frontend
echo -e "${YELLOW}🎨 Starting frontend server...${NC}"
cd ../frontend

# Start frontend in background
npm run dev > frontend.log 2>&1 &
FRONTEND_PID=$!

# Wait for frontend to start
echo -e "${BLUE}⏳ Waiting for frontend to start...${NC}"
for i in {1..15}; do
    if curl -s http://localhost:5173 > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Frontend server started successfully${NC}"
        break
    fi
    if [ $i -eq 15 ]; then
        echo -e "${RED}❌ Frontend failed to start within 15 seconds${NC}"
        echo "Check frontend.log for details:"
        tail -20 frontend.log
        exit 1
    fi
    sleep 1
done

echo ""
echo -e "${GREEN}🎉 Bucket List Generator is now running!${NC}"
echo ""
echo -e "${BLUE}📱 Frontend:${NC} http://localhost:5173"
echo -e "${BLUE}🔧 Backend API:${NC} http://localhost:8080"
echo -e "${BLUE}📊 Health Check:${NC} http://localhost:8080/actuator/health"
echo ""
if [ -z "$OPENAI_API_KEY" ]; then
    echo -e "${YELLOW}💡 You'll need to enter your OpenAI API key in the frontend to use the application${NC}"
else
    echo -e "${GREEN}✅ OpenAI API key is configured${NC}"
fi
echo ""
echo -e "${YELLOW}Press Ctrl+C to stop all servers${NC}"
echo ""

# Keep script running
wait