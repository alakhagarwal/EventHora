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

> **Access:** Most event management endpoints require ADMIN role. Read-only dashboard endpoints are also accessible to STAFF.
> Add `Authorization: Bearer <token>` to every request.

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

### 5. List All Events (Admin & Staff Dashboard)

Returns a summary list of all events in the system, ordered by event date (newest first). Includes events of all statuses.

```
GET /api/admin/events
```

**Access:** ADMIN, STAFF

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

### 6. Get Single Event (Admin & Staff Detail View)

Returns the **full details** of a single event regardless of its status (DRAFT, PUBLISHED, CANCELLED). Use this before making a `PATCH` update call so you can see the current values of every field.

```
GET /api/admin/events/{id}
```

**Access:** ADMIN, STAFF

No request body needed.

**Success Response `200 OK`:** Full `EventResponse` with all fields:
```json
{
  "id": "uuid-of-event",
  "title": "Mere Mehboob Na Ja…",
  "description": "A musical tribute...",
  "category": "MUSIC",
  "bannerUrl": "https://...",
  "eventDate": "2026-07-08",
  "startTime": "18:30:00",
  "endTime": "20:00:00",
  "registrationDeadline": "2026-07-07T15:00:00",
  "venue": "Main Audi, RIC",
  "additionalVenueInfo": "Convention Hall with Lawn",
  "totalCapacity": 500,
  "bookedCount": 120,
  "availableCount": 380,
  "maxTicketsPerMember": 4,
  "freeTicketsPerRegistration": 2,
  "ticketPrice": 1000.00,
  "platformFeePerTicket": 0.00,
  "minimumAge": 18,
  "importantNotes": ["Please carry your membership card"],
  "contactPersonName": "Mr. Keyur Patel",
  "contactPersonPhone": "9462200225",
  "status": "PUBLISHED",
  "uniqueEventLink": "mere-mehboob-na-ja-3f8a2b",
  "createdByName": "EventHora Admin",
  "createdAt": "2026-07-01T10:00:00",
  "updatedAt": "2026-07-05T14:30:00"
}
```

**Error `404 Not Found`:** If no event exists with that ID.

---

### 7. Upload Event Banner


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

## Admin Reporting API

> **Access:** All endpoints in this section require a valid JWT with `ADMIN` or `STAFF` role.
> Add `Authorization: Bearer <token>` to every request.

These endpoints give admins and staff visibility into who registered for an event, what payments were collected, and how full each event is.

---

### 1. List All Registrations for an Event

Returns the full attendee list for a specific event. One row per registration (booking). Ordered by booking time, newest first.

```
GET /api/admin/events/{eventId}/registrations
```

**Access:** ADMIN or STAFF

**Path Parameters:**

| Parameter | Type | Required | Notes |
|---|---|---|---|
| `eventId` | UUID | ✅ | The internal UUID of the event |

**Success Response `200 OK`:**

Returns an array. Each element is one member's booking:

```json
[
  {
    "registrationId": "uuid-of-registration",
    "ticketReference": "TKT-2026-AB12CD",
    "memberId": "RIC-2024-04512",
    "memberType": "INDIAN",
    "quantity": 2,
    "totalAmount": 2000.00,
    "paymentStatus": "CONFIRMED",
    "paymentPreference": "ONLINE",
    "isCheckedIn": true,
    "checkedInAt": "2026-07-08T18:35:22",
    "bookedAt": "2026-07-05T10:30:00"
  },
  {
    "registrationId": "uuid-of-registration-2",
    "ticketReference": "TKT-2026-XY9Z01",
    "memberId": "RIC-2024-08821",
    "memberType": "OVERSEAS",
    "quantity": 1,
    "totalAmount": 1000.00,
    "paymentStatus": "PAY_AT_GATE",
    "paymentPreference": "PAY_AT_GATE",
    "isCheckedIn": false,
    "checkedInAt": null,
    "bookedAt": "2026-07-06T14:15:00"
  }
]
```

**Response Fields:**

| Field | Type | Notes |
|---|---|---|
| `registrationId` | UUID | Internal ID (for admin operations) |
| `ticketReference` | String | User-facing ticket ID (also in QR code) |
| `memberId` | String | RIC Member ID |
| `memberType` | String | `INDIAN` or `OVERSEAS` |
| `quantity` | Integer | Number of tickets booked by this member |
| `totalAmount` | Decimal | Total charged for this booking |
| `paymentStatus` | String | `CONFIRMED`, `FREE`, `PAY_AT_GATE`, `COMPLIMENTARY`, `PENDING`, `FAILED` |
| `paymentPreference` | String | How member chose to pay: `ONLINE` or `PAY_AT_GATE` |
| `isCheckedIn` | Boolean | Whether the member has been scanned at the gate |
| `checkedInAt` | DateTime | Gate check-in timestamp — `null` if not yet checked in |
| `bookedAt` | DateTime | When the booking was created |

