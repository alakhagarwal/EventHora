# Frontend Prompt (Next.js + TypeScript)

You are an expert frontend engineer.

## Goal
Generate a **complete frontend** for the existing EventHora backend.
The frontend must be a **Next.js + TypeScript** app and should be provided to the user as a **single ZIP file**.

## Packaging requirement (must follow)
- Output a **.zip** file of the frontend project.
- Include the full folder structure.
- In your response, **mention the file paths** you created inside the zip.

## Backend integration
Base URL: `http://localhost:8080`
All protected endpoints require:
`Authorization: Bearer <token>`

## Routes / Pages required
Create these pages (implement all UI + logic):

### 1) Landing page (public)
- Route: `/`
- Landing page for any event management app.
- Publicly shows a hero section and a small events preview grid.
- Fetch published events using:
  - `GET /api/events`
- Provide navigation CTA buttons to Admin/Staff login and Member tab login.

### 2) Auth: Login page with 3 logical entry points
- Route: `/login`
- Implement **top-level UI that clearly separates 3 flows**:
  1) **Admin/Staff Login** (one form)
  2) **Member Login → INDIAN** (one form state)
  3) **Member Login → OVERSEAS** (one form state)

#### Admin/Staff Login (Admin + Staff)
- One tab/panel/section: “Admin/Staff Login”.
- Inputs: `email`, `password`.
- Call:
  - `POST /api/auth/login` with body `{ "email": "...", "password": "..." }`
- After login, read JWT fields from the response (`role`).
- Redirect:
  - `role === "ADMIN"` -> `/admin/my-events`
  - `role === "STAFF"` -> `/profile`

#### Member Login (INIDIAN)
- Shown as a tab inside Member Login section: “INDIAN”.
- Inputs:
  - `memberId`
  - `identifier` (mobile/phone number)
- Internally set:
  - `memberType = "INDIAN"`
- Call:
  - `POST /api/registration/verify-member`
  - body: `{ memberId, identifier, memberType }`

#### Member Login (OVERSEAS)
- Shown as a tab inside Member Login section: “OVERSEAS”.
- Inputs:
  - `memberId`
  - `identifier` (email)
- Internally set:
  - `memberType = "OVERSEAS"`
- Call:
  - `POST /api/registration/verify-member`
  - body: `{ memberId, identifier, memberType }`

#### After either member verification succeeds
- Store `sessionToken` from the response.
- Navigate to OTP booking start UI route: `/member/otp`.



### 3) Auth: Member login (must be inside Tab 2 of `/login`)
- Do NOT create a separate `/member` route.
- Member verification must be done only from the Member Login tab:
  - `POST /api/registration/verify-member`
  - body: `{ memberId, identifier, memberType }`
- Store `sessionToken` returned in response and proceed to booking start UI (OTP step), route `/member/otp`.


### 4) Member booking flow (OTP)
- Route after verify: `/member/otp`
- Start OTP verification popup/timer after calling booking initiate:
  - Call `POST /api/registration/initiate` with:
    `{ sessionToken, eventId, quantity, paymentPreference }`
- `paymentPreference` options:
  - ONLINE
  - AT_GATE
- `quantity` must validate minimum 1 and maximum should be based on `maxTicketsPerMember` from the selected event.
- Show a 5-minute countdown using `expiresInSeconds`.

### 5) Member events listing (Published - public)
- Route: `/events`
- Public.
- Fetch published events list:
  - `GET /api/events`
- Render cards with:
  - title
  - date/time
  - venue
  - status-ish (registrationOpen + isSoldOut)
  - link to details page

### 5b) Admin published events listing (Read-only)
- Route: `/admin/events/public`
- ADMIN only.
- Fetch only published events using:
  - `GET /api/events`
- Render a beautiful read-only events listing (same card style as `/events`).
- Each card navigates to the public event details page (not an editor form):
  - `/events/[link]`


### 6) Member event details
- Route: `/events/[link]`
- Public.
- Use:
  - `GET /api/events/{link}`
- Show event banner, description, rules, and a booking panel.
- If `registrationOpen === false` disable booking.
- Booking panel should:
  - select quantity
  - select payment preference (ONLINE / AT_GATE)
  - proceed to OTP step.

### 7) Admin my events page
- Route: `/admin/my-events`
- ADMIN only.
- Fetch listing:
  - `GET /api/admin/events`
- Display filters/tabs for event statuses (at least):
  - DRAFT
  - PUBLISHED
  - CANCELLED
  - (If COMPLETED exists in backend, also show it.)
