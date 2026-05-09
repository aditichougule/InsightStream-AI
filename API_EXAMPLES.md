# 📡 API Examples & Usage Guide

## 🌐 Base URL
```
http://localhost:8080/api
```

## 📚 Authentication Endpoints

### 1. Register User
**Endpoint**: `POST /auth/register`

**Request**:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "SecurePassword123",
    "firstName": "John",
    "lastName": "Doe",
    "username": "johndoe"
  }'
```

**Response** (201 Created):
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqb2huQGV4YW1wbGUuY29tIiwic...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": 1,
      "email": "john@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "username": "johndoe",
      "role": "USER",
      "active": true,
      "createdAt": "2026-05-09T10:30:00",
      "updatedAt": "2026-05-09T10:30:00"
    }
  },
  "statusCode": 201
}
```

### 2. Login User
**Endpoint**: `POST /auth/login`

**Request**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "SecurePassword123"
  }'
```

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqb2huQGV4YW1wbGUuY29tIiwic...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": 1,
      "email": "john@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "username": "johndoe",
      "role": "USER",
      "active": true,
      "createdAt": "2026-05-09T10:30:00",
      "updatedAt": "2026-05-09T10:30:00"
    }
  },
  "statusCode": 200
}
```

### 3. Get Current User
**Endpoint**: `GET /auth/me`

**Request**:
```bash
curl -X GET "http://localhost:8080/api/auth/me?userId=1" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

**Response** (200 OK):
```json
{
  "success": true,
  "message": "User fetched successfully",
  "data": {
    "id": 1,
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "username": "johndoe",
    "role": "USER",
    "active": true,
    "createdAt": "2026-05-09T10:30:00",
    "updatedAt": "2026-05-09T10:30:00"
  },
  "statusCode": 200
}
```

### 4. Health Check
**Endpoint**: `GET /auth/health`

**Request**:
```bash
curl -X GET http://localhost:8080/api/auth/health
```

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Service is running",
  "data": "OK",
  "statusCode": 200
}
```

---

## 🎥 Video Management Endpoints

### 1. Create Video
**Endpoint**: `POST /videos`

**Request**:
```bash
curl -X POST "http://localhost:8080/api/videos?userId=1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -d '{
    "title": "Introduction to Machine Learning",
    "description": "A comprehensive guide to ML concepts",
    "sourceUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
    "source": "YOUTUBE",
    "thumbnailUrl": "https://img.youtube.com/vi/dQw4w9WgXcQ/default.jpg",
    "durationSeconds": 3600
  }'
```

**Response** (201 Created):
```json
{
  "success": true,
  "message": "Video created successfully",
  "data": {
    "id": 1,
    "title": "Introduction to Machine Learning",
    "description": "A comprehensive guide to ML concepts",
    "sourceUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
    "source": "YOUTUBE",
    "thumbnailUrl": "https://img.youtube.com/vi/dQw4w9WgXcQ/default.jpg",
    "durationSeconds": 3600,
    "ownerId": 1,
    "processingStatus": "PENDING",
    "errorMessage": null,
    "createdAt": "2026-05-09T10:35:00",
    "updatedAt": "2026-05-09T10:35:00"
  },
  "statusCode": 201
}
```

### 2. Get Video Details
**Endpoint**: `GET /videos/{videoId}`

**Request**:
```bash
curl -X GET http://localhost:8080/api/videos/1 \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Video fetched successfully",
  "data": {
    "id": 1,
    "title": "Introduction to Machine Learning",
    "description": "A comprehensive guide to ML concepts",
    "sourceUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
    "source": "YOUTUBE",
    "thumbnailUrl": "https://img.youtube.com/vi/dQw4w9WgXcQ/default.jpg",
    "durationSeconds": 3600,
    "ownerId": 1,
    "processingStatus": "PENDING",
    "errorMessage": null,
    "createdAt": "2026-05-09T10:35:00",
    "updatedAt": "2026-05-09T10:35:00"
  },
  "statusCode": 200
}
```

### 3. List User's Videos
**Endpoint**: `GET /videos/user/{userId}`

**Request**:
```bash
curl -X GET "http://localhost:8080/api/videos/user/1?page=0&size=10&sort=createdAt,desc" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Videos fetched successfully",
  "data": {
    "content": [
      {
        "id": 2,
        "title": "Advanced Python Tutorial",
        "description": "Learn advanced Python concepts",
        "sourceUrl": "https://www.youtube.com/watch?v=abc123",
        "source": "YOUTUBE",
        "thumbnailUrl": "https://img.youtube.com/vi/abc123/default.jpg",
        "durationSeconds": 7200,
        "ownerId": 1,
        "processingStatus": "COMPLETED",
        "errorMessage": null,
        "createdAt": "2026-05-09T11:00:00",
        "updatedAt": "2026-05-09T11:15:00"
      },
      {
        "id": 1,
        "title": "Introduction to Machine Learning",
        "description": "A comprehensive guide to ML concepts",
        "sourceUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        "source": "YOUTUBE",
        "thumbnailUrl": "https://img.youtube.com/vi/dQw4w9WgXcQ/default.jpg",
        "durationSeconds": 3600,
        "ownerId": 1,
        "processingStatus": "PENDING",
        "errorMessage": null,
        "createdAt": "2026-05-09T10:35:00",
        "updatedAt": "2026-05-09T10:35:00"
      }
    ],
    "pageable": {
      "sort": {
        "empty": false,
        "sorted": true,
        "unsorted": false
      },
      "offset": 0,
      "pageNumber": 0,
      "pageSize": 10,
      "paged": true,
      "unpaged": false
    },
    "last": true,
    "totalElements": 2,
    "totalPages": 1,
    "size": 10,
    "number": 0,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "first": true,
    "numberOfElements": 2,
    "empty": false
  },
  "statusCode": 200
}
```

