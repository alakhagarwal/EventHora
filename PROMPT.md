# Implementation Prompt

We need to implement three new features using backend endpoints that already exist but have no frontend.

---

## Feature 1: Admin Dashboard Stats

### Backend Endpoint
```
GET /api/admin/dashboard
```
Returns a platform-wide snapshot:
- totalEvents, publishedEvents, upcomingEvents, draftEvents, completedEvents, cancelledEvents
- totalRegistrations, lockedRegistrations, totalTicketsSold
- registrationsThisMonth, ticketsSoldThisMonth
- totalRevenue, pendingGateCollection, complimentaryWaived, revenueThisMonth

### What to Build
Modify the existing `/admin/dashboard` page (`app/admin/dashboard/page.tsx`) to show stats ABOVE the events list.

Current page only lists events. Add a stats section at the top:

1. **Stats Section** — A grid of stat cards at the top of the page, before the events list:
   - Row 1 (Events overview): Total Events, Published, Upcoming, Draft, Completed, Cancelled — 6 small stat cards in a 3x2 grid (2-col on mobile)
   - Row 2 (Registrations): Total Registrations, Locked Registrations, Tickets Sold — 3 cards in a row
   - Row 3 (This Month): Registrations This Month, Tickets Sold This Month — 2 cards
   - Row 4 (Revenue): Total Revenue, Pending Gate Collection, Complimentary Waived, Revenue This Month — 4 cards in a row (2-col on mobile)

2. Use the same StatCard pattern from the payment summary page (`app/admin/events/[id]/summary/page.tsx`) — small card with a colored accent bar, value, and label.

3. Add API method in `src/lib/api.ts`: `dashboardStats: () => apiFetch<any>("/api/admin/dashboard")`

4. The events list remains below the stats, unchanged.

5. Add loading skeleton for the stats section while fetching.

---

## Feature 2: Member My Bookings Page

### Backend Endpoint
```
GET /api/registration/my-bookings?sessionToken={token}
```
Returns an array of the member's bookings across all events:
- ticketReference, quantity, totalAmount, paymentStatus, paymentPreference
- isCheckedIn, checkedInAt
- eventTitle, eventDate, eventStartTime, eventVenue, eventUniqueLink
- bookedAt

Auth: `sessionToken` query param (from localStorage key `"memberSession"` → `sessionToken` field).

### What to Build
New page: `/member/bookings`

1. **Access**: Any logged-in member (has `memberSession` in localStorage). If no `memberSession`, redirect to `/login`.

2. **Fetch**: Read `sessionToken` from `localStorage("memberSession")`, call the endpoint with it as a query param.

3. **Layout**:
   - Page title: "My Bookings" with eyebrow "Member"
   - List of booking cards, one per registration, sorted newest first
   - Each card shows:
     - Event title (linked to `/events/{eventUniqueLink}`)
     - Event date and venue
     - Ticket reference
     - Quantity and amount
     - Payment status badge (same color scheme as registrations page: CONFIRMED=green, FREE=blue, PAY_AT_GATE=orange, COMPLIMENTARY=purple, PENDING=yellow, FAILED=red)
     - Check-in status (green check or red X)
     - Booked at date
   - Empty state: "No bookings yet" with a link to `/events` to browse events
   - Loading state while fetching

4. **Nav links**:
   - Add "My Bookings" to the Navbar desktop nav for members (logged in with `memberSession`, no admin/staff role)
   - Add "My Bookings" to the mobile drawer for members
   - Add "Bookings" to the MobileBottomNav for members (between Events and Profile)

5. Add API method: `myBookings: (sessionToken: string) => apiFetch<any[]>(/api/registration/my-bookings?sessionToken=${sessionToken}, { auth: false })`

---

## Feature 3: Admin Direct Booking (Walk-in Registration)

### Backend Endpoint
```
POST /api/admin/bookings/register
```

Request body:
```json
{
  "memberId": "RIC-2024-04512",
  "memberType": "INDIAN",
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "quantity": 2,
  "action": "PAY_AT_GATE"
}
```

- `action` can be `"PAY_AT_GATE"` or `"COMPLIMENTARY"`
- No `ONLINE` option — admin bookings never go through Razorpay
- For free events, `action` is ignored (result is `FREE` automatically)

Response:
```json
{
  "ticketReference": "TKT-2026-GH34JK",
  "quantity": 2,
  "totalAmount": 2000.00,
  "paymentStatus": "PAY_AT_GATE",
  "memberId": "RIC-2024-04512",
  "eventTitle": "Mere Mehboob Na Ja…",
  "eventDate": "2026-07-08",
  "eventStartTime": "18:30:00",
  "eventVenue": "Main Audi, RIC",
  "bookedBy": "admin@ric.org",
  "bookedAt": "2026-07-24T22:45:00"
}
```

### What to Build
New page: `/admin/bookings/new`

1. **Access**: ADMIN or STAFF (guard with session check)

2. **Form fields**:
   - Event selector — dropdown populated from `api.adminEvents()`, show title + date, filter for PUBLISHED only
   - Member ID — text input, placeholder `"RIC-2024-XXXXX"`, validated to start with `"RIC"`
   - Member Type — toggle/radio: INDIAN or OVERSEAS
   - Quantity — number input with +/- buttons, min 1, max = event's `maxTicketsPerMember`
   - Action — radio: PAY_AT_GATE or COMPLIMENTARY (only shown if event has `ticketPrice > 0`; hidden for free events)
   - Show order summary before submitting: quantity × ticketPrice (if paid), or "Free"

3. **Submit**: Call `POST /api/admin/bookings/register` with JWT auth

4. **Success**: Show a success card with:
   - Green checkmark
   - Ticket reference
   - Member ID
   - Event title, date
   - Quantity, amount, payment status
   - "Booked by" email
   - Button: "Register Another Member" (resets form)
   - Button: "Back to Dashboard" (navigates to `/admin/dashboard`)

5. **Error handling**:
   - 400: Show validation error message
   - 409 (duplicate): "This member is already registered for this event"
   - 404: "Event not found"

6. **Nav links**: Add "Register Member" to the Navbar desktop nav for ADMIN only (in the admin links section). Add to mobile drawer too.

7. Add API method: `adminRegisterMember: (body) => apiFetch("/api/admin/bookings/register", { method: "POST", json: body })`

---

## Code Patterns to Follow

- All pages are `"use client"` components
- Use `apiFetch` from `@/lib/api` for API calls
- Auth guard: check `getSession()` in useEffect, redirect to `/login` if unauthorized
- Use existing Tailwind classes: `card`, `btn`, `btn-primary`, `btn-outline`, `eyebrow`, `h1`, `input`, `label`, `chip`
- Use the existing navy/gold/cream theme from `tailwind.config`
- Loading states: skeleton cards or "Loading..." text
- Error states: red error message
- Amounts with ₹ prefix and locale formatting
- All new pages go in `app/` directory following existing folder structure
- Add new API methods to the `api` object in `src/lib/api.ts`
- Stat cards follow the pattern in `app/admin/events/[id]/summary/page.tsx` (StatCard component)
