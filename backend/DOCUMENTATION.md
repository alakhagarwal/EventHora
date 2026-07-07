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

---

## Event Management API

> **Access:** All event management endpoints require ADMIN role.
> Add `Authorization: Bearer <admin-token>` to every request.

### Event Status Lifecycle

```
DRAFT → PUBLISHED → COMPLETED
           ↓
       CANCELLED
```

- Events start as **DRAFT** and are invisible to members.
- ADMIN explicitly calls `/publish` to make them live.
- **CANCELLED** and **COMPLETED** are terminal states.

---

### 1. Create Event

```
POST /api/events
```

**Request Body** (`application/json`):
```json
{
  "title": "Mere Mehboob Na Ja…",
  "description": "A musical tribute to Suman Kalyanpur...",
  "category": "MUSIC",
  "eventDate": "2026-07-08",
  "startTime": "18:30:00",
  "endTime": "20:00:00",
  "registrationDeadline": "2026-07-07T15:00:00",
  "venue": "Main Audi, RIC",
  "additionalVenueInfo": "Convention Hall with Lawn",
  "totalCapacity": 500,
  "maxTicketsPerMember": 4,
  "freeTicketsPerRegistration": 2,
  "ticketPrice": 1000.00,
  "platformFeePerTicket": 0.00,
  "minimumAge": 18,
  "importantNotes": [
    "Please carry your membership card",
    "Blocking seats for later arrivals is not permitted"
  ],
  "contactPersonName": "Mr. Keyur Patel, Marketing Manager",
  "contactPersonPhone": "9462200225"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `title` | String | ✅ | Event name |
| `description` | String | ✅ | Full invite text |
| `category` | Enum | ✅ | `MUSIC`, `DANCE`, `CULTURAL`, `EDUCATIONAL`, `SOCIAL`, `SPORTS`, `OTHER` |
| `eventDate` | Date `YYYY-MM-DD` | ✅ | Must be today or future |
| `startTime` | Time `HH:mm:ss` | ✅ | |
| `endTime` | Time `HH:mm:ss` | ✅ | |
| `registrationDeadline` | DateTime | ✅ | Must be in the future |
| `venue` | String | ✅ | Primary venue |
| `additionalVenueInfo` | String | ❌ | Secondary venue (e.g., for gala dinner) |
| `totalCapacity` | Integer | ✅ | Min 1 |
| `maxTicketsPerMember` | Integer | ✅ | Total tickets per registration (member + anyone they bring) |
| `freeTicketsPerRegistration` | Integer | ✅ | How many of those are free |
| `ticketPrice` | Decimal | ✅ | Unified price per paid ticket. `0.00` for fully free events |
| `platformFeePerTicket` | Decimal | ✅ | EventHora fee per paid ticket |
| `minimumAge` | Integer | ❌ | `null` = no restriction |
| `importantNotes` | Array of strings | ❌ | Bullet points shown on event page |
| `contactPersonName` | String | ❌ | |
| `contactPersonPhone` | String | ❌ | |

**Success Response `201 Created`:**
```json
{
  "id": "uuid-of-event",
  "title": "Mere Mehboob Na Ja…",
  "status": "DRAFT",
  "uniqueEventLink": "mere-mehboob-na-ja-3f8a2b",
  "createdByName": "EventHora Admin",
  ...
}
```

---

### 2. Update Event

Partially updates an event. Only fields present in the body are changed. Omit any field you don't want to change.

```
PATCH /api/events/{id}
```

**Example — update only venue and capacity:**
```json
{
  "venue": "Mini Audi-1, RIC",
  "totalCapacity": 300
}
```

**Success Response `200 OK`:** Full `EventResponse` with all updated fields.

---

### 3. Publish Event

Transitions a `DRAFT` event to `PUBLISHED`, making it visible to members for registration.

```
PATCH /api/events/{id}/publish
```

No request body needed.

**Success Response `200 OK`:** Full `EventResponse` with `"status": "PUBLISHED"`.

**Error `409 Conflict`:** If the event is already `CANCELLED`.

---

### 4. Cancel Event

Marks an event as `CANCELLED`. This is a soft operation — the event remains in the database.

```
DELETE /api/events/{id}
```

No request body needed.

**Success Response `200 OK`:**
```json
{
  "message": "Event cancelled successfully"
}
```

**Error `409 Conflict`:** If the event is already `COMPLETED`.

---

### 5. List All Events (Admin Dashboard)

Returns a summary list of all events in the system, ordered by event date (newest first). Includes events of all statuses.

```
GET /api/admin/events
```

**Success Response `200 OK`:**
```json
[
  {
    "id": "uuid",
    "title": "Mere Mehboob Na Ja…",
    "category": "MUSIC",
    "bannerUrl": "https://bucket.s3.region.amazonaws.com/events/banners/uuid.jpg",
    "eventDate": "2026-07-08",
    "startTime": "18:30:00",
    "venue": "Main Audi, RIC",
    "status": "PUBLISHED",
    "uniqueEventLink": "mere-mehboob-na-ja-3f8a2b",
    "totalCapacity": 500,
    "bookedCount": 120,
    "availableCount": 380,
    "registrationOpen": true,
    "isSoldOut": false
  }
]
```

---

### 6. Upload Event Banner

Uploads a banner/poster image for an event to AWS S3. The S3 URL is automatically saved to the event. If a banner already exists, it is deleted from S3 before uploading the new one.

```
POST /api/events/{id}/banner
Content-Type: multipart/form-data
```

| Form field | Type | Required | Notes |
|---|---|---|---|
| `file` | File | ✅ | Image file (JPG, PNG, WebP recommended) |

**Success Response `200 OK`:** Full `EventResponse` with the updated `bannerUrl` field.

**How the URL is formed:**
```
https://{bucket-name}.s3.{region}.amazonaws.com/events/banners/{uuid}.jpg
```

This URL is saved to `Event.bannerUrl` and is what the frontend uses to display the banner.

---

### 7. List Public Events (Member Landing Page)

Returns a summary list of all `PUBLISHED` events, ordered by event date (newest first). 

```
GET /api/events
```
> **Access:** PUBLIC (No token required)

**Success Response `200 OK`:**
Returns an array of `EventSummaryResponse` objects. Each object includes:
- `registrationOpen` (boolean): `true` if the event is published, the deadline has not passed, and it is not sold out.
- `isSoldOut` (boolean): `true` if `bookedCount >= totalCapacity`.

---

### 8. Get Public Event Details

Returns the full details of a single `PUBLISHED` event by its unique link. Used for the event details page where a member begins the booking process.

```
GET /api/events/{link}
```
> **Access:** PUBLIC (No token required)

**Success Response `200 OK`:**
Returns a `PublicEventResponse` object containing the event details, ticket limits, rules, and notes.
Also includes `registrationOpen` and `isSoldOut` booleans to easily determine the booking status.

**Error `404 Not Found`:**
If the slug doesn't exist or the event is in `DRAFT`/`CANCELLED` status.

---

## File Structure (Event Module)

```
src/main/java/com/eventHora/backend/
│
├── Enum/
│   ├── EventStatus.java        # DRAFT, PUBLISHED, CANCELLED, COMPLETED
│   ├── EventCategory.java      # MUSIC, DANCE, CULTURAL, etc.
│   └── SeatingType.java        # FIRST_COME_FIRST_SERVED, ASSIGNED_SEATING
│
├── model/
│   └── Event.java              # JPA Entity → events table + event_notes table
│
├── repository/
│   └── EventRepository.java    # findAllByDate, findByStatus, findBySlug
│
├── service/
│   ├── EventService.java       # Business logic for all 6 endpoints
│   └── S3Service.java          # File upload / delete / presigned URL
│
├── controller/
│   └── EventController.java    # REST endpoints
│
└── dto/
    ├── CreateEventRequest.java      # POST body
    ├── UpdateEventRequest.java      # PATCH body (all optional)
    ├── EventResponse.java           # Full response for admin/staff
    ├── PublicEventResponse.java     # Stripped response for members
    └── EventSummaryResponse.java    # Minimal card for list views
```