### 4. Update Video
**Endpoint**: `PUT /videos/{videoId}`

**Request**:
```bash
curl -X PUT http://localhost:8080/api/videos/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -d '{
    "title": "Updated: Introduction to Machine Learning",
    "description": "Updated description with more details",
    "sourceUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
    "source": "YOUTUBE"
  }'
```

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Video updated successfully",
  "data": {
    "id": 1,
    "title": "Updated: Introduction to Machine Learning",
    "description": "Updated description with more details",
    "sourceUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
    "source": "YOUTUBE",
    "thumbnailUrl": "https://img.youtube.com/vi/dQw4w9WgXcQ/default.jpg",
    "durationSeconds": 3600,
    "ownerId": 1,
    "processingStatus": "PENDING",
    "errorMessage": null,
    "createdAt": "2026-05-09T10:35:00",
    "updatedAt": "2026-05-09T11:20:00"
  },
  "statusCode": 200
}
```

### 5. Delete Video
**Endpoint**: `DELETE /videos/{videoId}`

**Request**:
```bash
curl -X DELETE http://localhost:8080/api/videos/1 \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Video deleted",
  "data": "Video deleted successfully",
  "statusCode": 200
}
```

---

## ⚠️ Error Responses

### 400 Bad Request - Validation Error
```json
{
  "timestamp": "2026-05-09T10:40:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid input parameters",
  "validationErrors": {
    "email": "Email should be valid",
    "password": "Password should be at least 6 characters"
  },
  "path": "/api/auth/register"
}
```

### 401 Unauthorized - Missing Token
```json
{
  "timestamp": "2026-05-09T10:40:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Missing or invalid JWT token",
  "path": "/api/videos/1"
}
```

### 404 Not Found
```json
{
  "timestamp": "2026-05-09T10:40:00",
  "status": 404,
  "error": "Not Found",
  "message": "Video not found with id: '999'",
  "path": "/api/videos/999"
}
```

### 409 Conflict - Email Already Exists
```json
{
  "timestamp": "2026-05-09T10:40:00",
  "status": 409,
  "error": "Conflict",
  "message": "Email already registered",
  "path": "/api/auth/register"
}
```

### 500 Internal Server Error
```json
{
  "timestamp": "2026-05-09T10:40:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Database connection failed",
  "path": "/api/videos"
}
```

---

## 🔑 Authentication Headers

All protected endpoints require the JWT token:

```bash
Authorization: Bearer <your_jwt_token>
```

**Token Format**:
```
Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.
  eyJzdWIiOiJqb2huQGV4YW1wbGUuY29tIiwidXNlcklkIjoxLCJpYXQiOjE2NzA1OTUwMDB9.
  4a6Z7u2Q8kL3mNpBvCxYpZqRsWtUvWxYzAbCdEfGhIjKlMnOpQrStUvWxYzAbCdEfGhIjKlMnOpQr
```

---

## 📝 Request/Response Examples with HTTPie

If you prefer using [HTTPie](https://httpie.io/):

### Register
```bash
http POST localhost:8080/api/auth/register \
  email=john@example.com \
  password=SecurePassword123 \
  firstName=John \
  lastName=Doe \
  username=johndoe
```

### Login
```bash
http POST localhost:8080/api/auth/login \
  email=john@example.com \
  password=SecurePassword123
```

### Create Video
```bash
http POST localhost:8080/api/videos \
  userId==1 \
  Authorization:"Bearer YOUR_TOKEN" \
  title="ML Intro" \
  sourceUrl="https://youtube.com/watch?v=..." \
  source=YOUTUBE
```

---

## 🧪 Testing with Postman

1. **Import Collection**:
   - Create a new collection in Postman
   - Add the endpoints above

2. **Set Base URL**:
   - Create a variable: `baseUrl = http://localhost:8080/api`
   - Use `{{baseUrl}}` in requests

3. **Store JWT Token**:
   - In login response, extract token
   - Use `pm.environment.set("token", response.data.accessToken)`
   - Use `Authorization: Bearer {{token}}` header

---

## 📊 Response Status Codes

| Code | Meaning | Example |
|------|---------|---------|
| 200 | Success | GET, PUT requests |
| 201 | Created | POST requests successful |
| 204 | No Content | DELETE successful |
| 400 | Bad Request | Invalid input |
| 401 | Unauthorized | Missing/invalid token |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Email already exists |
| 500 | Server Error | Database error |

---

## 💾 Video Source Types

Valid values for `source` field:
- `YOUTUBE` - YouTube videos
- `UPLOADED` - User-uploaded files
- `URL` - Direct URL link
- `PODCAST` - Podcast episodes
- `WEBINAR` - Webinar recordings

## ⏳ Processing Status Types

Status values for `processingStatus`:
- `PENDING` - Waiting to be processed
- `DOWNLOADING` - Downloading video
- `TRANSCRIBING` - Converting to text
- `SUMMARIZING` - Creating summary
- `COMPLETED` - Processing finished
- `FAILED` - Processing error

---

**Ready to test? Start your server with `mvn spring-boot:run` and use these examples!**