- Each card is clickable and opens the **same event editor form** used by event creation.

### 8) Event editor + event creation
- Route:
  - Creation: `/admin/events/new`
  - Edit: `/admin/events/[id]`
- ADMIN only.

#### Common requirements for event form
Create a single reusable component used by both creation and editing.
Form fields (match backend CreateEventRequest / UpdateEventRequest shape):
- title
- description
- category (enum)
- eventDate
- startTime
- endTime
- registrationDeadline
- venue
- additionalVenueInfo
- totalCapacity
- maxTicketsPerMember
- freeTicketsPerRegistration
- ticketPrice
- platformFeePerTicket
- minimumAge (optional)
- importantNotes (array input)
- contactPersonName
- contactPersonPhone
- banner upload (optional UI) using:
  - `POST /api/events/{id}/banner` multipart/form-data with key `file`

#### Editing behavior
- When opening an event from listing:
  - first fetch full event by id:
    `GET /api/admin/events/{id}`
  - prefill form fields with returned values.
- Saving the form:
  - must call patch endpoint:
    `PATCH /api/events/{id}`
  - On save/edit, send updated values.

#### Draft/Publish controls
The form must provide buttons after editing:
- “Save Draft”
  - keeps status DRAFT (send PATCH only; do not call publish)
- “Publish”
  - must call:
    `PATCH /api/events/{id}/publish`
- “Cancel” (optional but recommended)
  - must call:
    `DELETE /api/events/{id}`

Also provide a “Create Draft” button on the creation page that calls:
- `POST /api/events` to create event in DRAFT status.

### 9) Events listing page (admin)
- Route: `/admin/events`
- ADMIN only.
- Similar to my-events but can be all events; still use:
  - `GET /api/admin/events`
- Cards should link to details/editor page.
- Card click opens the same editor page used above.

### 10) Event card details page
- Route: `/admin/events/[id]/details`
- ADMIN only.
- Could be a read-only view OR route to editor.
- If read-only, include an “Edit” CTA that navigates to `/admin/events/[id]`.

### 11) Profile pages
Role-based:
- Route for both roles: `/profile`
- Or separate routes:
  - ADMIN: `/admin/profile`
  - STAFF: `/staff/profile`

Requirements:
- Display:
  - name
  - email
  - role
  - createdAt (if returned)
- Fetch with:
  - `GET /api/auth/me`

### 12) User management (admin)
- Route: `/admin/users`
- ADMIN only.

UI:
- List all users from:
  - `GET /api/auth/users`
- Include a form to create new users:
  - route `/admin/users/new` OR modal on same page
  - POST ` /api/auth/users`
  - body `{ name, email, password, role }`
  - role can be ADMIN or STAFF
- Include deactivate action for each user:
  - PATCH `/api/auth/users/{email}/deactivate`

## Navigation bar
- Navbar must be at the top on all pages (authenticated and public).
- Role-aware links:
  - ADMIN links: My Events, Events, Users, Profile
  - STAFF links: Profile
  - Public links: Landing, Events, Member login
- Add logout button for JWT sessions.

## Token storage
- Store `accessToken` in `localStorage` (simple approach).
- Attach Authorization header to all protected API calls.
- If token missing or request returns 401/403:
  - redirect to `/login`.

## Styling requirement
- Light themed UI only.
- Use a simple component library or Tailwind.
- Ensure forms are clean, responsive, and accessible.

## API client abstraction
Create an `api` utility layer to wrap:
- login
- getMe
- admin users
- admin events
- public events
- member verify/initiate

## Implementation details
- Use Next.js App Router (recommended).
- Use TypeScript throughout.
- Use environment configuration for base URL:
  - `NEXT_PUBLIC_API_BASE_URL` default to `http://localhost:8080`

## Final deliverable
Provide a ZIP that contains the complete Next.js + TypeScript frontend.

## Mention file paths
In the response, explicitly list the key file paths in the created zip, e.g.:
- `frontend/package.json`
- `frontend/app/page.tsx`
- `frontend/app/login/page.tsx`
- `frontend/app/member/page.tsx`
- `frontend/app/events/page.tsx`
- `frontend/app/events/[link]/page.tsx`
- `frontend/app/admin/my-events/page.tsx`
- `frontend/app/admin/events/new/page.tsx`
- `frontend/app/admin/events/[id]/page.tsx`
- `frontend/app/admin/users/page.tsx`
- `frontend/app/admin/users/new/page.tsx`
- `frontend/app/profile/page.tsx`
- `frontend/src/components/*`

Generate only the frontend code + instructions to run/build it.