**Error Responses:**

| HTTP | Scenario |
|---|---|
| `404 Not Found` | Event ID does not exist |
| `401 Unauthorized` | JWT missing or expired |
| `403 Forbidden` | Non-ADMIN role (STAFF cannot access this endpoint) |

---

### 2. Payment Summary for an Event

Returns a single-glance financial and capacity snapshot for one event: seat availability, registration counts by payment status, gate check-in statistics, and a revenue breakdown.

```
GET /api/admin/events/{eventId}/payment-summary
```

**Access:** ADMIN or STAFF

**Path Parameters:**

| Parameter | Type | Required | Notes |
|---|---|---|---|
| `eventId` | UUID | ✅ | The internal UUID of the event |

**Success Response `200 OK`:**

```json
{
  "totalCapacity": 500,
  "seatsLocked": 145,
  "seatsRemaining": 355,

  "confirmedCount": 110,
  "payAtGateCount": 25,
  "freeCount": 5,
  "complimentaryCount": 5,
  "pendingCount": 3,
  "failedCount": 8,
  "totalRegistrations": 156,

  "checkedInCount": 118,
  "notCheckedInCount": 27,
  "checkedInTickets": 130,
  "notCheckedInTickets": 15,

  "totalRevenue": 110000.00,
  "pendingGateCollection": 25000.00,
  "complimentaryWaived": 5000.00
}
```

> **Units note:** Capacity fields (`totalCapacity`, `seatsLocked`, `seatsRemaining`) are measured in **tickets** (individual seats). Registration count fields (`confirmedCount`, etc.) are measured in **bookings** — one per member, regardless of how many tickets they booked. Check-in stats exist in both units — see field descriptions below.

**Response Fields Explained:**

| Field | Unit | Notes |
|---|---|---|
| `totalCapacity` | Tickets | Total seats configured for the event |
| `seatsLocked` | Tickets | Sum of `quantity` for `CONFIRMED + FREE + PAY_AT_GATE + COMPLIMENTARY` bookings |
| `seatsRemaining` | Tickets | `totalCapacity - seatsLocked` (0 if sold out) |
| `confirmedCount` | Bookings | Members whose online payment was collected via Razorpay |
| `payAtGateCount` | Bookings | Members who reserved a seat but cash not yet collected |
| `freeCount` | Bookings | Members on free events (no payment required) |
| `complimentaryCount` | Bookings | Members whose fee was waived by staff at the gate |
| `pendingCount` | Bookings | Members with an incomplete Razorpay payment (seat NOT held) |
| `failedCount` | Bookings | Members whose payment failed or timed out (seat NOT held) |
| `totalRegistrations` | Bookings | All booking rows in the DB (every status combined) |
| `checkedInCount` | Bookings | Number of members who have been scanned at the gate |
| `notCheckedInCount` | Bookings | Locked members who haven't arrived at the gate yet |
| `checkedInTickets` | Tickets | Sum of `quantity` for checked-in registrations — comparable to `seatsLocked` |
| `notCheckedInTickets` | Tickets | `seatsLocked - checkedInTickets` — seats still expected to arrive |
| `totalRevenue` | Money | Sum of `totalAmount` for all `CONFIRMED` bookings (online money collected) |
| `pendingGateCollection` | Money | Sum of `totalAmount` for `PAY_AT_GATE` bookings (cash not yet collected) |
| `complimentaryWaived` | Money | Sum of `totalAmount` for `COMPLIMENTARY` bookings (fees waived) |

> **Note:** `PENDING` and `FAILED` bookings do **not** count towards `seatsLocked`. They are included in `totalRegistrations` for visibility only — they hold no seat.

**Error Responses:**

| HTTP | Scenario |
|---|---|
| `404 Not Found` | Event ID does not exist |
| `401 Unauthorized` | JWT missing or expired |
| `403 Forbidden` | Non-ADMIN/STAFF role |

---

### 3. Admin Dashboard

Returns a platform-wide snapshot across all events in a single call — event status breakdown, all-time registration and revenue figures, and this-month stats. Designed to power an admin/staff home screen.

```
GET /api/admin/dashboard
```

**Access:** ADMIN or STAFF

