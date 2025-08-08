나는 프론트엔드야 백엔드에 맞춰 코드를 작성중이야.
# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BBATTY is a baseball fan platform that enables GPS-based attendance verification, real-time fan communication, and comprehensive statistics tracking. The system consists of three Spring Boot microservices and a React Native mobile application.

## Repository Structure

```
S13P11A403/
├── frontend/bbatty/          # React Native mobile app (Expo)
├── backend/
│   ├── bbatty/              # Main API service (port 8080)
│   ├── chat/                # Chat service with WebSocket & Kafka (port 8084)  
│   ├── schedule/            # Scheduling & crawling service (port 8082)
│   └── docker-compose.yml   # Full stack orchestration
└── sub_pjt_1/              # Individual team member documentation
```

## Development Commands

### Frontend (React Native + Expo)
```bash
cd frontend/bbatty
npm start                 # Start Expo development server
npm run android          # Run on Android emulator/device
npm run ios             # Run on iOS simulator/device
npm run web             # Run in web browser
```

### Backend Services
Each Spring Boot service can be run individually:

```bash
# Main service
cd backend/bbatty
./gradlew bootRun

# Chat service  
cd backend/chat
./gradlew bootRun

# Schedule service
cd backend/schedule
./gradlew bootRun
```

### Docker Development
The backend includes a comprehensive Docker Compose setup:

```bash
cd backend
make up          # Build and start all services
make down        # Stop all services
make logs        # View logs
make clean       # Remove everything including volumes
make restart     # Restart all services
```

## Architecture Overview

### Microservices Architecture
- **bbatty**: Core API service handling authentication, user profiles, attendance verification, statistics, and community features
- **chat**: Dual chat system using Redis Pub/Sub (game chat) and Kafka (match chat) with WebSocket connections
- **schedule**: Web crawling service for game data and batch processing for statistics

### Technology Stack
- **Backend**: Spring Boot 3.x, Java 21, MySQL 8.0, Redis, Apache Kafka
- **Frontend**: React Native (Expo), TypeScript, Zustand for state management, TanStack Query
- **Infrastructure**: Docker, AWS S3, JWT authentication

### Key Architectural Patterns

#### Dual Chat System
The chat service implements two distinct messaging patterns:
- **Game Chat**: Redis Pub/Sub for volatile, high-speed messaging during live games
- **Match Chat**: Kafka for persistent, ordered messaging for finding game partners

#### GPS-Based Verification
The main service implements location-based attendance verification:
- 12 baseball stadiums with GPS coordinates stored in database
- Time-window validation (2 hours before game to midnight)
- Duplicate prevention for same game attendance

#### Statistics Engine
Asynchronous batch processing for user statistics:
- Redis Sorted Sets for real-time rankings
- Complex queries with caching for multi-dimensional statistics (by stadium, opponent, day of week)
- Batch updates triggered by game results

## Database Schema

The system uses a shared MySQL database across all services with key entities:
- Users and authentication (Kakao OAuth 2.0 + JWT)
- Teams and game schedules
- Attendance records and statistics
- Posts and comments for community
- Chat rooms and messages

## Environment Configuration

Each service requires environment variables:
- Database: `MYSQL_*` variables
- Redis: `REDIS_PASSWORD`, `SPRING_DATA_REDIS_*`
- Kafka: `KAFKA_BOOTSTRAP_SERVERS`
- AWS S3: `CLOUD_AWS_*` variables
- JWT: `JWT_SECRET`
- Kakao OAuth: `KAKAO_*` variables

## Testing

```bash
# Backend tests
./gradlew test           # Run all tests
./gradlew test --info    # Run with detailed output

# Frontend tests
cd frontend/bbatty
# No test framework currently configured
```

## Key Business Logic

### Attendance Verification Process
1. User initiates check-in at stadium location
2. System validates GPS coordinates against stadium database
3. Checks if user's team is playing at that venue
4. Verifies time window (2h before game to midnight)
5. Prevents duplicate attendance for same game
6. Records attendance and updates statistics

### Chat Room Management
- **Game Chat**: Auto-created 2 hours before games, auto-deleted at midnight
- **Match Chat**: User-created with filters (age, gender, team preference)
- Access control based on attendance verification for game chats

### Statistics Calculation
- Win rate calculations across multiple dimensions
- Streak tracking (current and maximum)
- Redis-based ranking system for performance
- Batch processing after game results are crawled

## Important Notes

- All services share the same MySQL database but are designed to be independently deployable
- JWT tokens are used across services for authentication
- The system is designed to handle concurrent users during peak game times
- S3 presigned URLs are used for image uploads to reduce server load
- Redis is used both for caching and as a message broker depending on the service