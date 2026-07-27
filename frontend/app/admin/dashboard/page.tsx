"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { Calendar, MapPin, Search } from "lucide-react";
import { api, displayStatus } from "@/lib/api";
import { getSession } from "@/lib/auth";

type AdminEvent = {
  id: string;
  title?: string;
  eventDate?: string;
  startTime?: string;
  venue?: string;
  status?: string;
  availableCount?: number;
  totalCapacity?: number;
};

function formatDateTime(eventDate?: string, startTime?: string) {
  if (!eventDate) return "Date TBD";
  const date = new Date(eventDate);
  const datePart = date.toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
  const timePart = startTime || date.toLocaleTimeString("en-US", {
    hour: "numeric",
    minute: "2-digit",
  });
  return `${datePart} · ${timePart}`;
}

function StatusBadge({ status }: { status: string }) {
  const tone =
    status === "PUBLISHED"
      ? "bg-green-100 text-green-800"
      : status === "CANCELLED"
      ? "bg-red-100 text-red-800"
      : status === "COMPLETED"
      ? "bg-slate-100 text-slate-700"
      : "bg-amber-100 text-amber-800";

  return <span className={`chip ${tone}`}>{status}</span>;
}

export default function AdminDashboardPage() {
  const router = useRouter();
  const [events, setEvents] = useState<AdminEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [authReady, setAuthReady] = useState(false);

  useEffect(() => {
    const session = getSession();
    if (!session || (session.role !== "ADMIN" && session.role !== "STAFF")) {
      router.replace("/login");
      setAuthReady(true);
      return;
    }

    setAuthReady(true);
    setLoading(true);
    api.adminEvents()
      .then((data) => setEvents(data || []))
      .catch((err) => setError(err?.message || "Failed to load events."))
      .finally(() => setLoading(false));
  }, [router]);

  const filteredEvents = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    return [...events]
      .sort((a, b) => new Date(b.eventDate || 0).getTime() - new Date(a.eventDate || 0).getTime())
      .filter((event) => !normalizedQuery || (event.title || "").toLowerCase().includes(normalizedQuery));
  }, [events, query]);

  if (!authReady) {
    return <div className="mx-auto max-w-7xl px-4 py-12 text-navy/60">Loading...</div>;
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 md:px-6 md:py-12">
      <div className="mb-6 flex flex-col gap-4 md:mb-8 md:flex-row md:items-end md:justify-between">
        <div>
          <div className="eyebrow">Admin</div>
          <h1 className="h1 mt-2">Dashboard</h1>
        </div>

        <div className="relative w-full md:max-w-md">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-navy/40" />
          <input
            className="input pl-10"
            placeholder="Search events by title"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
        </div>
      </div>

      {loading ? (
        <div className="card divide-y divide-navy/10 overflow-hidden">
          {[1, 2, 3, 4].map((item) => (
            <div key={item} className="flex flex-col gap-4 p-5 md:flex-row md:items-center md:justify-between animate-pulse">
              <div className="space-y-3">
                <div className="h-5 w-72 rounded bg-navy/10" />
                <div className="h-4 w-52 rounded bg-navy/10" />
                <div className="h-4 w-40 rounded bg-navy/10" />
              </div>
              <div className="flex gap-3">
                <div className="h-10 w-32 rounded-md bg-navy/10" />
                <div className="h-10 w-36 rounded-md bg-navy/10" />
              </div>
            </div>
          ))}
        </div>
      ) : error ? (
        <div className="card border-red-200 bg-red-50 p-8 text-red-700">{error}</div>
      ) : filteredEvents.length === 0 ? (
        <div className="card p-12 text-center text-navy/60">
          <Calendar className="mx-auto mb-3 h-12 w-12 text-navy/30" />
          <div className="font-display text-lg font-semibold text-navy">No events found</div>
          <p className="mt-1 text-sm">There are no events matching your search.</p>
        </div>
      ) : (
        <div className="card divide-y divide-navy/10 overflow-hidden">
          {filteredEvents.map((event) => (
            <div
              key={event.id}
              className="flex flex-col gap-4 p-5 transition-colors hover:bg-navy/5 md:flex-row md:items-center md:justify-between"
            >
              <div className="min-w-0 flex-1 space-y-2">
                <div className="flex flex-wrap items-center gap-2">
                  <h2 className="min-w-0 font-display text-xl font-semibold text-navy">{event.title || "Untitled event"}</h2>
                  <StatusBadge status={displayStatus(event)} />
                </div>
                <div className="flex flex-wrap items-center gap-4 text-sm text-navy/70">
                  <span className="flex items-center gap-2">
                    <Calendar className="h-4 w-4 text-gold" />
                    {formatDateTime(event.eventDate, event.startTime)}
                  </span>
                  <span className="flex items-center gap-2">
                    <MapPin className="h-4 w-4 text-gold" />
                    {event.venue || "Venue TBD"}
                  </span>
                </div>
                <div className="text-sm text-navy/70">
                  Seats: {event.availableCount ?? 0} / {event.totalCapacity ?? 0}
                </div>
              </div>

              <div className="flex flex-col gap-2 sm:flex-row">
                <Link href={`/admin/events/${event.id}/registrations`} className="btn-outline whitespace-nowrap">
                  Registrations
                </Link>
                <Link href={`/admin/events/${event.id}/summary`} className="btn-dark whitespace-nowrap">
                  Payment Summary
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}