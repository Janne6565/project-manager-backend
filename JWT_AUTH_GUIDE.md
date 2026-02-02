# JWT Authentication - Frontend Integration Guide (Cookie-Based)

## Overview
Your Spring Boot application uses JWT (JSON Web Token) authentication with **HttpOnly cookies** for enhanced security. This means:
- ✅ Frontend logs in **once** with username/password
- ✅ Backend sets JWT in a secure HttpOnly cookie (cannot be accessed by JavaScript)
- ✅ Browser automatically sends cookie with each request
- ✅ Protected against XSS attacks!
- ✅ Automatic logout via cookie clearing

## How to Use from Frontend

### 1. Login (Get JWT in Cookie)
```javascript
// POST to /api/v1/auth/login
const response = await fetch('http://localhost:8080/api/v1/auth/login', {
  method: 'POST',
  credentials: 'include',  // IMPORTANT: Required to receive cookies
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
//   "message": "Login successful",
//   "username": "janne",
//   "expiresIn": 86400000  // milliseconds (24 hours)
// }

// Cookie "JWT-TOKEN" is automatically set by browser (HttpOnly, can't access via JS)
```

### 2. Make Authenticated Requests
```javascript
// For protected endpoints (POST, PUT, DELETE)
const response = await fetch('http://localhost:8080/api/v1/projects', {
  method: 'POST',
  credentials: 'include',  // IMPORTANT: Sends cookie automatically
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    name: 'New Project',
    description: 'Project description'
  })
});
```

### 3. Logout
```javascript
// POST to /api/v1/auth/logout
const response = await fetch('http://localhost:8080/api/v1/auth/logout', {
  method: 'POST',
  credentials: 'include'  // IMPORTANT: Required to clear cookie
});

if (response.ok) {
  // Cookie cleared, redirect to login page
  window.location.href = '/login';
}
```

### 4. Check Authentication Status
```javascript
// GET /api/v1/auth/status
const response = await fetch('http://localhost:8080/api/v1/auth/status', {
  credentials: 'include'  // IMPORTANT: Required to send cookie
});

const data = await response.json();
// Response when logged in:
// {
//   "authenticated": true,
//   "username": "janne"
// }

// Response when not logged in:
// {
//   "authenticated": false,
//   "username": null
// }

// Use on app initialization to check login state
if (data.authenticated) {
  // User is logged in, proceed to dashboard
  console.log(`Welcome back, ${data.username}!`);
} else {
  // User is not logged in, show login page
  window.location.href = '/login';
}
```

### 5. Handle Token Expiration
```javascript
// If you get 403 Forbidden, cookie might be expired
if (response.status === 403) {
  // Redirect to login
  window.location.href = '/login';
}
```

## API Endpoints

### Authentication
- **POST** `/api/v1/auth/login` - Login and get JWT in HttpOnly cookie
  - Body: `{"username": "string", "password": "string"}`
  - Returns: `{"message": "Login successful", "username": "string", "expiresIn": number}`
  - Sets Cookie: `JWT-TOKEN` (HttpOnly, Secure in production, SameSite=Lax)

- **POST** `/api/v1/auth/logout` - Logout and clear JWT cookie
  - No body required
  - Returns: 200 OK
  - Clears Cookie: `JWT-TOKEN`

- **GET** `/api/v1/auth/status` - Check authentication status
  - No body required
  - Returns: `{"authenticated": boolean, "username": "string" | null}`
  - Use on page load to check if user is logged in

### Projects (Examples)
- **GET** `/api/v1/projects` - List all projects (public, no auth needed)
- **GET** `/api/v1/projects/{uuid}` - Get specific project (public)
- **POST** `/api/v1/projects` - Create project (requires JWT cookie with ADMIN role)
- **PUT** `/api/v1/projects/{uuid}` - Update project (requires JWT cookie with ADMIN role)
- **PATCH** `/api/v1/projects/{uuid}/index` - Update project index (requires JWT cookie with ADMIN role)
  - Body: `{"index": number}`
  - Returns: Updated project
- **DELETE** `/api/v1/projects/{uuid}` - Delete project (requires JWT cookie with ADMIN role)

## Configuration
Current settings in `application.yaml`:
```yaml
jwt:
  secret: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
  expiration: 86400000  # 24 hours in milliseconds
  cookie:
    name: JWT-TOKEN
    secure: false  # Set to true in production (requires HTTPS)
    same-site: Lax  # CSRF protection

cors:
  enabled: true
  allowed-origins:
    - http://localhost:3000  # React default
    - http://localhost:5173  # Vite default
    - http://localhost:8081  # Alternative port
  allowed-methods:
    - GET
    - POST
    - PUT
    - PATCH
    - DELETE
    - OPTIONS
  allowed-headers:
    - "*"
  allow-credentials: true  # Required for cookies
  max-age: 3600  # Preflight cache duration in seconds

spring:
  security:
    user:
      name: janne
      password: adminpassword
      roles: ADMIN
```

### CORS Configuration
The application includes configurable CORS support for cross-origin requests:

