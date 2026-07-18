"use client";
import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api, displayStatus } from "@/lib/api";
import EventCard, { type EventSummary } from "@/components/EventCard";
import { getSession } from "@/lib/auth";

const TABS = ["ALL", "DRAFT", "PUBLISHED", "CANCELLED", "COMPLETED"] as const;

export default function MyEvents() {
  const [events, setEvents] = useState<EventSummary[]>([]);
  const [tab, setTab] = useState<(typeof TABS)[number]>("ALL");
  const [err, setErr] = useState<string | null>(null);
  const router = useRouter();

  useEffect(() => {
    const s = getSession();
    if (!s || s.role !== "ADMIN") { router.push("/login"); return; }
    api.adminEvents().then(setEvents).catch((e) => setErr(e.message));
  }, [router]);

  const filtered = useMemo(() => tab === "ALL" ? events : events.filter((e) => displayStatus(e) === tab), [events, tab]);

  return (
    <div className="mx-auto max-w-7xl px-4 md:px-6 py-8 md:py-12">
      <div className="flex flex-col sm:flex-row sm:items-end justify-between mb-6 md:mb-8 gap-4">
        <div>
          <div className="eyebrow">Admin</div>
          <h1 className="h1 mt-2">My Events</h1>
        </div>
        <Link href="/admin/events/new" className="btn-primary self-start sm:self-auto">+ Create Event</Link>
      </div>
      <div className="card p-2 mb-6 md:mb-8 flex gap-1 tabs-scroll-mobile">
        {TABS.map((t) => (
          <button key={t} onClick={() => setTab(t)}
            className={`px-3 md:px-4 py-2 rounded-md text-xs md:text-sm font-semibold ${tab === t ? "bg-navy text-white" : "text-navy/70 hover:bg-navy/5"}`}>
            {t} ({t === "ALL" ? events.length : events.filter(e => displayStatus(e) === t).length})
          </button>
        ))}
      </div>
      {err && <div className="text-red-600">{err}</div>}
      {filtered.length === 0 ? (
        <div className="card p-10 text-center text-navy/60">No events in this status.</div>
      ) : (
        <div className="grid gap-4 md:gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((e) => <EventCard key={e.id} event={e} href={`/admin/events/${e.id}`} actionLabel="Edit" showStatus />)}
        </div>
      )}
    </div>
  );
}
