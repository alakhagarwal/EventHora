# EventHora Frontend (Next.js + TypeScript)

## Setup
```
npm install
cp .env.local.example .env.local
npm run dev
```
App runs at http://localhost:3000. Backend expected at http://localhost:8080.

## Key paths
- app/page.tsx (landing)
- app/login/page.tsx
- app/events/page.tsx, app/events/[link]/page.tsx
- app/member/otp/page.tsx
- app/admin/my-events/page.tsx
- app/admin/events/page.tsx, /new, /[id], /[id]/details, /public
- app/admin/users/page.tsx, /new
- app/profile/page.tsx
- src/lib/api.ts, src/lib/auth.ts
- src/components/Navbar.tsx, EventCard.tsx, EventForm.tsx