No request body or query parameters needed.

**Success Response `200 OK`:**

```json
{
  "totalEvents": 12,
  "publishedEvents": 3,
  "upcomingEvents": 2,
  "draftEvents": 4,
  "completedEvents": 4,
  "cancelledEvents": 1,

  "totalRegistrations": 480,
  "lockedRegistrations": 445,
  "totalTicketsSold": 620,

  "registrationsThisMonth": 85,
  "ticketsSoldThisMonth": 115,

  "totalRevenue": 440000.00,
  "pendingGateCollection": 35000.00,
  "complimentaryWaived": 5000.00,

  "revenueThisMonth": 82000.00
}
```

**Response Fields Explained:**

| Field | Unit | Notes |
|---|---|---|
| `totalEvents` | Events | All events in the system, every status |
| `publishedEvents` | Events | Events currently in `PUBLISHED` status |
| `upcomingEvents` | Events | `PUBLISHED` events with `eventDate >= today` |
| `draftEvents` | Events | Events in `DRAFT` (not yet live) |
| `completedEvents` | Events | Events marked `COMPLETED` |
| `cancelledEvents` | Events | Events marked `CANCELLED` |
| `totalRegistrations` | Bookings | All registration rows in the DB (every status) |
| `lockedRegistrations` | Bookings | `CONFIRMED + FREE + PAY_AT_GATE + COMPLIMENTARY` bookings (hold a seat) |
| `totalTicketsSold` | Tickets | Sum of `quantity` across all locked registrations |
| `registrationsThisMonth` | Bookings | All registrations created in the current calendar month (1st to today) |
| `ticketsSoldThisMonth` | Tickets | Sum of `quantity` for locked registrations created this month |
| `totalRevenue` | Money (₹) | Sum of `totalAmount` for all `CONFIRMED` registrations (online payments collected) |
| `pendingGateCollection` | Money (₹) | Sum of `totalAmount` for `PAY_AT_GATE` registrations (cash not yet collected) |
| `complimentaryWaived` | Money (₹) | Sum of `totalAmount` for `COMPLIMENTARY` registrations (fees waived by staff) |
| `revenueThisMonth` | Money (₹) | Sum of `totalAmount` for `CONFIRMED` registrations created this month |

> **Note on "this month":** All `*ThisMonth` fields are scoped to the current **calendar month** from the 1st at midnight to the current moment. They reset on the 1st of every new month automatically.

> **Note on `upcomingEvents` vs `publishedEvents`:** `publishedEvents` includes all live events regardless of date (some may have already happened but not yet been marked `COMPLETED`). `upcomingEvents` is the subset where `eventDate >= today`.

**Error Responses:**

| HTTP | Scenario |
|---|---|
| `401 Unauthorized` | JWT missing or expired |
| `403 Forbidden` | Non-ADMIN/STAFF role |

---

## Staff Operations API

> **Access:** All endpoints in this section require a valid JWT with `STAFF` or `ADMIN` role.
> Add `Authorization: Bearer <token>` to every request.

These endpoints are designed for use on event day — typically by a STAFF member on their phone or tablet at the entry gate.

---

### Event Day Gate Application Flow (Frontend Guide)

For developers building the gate scanner application, here is the expected step-by-step UI flow for every possible scenario when staff scans a ticket.

#### Scenario A: Member has already paid (or it's a free event)
**Status:** `CONFIRMED`, `FREE`, or `COMPLIMENTARY`
1. Staff opens the scanner app and scans the member's QR code.
2. App calls `POST /api/staff/checkin`.
3. Backend returns `200 OK` (with `alreadyCheckedIn: false`).
4. **UI Action:** Show a **Green Success Screen** with the member's ID and quantity of tickets. Admittance complete.

#### Scenario B: Member chose Pay-at-Gate
**Status:** `PAY_AT_GATE`
1. Staff scans the member's QR code.
2. App calls `POST /api/staff/checkin`.
3. Backend rejects the check-in and returns `409 Conflict` (Message: *"Payment collection required before entry..."*).
4. **UI Action:** App catches the `409` and automatically transitions to a **Payment Collection Screen** showing the total amount due.
5. Staff collects cash from the member and taps "Confirm Payment" in the app.
6. App calls `POST /api/staff/record-payment` with action `"PAID"`.
7. Backend returns `200 OK` (recording payment and check-in atomically).
8. **UI Action:** Show the **Green Success Screen**. Admittance complete.

