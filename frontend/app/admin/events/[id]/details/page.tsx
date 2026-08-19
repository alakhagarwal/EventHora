"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { api, displayStatus, type EventMedia } from "@/lib/api";
import { getSession } from "@/lib/auth";
import { ChevronLeft, ChevronRight } from "lucide-react";

function toEmbedUrl(url: string): string {
  const ytMatch = url.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/)([^&?#]+)/);
  if (ytMatch) return `https://www.youtube-nocookie.com/embed/${ytMatch[1]}`;
  return url;
}

export default function AdminEventDetails() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const [ev, setEv] = useState<any>(null);
  const [slideIdx, setSlideIdx] = useState(0);

  useEffect(() => {
    const s = getSession(); if (!s || s.role !== "ADMIN") { router.push("/login"); return; }
    api.adminEvent(id).then(setEv).catch(() => {});
  }, [id, router]);

  if (!ev) return <div className="mx-auto max-w-3xl px-6 py-16 text-navy/60">Loading…</div>;

  const photos: EventMedia[] = (ev.media ?? []).filter((m: EventMedia) => m.mediaType === "PHOTO");
  const videos: EventMedia[] = (ev.media ?? []).filter((m: EventMedia) => m.mediaType === "VIDEO");

  useEffect(() => {
    if (photos.length > 0) {
      setSlideIdx((i) => Math.min(i, photos.length - 1));
    }
  }, [photos.length]);

  return (
    <div className="mx-auto max-w-4xl px-6 py-12">
      <div className="card overflow-hidden">
        {ev.bannerUrl && (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={ev.bannerUrl} alt={ev.title} className="w-full aspect-[21/9] object-cover" />
        )}
        <div className="p-8 space-y-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <div className="eyebrow">{ev.category} · {displayStatus(ev)}</div>
              <h1 className="h1 mt-2">{ev.title}</h1>
            </div>
            <Link href={`/admin/events/${id}`} className="btn-primary">Edit Event</Link>
          </div>
          <div className="grid md:grid-cols-2 gap-4 text-sm">
            <Info label="Event date" value={ev.eventDate} />
            <Info label="Time" value={`${ev.startTime?.slice(0,5) ?? "—"} – ${ev.endTime?.slice(0,5) ?? "—"}`} />
            <Info label="Registration deadline" value={ev.registrationDeadline} />
            <Info label="Venue" value={ev.venue} />
            <Info label="Capacity" value={`${ev.bookedCount ?? 0} / ${ev.totalCapacity}`} />
            <Info label="Available" value={ev.availableCount} />
            <Info label="Min age" value={ev.minimumAge ?? "—"} />
            <Info label="Link" value={ev.uniqueEventLink} />
          </div>

          {/* Dual-tier pricing */}
          <div className="grid gap-4 md:grid-cols-2">
            <div className="rounded-lg border border-navy/10 p-4 space-y-2">
              <div className="text-xs font-semibold uppercase tracking-wider text-navy/50">Member tickets</div>
              <div className="grid grid-cols-3 gap-2 text-sm">
                <Info label="Max" value={ev.maxMemberTickets} />
                <Info label="Free" value={ev.freeMemberTickets} />
                <Info label="Price" value={ev.memberTicketPrice != null ? `₹${ev.memberTicketPrice}` : "—"} />
              </div>
            </div>
            <div className="rounded-lg border border-navy/10 p-4 space-y-2">
              <div className="text-xs font-semibold uppercase tracking-wider text-navy/50">Guest tickets</div>
              <div className="grid grid-cols-3 gap-2 text-sm">
                <Info label="Max" value={ev.maxGuestTickets} />
                <Info label="Free" value={ev.freeGuestTickets} />
                <Info label="Price" value={ev.guestTicketPrice != null ? `₹${ev.guestTicketPrice}` : "—"} />
              </div>
            </div>
          </div>
          <div className="text-sm">
            <Info label="Platform fee / ticket" value={ev.platformFeePerTicket != null ? `₹${ev.platformFeePerTicket}` : "—"} />
          </div>

          <div>
            <div className="label">Description</div>
            <p className="whitespace-pre-line text-navy/80">{ev.description}</p>
          </div>
          {ev.importantNotes?.length > 0 && (
            <div>
              <div className="label">Important notes</div>
              <ul className="list-disc pl-5 text-navy/80">
                {ev.importantNotes.map((n: string, i: number) => <li key={i}>{n}</li>)}
              </ul>
            </div>
          )}

          {/* Photo slideshow */}
          {photos.length > 0 && (
            <div>
              <div className="label mb-3">Photos ({photos.length})</div>
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
            <div>
              <div className="label mb-3">Videos ({videos.length})</div>
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
      </div>
    </div>
  );
}
function Info({ label, value }: { label: string; value: any }) {
  return <div><div className="text-xs uppercase tracking-wider text-navy/50">{label}</div><div className="text-navy font-medium">{String(value ?? "—")}</div></div>;
}
