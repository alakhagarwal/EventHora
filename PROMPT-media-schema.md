# Implementation Prompt: Event Schema Alignment (Dual-Tier Pricing) + Media Gallery

The backend was updated (commit `3b27797`) and the new contract is live. The frontend still
uses the old pricing fields and has no media gallery UI. This prompt brings the frontend in
line with the backend.

---

## Backend contract (authoritative — match these exactly)

### Pricing fields

The old fields `maxTicketsPerMember`, `freeTicketsPerRegistration`, and `ticketPrice` were
**removed**. Event create/update payloads now use:

- `maxMemberTickets` (int, min 1) — max member-tier seats per booking
- `freeMemberTickets` (int, min 0) — free member tickets per booking
- `memberTicketPrice` (number, min 0)
- `maxGuestTickets` (int, min 0 — `0` = guests not allowed)
- `freeGuestTickets` (int, min 0)
- `guestTicketPrice` (number, min 0)

`totalCapacity` (hard ceiling combining both tiers) and `platformFeePerTicket` are unchanged.

### Media gallery

Event responses now include:

```
media: Array<{
  id: string;
  mediaType: "PHOTO" | "VIDEO";
  url: string;
  caption: string | null;
  sortOrder: number;
  uploadedAt: string;
}>
```

- PHOTO `url` = 7-day presigned S3 URL — **never** send it back to the server
- VIDEO `url` = external embed URL — returned/stored as-is, safe for `<iframe>`

### New admin endpoints (all ADMIN-only)

| Method | Endpoint | Body |
| --- | --- | --- |
| POST | `/api/events/{id}/media/photo` | multipart form: `file` (required), `caption` (optional), `sortOrder` (required int) |
| POST | `/api/events/{id}/media/video` | JSON `{ url, caption?, sortOrder }` |
| DELETE | `/api/events/{id}/media/{mediaId}` | — returns `{ message }` |
| PATCH | `/api/events/{id}/media/reorder` | JSON `{ orderedIds: string[] }` (new 0-based order) |

---

## Work items

### 4.1 `frontend/src/lib/api.ts`

- Add an `EventMedia` type and a `MediaType` union (`"PHOTO" | "VIDEO"`).
- Add methods:
  - `uploadEventPhoto(id, file, caption, sortOrder)` — FormData (same pattern as `uploadBanner`), returns `EventResponse`
  - `addEventVideo(id, url, caption, sortOrder)` — JSON body, returns `EventResponse`
  - `deleteEventMedia(id, mediaId)` — returns `{ message }`
  - `reorderEventMedia(id, orderedIds)` — returns `EventResponse`

### 4.2 `frontend/src/components/EventForm.tsx` — schema + media gallery section

- Update `EventFormValues`: remove `maxTicketsPerMember`, `freeTicketsPerRegistration`, `ticketPrice`; add `maxMemberTickets`, `freeMemberTickets`, `memberTicketPrice`, `maxGuestTickets`, `freeGuestTickets`, `guestTicketPrice`.
- "Capacity & pricing" section: split into **Member tickets** (Max / Free / Price) and **Guest tickets** (Max / Free / Price) groups. Keep `totalCapacity` and `platformFeePerTicket`. Same `Grid`/`Field` styling.
- Add a **"Media gallery"** section AFTER "Contact" (last section). Only render when `currentId` exists; otherwise show "Save the event before adding media."
- **Photo upload:** reuse the banner drop-zone look (dashed border, Upload icon, click to upload, `accept="image/*"`) PLUS an optional caption input above/below the drop zone. On success, add the returned media item to a local `media` list.
- **Video add:** reuse the "Important notes" add pattern — URL text input + optional caption input + "Add video" button. Light validation only: must start with `http://` or `https://`. Store as typed.
- **Thumbnails:** render all media as SMALL thumbnails in a horizontal flex-wrap row (`w-28 h-20 object-cover rounded`, play icon overlaid on VIDEO items — do NOT embed videos inside the form). Each thumbnail shows: caption line, ↑ / ↓ reorder buttons (disabled at first/last position — call `reorderEventMedia` with the new ordered id list and refresh local state from the response), and a ✕ delete button (confirm first).
- Compute `sortOrder` for new items as the current media count (append). The backend requires an explicit `sortOrder`.
- Do NOT include `media` in the create/PATCH body — keep the existing `bannerUrl` exclusion behavior. Media is managed only through the endpoints above.
- Keep the sticky action bar and all existing create/save/publish/cancel logic.

### 4.3 `frontend/app/admin/events/[id]/details/page.tsx` — pricing display + media gallery

- Replace the "Max tickets / member", "Free per registration", "Ticket price" Info rows with **Member tier** (Max / Free / Price) and **Guest tier** (Max / Free / Price) rows.
- **Photo slideshow** at the end of the description, rendered only when the event has photos:
  - Fixed 16:9 aspect container; photos as `<img>` with `object-cover`.
  - **Caption overlay:** caption text absolutely positioned bottom-left inside the slide over a bottom-up gradient scrim (`bg-gradient-to-t from-black/70`); render nothing when an item has no caption.
  - Prev/next chevron controls + dot indicators below. No autoplay. Start at `sortOrder` 0.
- **Separate "Videos" section** below the slideshow, rendered only when the event has videos: 16:9 `<iframe>` embeds using `url` directly, optional caption under each.

### 4.4 `frontend/app/events/[link]/book/page.tsx` — dual-tier booking

- Replace the single `quantity` / `max` / `free` / `ticketPrice` logic with two counters:
  - **Member tickets** (required, min 1, max `maxMemberTickets`; first `freeMemberTickets` free at `memberTicketPrice`)
  - **Guest tickets** (min 0, max `maxGuestTickets`; first `freeGuestTickets` free at `guestTicketPrice`); hide/disable the guest counter when `maxGuestTickets === 0`
- Send `quantity = memberCount + guestCount` to `initiateBooking` (backend combined-quantity contract is unchanged).
- Total = paid member tickets × `memberTicketPrice` + paid guest tickets × `guestTicketPrice`.

### 4.5 `frontend/app/admin/bookings/new/page.tsx`

- Same dual-tier treatment as 4.4: member + guest counters, totals, and the combined quantity sent to `adminRegisterMember`.

### 4.6 `frontend/src/__tests__/EventForm.test.tsx`

- Update payload assertions to the new field names.
- Add a test that the pricing section renders the 6 new inputs.

---

## Acceptance checks

- `npx tsc --noEmit` (in `frontend/`) passes.
- Grep: no stale references to `maxTicketsPerMember`, `freeTicketsPerRegistration`, or `ticketPrice` remain (outside of legitimate mapping to the new fields).
- The form's create/PATCH payload contains exactly the 6 new pricing fields and never `media`.
- Media section is hidden until the event is saved; thumbnail upload, ↑/↓ reorder, delete, and video-add all hit the correct endpoints and update local state from the returned `EventResponse`.
- Existing tests pass.

---

## Code Patterns to Follow

- All pages are `"use client"` components.
- Use `apiFetch` from `@/lib/api` for API calls.
- Use existing Tailwind classes: `card`, `btn`, `btn-primary`, `btn-outline`, `eyebrow`, `h1`, `input`, `label`, `chip`.
- Use the existing navy/gold/cream theme from `tailwind.config`.
- Loading states: skeleton cards or "Loading..." text. Error states: red error message.
- Amounts with ₹ prefix and locale formatting.
- Add new API methods to the `api` object in `src/lib/api.ts`.
