"use client";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import EventCard, { type EventSummary } from "@/components/EventCard";
import { getSession } from "@/lib/auth";

export default function AdminPublishedEvents() {
  const [events, setEvents] = useState<EventSummary[]>([]);
  const router = useRouter();
  useEffect(() => {
    const s = getSession(); if (!s || s.role !== "ADMIN") { router.push("/login"); return; }
    api.publicEvents().then(setEvents).catch(() => {});
  }, [router]);
  return (
    <div className="mx-auto max-w-7xl px-6 py-12">
      <div className="eyebrow">Admin</div>
      <h1 className="h1 mt-2 mb-8">Published Events (Read-only)</h1>
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        {events.map((e) => <EventCard key={e.id} event={e} href={`/events/${e.uniqueEventLink}`} actionLabel="View Public Page" />)}
      </div>
    </div>
  );
}