**Key Settings:**
- `cors.enabled` - Enable/disable CORS (default: true)
- `cors.allowed-origins` - List of allowed origins (your frontend URLs)
- `cors.allow-credentials` - **MUST be true** for cookie-based auth
- `cors.allowed-methods` - HTTP methods to allow
- `cors.allowed-headers` - Request headers to allow
- `cors.max-age` - How long browsers cache preflight responses

**Production Example:**
```yaml
cors:
  enabled: true
  allowed-origins:
    - https://yourdomain.com
    - https://www.yourdomain.com
  allow-credentials: true
```

**Development Example (allow all origins - NOT for production):**
```yaml
cors:
  allowed-origins:
    - "*"
  allow-credentials: false  # Cannot use credentials with wildcard
```

⚠️ **Important:** When `allow-credentials: true`, you cannot use `*` for origins. You must specify exact domains.

## Security Features
- ✅ **HttpOnly Cookie** - JavaScript cannot access the token (XSS protection)
- ✅ **SameSite=Lax** - Protection against CSRF attacks
- ✅ **Secure Flag** - Cookie only sent over HTTPS (when enabled in production)
- ✅ **24-hour expiration** - Token automatically expires
- ✅ **Explicit logout** - Users can invalidate their session
- ✅ **Stateless sessions** - No server-side session storage needed

## Security Notes
- Token expires after 24 hours (configurable)
- Only ADMIN role can create/update/delete projects
- GET endpoints are public (no authentication required)
- Invalid tokens return 403 Forbidden
- Wrong username/password returns 403 Forbidden
- Cookie is automatically managed by the browser

## Frontend Requirements
⚠️ **CRITICAL**: Always include `credentials: 'include'` in fetch requests:
```javascript
fetch(url, {
  method: 'POST',
  credentials: 'include',  // This is REQUIRED!
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(data)
})
```

For Axios:
```javascript
axios.defaults.withCredentials = true;  // Set globally

// Or per request:
axios.post(url, data, { withCredentials: true })
```

## CORS Configuration
The application includes built-in configurable CORS support. **No additional code needed** - just configure in `application.yaml`:

```yaml
cors:
  enabled: true
  allowed-origins:
    - http://localhost:3000      # Your frontend URL
  allowed-methods:
    - GET
    - POST
    - PUT
    - PATCH
    - DELETE
    - OPTIONS
  allowed-headers:
    - "*"
  allow-credentials: true         # Required for cookies!
  max-age: 3600
```

### Important CORS Rules:
1. **`allow-credentials: true`** is **required** for cookie-based authentication
2. When `allow-credentials: true`, you **cannot** use `*` for `allowed-origins`
3. Must specify exact frontend URLs in `allowed-origins`
4. Browser will block requests if CORS not configured correctly

### Common CORS Issues:
- ❌ **"No 'Access-Control-Allow-Origin' header"** → Add your frontend URL to `allowed-origins`
- ❌ **"The value of the 'Access-Control-Allow-Origin' header must not be the wildcard '*'"** → Cannot use `*` with `allow-credentials: true`
- ❌ **Cookies not being sent** → Ensure `allow-credentials: true` AND frontend uses `credentials: 'include'`
- ❌ **"CORS request did not succeed" for PATCH/PUT requests** → Add the HTTP method to `allowed-methods` list (e.g., PATCH)

## Testing Examples (curl)
```bash
# 1. Login (saves cookie to file)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"janne","password":"adminpassword"}' \
  -c cookies.txt

# 2. Check authentication status
curl http://localhost:8080/api/v1/auth/status \
  -b cookies.txt

# 3. Use cookie for protected endpoint
curl -X POST http://localhost:8080/api/v1/projects \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Project","description":"Test"}' \
  -b cookies.txt

# 4. Logout (clears cookie)
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -b cookies.txt \
  -c cookies.txt

# 5. Verify logged out
curl http://localhost:8080/api/v1/auth/status \
  -b cookies.txt
```

## Migration from Token-Based Auth
### What Changed:
- ❌ **Removed**: Token in response body
- ✅ **Added**: HttpOnly cookie automatically set/cleared
- ✅ **Added**: Logout endpoint
- ⚠️ **Breaking**: LoginResponse structure changed

### Frontend Changes Required:
1. **Add `credentials: 'include'`** to all fetch/axios requests
2. **Remove** token storage code (localStorage/sessionStorage)
3. **Update** login success handler (no token in response)
4. **Implement** logout by calling `/auth/logout` endpoint

### Backwards Compatibility:
The authentication filter still accepts the `Authorization: Bearer <token>` header as a fallback during migration, but this should not be used for new implementations.

## Benefits Over Token Storage
| Feature | HttpOnly Cookie | localStorage/sessionStorage |
|---------|----------------|---------------------------|
| XSS Protection | ✅ Immune | ❌ Vulnerable |
| Automatic sending | ✅ Yes | ❌ Manual |
| CSRF Protection | ✅ SameSite | ⚠️ Need CSRF tokens |
| Mobile-friendly | ⚠️ Varies | ✅ Yes |
| Debugging | ⚠️ Hidden | ✅ Visible |
