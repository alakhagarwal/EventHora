import Link from "next/link";
import { MapPin, Calendar } from "lucide-react";
import { displayStatus } from "@/lib/api";


export type EventSummary = {
  id: string;
  title: string;
  category?: string;
  bannerUrl?: string | null;
  eventDate?: string;
  startTime?: string;
  venue?: string;
  status?: string;
  uniqueEventLink?: string;
  totalCapacity?: number;
  bookedCount?: number;
  availableCount?: number;
  registrationOpen?: boolean;
  isSoldOut?: boolean;
};

export default function EventCard({
  event,
  href,
  actionLabel,
  showStatus,
  compact,
}: {
  event: EventSummary;
  href: string;
  actionLabel?: string;
  showStatus?: boolean;
  /** Compact mode: smaller banner, reduced padding/type, no "seats left" text */
  compact?: boolean;
}) {
  const banner =
    event.bannerUrl ||
    `https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&w=1200&q=60`;

  return (
    <div className="card overflow-hidden flex flex-col">
      {/* Banner */}
      <div
        className={`relative overflow-hidden bg-navy/10 ${
          compact ? "aspect-[16/9]" : "aspect-[16/10]"
        }`}
      >
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src={banner}
          alt={event.title}
          className="h-full w-full object-cover transition-transform duration-500 hover:scale-105"
        />
        <div className="absolute left-3 top-3 flex gap-2">
          {event.category && (
            <span className="rounded bg-navy px-2 py-1 text-[10px] font-semibold uppercase tracking-wider text-white">
              {event.category}
            </span>
          )}
          {showStatus && (
            <span className="rounded bg-gold px-2 py-1 text-[10px] font-semibold uppercase tracking-wider text-navy">
              {displayStatus(event)}
            </span>
          )}
        </div>
        {event.isSoldOut && (
          <div className="absolute right-3 top-3 rounded bg-red-600/90 px-2 py-1 text-[10px] font-bold uppercase text-white">
            Sold out
          </div>
        )}
      </div>

      {/* Body */}
      <div className={`flex flex-1 flex-col ${compact ? "p-3 md:p-4" : "p-5"}`}>
        <h3
          className={`font-display text-navy leading-tight ${
            compact ? "text-base md:text-lg" : "text-xl"
          }`}
        >
          {event.title}
        </h3>
        <div
          className={`mt-2 flex items-center gap-2 text-navy/70 ${
            compact ? "text-xs" : "text-sm"
          }`}
        >
          <MapPin size={compact ? 12 : 14} className="shrink-0" />
          <span>{event.venue || "TBA"}</span>
        </div>
        <div
          className={`mt-1 flex items-center gap-3 text-navy/70 ${
            compact ? "text-xs" : "text-sm"
          }`}
        >
          <span className="flex items-center gap-1">
            <Calendar size={compact ? 12 : 14} className="shrink-0" />
            {event.eventDate || "TBA"}
          </span>
          {event.startTime && <span>· {event.startTime.slice(0, 5)}</span>}
        </div>

        {/* Status row — open/closed chip sits inline with the CTA */}
        <div className="mt-3 flex items-center justify-between gap-3 border-t border-navy/10 pt-3">
          <div className="flex min-w-0 items-center gap-2">
            {!compact && typeof event.availableCount === "number" && (
              <span className="whitespace-nowrap text-xs text-navy/60">
                {event.availableCount}/{event.totalCapacity} seats left
              </span>
            )}
            {event.registrationOpen ? (
              <span className="chip bg-green-100 text-green-800">Open</span>
            ) : (
              <span className="chip bg-navy/10">Closed</span>
            )}
          </div>
          <Link
            href={href}
            className={`${compact ? "btn-dark px-3 py-1.5 text-xs" : "btn-dark"} shrink-0`}
          >
            {actionLabel || "View"}
          </Link>
        </div>
      </div>
    </div>
  );
}
