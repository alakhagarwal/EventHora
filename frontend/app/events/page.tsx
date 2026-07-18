"use client";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import EventCard, { type EventSummary } from "@/components/EventCard";

export default function EventsPage() {
  const [events, setEvents] = useState<EventSummary[]>([]);
  const [q, setQ] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => { api.publicEvents().then((e) => setEvents(e || [])).catch(() => {}).finally(() => setLoading(false)); }, []);

  const filtered = events
    .filter((e) => {
      if (e.eventDate && new Date(e.eventDate) < new Date()) return false;
      return !q || e.title?.toLowerCase().includes(q.toLowerCase()) || e.venue?.toLowerCase().includes(q.toLowerCase());
    })
    .sort((a, b) => new Date(a.eventDate || "9999-12-31").getTime() - new Date(b.eventDate || "9999-12-31").getTime());

  return (
    <div className="mx-auto max-w-7xl px-4 md:px-6 py-8 md:py-16">
      <div className="mb-6 md:mb-10 flex flex-col md:flex-row md:items-end md:justify-between gap-4">
        <div>
          <div className="eyebrow">Discover</div>
          <h1 className="h1 mt-2">Upcoming Events</h1>
        </div>
        <input className="input md:w-96" placeholder="Search title or venue…" value={q} onChange={(e) => setQ(e.target.value)} />
      </div>
      {loading ? <div className="text-navy/60">Loading…</div> :
        filtered.length === 0 ? <div className="card p-10 text-center text-navy/60">No events found.</div> :
        <div className="grid gap-4 md:gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((e) => <EventCard key={e.id} event={e} href={`/events/${e.uniqueEventLink}`} actionLabel="Book Now" />)}
        </div>}
    </div>
  );
}
