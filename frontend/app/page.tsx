"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { eventsApi } from "@/src/lib/api";
import EventCard from "@/src/components/EventCard";

export default function Home() {
  const [events, setEvents] = useState<any[]>([]);
  const [err, setErr] = useState<string | null>(null);
  useEffect(() => { eventsApi.publicList().then(setEvents).catch(e => setErr(e.message)); }, []);
  return (
    <div className="space-y-10">
      <section className="rounded-2xl bg-gradient-to-br from-indigo-600 to-indigo-400 p-10 text-white">
        <h1 className="text-4xl font-bold">EventHora</h1>
        <p className="mt-2 max-w-xl text-indigo-100">Book cultural, musical, and community events at your chapter — smoothly and securely.</p>
        <div className="mt-6 flex flex-wrap gap-3">
          <Link href="/events" className="btn-outline bg-white/95">Browse Events</Link>
          <Link href="/login" className="btn bg-white text-indigo-700 hover:bg-indigo-50">Login</Link>
        </div>
      </section>
      <section>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-2xl font-semibold">Upcoming Events</h2>
          <Link href="/events" className="text-sm text-indigo-600">See all →</Link>
        </div>
        {err && <p className="text-sm text-red-600">{err}</p>}
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {events.slice(0,6).map(e => <EventCard key={e.id} e={e} />)}
        </div>
      </section>
    </div>
  );
}
