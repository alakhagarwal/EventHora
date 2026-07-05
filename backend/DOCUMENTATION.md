# EventHora Backend — API Documentation

> **Base URL:** `http://localhost:8080`
> **Content-Type:** `application/json`
> **Auth:** All protected endpoints require `Authorization: Bearer <token>` header.

---

## Authentication Overview

The system has **two types of identities**:

| Identity | Has Account | Authentication Method |
|---|---|---|
| **System User** (ADMIN / STAFF) | ✅ Yes | Email + Password → JWT |
| **Member** (RIC Member) | ❌ No | RIC API Verify + OTP → Redis Session Token |

This document covers **System User (JWT) auth only**.

---

## Roles

### `ADMIN`
Full control of the platform.
- Create, edit, publish events
- Create and manage STAFF accounts
- View all registrations and payment reports
- Manage Pay Later payments and fee remittances

### `STAFF`
Day-to-day operational access only.
- Register members on behalf of the chapter
- Record cash payments on event day
- Scan QR codes at the entry gate
- View registrations for events
- **Cannot** create/modify events
- **Cannot** access financial reports or account management

---

## Auth Endpoints

### 1. Login

Login for both ADMIN and STAFF. The role embedded in the JWT controls what the user can do.

```
POST /api/auth/login
```

**Access:** Public (no JWT required)

**Request Body:**
```json
{
  "email": "admin@eventric.org",
  "password": "your-password"
}
```

