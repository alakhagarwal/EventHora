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
              "url(/hero.webp)",
            backgroundSize: "cover",
            backgroundPosition: "center",
          }}
        />
        <div className="absolute inset-0 bg-gradient-to-b from-navy/70 via-navy/85 to-navy" />
        <div className="relative mx-auto max-w-7xl px-4 py-16 md:px-6 md:py-32 text-center">
          <span className="inline-flex items-center gap-2 rounded-full border border-gold/40 bg-navy/40 px-3 py-1 text-[10px] md:text-xs font-semibold uppercase tracking-[0.25em] text-gold">
            ✦ Premier Event Platform
          </span>
          <h1 className="font-display mt-4 md:mt-6 text-3xl sm:text-4xl md:text-5xl lg:text-7xl font-bold leading-[1.1] md:leading-[1.05]">
            Where Every <span className="italic text-gold">Moment</span>
            <br /> Truly Matters.
          </h1>
          <p className="mx-auto mt-4 md:mt-6 max-w-2xl text-sm md:text-base text-white/70">
            Discover, register, and manage extraordinary events — cultural evenings, conferences,
            music nights, and every celebration in between.
          </p>
          <div className="mt-8 md:mt-10 flex flex-col sm:flex-row flex-wrap justify-center gap-3">
            <Link href="/events" className="btn-primary">Browse Events</Link>
            <Link href="/login" className="btn-outline bg-transparent border-white/30 text-white hover:bg-white/10">Admin / Staff Login</Link>
            <Link href="/login" className="btn-ghost text-white hover:bg-white/10">Member Login →</Link>
          </div>
          <div className="mt-10 md:mt-14 grid grid-cols-2 md:grid-cols-4 gap-4 md:gap-6 max-w-3xl mx-auto">
            {[
              ["50+", "Events Hosted"],
              ["12,000+", "Members"],
              ["98%", "Satisfaction"],
              ["24/7", "Support"],
            ].map(([n, l]) => (
              <div key={l}>
                <div className="font-display text-2xl md:text-3xl text-gold">{n}</div>
                <div className="text-[10px] md:text-xs uppercase tracking-widest text-white/60 mt-1">{l}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Featured */}
      <section className="mx-auto max-w-7xl px-4 md:px-6 py-12 md:py-20">
        <div className="flex flex-col sm:flex-row sm:items-end justify-between mb-8 md:mb-10 gap-4">
          <div>
            <div className="eyebrow">Handpicked for you</div>
            <h2 className="h2 mt-2">Featured Events</h2>
          </div>
          <Link href="/events" className="btn-outline self-start sm:self-auto">View All →</Link>
        </div>
        {loading ? (
          <div className="text-navy/60">Loading…</div>
        ) : events.length === 0 ? (
          <div className="card p-10 text-center text-navy/60">No published events yet.</div>
        ) : (
          <div className="grid gap-4 md:gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {events.slice(0, 6).map((e) => (
              <EventCard key={e.id} event={e} href={`/events/${e.uniqueEventLink}`} actionLabel="Book Now" />
            ))}
          </div>
        )}
      </section>
    </>
  );
}
