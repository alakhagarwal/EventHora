"use client";
import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { eventsApi, memberApi } from "@/src/lib/api";
import { getSessionToken } from "@/src/lib/auth";

export default function EventDetail() {
  const { link } = useParams<{ link: string }>();
  const router = useRouter();
  const [e, setE] = useState<any>(null);
  const [err, setErr] = useState<string | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [payment, setPayment] = useState<"ONLINE"|"AT_GATE">("ONLINE");
  const [busy, setBusy] = useState(false);

  useEffect(()=>{ eventsApi.publicDetail(link as string).then(setE).catch(x=>setErr(x.message)); },[link]);

  async function book() {
    setErr(null);
    const st = getSessionToken();
    if (!st) { router.push("/login"); return; }
    setBusy(true);
    try {
      const res = await memberApi.initiate(st, e.id, quantity, payment);
      if (typeof window !== "undefined") {
        localStorage.setItem("otpExpiresAt", String(Date.now() + res.expiresInSeconds*1000));
        localStorage.setItem("otpMessage", res.message);
      }
      router.push("/member/otp");
    } catch (x: any) { setErr(x.message); } finally { setBusy(false); }
  }

  if (err) return <p className="text-red-600">{err}</p>;
  if (!e) return <p>Loading…</p>;
  const max = e.maxTicketsPerMember || 1;

  return (
    <div className="grid gap-8 md:grid-cols-3">
      <div className="md:col-span-2 space-y-4">
        {e.bannerUrl && <img src={e.bannerUrl} alt={e.title} className="w-full rounded-xl object-cover" />}
        <h1 className="text-3xl font-bold">{e.title}</h1>
        <p className="text-slate-600">{e.eventDate} · {e.startTime} – {e.endTime} · {e.venue}</p>
        <p className="whitespace-pre-line">{e.description}</p>
        {e.importantNotes?.length > 0 && (
          <div><h3 className="font-semibold">Important notes</h3>
            <ul className="ml-6 list-disc text-sm text-slate-700">{e.importantNotes.map((n:string,i:number)=><li key={i}>{n}</li>)}</ul>
          </div>
        )}
      </div>
      <aside className="card h-fit p-4">
        <h3 className="mb-3 font-semibold">Book tickets</h3>
        {!e.registrationOpen && <p className="mb-2 text-sm text-red-600">Registration is closed.</p>}
        {e.isSoldOut && <p className="mb-2 text-sm text-red-600">Sold out.</p>}
        <div className="space-y-3">
          <div><label className="label">Quantity (1–{max})</label>
            <input type="number" min={1} max={max} className="input" value={quantity} onChange={ev=>setQuantity(Math.min(max,Math.max(1,Number(ev.target.value))))} />
          </div>
          <div><label className="label">Payment</label>
            <select className="input" value={payment} onChange={ev=>setPayment(ev.target.value as any)}>
              <option value="ONLINE">Online</option><option value="AT_GATE">At Gate</option>
            </select>
          </div>
          <p className="text-sm text-slate-600">Ticket price: ₹{e.ticketPrice}</p>
          <button disabled={!e.registrationOpen || e.isSoldOut || busy} onClick={book} className="btn-primary w-full">{busy?"Please wait…":"Proceed to OTP"}</button>
        </div>
      </aside>
    </div>
  );
}
