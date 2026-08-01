"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { api } from "@/lib/api";
import { getSession, isLoggedIn, clearSession, clearMemberSession } from "@/lib/auth";
import EventSlider from "@/components/EventSlider";
import type { EventSummary } from "@/components/EventCard";

export default function Landing() {
  const router = useRouter();
  const [events, setEvents] = useState<EventSummary[]>([]);
  const [staffEvents, setStaffEvents] = useState<EventSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [loggedIn, setLoggedIn] = useState(false);
  const [isStaffOrAdmin, setIsStaffOrAdmin] = useState(false);

  useEffect(() => {
    setLoggedIn(isLoggedIn());
    const session = getSession();
    const staffOrAdmin = !!session && (session.role === "ADMIN" || session.role === "STAFF");
    setIsStaffOrAdmin(staffOrAdmin);
    if (staffOrAdmin) {
      api.adminEvents()
        .then((e) => setStaffEvents(e || []))
        .catch(() => setStaffEvents([]))
        .finally(() => setLoading(false));
    } else {
      api.publicEvents()
        .then((e) => setEvents(e || []))
        .catch(() => setEvents([]))
        .finally(() => setLoading(false));
    }
  }, []);

  const logout = () => {
    clearSession();
    clearMemberSession();
    setLoggedIn(false);
    router.push("/");
  };

  const upcomingEvents = useMemo(
    () =>
      events
        .filter((e) => !e.eventDate || new Date(e.eventDate) >= new Date())
        .sort((a, b) => new Date(a.eventDate || "9999-12-31").getTime() - new Date(b.eventDate || "9999-12-31").getTime()),
    [events]
  );

  const pastEvents = useMemo(
    () =>
      staffEvents
        .filter((e) => e.eventDate && new Date(e.eventDate) < new Date())
        .sort((a, b) => new Date(b.eventDate || "1000-01-01").getTime() - new Date(a.eventDate || "1000-01-01").getTime()),
    [staffEvents]
  );

  return (
    <>
      {/* Hero */}
      <section className="relative overflow-hidden bg-navy text-white">
        <div
          className="absolute inset-0 opacity-30"
          style={{
            backgroundImage: "url(/hero.webp)",
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
            {loggedIn ? (
              <button onClick={logout} className="btn-outline bg-transparent border-white/30 text-white hover:bg-white/10">Logout</button>
            ) : (
              <Link href="/login" className="btn-outline bg-transparent border-white/30 text-white hover:bg-white/10">Login</Link>
            )}
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

      {/* Featured Events for guests/members; Past Events for staff/admin */}
      {isStaffOrAdmin ? (
        <section className="py-8 md:py-16">
          <EventSlider
            title="Past Events"
            eyebrow="Your event history"
            events={pastEvents}
            getHref={(e) => `/events/${e.uniqueEventLink}`}
            actionLabel="View"
            viewAllHref="/events"
            loading={loading}
            emptyMessage="No past events yet."
          />
        </section>
      ) : (
        <section className="py-8 md:py-16">
          <EventSlider
            title="Featured Events"
            eyebrow="Handpicked for you"
            events={upcomingEvents}
            getHref={(e) => `/events/${e.uniqueEventLink}`}
            actionLabel="Book Now"
            viewAllHref="/events"
            loading={loading}
            emptyMessage="No published events yet."
          />
        </section>
      )}
    </>
  );
}
