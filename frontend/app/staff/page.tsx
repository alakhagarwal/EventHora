"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Calendar, MapPin, Ticket, ArrowRight, ShieldCheck } from "lucide-react";
import { api } from "@/lib/api";
import { requireStaffAuth } from "@/lib/auth";
import type { EventSummary } from "@/components/EventCard";
import { toast } from "@/lib/toast";

export default function StaffHomePage() {
  const router = useRouter();
  const [events, setEvents] = useState<EventSummary[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const session = requireStaffAuth();
    if (!session) {
      router.push("/login");
      return;
    }

    api.adminEvents()
      .then((data) => {
        // Filter for PUBLISHED events
        const published = (data || []).filter((e: EventSummary) => e.status === "PUBLISHED");
        setEvents(published);
      })
      .catch((err) => {
        toast.error(err.message || "Failed to load events.");
      })
      .finally(() => {
        setLoading(false);
      });
  }, [router]);

  const selectEvent = (event: EventSummary) => {
    if (typeof window !== "undefined") {
      sessionStorage.setItem("selectedEvent", JSON.stringify({ id: event.id, title: event.title }));
      sessionStorage.setItem("selectedEventId", event.id);
      sessionStorage.setItem("selectedEventTitle", event.title);
    }
    router.push("/staff/scan");
  };

  return (
    <div className="min-h-[calc(100vh-80px)] bg-cream py-8 px-4 md:px-6">
      <div className="mx-auto max-w-5xl">
        {/* Header */}
        <div className="mb-8 text-center md:text-left">
          <div className="eyebrow flex items-center justify-center md:justify-start gap-1.5 text-gold font-semibold">
            <ShieldCheck className="h-4 w-4" /> Gate Scanner Portal
          </div>
          <h1 className="h1 mt-2 text-navy">Select Event for Gate Entry</h1>
          <p className="mt-2 text-navy/70 text-sm md:text-base">
            Choose an active event to start scanning tickets and checking in members at the entry gate.
          </p>
        </div>

        {/* Content */}
        {loading ? (
          <div className="grid gap-4 md:grid-cols-2">
            {[1, 2, 3].map((n) => (
              <div key={n} className="card p-6 animate-pulse space-y-4">
                <div className="h-6 bg-navy/10 rounded w-3/4" />
                <div className="h-4 bg-navy/10 rounded w-1/2" />
                <div className="h-4 bg-navy/10 rounded w-1/3" />
              </div>
            ))}
          </div>
        ) : events.length === 0 ? (
          <div className="card p-12 text-center text-navy/60">
            <Ticket className="mx-auto h-12 w-12 text-navy/30 mb-3" />
            <h3 className="font-display text-lg font-semibold text-navy">No Published Events</h3>
            <p className="text-sm mt-1">There are no currently published events available for gate scanning.</p>
          </div>
        ) : (
          <div className="grid gap-6 md:grid-cols-2">
            {events.map((event) => {
              const formattedDate = event.eventDate
                ? new Date(event.eventDate).toLocaleDateString("en-US", {
                    weekday: "short",
                    month: "short",
                    day: "numeric",
                    year: "numeric",
                  })
                : "Date TBD";

              const seatsText =
                event.availableCount !== undefined
                  ? `${event.availableCount} ${event.availableCount === 1 ? "seat" : "seats"} available`
                  : event.totalCapacity
                  ? `${event.totalCapacity} capacity`
                  : "Seats info unavailable";

              return (
                <div
                  key={event.id}
                  onClick={() => selectEvent(event)}
                  className="card p-6 cursor-pointer hover:border-gold hover:shadow-md transition-all group flex flex-col justify-between"
                >
                  <div>
                    <div className="flex items-start justify-between gap-3">
                      <h2 className="font-display text-xl font-bold text-navy group-hover:text-gold transition-colors">
                        {event.title}
                      </h2>
                      <span className="chip-primary text-xs whitespace-nowrap">PUBLISHED</span>
                    </div>

                    <div className="mt-4 space-y-2 text-sm text-navy/70">
                      <div className="flex items-center gap-2">
                        <Calendar className="h-4 w-4 text-gold shrink-0" />
                        <span>{formattedDate} {event.startTime ? `• ${event.startTime}` : ""}</span>
                      </div>
                      {event.venue && (
                        <div className="flex items-center gap-2">
                          <MapPin className="h-4 w-4 text-gold shrink-0" />
                          <span>{event.venue}</span>
                        </div>
                      )}
                      <div className="flex items-center gap-2">
                        <Ticket className="h-4 w-4 text-gold shrink-0" />
                        <span>{seatsText}</span>
                      </div>
                    </div>
                  </div>

                  <div className="mt-6 pt-4 border-t border-navy/10 flex items-center justify-between text-xs font-semibold text-navy group-hover:text-gold">
                    <span>Tap to start scanning</span>
                    <ArrowRight className="h-4 w-4 transform group-hover:translate-x-1 transition-transform" />
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
