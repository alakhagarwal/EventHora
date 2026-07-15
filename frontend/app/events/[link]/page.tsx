"use client";
import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { api, displayStatus } from "@/lib/api";
import { Calendar, Clock, MapPin } from "lucide-react";

export default function EventDetails() {
  const { link } = useParams<{ link: string }>();
  const router = useRouter();
  const [ev, setEv] = useState<any>(null);
  const [err, setErr] = useState<string | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [pay, setPay] = useState<"ONLINE" | "AT_GATE">("ONLINE");
  const [busy, setBusy] = useState(false);

  useEffect(() => { api.publicEvent(link).then(setEv).catch((e) => setErr(e.message)); }, [link]);

  const proceed = async () => {
    setBusy(true); setErr(null);
    try {
      const raw = localStorage.getItem("memberSession");
      if (!raw) { router.push("/login"); return; }
      const sess = JSON.parse(raw);
      const res: any = await api.initiateBooking({ sessionToken: sess.sessionToken, eventId: ev.id, quantity, paymentPreference: pay });
      localStorage.setItem("bookingCtx", JSON.stringify({ ...res, eventId: ev.id, quantity, pay, startedAt: Date.now() }));
      router.push("/member/otp");
    } catch (e: any) { setErr(e.message || "Could not initiate booking"); } finally { setBusy(false); }
  };

  if (err && !ev) return <div className="mx-auto max-w-3xl px-4 md:px-6 py-12 md:py-16 text-red-600">{err}</div>;
  if (!ev) return <div className="mx-auto max-w-3xl px-4 md:px-6 py-12 md:py-16 text-navy/60">Loading…</div>;

  const max = ev.maxTicketsPerMember || 4;

  return (
    <div className="mx-auto max-w-6xl px-4 md:px-6 py-8 md:py-12">
      <div className="card overflow-hidden">
        <div className="relative aspect-[16/9] md:aspect-[21/9] bg-navy/20">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={ev.bannerUrl || "https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?auto=format&fit=crop&w=2000&q=70"} alt={ev.title} className="h-full w-full object-cover" />
        </div>
        <div className="grid md:grid-cols-3 gap-6 md:gap-8 p-4 md:p-8">
          <div className="md:col-span-2">
            <div className="eyebrow">{ev.category}</div>
            <h1 className="h1 mt-2">{ev.title}</h1>
            <div className="mt-3 md:mt-4 flex flex-wrap gap-3 md:gap-4 text-sm text-navy/70">
              <span className="inline-flex items-center gap-1.5"><Calendar className="h-4 w-4" /> {ev.eventDate}</span>
              <span className="inline-flex items-center gap-1.5"><Clock className="h-4 w-4" /> {ev.startTime?.slice(0,5)} – {ev.endTime?.slice(0,5)}</span>
              <span className="inline-flex items-center gap-1.5"><MapPin className="h-4 w-4" /> {ev.venue}</span>
            </div>
            <p className="mt-4 md:mt-6 whitespace-pre-line text-navy/80 leading-relaxed text-sm md:text-base">{ev.description}</p>
            {ev.importantNotes?.length > 0 && (
              <div className="mt-6 md:mt-8">
                <h3 className="font-display text-lg md:text-xl text-navy mb-3">Important Notes</h3>
                <ul className="list-disc pl-5 space-y-1 text-navy/80 text-sm md:text-base">
                  {ev.importantNotes.map((n: string, i: number) => <li key={i}>{n}</li>)}
                </ul>
              </div>
            )}
            {ev.contactPersonName && (
              <div className="mt-4 md:mt-6 text-sm text-navy/70">
                Contact: <span className="font-medium">{ev.contactPersonName}</span>
                {ev.contactPersonPhone && <> · {ev.contactPersonPhone}</>}
              </div>
            )}
          </div>

          <aside className="card p-4 md:p-6 h-fit bg-cream border-gold/30">
            <div className="text-xs uppercase tracking-widest text-gold-600">Price</div>
            <div className="font-display text-2xl md:text-3xl text-navy">₹{Number(ev.ticketPrice || 0).toLocaleString()}<span className="text-sm text-navy/60"> /ticket</span></div>
            <div className="mt-3 md:mt-4 text-xs text-navy/60">Free tickets per registration: {ev.freeTicketsPerRegistration ?? 0}</div>

            <div className="mt-4 md:mt-6">
              <label className="label">Quantity (max {max})</label>
              <input type="number" min={1} max={max} className="input" value={quantity} onChange={(e) => setQuantity(Math.min(max, Math.max(1, Number(e.target.value) || 1)))} />
            </div>
            <div className="mt-3 md:mt-4">
              <label className="label">Payment</label>
              <div className="grid grid-cols-2 gap-2">
                {(["ONLINE", "AT_GATE"] as const).map((p) => (
                  <button key={p} type="button" onClick={() => setPay(p)}
                    className={`rounded-md border px-3 py-2 text-sm font-medium ${pay === p ? "bg-navy text-white border-navy" : "border-navy/20 text-navy hover:bg-navy/5"}`}>
                    {p === "ONLINE" ? "Online" : "At Gate"}
                  </button>
                ))}
              </div>
            </div>
            {err && <div className="mt-3 text-sm text-red-600">{err}</div>}
            <button disabled={!ev.registrationOpen || busy || displayStatus(ev) === "COMPLETED"} onClick={proceed}
              className="btn-primary w-full mt-4 md:mt-6 disabled:opacity-50 disabled:cursor-not-allowed">
              {displayStatus(ev) === "COMPLETED" ? "Event Completed" : !ev.registrationOpen ? (ev.isSoldOut ? "Sold Out" : "Registration Closed") : busy ? "Please wait…" : "Book Now"}
            </button>
            <p className="mt-3 text-[11px] text-navy/50 text-center">Members must be verified. You will receive an OTP on your registered contact.</p>
          </aside>
        </div>
      </div>
    </div>
  );
}
