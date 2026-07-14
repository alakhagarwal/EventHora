"use client";
import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { eventsApi } from "@/src/lib/api";
import EventCard from "@/src/components/EventCard";

const TABS = ["ALL","DRAFT","PUBLISHED","CANCELLED","COMPLETED"];
export default function MyEvents() {
  const [ev,setEv]=useState<any[]>([]); const [err,setErr]=useState<string|null>(null); const [tab,setTab]=useState("ALL");
  useEffect(()=>{eventsApi.adminList().then(setEv).catch(e=>setErr(e.message));},[]);
  const filtered = useMemo(()=> tab==="ALL"?ev:ev.filter(e=>e.status===tab),[ev,tab]);
  return (<div>
    <div className="mb-6 flex items-center justify-between">
      <h1 className="text-2xl font-semibold">My Events</h1>
      <Link href="/admin/events/new" className="btn-primary">+ New Event</Link>
    </div>
    <div className="mb-4 flex flex-wrap gap-2">
      {TABS.map(t=>(<button key={t} onClick={()=>setTab(t)} className={`rounded-md px-3 py-1.5 text-sm ${tab===t?"bg-indigo-600 text-white":"bg-slate-100 hover:bg-slate-200"}`}>{t}</button>))}
    </div>
    {err && <p className="text-red-600">{err}</p>}
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {filtered.map(e => <EventCard key={e.id} e={e} href={`/admin/events/${e.id}`} />)}
    </div>
  </div>);
}