#### Scenario C: Member abandoned online payment
**Status:** `PENDING`
1. Staff scans the member's QR code.
2. App calls `POST /api/staff/checkin`.
3. Backend returns `409 Conflict` (Message: *"This ticket has an incomplete online payment..."*).
4. **UI Action:** Show a **Red Error Screen** with the backend message. Staff instructs the member to step aside and make a fresh Pay-at-Gate booking on their phone (if seats remain).

#### Scenario D: Accidental Double-Scan
**Status:** Any valid status, but already checked in
1. Staff accidentally scans a QR code that was already scanned a few minutes ago.
2. App calls `POST /api/staff/checkin`.
3. Backend returns `200 OK` but with the flag `alreadyCheckedIn: true`.
4. **UI Action:** Show a **Yellow Warning Screen** ("⚠️ Already checked in"). Staff visually verifies if this is the same person who just walked in, or someone trying to share their ticket.

---

### 1. QR Code Gate Check-In

Scans a member's QR ticket at the event gate and records their entry.

```
POST /api/staff/checkin
```

**Access:** STAFF, ADMIN

**Request Body** (`application/json`):
```json
{
  "ticketReference": "TKT-2026-AB12CD"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `ticketReference` | String | ✅ | Embedded in the member's QR code |

---

#### Success Response `200 OK` — First-time scan

```json
{
  "ticketReference": "TKT-2026-AB12CD",
  "memberId": "RIC-2024-04512",
  "eventTitle": "Mere Mehboob Na Ja…",
  "quantity": 2,
  "totalAmount": 2000.00,
  "paymentStatus": "CONFIRMED",
  "alreadyCheckedIn": false,
  "checkedInAt": "2026-07-08T18:35:22",
  "message": "✅ Check-in successful"
}
```

The staff member's screen should display a **green confirmation** with the member's ID, event name, and number of tickets (people) admitted.

---

#### Success Response `200 OK` — Duplicate scan ⚠️

If the same QR code is scanned a second time (accidental re-scan), the endpoint still returns `200 OK` but with `alreadyCheckedIn: true`:

```json
{
  "ticketReference": "TKT-2026-AB12CD",
  "memberId": "RIC-2024-04512",
  "eventTitle": "Mere Mehboob Na Ja…",
  "quantity": 2,
  "totalAmount": 2000.00,
  "paymentStatus": "CONFIRMED",
  "alreadyCheckedIn": true,
  "checkedInAt": "2026-07-08T18:35:22",
  "message": "⚠️ Already checked in at 2026-07-08T18:35:22"
}
```

> **Why `200` and not an error?** Accidental double-scans are common (busy gates, QR re-shown on phone). A `409` would crash many frontend implementations and create confusion. Instead, the response carries the `alreadyCheckedIn` flag so the UI can show a distinct **yellow warning screen** vs. the green success screen.
>
> The `checkedInAt` timestamp preserves the **original** check-in time — it is never overwritten.

---

#### Error Responses

| HTTP | Scenario | Message |
|---|---|---|
| `404 Not Found` | Ticket reference doesn't exist | `"Ticket not found: TKT-2026-XXXXXX"` |
| `409 Conflict` | Payment is `PENDING` (online payment incomplete) | `"This ticket has an incomplete online payment. Please ask the member to make a new Pay-at-Gate booking if seats are still available."` |
| `409 Conflict` | Payment is `FAILED` | `"This ticket's payment failed. The member does not have a valid booking."` |
| `401 Unauthorized` | JWT missing or expired | Standard auth error |
| `403 Forbidden` | Insufficient role | Standard RBAC error |

---

#### Valid Statuses for Entry

| Payment Status | Admitted? | Notes |
|---|---|---|
| `CONFIRMED` | ✅ Yes | Online payment was successful |
| `FREE` | ✅ Yes | Free event or complimentary booking |
| `COMPLIMENTARY` | ✅ Yes | Fee waived by staff |
| `PAY_AT_GATE` | ➡️ Redirect | Use `POST /api/staff/record-payment` to collect payment and check in atomically |
| `PENDING` | ❌ No | Online payment started but not completed |
| `FAILED` | ❌ No | Payment attempt failed |

---

#### Database Change on Check-In

| Field | Before | After |
|---|---|---|
| `isCheckedIn` | `false` | `true` |
| `checkedInAt` | `null` | Current timestamp |

---

### 2. Record Gate Payment (PAY_AT_GATE tickets)

Records cash or complimentary collection for a `PAY_AT_GATE` ticket.

This is the **only way to admit a PAY_AT_GATE member**. Recording payment and checking in are one atomic operation — staff does **not** do a separate QR scan after this.

```
POST /api/staff/record-payment
```

**Access:** STAFF, ADMIN

**Request Body** (`application/json`):
```json
{
  "ticketReference": "TKT-2026-XY9Z01",
  "action": "PAID"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `ticketReference` | String | ✅ | From the member's QR code or booking |
| `action` | String | ✅ | `"PAID"` or `"COMPLIMENTARY"` |

**What each action does:**

| Action | New `paymentStatus` | Meaning |
|---|---|---|
| `PAID` | `CONFIRMED` | Member paid cash/card at the gate |
| `COMPLIMENTARY` | `COMPLIMENTARY` | Staff waived the fee for this member |

---

#### Success Response `200 OK`

```json
{
  "ticketReference": "TKT-2026-XY9Z01",
  "memberId": "RIC-2024-04512",
  "eventTitle": "Mere Mehboob Na Ja…",
  "quantity": 2,
  "totalAmount": 2000.00,
  "paymentStatus": "CONFIRMED",
  "alreadyCheckedIn": false,
  "checkedInAt": "2026-07-08T18:40:15",
  "message": "✅ Payment recorded and member checked in"
}
```

For `COMPLIMENTARY` action, `paymentStatus` will be `"COMPLIMENTARY"` and the message will be `"✅ Marked complimentary and member checked in"`.

---

#### Error Responses

| HTTP | Scenario | Message |
|---|---|---|
| `404 Not Found` | Ticket reference doesn't exist | `"Ticket not found: TKT-2026-XXXXXX"` |
| `400 Bad Request` | Invalid action value | `"Action must be 'PAID' or 'COMPLIMENTARY'"` |
| `409 Conflict` | Status is already `CONFIRMED` | `"This ticket has already been paid online and checked in via QR scan."` |
| `409 Conflict` | Status is `FREE` | `"This is a free ticket — no payment collection needed. Use QR check-in."` |
| `409 Conflict` | Status is already `COMPLIMENTARY` | `"This ticket has already been marked complimentary."` |
| `409 Conflict` | Status is `PENDING` | `"This ticket has an incomplete online payment, not a Pay-at-Gate booking."` |
| `409 Conflict` | Status is `FAILED` | `"This ticket's payment failed. The member does not have a valid booking."` |

---

#### Database Changes

| Field | Before | After |
|---|---|---|
| `paymentStatus` | `PAY_AT_GATE` | `CONFIRMED` or `COMPLIMENTARY` |
| `isCheckedIn` | `false` | `true` |
| `checkedInAt` | `null` | Current timestamp |

---

#### Why PAY_AT_GATE uses record-payment instead of the check-in scan

For `CONFIRMED`, `FREE`, and `COMPLIMENTARY` tickets, money has already been settled before the member arrives at the gate, so scanning the QR code is the only remaining step.

For `PAY_AT_GATE`, the member hasn't paid yet. If the QR scan admitted them before payment was recorded, staff could forget to record it and the member would be inside with an unpaid ticket. By requiring payment to be recorded first, the two steps are fused into one action — the money is confirmed and the member is admitted simultaneously with no gap.

---



## Member Registration API

> **Access:** These endpoints are PUBLIC but are used exclusively for member ticketing.


### 1. Verify Member (Soft Login)

Validates the member's details via the external RIC API and establishes a secure Redis session. This must be called before initiating a booking.

```
POST /api/registration/verify-member
```

**Request Body** (`application/json`):
```json
{
  "memberId": "RIC-12345",
  "identifier": "9876543210",
  "memberType": "INDIAN"
}
```
- `memberType` can be `INDIAN` (identifier = mobile) or `OVERSEAS` (identifier = email).

**Success Response `200 OK`:**
```json
{
  "sessionToken": "a1b2c3d4-e5f6-7890-abcd-1234567890ab",
  "memberId": "RIC-12345",
  "memberType": "INDIAN",
  "maskedIdentifier": "98****10"
}
```
- The frontend **must** save the `sessionToken` and send it in the next step (booking).
- `maskedIdentifier` can be shown to the user (e.g. "OTP will be sent to 98****10").

**Error `400 Bad Request`:**
If the member ID or identifier is invalid/does not match RIC records.

---

### 2. Initiate Booking

Validates booking rules (capacity, deadlines, quotas), generates a 6-digit OTP, and locks the booking intent in Redis for 10 minutes. 
*Note: OTP delivery is mocked to the console log for testing.*

```
POST /api/registration/initiate
```

**Request Body** (`application/json`):
```json
{
  "sessionToken": "a1b2c3d4-e5f6-7890-abcd-1234567890ab",
  "eventId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "quantity": 3,
  "paymentPreference": "ONLINE"
}
```
- `paymentPreference` must be either `ONLINE` or `AT_GATE`.
- `quantity` must be between 1 and the event's `maxTicketsPerMember`.

**Success Response `200 OK`:**
```json
{
  "message": "OTP sent to 98****10",
  "expiresInSeconds": 300
}
```
- The frontend should start a 5-minute (300s) countdown timer and show the OTP input popup.

**Error Responses:**
- `401 Unauthorized`: If the `sessionToken` is invalid or expired.
- `404 Not Found`: If the `eventId` does not exist.
- `400 Bad Request`: If capacity is exceeded, deadline passed, event is not PUBLISHED, or quantity exceeds allowed limit.
- `409 Conflict`: If the member already has a **confirmed** registration (`CONFIRMED`, `FREE`, or `PAY_AT_GATE`). Members with a `PENDING` or `FAILED` registration **are allowed to re-initiate** — their previous booking row is reused with a fresh ticket reference.

---

### 3. Verify OTP & Finalize Booking

The grand finale of the member booking flow. Verifies the OTP, calculates the price, persists the booking in Postgres, and returns one of three outcomes based on the payment path.

```
POST /api/registration/verify-otp
```

**Access:** PUBLIC (internally guarded by `sessionToken` + OTP)

**Request Body:**
```json
{
  "sessionToken": "1ef1bb1b-31cb-4f39-08fa-2b6edcdd08c3",
  "otp": "458129"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `sessionToken` | String | ✅ | Token from `/verify-member` |
| `otp` | String | ✅ | Exactly 6 digits |

---

#### Path A — Free Booking (`paymentStatus: "FREE"`)

Triggered when `totalAmount == 0` (all tickets are free based on `freeTicketsPerRegistration`).

**Success Response `200 OK`:**
```json
{
  "ticketReference": "TKT-2026-AB12CD",
  "eventTitle": "Summer Gala Dinner 2026",
  "quantity": 1,
  "totalAmount": 0.00,
  "paymentStatus": "FREE",
  "razorpayOrderId": null
}
```
→ Frontend shows the **"Booking Confirmed!"** screen immediately.

---

#### Path B — Pay at Gate (`paymentStatus: "PAY_AT_GATE"`)

Triggered when `paymentPreference == "AT_GATE"` and the event has a paid amount.

**Success Response `200 OK`:**
```json
{
  "ticketReference": "TKT-2026-XY9Z01",
  "eventTitle": "Summer Gala Dinner 2026",
  "quantity": 3,
  "totalAmount": 2000.00,
  "paymentStatus": "PAY_AT_GATE",
  "razorpayOrderId": null
}
```
→ Frontend shows a **"Seat Reserved — Pay ₹2000 at the venue"** screen with the QR code.

---

#### Path C — Online Payment (`paymentStatus: "PENDING"`)

Triggered when `paymentPreference == "ONLINE"` and `totalAmount > 0`. A Razorpay Order is created behind the scenes.

**Success Response `200 OK`:**
```json
{
  "ticketReference": "TKT-2026-MN4P8Q",
  "eventTitle": "Summer Gala Dinner 2026",
  "quantity": 3,
  "totalAmount": 2000.00,
  "paymentStatus": "PENDING",
  "razorpayOrderId": "order_PwZa8xyzABC123"
}
```
→ Frontend uses `razorpayOrderId` to open the **Razorpay JS Checkout popup** for the user to pay.

---

#### Price Calculation Logic

```
paidTickets = max(0, quantity - event.freeTicketsPerRegistration)
totalAmount = paidTickets × event.ticketPrice
```

**Example:** Event has `freeTicketsPerRegistration = 1`, `ticketPrice = ₹1000`.
- Member books 3 tickets → `paidTickets = 3 - 1 = 2` → `totalAmount = ₹2000`

---

#### Ticket Reference Format

```
TKT-{YEAR}-{6 random alphanumeric chars}
e.g. TKT-2026-AB12CD
```

The ticket reference is unique across the entire system and is what gets encoded into the member's QR code.

---

#### What Happens in Redis After This Call

After booking is finalized (any path), the backend cleans up Redis:
- ❌ `otp:{sessionToken}` — deleted
- ❌ `intent:{sessionToken}` — deleted
- ✅ `session:{sessionToken}` — kept (member can still navigate the app)

---

**Error Responses:**
- `401 Unauthorized`: OTP is wrong — `"Incorrect OTP. Please try again."`
- `401 Unauthorized`: OTP expired (5-min window passed) — `"OTP has expired. Please restart the booking process."`
- `401 Unauthorized`: Booking session expired (10-min intent window) — `"Booking session expired. Please restart the booking process."`
- `400 Bad Request`: Event is no longer PUBLISHED or deadline has passed.
- `400 Bad Request`: Event sold out between `/initiate` and now — `"Sorry, this event just filled up. Only X seat(s) remain."`
- `500 Internal Server Error`: Razorpay API call failed (Path C only) — `"Payment gateway error. Please try again."`

---

### 4. Confirm Payment

The "fast path" endpoint that finalizes an online payment. Called by the frontend immediately after the Razorpay popup closes with a success.

```
POST /api/registration/confirm-payment
```

**Access:** PUBLIC (secured internally by Razorpay cryptographic signature — no JWT required)

---

#### When to call this

This endpoint is only relevant for **Path C (Online Payment)** from `/verify-otp`. It should be called when the Razorpay JS SDK fires its `handler` callback after a successful payment.

**Do NOT call this for:**
- `FREE` bookings — they are already confirmed by `/verify-otp`
- `PAY_AT_GATE` bookings — staff confirms these at the venue

---

#### Request Body

```json
{
  "ticketReference":  "TKT-2026-AB12CD",
  "razorpayOrderId":  "order_PwZa8xyz...",
  "razorpayPaymentId": "pay_Qx3Rabc...",
  "razorpaySignature": "a3f2b9c1d4e5f6..."
}
```

| Field | Type | Source | Required |
|---|---|---|---|
| `ticketReference` | String | From `/verify-otp` response | ✅ |
| `razorpayOrderId` | String | From `/verify-otp` response | ✅ |
| `razorpayPaymentId` | String | From Razorpay JS SDK `handler` callback | ✅ |
| `razorpaySignature` | String | From Razorpay JS SDK `handler` callback | ✅ |

**How to get `razorpayPaymentId` and `razorpaySignature` in the frontend:**

The Razorpay JS SDK fires a callback when the user completes payment:
```javascript
handler: function (response) {
    // These three values come directly from Razorpay — send them to our backend
    const razorpayOrderId   = response.razorpay_order_id;
    const razorpayPaymentId = response.razorpay_payment_id;
    const razorpaySignature = response.razorpay_signature;
}
```

---

#### Success Response `200 OK`

```json
{
  "ticketReference": "TKT-2026-AB12CD",
  "eventTitle": "Summer Gala Dinner 2026",
  "quantity": 3,
  "totalAmount": 2000.00,
  "paymentStatus": "CONFIRMED",
  "razorpayOrderId": "order_PwZa8xyz..."
}
```

→ Frontend shows the **"Booking Confirmed!"** screen with the QR code.

---

#### Security — Signature Verification

The backend computes:
```
expected = HMAC_SHA256(razorpayOrderId + "|" + razorpayPaymentId, API_SECRET)
valid    = (expected == razorpaySignature)
```

If the signature does not match, the request is rejected with `400 Bad Request`. This makes it cryptographically impossible for a bad actor to fabricate a payment confirmation.

---

#### Idempotency (Safe to Call Twice)

If the Razorpay webhook already confirmed the ticket before this endpoint is called, this endpoint detects that (`paymentStatus == CONFIRMED`) and silently returns success — it does **not** error out. This ensures the frontend always gets a clean success response.

---

#### Sold-Out Race Condition

Because `PENDING` tickets do not lock seats, it is theoretically possible for an event to sell out while a member is on the payment screen.

If this happens:
1. The backend marks the registration as `FAILED`.
2. Automatically triggers a full refund via the Razorpay API (`speed: normal`).
3. Returns `409 Conflict` with the message: `"We're sorry — this event just sold out while your payment was processing. A full refund will be issued to your account within 5-7 business days."`

---

#### Database Changes After This Call

| Field | Before | After |
|---|---|---|
| `paymentStatus` | `PENDING` | `CONFIRMED` |
| `razorpayPaymentId` | `null` | `"pay_Qx3Rabc..."` |

---

**Error Responses:**
- `404 Not Found`: `ticketReference` does not exist — `"Ticket not found: TKT-2026-AB12CD"`
- `400 Bad Request`: Signature invalid — `"Payment verification failed. The payment data is invalid or was tampered with."`
- `409 Conflict`: Ticket is `FREE` or `PAY_AT_GATE` — those are not online payments, this endpoint does not apply — `"Cannot confirm payment for a ticket with status: FREE"`
- `409 Conflict`: Sold-out race condition — `"We're sorry — this event just sold out while your payment was processing..."`

> **Note on `FAILED` status:** If a `payment.failed` webhook arrived before this call (e.g. the user's first UPI attempt failed but they retried with a credit card on the same Razorpay order), the registration may be in `FAILED` state. This endpoint **does not reject** `FAILED` status — it allows the request through and lets the Razorpay signature verify whether the payment is genuinely successful.

---


## Registration Endpoints — Webhook

### 5. Razorpay Webhook

Server-to-server event notifications from Razorpay. This is the **safety net** for the entire payment flow — it fires regardless of what the user's browser does.

```
POST /api/webhooks/razorpay
```

**Access:** Called by Razorpay servers only. Secured by HMAC-SHA256 signature verification on every request.

---

#### Why This Exists

The normal flow after payment is:
```
User pays → Razorpay popup closes → Frontend calls /confirm-payment → Ticket confirmed
```

But if the user's browser crashes, their internet drops, or they close the tab the instant payment succeeds, `/confirm-payment` is never called. The ticket stays `PENDING` forever even though money was taken.

Razorpay's server-to-server webhook fires **regardless of the frontend**. This endpoint catches those orphaned payments and confirms the ticket automatically.

---

#### Request

Razorpay sends a raw JSON body with a signature header:

```
Content-Type: application/json
X-Razorpay-Signature: a3f2b9c1d4e5f6...
```

Example body for `payment.captured`:
```json
{
  "event": "payment.captured",
  "payload": {
    "payment": {
      "entity": {
        "id": "pay_Qx3Rabc...",
        "order_id": "order_PwZa8xyz...",
        "status": "captured"
      }
    }
  }
}
```

---

#### Signature Verification

Before processing any event, the backend verifies the request is genuinely from Razorpay:
```
expected = HMAC-SHA256(entire raw request body, RAZORPAY_WEBHOOK_SECRET)
valid    = (expected == X-Razorpay-Signature header)
```

This uses a **separate webhook secret** (not the API key secret). It is configured in the Razorpay dashboard when you create the webhook.

If the signature does not match, the request is silently ignored and `200 OK` is still returned.

---

#### Events Handled

| Event | Trigger | Action |
|---|---|---|
| `payment.captured` | User's payment succeeded | Marks registration `CONFIRMED`. Applies sold-out guard; refunds automatically if sold out. |
| `payment.failed` | User's card/UPI was declined | Marks registration `FAILED` — member can retry immediately. |
| Any other event | Settlements, disputes, etc. | Logged and ignored. |

---

#### Retry-After-Failure on Same Razorpay Order

Razorpay allows the user to try multiple payment methods within the same popup (same `order_id`). For example:

1. User tries UPI → fails → `payment.failed` webhook → registration marked `FAILED`
2. User tries credit card (same popup, same `order_id`) → succeeds
3. `payment.captured` webhook fires → finds registration by `order_id` → status is `FAILED` (not `CONFIRMED`) → proceeds to confirm ✅
4. Ticket marked `CONFIRMED`

The same scenario works via `/confirm-payment` too — that endpoint also accepts `FAILED` status since the Razorpay signature cryptographically proves the payment is genuine.

---

#### Idempotency

Razorpay retries webhooks if it does not receive `200 OK` quickly. Every handler checks the current state before acting:

| Webhook event | Current status | Action |
|---|---|---|
| `payment.captured` | `PENDING` or `FAILED` | Confirm ticket ✅ |
| `payment.captured` | `CONFIRMED` | Skip — already confirmed (idempotent) |
| `payment.failed` | `PENDING` | Mark FAILED ✅ |
| `payment.failed` | `FAILED` | Skip — already failed (idempotent) |
| `payment.failed` | `CONFIRMED` | Skip — **never downgrade a confirmed ticket** |

---

#### Response

This endpoint **always** returns `200 OK` with body `"ok"`, even on internal errors or invalid signatures. This is intentional:
- Returning `4xx`/`5xx` causes Razorpay to retry for up to 24 hours, risking duplicate processing.
- Attackers get no feedback about why a spoofed request was rejected.

---

#### Database Changes

**On `payment.captured` (success path):**

| Field | Before | After |
|---|---|---|
| `paymentStatus` | `PENDING` or `FAILED` | `CONFIRMED` |
| `razorpayPaymentId` | `null` | `"pay_Qx3Rabc..."` |

**On `payment.failed`:**

| Field | Before | After |
|---|---|---|
| `paymentStatus` | `PENDING` | `FAILED` |

---
