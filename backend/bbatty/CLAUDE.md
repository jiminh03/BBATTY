# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Common Development Commands

### Build and Run
```bash
# Build the application
./gradlew build

# Run the application
./gradlew bootRun

# Run tests
./gradlew test

# Clean build
./gradlew clean build
```

### Docker Operations
```bash
# Start all services (MySQL, Redis, Spring Boot app)
make up

# View logs
make logs

# Stop services
make down

# Restart services
make restart

# Clean up containers and volumes
make clean
```

## Architecture Overview

### Technology Stack
- **Framework**: Spring Boot 3.5.3 with Java 21
- **Database**: MySQL 8.0 with JPA/Hibernate
- **Cache**: Redis with Lettuce driver
- **Real-time Communication**: WebSocket for chat functionality
- **Cloud Storage**: AWS S3 for file uploads
- **Containerization**: Docker with Docker Compose

### Domain Structure
The application follows a domain-driven design pattern with these main domains:

- **`chat`**: Real-time chat system with WebSocket support
  - Game chat and match chat with Redis Pub/Sub
  - Base WebSocket handler with session management
  - Rate limiting and message filtering capabilities
- **`user`**: User management and authentication
- **`team`**: Team-related functionality
- **`game`**: Game logic and management
- **`board`**: Post management with image support
- **`ranking`**: Ranking and statistics
- **`attendance`**: Attendance tracking
- **`crawler`**: Data crawling services

### Key Architecture Patterns

#### WebSocket Chat System
- **Base Handler**: `BaseChatWebSocketHandler` provides common WebSocket functionality
- **Domain Handlers**: `GameChatWebSocketHandler`, `MatchChatWebSocketHandler`
- **Redis Integration**: Pub/Sub pattern for distributed chat messaging
- **Session Management**: User session tracking with concurrent maps

#### Global Infrastructure
- **Base Entity**: `BaseTimeEntity` for audit fields (createdAt, updatedAt)
- **API Response**: Standardized `ApiResponse<T>` wrapper for all REST endpoints
- **Exception Handling**: Global exception handler with custom error codes
- **Configuration**: Centralized config for WebSocket, Redis, S3

#### Data Layer
- **JPA Auditing**: Enabled with `@EnableJpaAuditing`
- **Repository Pattern**: Spring Data JPA repositories
- **Connection Pooling**: Lettuce for Redis connections

## Environment Configuration

### Required Environment Variables
```
# Database
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/bbatty
SPRING_DATASOURCE_USERNAME=your_username
SPRING_DATASOURCE_PASSWORD=your_password

# Redis
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379

# AWS S3
CLOUD_AWS_CREDENTIALS_ACCESS_KEY=your_access_key
CLOUD_AWS_CREDENTIALS_SECRET_KEY=your_secret_key
CLOUD_AWS_S3_BUCKET=your_bucket_name
```

### WebSocket Configuration
- Game chat endpoint: `/ws/game-chat`
- Match chat endpoint: `/ws/match-chat`
- Required query parameters: `userId`, `teamId` (for game chat)

## Development Guidelines

### Adding New Chat Features
1. Extend `BaseChatWebSocketHandler` for new chat types
2. Implement abstract methods for domain-specific logic
3. Register handler in `WebSocketConfig`
4. Use `RedisPubSubService` for distributed messaging

### Database Changes
1. Create entity extending `BaseTimeEntity`
2. Add repository interface extending `JpaRepository`
3. Update `application.properties` if needed
4. Run with `spring.jpa.hibernate.ddl-auto=update`

### API Development
1. Use `ApiResponse<T>` for all controller responses
2. Follow domain package structure
3. Implement proper exception handling
4. Use validation annotations on DTOs