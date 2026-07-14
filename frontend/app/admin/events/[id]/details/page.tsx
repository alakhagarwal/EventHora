"use client";
import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { eventsApi } from "@/src/lib/api";
export default function EventDetails() {
  const { id } = useParams<{ id: string }>();
  const [e,setE]=useState<any>(null); const [err,setErr]=useState<string|null>(null);
  useEffect(()=>{eventsApi.adminDetail(id as string).then(setE).catch(x=>setErr(x.message));},[id]);
  if (err) return <p className="text-red-600">{err}</p>;
  if (!e) return <p>Loading…</p>;
  return (<div className="space-y-4">
    <div className="flex items-center justify-between">
      <h1 className="text-2xl font-semibold">{e.title}</h1>
      <Link href={`/admin/events/${e.id}`} className="btn-primary">Edit</Link>
    </div>
    {e.bannerUrl && <img src={e.bannerUrl} alt={e.title} className="w-full max-w-2xl rounded-xl" />}
    <p className="text-slate-600">{e.eventDate} · {e.startTime} – {e.endTime} · {e.venue}</p>
    <p className="whitespace-pre-line">{e.description}</p>
    <p className="text-sm text-slate-500">Status: {e.status} · Booked {e.bookedCount}/{e.totalCapacity}</p>
  </div>);
}
