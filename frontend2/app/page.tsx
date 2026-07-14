"use client";
import Link from "next/link";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import EventCard, { type EventSummary } from "@/components/EventCard";

export default function Landing() {
  const [events, setEvents] = useState<EventSummary[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.publicEvents().then((e) => setEvents(e || [])).catch(() => setEvents([])).finally(() => setLoading(false));
  }, []);

  return (
    <>
      {/* Hero */}
      <section className="relative overflow-hidden bg-navy text-white">
        <div
          className="absolute inset-0 opacity-30"
          style={{
            backgroundImage:
              "url(https://images.unsplash.com/photo-1519671482749-fd09be7ccebf?auto=format&fit=crop&w=2000&q=70)",
            backgroundSize: "cover",
            backgroundPosition: "center",
          }}
        />
        <div className="absolute inset-0 bg-gradient-to-b from-navy/70 via-navy/85 to-navy" />
        <div className="relative mx-auto max-w-7xl px-6 py-24 md:py-32 text-center">
          <span className="inline-flex items-center gap-2 rounded-full border border-gold/40 bg-navy/40 px-4 py-1 text-xs font-semibold uppercase tracking-[0.25em] text-gold">
            ✦ Premier Event Platform
          </span>
          <h1 className="font-display mt-6 text-5xl md:text-7xl font-bold leading-[1.05]">
            Where Every <span className="italic text-gold">Moment</span>
            <br /> Truly Matters.
          </h1>
          <p className="mx-auto mt-6 max-w-2xl text-white/70">
            Discover, register, and manage extraordinary events — cultural evenings, conferences,
            music nights, and every celebration in between.
          </p>
          <div className="mt-10 flex flex-wrap justify-center gap-3">
            <Link href="/events" className="btn-primary">Browse Events</Link>
            <Link href="/login" className="btn-outline bg-transparent border-white/30 text-white hover:bg-white/10">Admin / Staff Login</Link>
            <Link href="/login" className="btn-ghost text-white hover:bg-white/10">Member Login →</Link>
          </div>
          <div className="mt-14 grid grid-cols-2 md:grid-cols-4 gap-6 max-w-3xl mx-auto">
            {[
              ["50+", "Events Hosted"],
              ["12,000+", "Members"],
              ["98%", "Satisfaction"],
              ["24/7", "Support"],
            ].map(([n, l]) => (
              <div key={l}>
                <div className="font-display text-3xl text-gold">{n}</div>
                <div className="text-xs uppercase tracking-widest text-white/60 mt-1">{l}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Featured */}
      <section className="mx-auto max-w-7xl px-6 py-20">
        <div className="flex items-end justify-between mb-10">
          <div>
            <div className="eyebrow">Handpicked for you</div>
            <h2 className="h2 mt-2">Featured Events</h2>
          </div>
          <Link href="/events" className="btn-outline">View All →</Link>
        </div>
        {loading ? (
          <div className="text-navy/60">Loading…</div>
        ) : events.length === 0 ? (
          <div className="card p-10 text-center text-navy/60">No published events yet.</div>
        ) : (
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {events.slice(0, 6).map((e) => (
              <EventCard key={e.id} event={e} href={`/events/${e.uniqueEventLink}`} actionLabel="Book Now" />
            ))}
          </div>
        )}
      </section>
    </>
  );
}
