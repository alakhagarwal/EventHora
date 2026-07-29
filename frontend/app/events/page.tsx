"use client";
import { useEffect, useMemo, useState } from "react";
import { api } from "@/lib/api";
import EventCard, { type EventSummary } from "@/components/EventCard";
import SearchInput from "@/components/SearchInput";
import { useDebouncedValue } from "@/lib/useDebouncedValue";

export default function EventsPage() {
  const [events, setEvents] = useState<EventSummary[]>([]);
  const [q, setQ] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    const load = () =>
      api.publicEvents()
        .then((data) => {
          if (!cancelled) setEvents(data || []);
        })
        .catch(() => {});

    load().finally(() => {
      if (!cancelled) setLoading(false);
    });

    const interval = window.setInterval(load, 30000);
    return () => {
      cancelled = true;
      window.clearInterval(interval);
    };
  }, []);

  const debouncedQuery = useDebouncedValue(q, 300);

  const filtered = useMemo(
    () =>
      events
        .filter((e) => {
          if (e.eventDate && new Date(e.eventDate) < new Date()) return false;
          return !debouncedQuery || e.title?.toLowerCase().includes(debouncedQuery.toLowerCase());
        })
        .sort((a, b) => new Date(a.eventDate || "9999-12-31").getTime() - new Date(b.eventDate || "9999-12-31").getTime()),
    [debouncedQuery, events]
  );

  return (
    <div className="mx-auto max-w-7xl px-4 md:px-6 py-8 md:py-16">
      <div className="mb-6 md:mb-10 flex flex-col md:flex-row md:items-end md:justify-between gap-4">
        <div>
          <div className="eyebrow">Discover</div>
          <h1 className="h1 mt-2">Upcoming Events</h1>
        </div>
        <SearchInput className="w-full md:max-w-md" value={q} onChange={setQ} placeholder="Search by title" />
      </div>
      {loading ? <div className="text-navy/60">Loading…</div> :
        filtered.length === 0 ? <div className="card p-10 text-center text-navy/60">No events found.</div> :
        <div className="grid gap-4 md:gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((e) => <EventCard key={e.id} event={e} href={`/events/${e.uniqueEventLink}`} actionLabel="Book Now" />)}
        </div>}
    </div>
  );
}