**Success Response `200 OK`:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "role": "ADMIN",
  "name": "Alakh Agarwal",
  "email": "admin@eventric.org"
}
```

**Error Response `401 Unauthorized`:**
```json
{
  "error": "Invalid email or password"
}
```

**How to use the token:**
After receiving the `accessToken`, include it in every subsequent request:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Token Expiry:** 1 hour. After expiry, the user must log in again.

---

### 2. Get My Profile

Returns the profile of the currently authenticated user.

```
GET /api/auth/me
```

**Access:** ADMIN, STAFF

**Request Headers:**
```
Authorization: Bearer <token>
```

**Success Response `200 OK`:**
```json
{
  "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "name": "Alakh Agarwal",
  "email": "admin@eventric.org",
  "role": "ADMIN",
  "createdAt": "2026-07-05T15:00:00"
}
```

---

### 3. Create a System User Account

ADMIN creates a new STAFF or ADMIN account.

```
POST /api/auth/users
```

**Access:** ADMIN only

**Request Headers:**
```
Authorization: Bearer <admin-token>
```

**Request Body:**
```json
{
  "name": "Rahul Sharma",
  "email": "rahul@eventric.org",
  "password": "securePassword123",
  "role": "STAFF"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `name` | String | ✅ | Full name |
| `email` | String | ✅ | Must be a valid email, must be unique |
| `password` | String | ✅ | Will be BCrypt-hashed before storing |
| `role` | String | ✅ | `"ADMIN"` or `"STAFF"` |

**Success Response `201 Created`:**
```json
{
  "id": "a1b2c3d4-...",
  "name": "Rahul Sharma",
  "email": "rahul@eventric.org",
  "role": "STAFF",
  "createdAt": "2026-07-05T15:05:00"
}
```

**Error Response `400 Bad Request` (duplicate email):**
```json
{
  "error": "A user with this email already exists"
}
```

---

### 4. List All System Users

ADMIN views all registered ADMIN and STAFF accounts.

```
GET /api/auth/users
```

**Access:** ADMIN only

**Request Headers:**
```
Authorization: Bearer <admin-token>
```

**Success Response `200 OK`:**
```json
[
  {
    "id": "d290f1ee-...",
    "name": "Alakh Agarwal",
    "email": "admin@eventric.org",
    "role": "ADMIN",
    "createdAt": "2026-07-01T10:00:00"
  },
  {
    "id": "a1b2c3d4-...",
    "name": "Rahul Sharma",
    "email": "rahul@eventric.org",
    "role": "STAFF",
    "createdAt": "2026-07-05T15:05:00"
  }
]
```

---

### 5. Deactivate a User Account

ADMIN soft-deletes a STAFF (or ADMIN) account. Sets `active = false`. The user can no longer log in.

```
PATCH /api/auth/users/{email}/deactivate
```

**Access:** ADMIN only

**Request Headers:**
```
Authorization: Bearer <admin-token>
```

**Path Parameter:**

| Param | Type | Description |
|---|---|---|
| `email` | String | Email address of the user to deactivate |

**Example:**
```
PATCH /api/auth/users/rahul@eventric.org/deactivate
```

**Success Response `200 OK`:**
```json
{
  "message": "User deactivated successfully"
}
```

---

## JWT Token Details

### What's Inside the Token (Decoded Payload)

```json
{
  "sub": "admin@eventric.org",
  "role": "ADMIN",
  "userId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "name": "Alakh Agarwal",
  "iat": 1720000000,
  "exp": 1720003600
}
```

| Claim | Description |
|---|---|
| `sub` | Email of the user (subject) |
| `role` | `ADMIN` or `STAFF` |
| `userId` | UUID of the user in the database |
| `name` | Display name |
| `iat` | Issued at (Unix timestamp) |
| `exp` | Expiry time (Unix timestamp) — 1 hour after `iat` |

### Token Signing
- Algorithm: **HMAC-SHA256 (HS256)**
- Secret key is configured in `application.properties` under `jwt.secret`

---

## Error Responses

All error responses follow this structure:

```json
{
  "error": "Human readable error message"
}
```

| HTTP Status | When It Happens |
|---|---|
| `400 Bad Request` | Validation failed (missing fields, bad email format) |
| `401 Unauthorized` | Wrong credentials, or JWT missing/expired |
| `403 Forbidden` | Valid JWT, but insufficient role (e.g. STAFF hitting ADMIN-only route) |
| `404 Not Found` | Resource not found |
| `500 Internal Server Error` | Unexpected server error |

---

## Testing & Default Users

When running the application with the `dev` profile active (`spring.profiles.active=dev` in `application.properties`), the database will automatically be seeded with two default users on startup.

| Role | Email | Password |
|---|---|---|
| **ADMIN** | `admin@eventhora.com` | `Admin@1234` |
| **STAFF** | `staff@eventhora.com` | `Staff@1234` |

You can use these credentials to hit the `/api/auth/login` endpoint to receive your initial JWT for testing the other protected endpoints.

---

## File Structure (Auth Module)

```
src/main/java/com/eventHora/backend/
│
├── user/
│   ├── SystemUser.java            # JPA Entity mapped to system_users table
│   ├── SystemUserRepository.java  # Spring Data JPA repository
│   └── Role.java                  # Enum: ADMIN, STAFF
│
├── security/
│   ├── JwtProvider.java           # Generates + validates JWT tokens
│   ├── JwtAuthFilter.java         # Intercepts every request, validates JWT
│   └── SecurityConfig.java        # Spring Security filter chain + role config
│
└── auth/
    ├── AuthController.java        # REST controller — 5 endpoints
    ├── AuthService.java           # Business logic
    └── dto/
        ├── LoginRequest.java      # { email, password }
        ├── LoginResponse.java     # { accessToken, tokenType, role, name, email }
        ├── CreateStaffRequest.java # { name, email, password, role }
        └── UserProfileResponse.java # { id, name, email, role, createdAt }
```

---

## How Auth Works Internally (Step-by-Step)

```
1. User sends POST /api/auth/login with { email, password }

2. AuthController calls AuthService.login()

3. AuthService calls AuthenticationManager.authenticate()
   → Spring Security calls DaoAuthenticationProvider
   → DaoAuthenticationProvider loads user from DB via SystemUserRepository
   → Compares password using BCryptPasswordEncoder
   → Throws AuthenticationException if invalid

4. On success: JwtProvider.generateToken(user) creates a signed JWT
   containing { sub: email, role, userId, name, exp: now + 1hr }

5. JWT returned to frontend in LoginResponse

6. Frontend stores token (localStorage or cookie)

7. On every subsequent request, frontend sends:
   Authorization: Bearer <token>

8. JwtAuthFilter intercepts the request:
   → Extracts token from header
   → JwtProvider.extractEmail(token) → gets email
   → Loads user from DB
   → JwtProvider.isTokenValid(token, email) checks signature + expiry
   → Sets UsernamePasswordAuthenticationToken in SecurityContextHolder

9. Spring Security checks @PreAuthorize annotations on controller methods
   → ADMIN can hit all routes
   → STAFF is blocked from ADMIN-only routes (403)
```

