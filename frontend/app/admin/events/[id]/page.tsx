"use client";
import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { api } from "@/lib/api";
import EventForm from "@/components/EventForm";
import { getSession } from "@/lib/auth";

export default function EditEvent() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const [ev, setEv] = useState<any>(null);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    const s = getSession(); if (!s || s.role !== "ADMIN") { router.push("/login"); return; }
    api.adminEvent(id).then(setEv).catch((e) => setErr(e.message));
  }, [id, router]);

  if (err) return <div className="mx-auto max-w-3xl px-6 py-16 text-red-600">{err}</div>;
  if (!ev) return <div className="mx-auto max-w-3xl px-6 py-16 text-navy/60">Loading…</div>;

  const initial = {
    ...ev,
    startTime: ev.startTime || "18:00:00",
    endTime: ev.endTime || "20:00:00",
    importantNotes: ev.importantNotes || [],
  };

  return (
    <div className="mx-auto max-w-5xl px-6 py-12">
      <div className="flex items-end justify-between mb-8">
        <div>
          <div className="eyebrow">Admin · {ev.status}</div>
          <h1 className="h1 mt-2">{ev.title}</h1>
        </div>
        <Link href={`/admin/events/${id}/details`} className="btn-outline">View Details</Link>
      </div>
      <EventForm eventId={id} initial={initial} onSaved={(r) => r && setEv(r)} />
    </div>
  );
}
