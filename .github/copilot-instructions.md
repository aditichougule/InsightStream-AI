<!-- Use this file to provide workspace-specific custom instructions to Copilot. For more details, visit https://code.visualstudio.com/docs/copilot/copilot-customization#_use-a-githubcopilotinstructionsmd-file -->

# AI Video Intelligence Platform - Development Guidelines

This is a Spring Boot 3 backend project for the AI Video Intelligence Platform.

## Project Overview
An AI-powered system that converts YouTube videos, lectures, podcasts, meetings, and webinars into structured notes, timestamps, summaries, action items, with semantic search, AI chat capabilities.

## Tech Stack
- **Language**: Java 17
- **Framework**: Spring Boot 3.2
- **Build Tool**: Maven
- **Database**: PostgreSQL
- **Security**: JWT Authentication
- **API Docs**: Swagger/OpenAPI 3.0

## Project Structure
```
src/main/java/com/aivideoip/
├── controller/      # REST API endpoints
├── service/         # Business logic layer
├── repository/      # Data access layer (JPA)
├── entity/          # JPA entities
├── dto/             # Data transfer objects
├── config/          # Spring configuration
├── exception/       # Exception handling
├── worker/          # Async workers
└── utils/           # Utility classes
```

## Best Practices
- Follow Java naming conventions (PascalCase for classes, camelCase for methods/variables)
- Write unit tests for all service methods
- Use DTOs for API requests/responses
- Implement proper exception handling
- Add JavaDoc comments for public classes and methods
- Use Lombok to reduce boilerplate code (@Getter, @Setter, @Builder, etc.)
- Always use transactions (@Transactional) for database operations
- Log important operations using @Slf4j

## Building and Running

### Build
```bash
mvn clean package
```

### Run
```bash
mvn spring-boot:run
```

### Test
```bash
mvn test
```

### Development Setup
1. Configure PostgreSQL in `application-dev.yml`
2. Run with profile: `mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"`

## Database
- PostgreSQL 13+
- Tables auto-created via Hibernate (DDL)
- Current entities: User, Video
- Extend BaseEntity for new entities to inherit common fields (id, createdAt, updatedAt, active)

## Authentication
- JWT tokens generated on login/register
- All endpoints except `/auth/**` and `/health` require valid JWT token
- Token stored in Authorization header: `Bearer <token>`

## API Response Format
All API responses follow this format:
```json
{
  "success": true/false,
  "message": "Response message",
  "data": {...},
  "statusCode": 200
}
```

## Error Handling
- Use ResourceNotFoundException for missing resources
- GlobalExceptionHandler handles all exceptions and returns standardized error responses
- Validation errors from @Valid annotations are automatically handled

## Next Steps in Implementation Plan
1. ✅ Step 1: Create Spring Boot Project (COMPLETED)
2. Step 2: Whisper Integration (Speech-to-Text)
3. Step 3: LLM Integration (OpenAI/Gemini)
4. Step 4: Embedding & Vector DB (ChromaDB/Weaviate)
5. Step 5: RAG Implementation
6. Step 6: Frontend (Next.js)

## Common Commands

### Create new entity
1. Create entity class in `entity/` extending BaseEntity
2. Create repository in `repository/` extending JpaRepository
3. Create DTO classes in `dto/`
4. Create service in `service/`
5. Create controller in `controller/`

### Add new dependency
1. Update `pom.xml` in `<dependencies>` section
2. Run `mvn clean install`

### Configuration Management
- `application.yml` - Default configuration
- `application-dev.yml` - Development overrides
- `application-prod.yml` - Production overrides
- Environment variables can override YAML values

