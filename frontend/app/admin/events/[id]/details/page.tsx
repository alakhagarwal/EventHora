"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { getSession } from "@/lib/auth";

export default function AdminEventDetails() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const [ev, setEv] = useState<any>(null);
  useEffect(() => {
    const s = getSession(); if (!s || s.role !== "ADMIN") { router.push("/login"); return; }
    api.adminEvent(id).then(setEv).catch(() => {});
  }, [id, router]);
  if (!ev) return <div className="mx-auto max-w-3xl px-6 py-16 text-navy/60">Loading…</div>;

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
              <div className="eyebrow">{ev.category} · {ev.status}</div>
              <h1 className="h1 mt-2">{ev.title}</h1>
            </div>
            <Link href={`/admin/events/${id}`} className="btn-primary">Edit Event</Link>
          </div>
          <div className="grid md:grid-cols-2 gap-4 text-sm">
            <Info label="Event date" value={ev.eventDate} />
            <Info label="Time" value={`${ev.startTime?.slice(0,5)} – ${ev.endTime?.slice(0,5)}`} />
            <Info label="Registration deadline" value={ev.registrationDeadline} />
            <Info label="Venue" value={ev.venue} />
            <Info label="Capacity" value={`${ev.bookedCount ?? 0} / ${ev.totalCapacity}`} />
            <Info label="Available" value={ev.availableCount} />
            <Info label="Max tickets / member" value={ev.maxTicketsPerMember} />
            <Info label="Free per registration" value={ev.freeTicketsPerRegistration} />
            <Info label="Ticket price" value={`₹${ev.ticketPrice}`} />
            <Info label="Platform fee" value={`₹${ev.platformFeePerTicket}`} />
            <Info label="Min age" value={ev.minimumAge ?? "—"} />
            <Info label="Link" value={ev.uniqueEventLink} />
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
        </div>
      </div>
    </div>
  );
}
function Info({ label, value }: { label: string; value: any }) {
  return <div><div className="text-xs uppercase tracking-wider text-navy/50">{label}</div><div className="text-navy font-medium">{String(value ?? "—")}</div></div>;
}
