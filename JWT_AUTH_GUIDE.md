# JWT Authentication - Frontend Integration Guide

## Overview
Your Spring Boot application now uses JWT (JSON Web Token) authentication instead of Basic Auth. This means:
- ✅ Frontend logs in **once** with username/password
- ✅ Backend returns a JWT token (valid for 24 hours)
- ✅ Frontend stores token and sends it with each request
- ✅ No more sending username/password with every request!

## How to Use from Frontend

### 1. Login (Get JWT Token)
```javascript
// POST to /api/v1/auth/login
const response = await fetch('http://localhost:8080/api/v1/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    username: 'janne',
    password: 'adminpassword'
  })
});

const data = await response.json();
// Response: 
// {
//   "token": "eyJhbGciOiJIUzI1NiJ9...",
//   "expiresIn": 86400000,  // milliseconds (24 hours)
//   "username": "janne"
// }

// Store token (choose one method):
localStorage.setItem('jwt_token', data.token);      // Option 1
sessionStorage.setItem('jwt_token', data.token);    // Option 2
// Or use httpOnly cookies for better security
```

### 2. Make Authenticated Requests
```javascript
const token = localStorage.getItem('jwt_token');

// For protected endpoints (POST, DELETE)
const response = await fetch('http://localhost:8080/api/v1/projects', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`  // Important: "Bearer " prefix!
  },
  body: JSON.stringify({
    name: 'New Project',
    description: 'Project description'
  })
});
```

### 3. Handle Token Expiration
```javascript
// If you get 403 Forbidden, token might be expired
if (response.status === 403) {
  // Clear old token and redirect to login
  localStorage.removeItem('jwt_token');
  window.location.href = '/login';
}
```

## API Endpoints

### Authentication
- **POST** `/api/v1/auth/login` - Login and get JWT token
  - Body: `{"username": "string", "password": "string"}`
  - Returns: `{"token": "jwt_token", "expiresIn": number, "username": "string"}`

### Projects (Examples)
- **GET** `/api/v1/projects` - List all projects (public, no auth needed)
- **GET** `/api/v1/projects/{uuid}` - Get specific project (public)
- **POST** `/api/v1/projects` - Create project (requires JWT token with ADMIN role)
- **DELETE** `/api/v1/projects/{uuid}` - Delete project (requires JWT token with ADMIN role)

## Configuration
Current settings in `application.yaml`:
```yaml
jwt:
  secret: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
  expiration: 86400000  # 24 hours in milliseconds

spring:
  security:
    user:
      name: janne
      password: adminpassword
      roles: ADMIN
```

## Security Notes
- Token expires after 24 hours (configurable)
- Only ADMIN role can create/delete projects
- GET endpoints are public (no authentication required)
- Invalid tokens return 500 error (can be improved with better error handling)
- Wrong username/password returns 403 Forbidden

## Testing Examples (curl)
```bash
# 1. Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"janne","password":"adminpassword"}'

# 2. Use token for protected endpoint
curl -X POST http://localhost:8080/api/v1/projects \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{"name":"Test Project","description":"Test"}'
```

## Migration Notes
- ❌ **Old Basic Auth no longer works** - username/password in Authorization header won't work
- ✅ Frontend must implement login flow
- ✅ Frontend must store and manage JWT tokens
- ✅ Much more secure and efficient than Basic Auth
