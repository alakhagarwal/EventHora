"use client";
import { useEffect, useRef, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { api, displayStatus, type EventMedia } from "@/lib/api";
import { getSession } from "@/lib/auth";
import { Calendar, Clock, MapPin, Phone, User, ArrowLeft, ChevronLeft, ChevronRight, Play } from "lucide-react";
import { toast } from "@/lib/toast";

function toEmbedUrl(url: string): string {
  const ytMatch = url.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/)([^&?#]+)/);
  if (ytMatch) return `https://www.youtube-nocookie.com/embed/${ytMatch[1]}`;
  return url;
}

function ytThumbnail(url: string): string | null {
  const m = url.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/)([^&?#]+)/);
  return m ? `https://img.youtube.com/vi/${m[1]}/hqdefault.jpg` : null;
}

export default function EventDetails() {
  const { link } = useParams<{ link: string }>();
  const router = useRouter();
  const [ev, setEv] = useState<any>(null);
  const [isStaffOrAdmin, setIsStaffOrAdmin] = useState(false);
  const [slideIdx, setSlideIdx] = useState(0);
  const previousSoldOut = useRef(false);

  useEffect(() => {
    const s = getSession();
    setIsStaffOrAdmin(!!s && (s.role === "ADMIN" || s.role === "STAFF"));
  }, []);

  useEffect(() => {
    let cancelled = false;
    const load = () =>
      api.publicEvent(link)
        .then((nextEvent) => {
          if (cancelled) return;
          setEv((current: any) => {
            if (!previousSoldOut.current && nextEvent?.isSoldOut && current && !current.isSoldOut) {
              toast.error("This event has just sold out.");
            }
            previousSoldOut.current = !!nextEvent?.isSoldOut;
            return nextEvent;
          });
        })
        .catch((error) => {
          if (!cancelled) toast.error(error.message || "Failed to load event.");
        });

    load();
    const interval = window.setInterval(load, 15000);
    return () => {
      cancelled = true;
      window.clearInterval(interval);
    };
  }, [link]);

  const photos: EventMedia[] = (ev?.media ?? []).filter((m: EventMedia) => m.mediaType === "PHOTO");
  const videos: EventMedia[] = (ev?.media ?? []).filter((m: EventMedia) => m.mediaType === "VIDEO");

  useEffect(() => {
    if (photos.length > 0) {
      setSlideIdx((i) => Math.min(i, photos.length - 1));
    }
  }, [photos.length]);

  if (!ev)
    return (
      <div className="mx-auto max-w-3xl px-4 py-12 text-navy/60">Loading…</div>
    );

  const status = displayStatus(ev);
  const seatsRemaining = Number(ev.availableCount ?? 0);
  const totalCapacity = Number(ev.totalCapacity ?? 0);
  const bookingDisabled = !ev.registrationOpen || status === "COMPLETED" || ev.isSoldOut || seatsRemaining <= 0;
  const bookLabel =
    status === "COMPLETED"
      ? "Event Completed"
      : ev.isSoldOut
      ? "Sold Out"
      : !ev.registrationOpen
      ? "Registration Closed"
      : "Book Now";

  const handleBook = () => {
    if (!bookingDisabled) router.push(`/events/${link}/book`);
  };

  return (
    <>
      {/* ── Main scroll content ── */}
      <div className="mx-auto max-w-6xl px-4 md:px-6 py-8 md:py-12 pb-28 md:pb-12">
        {/* Back link */}
        <Link
          href="/events"
          className="inline-flex items-center gap-1.5 text-sm text-navy/60 hover:text-navy mb-5 transition-colors"
        >
          <ArrowLeft size={16} />
          All Events
        </Link>

        {/* Hero banner */}
        <div className="card overflow-hidden">
          <div className="relative aspect-[16/9] md:aspect-[21/9] bg-navy/20">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={
                ev.bannerUrl ||
                "https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?auto=format&fit=crop&w=2000&q=70"
              }
              alt={ev.title}
              className="h-full w-full object-cover"
            />
            {ev.category && (
              <span className="absolute left-4 top-4 rounded bg-navy px-3 py-1 text-[11px] font-semibold uppercase tracking-wider text-white">
                {ev.category}
              </span>
            )}
          </div>

          {/* ── Mobile: compact info box immediately below banner ── */}
          <div className="md:hidden border-b border-navy/10 bg-cream/60 px-4 py-4 flex flex-col gap-2.5 text-sm text-navy/80">
            <div className="flex items-center gap-2">
              <MapPin size={15} className="text-gold-600 shrink-0" />
              <span>{ev.venue || "TBA"}</span>
            </div>
            <div className="flex items-center gap-2">
              <Calendar size={15} className="text-gold-600 shrink-0" />
              <span>{ev.eventDate}</span>
              <Clock size={15} className="text-gold-600 shrink-0 ml-1" />
              <span>
                {ev.startTime?.slice(0, 5)}
                {ev.endTime ? ` – ${ev.endTime.slice(0, 5)}` : ""}
              </span>
            </div>
            {ev.contactPersonName && (
              <div className="flex items-center gap-2">
                <User size={15} className="text-gold-600 shrink-0" />
                <span>{ev.contactPersonName}</span>
                {ev.contactPersonPhone && (
                  <>
                    <Phone size={13} className="text-gold-600 shrink-0 ml-1" />
                    <span>{ev.contactPersonPhone}</span>
                  </>
                )}
              </div>
            )}
          </div>

          {/* ── Content grid ── */}
          <div className="grid md:grid-cols-3 gap-6 md:gap-8 p-4 md:p-8">
            {/* Left: description + notes */}
            <div className="md:col-span-2">
              <div className="eyebrow">{ev.category}</div>
              <h1 className="h1 mt-2">{ev.title}</h1>

              {ev.description && (
                <p className="mt-4 md:mt-6 whitespace-pre-line text-navy/80 leading-relaxed text-sm md:text-base">
                  {ev.description}
                </p>
              )}

              {ev.importantNotes?.length > 0 && (
                <div className="mt-6 md:mt-8">
                  <h3 className="font-display text-lg md:text-xl text-navy mb-3">
                    Important Notes
                  </h3>
                  <ul className="list-disc pl-5 space-y-1 text-navy/80 text-sm md:text-base">
                    {ev.importantNotes.map((n: string, i: number) => (
                      <li key={i}>{n}</li>
                    ))}
                  </ul>
                </div>
              )}

              {/* Photo slideshow */}
              {photos.length > 0 && (
                <div className="mt-6 md:mt-8">
                  <h3 className="font-display text-lg md:text-xl text-navy mb-3">
                    Photos ({photos.length})
                  </h3>
                  <div className="relative aspect-video rounded-lg overflow-hidden bg-navy/5">
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img
                      src={photos[slideIdx]?.url}
                      alt={photos[slideIdx]?.caption || `Photo ${slideIdx + 1}`}
                      className="w-full h-full object-cover"
                    />
                    {photos[slideIdx]?.caption && (
                      <div className="absolute bottom-0 inset-x-0 bg-gradient-to-t from-black/70 to-transparent p-4">
                        <p className="text-sm text-white">{photos[slideIdx].caption}</p>
                      </div>
                    )}
                    {photos.length > 1 && (
                      <>
                        <button
                          type="button"
                          className="absolute left-2 top-1/2 -translate-y-1/2 bg-black/40 hover:bg-black/60 text-white rounded-full p-1"
                          onClick={() => setSlideIdx((i) => (i - 1 + photos.length) % photos.length)}
                        >
                          <ChevronLeft className="h-5 w-5" />
                        </button>
                        <button
                          type="button"
                          className="absolute right-2 top-1/2 -translate-y-1/2 bg-black/40 hover:bg-black/60 text-white rounded-full p-1"
                          onClick={() => setSlideIdx((i) => (i + 1) % photos.length)}
                        >
                          <ChevronRight className="h-5 w-5" />
                        </button>
                      </>
                    )}
                  </div>
                  {photos.length > 1 && (
                    <div className="flex justify-center gap-1.5 mt-3">
                      {photos.map((_: any, i: number) => (
                        <button
                          key={i}
                          type="button"
                          className={`w-2 h-2 rounded-full transition-colors ${i === slideIdx ? "bg-navy" : "bg-navy/20"}`}
                          onClick={() => setSlideIdx(i)}
                        />
                      ))}
                    </div>
                  )}
                </div>
              )}

              {/* Videos */}
              {videos.length > 0 && (
                <div className="mt-6 md:mt-8">
                  <h3 className="font-display text-lg md:text-xl text-navy mb-3">
                    Videos ({videos.length})
                  </h3>
                  <div className="space-y-4">
                    {videos.map((vid) => (
                      <div key={vid.id}>
                        <div className="relative aspect-video rounded-lg overflow-hidden bg-navy/5">
                          <iframe
                            src={toEmbedUrl(vid.url)}
                            className="w-full h-full"
                            allowFullScreen
                            title={vid.caption || "Video"}
                          />
                        </div>
                        {vid.caption && (
                          <p className="mt-1 text-xs text-navy/60">{vid.caption}</p>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* Right: combined detail card + sticky Book Now (desktop only) */}
            <div className="hidden md:flex flex-col gap-4">
              {/* Combined Venue / Time / Contact card */}
              <div className="card p-4 space-y-4">
                <div className="flex items-start gap-3">
                  <MapPin size={18} className="text-gold-600 shrink-0 mt-0.5" />
                  <div>
                    <div className="text-[11px] uppercase tracking-widest text-navy/50 mb-0.5">Venue</div>
                    <div className="text-navy font-medium text-sm">{ev.venue || "TBA"}</div>
                  </div>
                </div>

                <div className="flex items-start gap-3">
                  <div className="flex flex-col gap-1">
                    <div className="flex items-center gap-2">
                      <Calendar size={18} className="text-gold-600 shrink-0" />
                      <div>
                        <div className="text-[11px] uppercase tracking-widest text-navy/50 mb-0.5">Date</div>
                        <div className="text-navy font-medium text-sm">{ev.eventDate}</div>
                      </div>
                    </div>
                    <div className="flex items-center gap-2 mt-2">
                      <Clock size={18} className="text-gold-600 shrink-0" />
                      <div>
                        <div className="text-[11px] uppercase tracking-widest text-navy/50 mb-0.5">Time</div>
                        <div className="text-navy font-medium text-sm">
                          {ev.startTime?.slice(0, 5)}
                          {ev.endTime ? ` – ${ev.endTime.slice(0, 5)}` : ""}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                {ev.contactPersonName && (
                  <div className="flex items-start gap-3">
                    <User size={18} className="text-gold-600 shrink-0 mt-0.5" />
                    <div>
                      <div className="text-[11px] uppercase tracking-widest text-navy/50 mb-0.5">Contact</div>
                      <div className="text-navy font-medium text-sm">{ev.contactPersonName}</div>
                      {ev.contactPersonPhone && (
                        <div className="flex items-center gap-1.5 mt-1 text-navy/70 text-sm">
                          <Phone size={13} />
                          {ev.contactPersonPhone}
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </div>

              {/* Desktop sticky Book Now */}
              {!isStaffOrAdmin && (
                <div className="sticky top-24">
                  <button
                    onClick={handleBook}
                    disabled={bookingDisabled}
                    className="btn-primary w-full disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {bookLabel}
                  </button>
                  <p className="mt-2 text-[11px] text-navy/50 text-center">
                    Members only · OTP verification required
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* ── Mobile: floating Book Now above bottom nav ── */}
      {!isStaffOrAdmin && (
        <div className="md:hidden fixed bottom-20 left-1/2 -translate-x-1/2 z-50 w-[calc(100%-2rem)] max-w-sm">
          <button
            onClick={handleBook}
            disabled={bookingDisabled}
            className="btn-primary w-full disabled:opacity-50 disabled:cursor-not-allowed shadow-lg"
          >
            {bookLabel}
          </button>
        </div>
      )}
    </>
  );
}
