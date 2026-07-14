"use client";
import { useEffect, useState } from "react";
import { eventsApi } from "@/src/lib/api";
import EventCard from "@/src/components/EventCard";
export default function AdminPublicEvents() {
  const [ev,setEv]=useState<any[]>([]); const [err,setErr]=useState<string|null>(null);
  useEffect(()=>{eventsApi.publicList().then(setEv).catch(e=>setErr(e.message));},[]);
  return (<div><h1 className="mb-6 text-2xl font-semibold">Published Events (Public View)</h1>
    {err && <p className="text-red-600">{err}</p>}
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {ev.map(e => <EventCard key={e.id} e={e} href={`/events/${e.uniqueEventLink}`} />)}
    </div>
  </div>);
}
