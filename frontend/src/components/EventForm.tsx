"use client";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { eventsApi } from "@/src/lib/api";

const CATEGORIES = ["MUSIC","DANCE","CULTURAL","EDUCATIONAL","SOCIAL","SPORTS","OTHER"];

export interface EventFormProps { eventId?: string; }

export default function EventForm({ eventId }: EventFormProps) {
  const router = useRouter();
  const [loading, setLoading] = useState(!!eventId);
  const [err, setErr] = useState<string | null>(null);
  const [msg, setMsg] = useState<string | null>(null);
  const [status, setStatus] = useState<string>("DRAFT");
  const [banner, setBanner] = useState<File | null>(null);
  const [form, setForm] = useState<any>({
    title: "", description: "", category: "MUSIC",
    eventDate: "", startTime: "", endTime: "", registrationDeadline: "",
    venue: "", additionalVenueInfo: "",
    totalCapacity: 100, maxTicketsPerMember: 4, freeTicketsPerRegistration: 0,
    ticketPrice: 0, platformFeePerTicket: 0, minimumAge: "",
    importantNotes: [""], contactPersonName: "", contactPersonPhone: "",
  });

  useEffect(() => {
    if (!eventId) return;
    (async () => {
      try {
        const data = await eventsApi.adminDetail(eventId);
        setForm({
          title: data.title || "", description: data.description || "", category: data.category || "MUSIC",
          eventDate: data.eventDate || "", startTime: data.startTime || "", endTime: data.endTime || "",
          registrationDeadline: data.registrationDeadline || "", venue: data.venue || "",
          additionalVenueInfo: data.additionalVenueInfo || "", totalCapacity: data.totalCapacity ?? 0,
          maxTicketsPerMember: data.maxTicketsPerMember ?? 0, freeTicketsPerRegistration: data.freeTicketsPerRegistration ?? 0,
          ticketPrice: data.ticketPrice ?? 0, platformFeePerTicket: data.platformFeePerTicket ?? 0,
          minimumAge: data.minimumAge ?? "", importantNotes: data.importantNotes?.length ? data.importantNotes : [""],
          contactPersonName: data.contactPersonName || "", contactPersonPhone: data.contactPersonPhone || "",
        });
        setStatus(data.status || "DRAFT");
      } catch (e: any) { setErr(e.message); } finally { setLoading(false); }
    })();
  }, [eventId]);

  function set(k: string, v: any) { setForm((f: any) => ({ ...f, [k]: v })); }
  function setNote(i: number, v: string) { setForm((f: any) => { const n=[...f.importantNotes]; n[i]=v; return {...f, importantNotes:n}; }); }

  function buildPayload() {
    const p = { ...form };
    p.importantNotes = (p.importantNotes || []).filter((n: string) => n.trim());
    if (p.minimumAge === "" || p.minimumAge === null) delete p.minimumAge; else p.minimumAge = Number(p.minimumAge);
    ["totalCapacity","maxTicketsPerMember","freeTicketsPerRegistration","ticketPrice","platformFeePerTicket"].forEach(k => p[k] = Number(p[k]));
    return p;
  }

  async function createDraft() {
    setErr(null); setMsg(null);
    try {
      const created = await eventsApi.create(buildPayload());
      if (banner) await eventsApi.uploadBanner(created.id, banner);
      router.push(`/admin/events/${created.id}`);
    } catch (e: any) { setErr(e.message); }
  }
  async function saveDraft() {
    if (!eventId) return createDraft();
    setErr(null); setMsg(null);
    try {
      await eventsApi.update(eventId, buildPayload());
      if (banner) await eventsApi.uploadBanner(eventId, banner);
      setMsg("Saved");
    } catch (e: any) { setErr(e.message); }
  }
  async function publish() {
    if (!eventId) { setErr("Save draft first."); return; }
    setErr(null); setMsg(null);
    try { await eventsApi.update(eventId, buildPayload()); await eventsApi.publish(eventId); setStatus("PUBLISHED"); setMsg("Published"); }
    catch (e: any) { setErr(e.message); }
  }
  async function cancel() {
    if (!eventId) return;
    if (!confirm("Cancel this event?")) return;
    try { await eventsApi.cancel(eventId); setStatus("CANCELLED"); setMsg("Cancelled"); }
    catch (e: any) { setErr(e.message); }
  }

  if (loading) return <p>Loading…</p>;

  return (
    <div className="space-y-6">
      {err && <div className="rounded-md bg-red-50 p-3 text-sm text-red-700">{err}</div>}
      {msg && <div className="rounded-md bg-green-50 p-3 text-sm text-green-700">{msg}</div>}
      {eventId && <p className="text-sm text-slate-500">Status: <span className="font-medium">{status}</span></p>}

      <div className="grid gap-4 md:grid-cols-2">
        <div><label className="label">Title</label><input className="input" value={form.title} onChange={e=>set("title",e.target.value)} /></div>
        <div><label className="label">Category</label>
          <select className="input" value={form.category} onChange={e=>set("category",e.target.value)}>
            {CATEGORIES.map(c=><option key={c} value={c}>{c}</option>)}
          </select>
        </div>
      </div>
      <div><label className="label">Description</label><textarea className="input min-h-[100px]" value={form.description} onChange={e=>set("description",e.target.value)} /></div>

      <div className="grid gap-4 md:grid-cols-3">
        <div><label className="label">Event Date</label><input type="date" className="input" value={form.eventDate} onChange={e=>set("eventDate",e.target.value)} /></div>
        <div><label className="label">Start Time</label><input type="time" step="1" className="input" value={form.startTime} onChange={e=>set("startTime",e.target.value)} /></div>
        <div><label className="label">End Time</label><input type="time" step="1" className="input" value={form.endTime} onChange={e=>set("endTime",e.target.value)} /></div>
      </div>
      <div><label className="label">Registration Deadline (ISO)</label><input className="input" placeholder="2026-07-07T15:00:00" value={form.registrationDeadline} onChange={e=>set("registrationDeadline",e.target.value)} /></div>

      <div className="grid gap-4 md:grid-cols-2">
        <div><label className="label">Venue</label><input className="input" value={form.venue} onChange={e=>set("venue",e.target.value)} /></div>
        <div><label className="label">Additional Venue Info</label><input className="input" value={form.additionalVenueInfo} onChange={e=>set("additionalVenueInfo",e.target.value)} /></div>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <div><label className="label">Total Capacity</label><input type="number" className="input" value={form.totalCapacity} onChange={e=>set("totalCapacity",e.target.value)} /></div>
        <div><label className="label">Max Tickets / Member</label><input type="number" className="input" value={form.maxTicketsPerMember} onChange={e=>set("maxTicketsPerMember",e.target.value)} /></div>
        <div><label className="label">Free Tickets / Registration</label><input type="number" className="input" value={form.freeTicketsPerRegistration} onChange={e=>set("freeTicketsPerRegistration",e.target.value)} /></div>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <div><label className="label">Ticket Price</label><input type="number" step="0.01" className="input" value={form.ticketPrice} onChange={e=>set("ticketPrice",e.target.value)} /></div>
        <div><label className="label">Platform Fee / Ticket</label><input type="number" step="0.01" className="input" value={form.platformFeePerTicket} onChange={e=>set("platformFeePerTicket",e.target.value)} /></div>
        <div><label className="label">Minimum Age (optional)</label><input type="number" className="input" value={form.minimumAge} onChange={e=>set("minimumAge",e.target.value)} /></div>
      </div>

      <div>
        <label className="label">Important Notes</label>
        {form.importantNotes.map((n: string, i: number) => (
          <input key={i} className="input mb-2" value={n} onChange={e=>setNote(i,e.target.value)} />
        ))}
        <button type="button" className="btn-outline" onClick={()=>set("importantNotes",[...form.importantNotes,""])}>+ Add note</button>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <div><label className="label">Contact Person Name</label><input className="input" value={form.contactPersonName} onChange={e=>set("contactPersonName",e.target.value)} /></div>
        <div><label className="label">Contact Person Phone</label><input className="input" value={form.contactPersonPhone} onChange={e=>set("contactPersonPhone",e.target.value)} /></div>
      </div>

      <div><label className="label">Banner Image</label><input type="file" accept="image/*" onChange={e=>setBanner(e.target.files?.[0] || null)} /></div>

      <div className="flex flex-wrap gap-2 border-t pt-4">
        {!eventId && <button className="btn-primary" onClick={createDraft}>Create Draft</button>}
        {eventId && <>
          <button className="btn-outline" onClick={saveDraft}>Save Draft</button>
          <button className="btn-primary" onClick={publish}>Publish</button>
          <button className="btn-danger" onClick={cancel}>Cancel Event</button>
        </>}
      </div>
    </div>
  );
}
