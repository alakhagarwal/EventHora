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
}: {
  event: EventSummary;
  href: string;
  actionLabel?: string;
  showStatus?: boolean;
}) {
  const banner =
    event.bannerUrl ||
    `https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&w=1200&q=60`;
  return (
    <div className="card overflow-hidden flex flex-col">
      <div className="relative aspect-[16/10] overflow-hidden bg-navy/10">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img src={banner} alt={event.title} className="h-full w-full object-cover transition-transform duration-500 hover:scale-105" />
        <div className="absolute left-3 top-3 flex gap-2">
          {event.category && <span className="rounded bg-navy px-2 py-1 text-[10px] font-semibold uppercase tracking-wider text-white">{event.category}</span>}
          {showStatus && (
            <span className="rounded bg-gold px-2 py-1 text-[10px] font-semibold uppercase tracking-wider text-navy">{displayStatus(event)}</span>
          )}
        </div>
        {event.isSoldOut && (
          <div className="absolute right-3 top-3 rounded bg-red-600/90 px-2 py-1 text-[10px] font-bold uppercase text-white">Sold out</div>
        )}
      </div>
      <div className="flex flex-1 flex-col p-5">
        <h3 className="font-display text-xl text-navy leading-tight">{event.title}</h3>
        <div className="mt-2 flex items-center gap-2 text-sm text-navy/70">
          <MapPin size={14} className="shrink-0" /><span>{event.venue || "TBA"}</span>
        </div>
        <div className="mt-1 flex items-center gap-3 text-sm text-navy/70">
          <span className="flex items-center gap-1"><Calendar size={14} className="shrink-0" /> {event.eventDate || "TBA"}</span>
          {event.startTime && <span>· {event.startTime.slice(0, 5)}</span>}
        </div>
        <div className="mt-3 flex items-center gap-3 text-xs text-navy/60 border-t border-navy/10 pt-3">
          {typeof event.availableCount === "number" && (
            <span>{event.availableCount}/{event.totalCapacity} seats left</span>
          )}
          {event.registrationOpen ? (
            <span className="chip bg-green-100 text-green-800">Open</span>
          ) : (
            <span className="chip bg-navy/10">Closed</span>
          )}
        </div>
        <div className="mt-4 flex items-center justify-end">
          <Link href={href} className="btn-dark">{actionLabel || "View"}</Link>
        </div>
      </div>
    </div>
  );
}
