# EventHora Frontend (Next.js + TypeScript)

Complete frontend for the EventHora backend.

## Getting started

```bash
npm install
cp .env.local.example .env.local     # optional
npm run dev
```

Open http://localhost:3000

Backend base URL defaults to `http://localhost:8080`. Override with `NEXT_PUBLIC_API_BASE_URL`.

Dev seed credentials:
- ADMIN: admin@eventhora.com / Admin@1234
- STAFF: staff@eventhora.com / Staff@1234

## Routes

Public:
- `/` — Landing (hero + featured events)
- `/events` — Published events grid
- `/events/[link]` — Event details + booking panel
- `/login` — 3 tabs: Admin/Staff · Member Indian · Member Overseas
- `/member/otp` — 5-minute OTP timer after member verification

Authenticated:
- `/profile` — Current user
- `/admin/my-events` — Status tabs (DRAFT / PUBLISHED / CANCELLED / COMPLETED)
- `/admin/events` — All events (search)
- `/admin/events/new` — Create event (draft)
- `/admin/events/[id]` — Edit + Save Draft / Publish / Cancel + Banner upload
- `/admin/events/[id]/details` — Read-only detail view
- `/admin/events/public` — Read-only published listing
- `/admin/users` — List + deactivate
- `/admin/users/new` — Create ADMIN/STAFF

## Key files

- `app/layout.tsx`, `app/globals.css`, `src/components/Navbar.tsx`
- `src/lib/api.ts` — typed API client with Bearer token + 401 redirect
- `src/lib/auth.ts` — localStorage session helpers
- `src/components/EventForm.tsx` — reusable create/edit form
- `src/components/EventCard.tsx` — shared event card

## Build

```bash
npm run build && npm start
```
