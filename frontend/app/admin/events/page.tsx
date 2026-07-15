"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import EventCard, { type EventSummary } from "@/components/EventCard";
import { getSession } from "@/lib/auth";

export default function AllAdminEvents() {
  const [events, setEvents] = useState<EventSummary[]>([]);
  const [q, setQ] = useState("");
  const router = useRouter();
  useEffect(() => {
    const s = getSession();
    if (!s || s.role !== "ADMIN") { router.push("/login"); return; }
    api.adminEvents().then(setEvents).catch(() => {});
  }, [router]);

  const filtered = events.filter((e) => !q || e.title?.toLowerCase().includes(q.toLowerCase()));

  return (
    <div className="mx-auto max-w-7xl px-4 md:px-6 py-8 md:py-12">
      <div className="flex flex-col sm:flex-row sm:items-end justify-between mb-6 md:mb-8 gap-4">
        <div>
          <div className="eyebrow">Admin</div>
          <h1 className="h1 mt-2">All Events</h1>
        </div>
        <div className="flex flex-col sm:flex-row gap-2">
          <input className="input" placeholder="Search…" value={q} onChange={(e) => setQ(e.target.value)} />
          <div className="flex gap-2">
            <Link href="/admin/events/public" className="btn-outline flex-1 sm:flex-none">Published</Link>
            <Link href="/admin/events/new" className="btn-primary flex-1 sm:flex-none">+ New</Link>
          </div>
        </div>
      </div>
      <div className="grid gap-4 md:gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {filtered.map((e) => <EventCard key={e.id} event={e} href={`/admin/events/${e.id}`} actionLabel="Edit" />)}
      </div>
    </div>
  );
}
