"use client";
import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import EventCard, { type EventSummary } from "@/components/EventCard";
import { getSession } from "@/lib/auth";
import SearchInput from "@/components/SearchInput";
import { toast } from "@/lib/toast";
import { useDebouncedValue } from "@/lib/useDebouncedValue";

export default function AllAdminEvents() {
  const [events, setEvents] = useState<EventSummary[]>([]);
  const [q, setQ] = useState("");
  const router = useRouter();
  useEffect(() => {
    const s = getSession();
    if (!s || s.role !== "ADMIN") { router.push("/login"); return; }
    api.adminEvents().then(setEvents).catch((err) => toast.error(err.message || "Failed to load events."));
  }, [router]);

  const debouncedQuery = useDebouncedValue(q, 300);

  const now = new Date();
  const past = events
    .filter((e) => e.eventDate && new Date(e.eventDate) < now)
    .sort((a, b) => new Date(b.eventDate!).getTime() - new Date(a.eventDate!).getTime());
  const filtered = useMemo(
    () => past.filter((e) => !debouncedQuery || e.title?.toLowerCase().includes(debouncedQuery.toLowerCase())),
    [debouncedQuery, past]
  );

  return (
    <div className="mx-auto max-w-7xl px-4 md:px-6 py-8 md:py-12">
      <div className="flex flex-col sm:flex-row sm:items-end justify-between mb-6 md:mb-8 gap-4">
        <div>
          <div className="eyebrow">Admin</div>
          <h1 className="h1 mt-2">Past Events</h1>
        </div>
        <div className="flex flex-col sm:flex-row gap-2">
          <SearchInput value={q} onChange={setQ} placeholder="Search by title" className="w-full sm:w-72" />
          <div className="flex gap-2">
            <Link href="/admin/events/public" className="btn-outline flex-1 sm:flex-none">Published</Link>
            <Link href="/admin/events/new" className="btn-primary flex-1 sm:flex-none">+ New</Link>
          </div>
        </div>
      </div>
      <div className="grid gap-4 md:gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {filtered.length === 0 ? (
          <div className="col-span-full card p-10 text-center text-navy/60">No past events yet.</div>
        ) : (
          filtered.map((e) => <EventCard key={e.id} event={e} href={`/admin/events/${e.id}`} actionLabel="Edit" showStatus />)
        )}
      </div>
    </div>
  );
}
