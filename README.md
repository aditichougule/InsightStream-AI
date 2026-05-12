# AI Video Intelligence Platform

An AI-powered system that converts YouTube videos, lectures, podcasts, meetings, and webinars into structured notes, timestamps, summaries, action items, with semantic search, AI chat, flashcards, and meeting intelligence capabilities.

## Tech Stack

### Backend
- **Framework**: Spring Boot 3.2
- **Java**: JDK 17 or higher
- **Database**: PostgreSQL
- **Build**: Maven
- **API Documentation**: Swagger/OpenAPI 3.0

### Security
- JWT Authentication
- Spring Security
- Password Encryption (BCrypt)

### Additional Features
- Async Processing with Spring Async
- RESTful API
- Global Exception Handling
- Request/Response DTOs
- Pagination Support

## Project Structure

```
src/main/java/com/aivideoip/
├── AiVideoIntelligencePlatformApplication.java  # Main application class
├── controller/                                   # REST Controllers
│   ├── AuthController.java                      # Authentication endpoints
│   ├── VideoController.java                     # Video management endpoints
│   ├── TranscriptChunkController.java           # Transcript chunks (Step 2) ⭐
│   ├── SummaryController.java                   # Summaries (Step 2) ⭐
│   └── ActionItemController.java                # Action items (Step 2) ⭐
├── service/                                      # Business logic
│   ├── AuthService.java                         # Authentication logic
│   ├── UserService.java                         # User management
│   ├── VideoService.java                        # Video operations
│   ├── TranscriptChunkService.java              # Transcript chunks (Step 2) ⭐
│   ├── SummaryService.java                      # Summaries (Step 2) ⭐
│   └── ActionItemService.java                   # Action items (Step 2) ⭐
├── repository/                                   # Data access layer
│   ├── UserRepository.java                      # User JPA Repository
│   ├── VideoRepository.java                     # Video JPA Repository
│   ├── TranscriptChunkRepository.java           # Transcript chunks (Step 2) ⭐
│   ├── SummaryRepository.java                   # Summaries (Step 2) ⭐
│   └── ActionItemRepository.java                # Action items (Step 2) ⭐
├── entity/                                       # JPA Entities
│   ├── BaseEntity.java                          # Base entity with common fields
│   ├── User.java                                # User entity
│   ├── Video.java                               # Video entity
│   ├── TranscriptChunk.java                     # Transcript chunks (Step 2) ⭐
│   ├── Summary.java                             # Summaries (Step 2) ⭐
│   └── ActionItem.java                          # Action items (Step 2) ⭐
├── dto/                                          # Data Transfer Objects
│   ├── UserDTO.java                             # User DTO
│   ├── VideoDTO.java                            # Video DTO
│   ├── AuthResponse.java                        # Auth response DTO
│   ├── RegisterRequest.java                     # Register request DTO
│   ├── LoginRequest.java                        # Login request DTO
│   ├── CreateVideoRequest.java                  # Create video request DTO
│   ├── ApiResponse.java                         # Generic API response wrapper
│   ├── TranscriptChunkDTO.java                  # Transcript chunks DTO (Step 2) ⭐
│   ├── SummaryDTO.java                          # Summaries DTO (Step 2) ⭐
│   └── ActionItemDTO.java                       # Action items DTO (Step 2) ⭐
├── config/                                       # Configuration classes
│   ├── SecurityConfig.java                      # Spring Security configuration
│   ├── HttpClientConfig.java                    # HTTP client beans
│   └── OpenApiConfig.java                       # Swagger/OpenAPI configuration
├── exception/                                    # Exception handling
│   ├── ResourceNotFoundException.java           # Custom exception
│   ├── GlobalExceptionHandler.java              # Global exception handler
│   └── ErrorResponse.java                       # Error response DTO
├── worker/                                       # Async workers
│   └── VideoProcessingWorker.java               # Video processing worker
└── utils/                                        # Utility classes
    └── JwtUtil.java                             # JWT token utility

src/main/resources/
├── application.yml                              # Main configuration
├── application-dev.yml                          # Development configuration
└── application-prod.yml                         # Production configuration
```

## Prerequisites

### Required
- Java 17 or higher
- PostgreSQL 13 or higher
- Maven 3.8 or higher

### Installation

1. **Install Java 17**
   ```bash
   # macOS with Homebrew
   brew install openjdk@17
   
   # Or download from https://openjdk.java.net/
   ```

2. **Install PostgreSQL**
   ```bash
   # macOS with Homebrew
   brew install postgresql
   
   # Start PostgreSQL service
   brew services start postgresql
   ```

3. **Create Database**
   ```sql
   createdb ai_video_ip
   ```

4. **Update Database Credentials** in `application.yml`
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/ai_video_ip
       username: postgres
       password: your_password
   ```

## Building and Running

### Build the Project

```bash
mvn clean package
```

### Run the Application

```bash
# Using Maven
mvn spring-boot:run

# Or run the JAR file
java -jar target/ai-video-intelligence-platform-1.0.0.jar
```

The application will start on `http://localhost:8080`

### Run Tests

```bash
mvn test
```

## API Documentation

Once the application is running, access the Swagger UI at:
```
http://localhost:8080/swagger-ui/index.html
```

### Authentication Endpoints

#### Register User
```
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securepassword",
  "firstName": "John",
  "lastName": "Doe",
  "username": "johndoe"
}
```

#### Login
```
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securepassword"
}
```

#### Get Current User
```
GET /api/auth/me?userId=1
Authorization: Bearer <token>
```

