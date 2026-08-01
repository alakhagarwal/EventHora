"use client";

import Link from "next/link";
import { useRef } from "react";
import { MapPin, Calendar, ChevronRight } from "lucide-react";
import type { EventSummary } from "@/components/EventCard";

function formatDate(dateStr?: string) {
  if (!dateStr) return "TBA";
  return new Date(dateStr).toLocaleDateString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

function EventSlideCard({
  event,
  href,
  actionLabel,
}: {
  event: EventSummary;
  href: string;
  actionLabel?: string;
}) {
  const banner =
    event.bannerUrl ||
    `https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&w=800&q=60`;

  return (
    <Link href={href} className="event-slide-card" style={{ textDecoration: "none" }}>
      {/* Banner */}
      <div className="event-slide-banner">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img src={banner} alt={event.title} className="event-slide-banner-img" />
        <div className="event-slide-gradient" />
        {event.category && (
          <span className="event-slide-category">{event.category}</span>
        )}
        {event.isSoldOut && (
          <span className="event-slide-sold-out">Sold Out</span>
        )}
      </div>
      {/* Info */}
      <div className="event-slide-info">
        <h3 className="event-slide-title">{event.title}</h3>
        <div className="event-slide-meta">
          <span className="event-slide-meta-item">
            <Calendar size={11} />
            {formatDate(event.eventDate)}
          </span>
          {event.venue && (
            <span className="event-slide-meta-item">
              <MapPin size={11} />
              <span className="event-slide-venue">{event.venue}</span>
            </span>
          )}
        </div>
        {event.registrationOpen !== undefined && (
          <span
            className={`event-slide-status ${
              event.registrationOpen
                ? "event-slide-status--open"
                : "event-slide-status--closed"
            }`}
          >
            {event.registrationOpen ? "Open" : "Closed"}
          </span>
        )}
        {actionLabel && (
          <span className="event-slide-action">{actionLabel}</span>
        )}
      </div>
    </Link>
  );
}

export interface EventSliderProps {
  title: string;
  eyebrow?: string;
  events: EventSummary[];
  getHref: (e: EventSummary) => string;
  actionLabel?: string;
  viewAllHref?: string;
  emptyMessage?: string;
  loading?: boolean;
}

export default function EventSlider({
  title,
  eyebrow,
  events,
  getHref,
  actionLabel,
  viewAllHref,
  emptyMessage = "No events found.",
  loading = false,
}: EventSliderProps) {
  const sliderRef = useRef<HTMLDivElement>(null);

  if (loading) {
    return (
      <section className="event-slider-section">
        <div className="event-slider-header">
          {eyebrow && <div className="eyebrow mb-1">{eyebrow}</div>}
          <h2 className="event-slider-heading">{title}</h2>
        </div>
        <div className="event-slider-track">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="event-slide-skeleton">
              <div className="event-slide-skeleton-img animate-pulse" />
              <div className="event-slide-skeleton-body">
                <div className="h-3 w-3/4 rounded bg-navy/10 animate-pulse" />
                <div className="h-2 w-1/2 rounded bg-navy/10 animate-pulse mt-2" />
              </div>
            </div>
          ))}
        </div>
      </section>
    );
  }

  if (events.length === 0) {
    return (
      <section className="event-slider-section">
        <div className="event-slider-header">
          {eyebrow && <div className="eyebrow mb-1">{eyebrow}</div>}
          <h2 className="event-slider-heading">{title}</h2>
        </div>
        <div className="event-slider-empty">{emptyMessage}</div>
      </section>
    );
  }

  return (
    <section className="event-slider-section">
      <div className="event-slider-header">
        <div>
          {eyebrow && <div className="eyebrow mb-0.5">{eyebrow}</div>}
          <h2 className="event-slider-heading">{title}</h2>
        </div>
        {viewAllHref && (
          <Link href={viewAllHref} className="event-slider-view-all">
            View All <ChevronRight size={14} />
          </Link>
        )}
      </div>
      <div className="event-slider-track" ref={sliderRef}>
        {events.map((e) => (
          <EventSlideCard
            key={e.id}
            event={e}
            href={getHref(e)}
            actionLabel={actionLabel}
          />
        ))}
      </div>
    </section>
  );
}