### Video Endpoints

#### Create Video
```
POST /api/videos?userId=1
Content-Type: application/json
Authorization: Bearer <token>

{
  "title": "Machine Learning Basics",
  "description": "Introduction to ML",
  "sourceUrl": "https://youtube.com/watch?v=...",
  "source": "YOUTUBE",
  "durationSeconds": 3600
}
```

#### Get Video
```
GET /api/videos/{videoId}
Authorization: Bearer <token>
```

#### Get User's Videos
```
GET /api/videos/user/{userId}?page=0&size=10
Authorization: Bearer <token>
```

#### Update Video
```
PUT /api/videos/{videoId}
Content-Type: application/json
Authorization: Bearer <token>
```

#### Delete Video
```
DELETE /api/videos/{videoId}
Authorization: Bearer <token>
```

## Configuration

### Application Properties

**application.yml** - Main configuration
```yaml
server:
  port: 8080                              # Server port
  servlet:
    context-path: /api                    # API context path

app:
  jwt:
    secret: your-secret-key               # JWT secret (min 32 chars)
    expiration: 86400000                  # Token expiration (ms)
  cors:
    allowed-origins: http://localhost:3000
```

**application-dev.yml** - Development configuration
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop               # Auto-create schema
    show-sql: true
```

**application-prod.yml** - Production configuration
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate                  # Validate schema
```

### Running with Different Profiles

```bash
# Development
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Production
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"
```

## Database Schema

### Users Table
```sql
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255) NOT NULL,
  username VARCHAR(255) UNIQUE,
  role VARCHAR(50) NOT NULL DEFAULT 'USER',
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Videos Table
```sql
CREATE TABLE videos (
  id BIGSERIAL PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  source_url VARCHAR(500) NOT NULL,
  source VARCHAR(50) NOT NULL,
  thumbnail_url VARCHAR(500),
  duration_seconds BIGINT,
  user_id BIGINT NOT NULL REFERENCES users(id),
  processing_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
  error_message TEXT,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Next Steps

This is **Step 1** of the implementation plan. The foundation includes:

✅ Spring Boot 3 with Java 21
✅ JWT Authentication & Authorization
✅ User Management
✅ Video Entity & Management
✅ Exception Handling
✅ API Documentation (Swagger)
✅ Database Configuration
✅ Async Processing Support

## Implementation Status

### ✅ Completed
- **Step 1**: Spring Boot Setup - User authentication, video management, JWT, Swagger (COMPLETE)
- **Step 2**: PostgreSQL Database Setup - Transcript chunks, summaries, action items (COMPLETE) ⭐

### 🔄 Upcoming
- Step 3: Whisper Integration (Speech-to-Text)
- Step 4: LLM Integration (OpenAI/Gemini)
- Step 5: Embedding & Vector DB Integration
- Step 6: RAG Implementation
- Step 7: Frontend (Next.js)

## Step 2: PostgreSQL Database Setup ⭐ NEW

Step 2 adds support for:
- **Transcript Chunks**: Store transcribed video content with timestamps
- **Summaries**: Generate and store AI-powered video summaries
- **Action Items**: Track tasks extracted from videos

### New API Endpoints (16 total)

**Transcript Chunks (5 endpoints)**
- `POST /api/videos/{videoId}/transcript-chunks` - Create chunk
- `GET /api/videos/{videoId}/transcript-chunks` - List chunks (paginated)
- `GET /api/videos/{videoId}/transcript-chunks/{chunkId}` - Get chunk
- `PUT /api/videos/{videoId}/transcript-chunks/{chunkId}` - Update chunk
- `DELETE /api/videos/{videoId}/transcript-chunks/{chunkId}` - Delete chunk

**Summaries (5 endpoints)**
- `POST /api/videos/{videoId}/summary` - Create summary
- `GET /api/videos/{videoId}/summary` - Get video summary
- `GET /api/videos/{videoId}/summary/{summaryId}` - Get by ID
- `PUT /api/videos/{videoId}/summary/{summaryId}` - Update summary
- `DELETE /api/videos/{videoId}/summary/{summaryId}` - Delete summary

**Action Items (6 endpoints)**
- `POST /api/videos/{videoId}/action-items` - Create action item
- `GET /api/videos/{videoId}/action-items` - List items (paginated)
- `GET /api/videos/{videoId}/action-items/status/{status}` - Filter by status
- `GET /api/videos/{videoId}/action-items/{itemId}` - Get item
- `PUT /api/videos/{videoId}/action-items/{itemId}` - Update item
- `DELETE /api/videos/{videoId}/action-items/{itemId}` - Delete item

### Documentation
See the following files for detailed documentation:
- **STEP2_QUICK_START.md** - Quick setup and testing guide
- **STEP2_POSTGRESQL_SETUP.md** - Detailed schema documentation
- **docs/DATABASE_API_REFERENCE.md** - Complete API reference
- **docs/STEP2_OVERVIEW.md** - Architecture and implementation overview

## Troubleshooting

### Database Connection Issues
```bash
# Check if PostgreSQL is running
psql -U postgres -d postgres -c "SELECT version();"

# Verify database exists
psql -U postgres -l | grep ai_video_ip
```

### Port Already in Use
```bash
# Change port in application.yml
server:
  port: 8081
```

### Clear Maven Cache
```bash
rm -rf ~/.m2/repository
mvn clean install
```

## Support

For issues and questions, please check the [project documentation](https://github.com/yourusername/ai-video-intelligence-platform)

## License

MIT License - See LICENSE file for details

