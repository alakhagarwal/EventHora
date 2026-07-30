"use client";
import { useEffect, useRef, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { api } from "@/lib/api";
import { ArrowLeft } from "lucide-react";
import { toast } from "@/lib/toast";

export default function BookEventPage() {
  const { link } = useParams<{ link: string }>();
  const router = useRouter();

  const [ev, setEv] = useState<any>(null);
  const [err, setErr] = useState<string | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [pay, setPay] = useState<"ONLINE" | "AT_GATE">("ONLINE");
  const [busy, setBusy] = useState(false);
  const quantityRef = useRef(1);

  useEffect(() => {
    quantityRef.current = quantity;
  }, [quantity]);

  useEffect(() => {
    let cancelled = false;
    const load = () =>
      api.publicEvent(link)
        .then((nextEvent) => {
          if (cancelled) return;
          setEv((current: any) => {
            const nextAvailable = Number(nextEvent?.availableCount ?? 0);
            if (current && current.availableCount !== undefined && nextAvailable < quantityRef.current) {
              toast.error("Seats are running low. Your quantity has been capped.");
              setQuantity(Math.max(1, nextAvailable));
            }
            return nextEvent;
          });
        })
        .catch((error) => {
          if (!cancelled) toast.error(error.message || "Failed to load event.");
        });

    load();
    const interval = window.setInterval(load, 10000);
    return () => {
      cancelled = true;
      window.clearInterval(interval);
    };
  }, [link]);

  const proceed = async () => {
    setBusy(true);
    setErr(null);
    try {
      const raw = localStorage.getItem("memberSession");
      if (!raw) {
        router.push("/login");
        return;
      }
      const availableCount = Number(ev?.availableCount ?? 0);
      if (ev?.isSoldOut || availableCount <= 0) {
        toast.error("This event is sold out.");
        return;
      }
      if (availableCount < quantity) {
        toast.error("Not enough seats remain for the selected quantity.");
        setQuantity(Math.max(1, availableCount));
        return;
      }
      const sess = JSON.parse(raw);
      const res: any = await api.initiateBooking({
        sessionToken: sess.sessionToken,
        eventId: ev.id,
        quantity,
        paymentPreference: pay,
      });
      localStorage.setItem(
        "bookingCtx",
        JSON.stringify({
          ...res,
          sessionToken: sess.sessionToken,
          eventId: ev.id,
          quantity,
          pay,
          startedAt: Date.now(),
          // Add these from the already-fetched `ev` object:
          eventTitle: ev.title,
          venue: ev.venue,
          additionalVenueInfo: ev.additionalVenueInfo || null,
          eventDate: ev.eventDate,
          startTime: ev.startTime,
          endTime: ev.endTime,
          contactPersonName: ev.contactPersonName || null,
          contactPersonPhone: ev.contactPersonPhone || null,
        })
      );
      router.push("/member/otp");
    } catch (e: any) {
      const msg = e.message || "Could not initiate booking";
      setErr(msg);
      toast.error(msg);
    } finally {
      setBusy(false);
    }
  };

  if (err && !ev)
    return (
      <div className="mx-auto max-w-lg px-4 py-12 text-navy/60">Loading…</div>
    );
  if (!ev)
    return (
      <div className="mx-auto max-w-lg px-4 py-12 text-navy/60">Loading…</div>
    );

  const max = ev.maxTicketsPerMember || 4;
  const ticketPrice = Number(ev.ticketPrice || 0);
  const total = ticketPrice * quantity;
  const availableCount = Number(ev.availableCount ?? 0);
  const isSoldOut = ev.isSoldOut || availableCount <= 0;

  if (isSoldOut) {
    return (
      <div className="mx-auto max-w-lg px-4 py-12">
        <div className="card p-8 text-center">
          <div className="eyebrow">Booking Unavailable</div>
          <h1 className="font-display text-3xl text-navy mt-2">Sold out</h1>
          <p className="mt-3 text-navy/70 text-sm">This event no longer has seats available.</p>
          <Link href={`/events/${link}`} className="btn-primary mt-6 inline-flex">Back to event</Link>
        </div>
      </div>
    );
  }

  return (
    <>
      {/* ── Page content ── */}
      <div className="mx-auto max-w-lg px-4 md:px-6 py-8 md:py-12">
        {/* Back */}
        <Link
          href={`/events/${link}`}
          className="inline-flex items-center gap-1.5 text-sm text-navy/60 hover:text-navy mb-6 transition-colors"
        >
          <ArrowLeft size={16} />
          Back to event
        </Link>

        <div className="card p-6 md:p-8">
          {/* Event summary */}
          <div className="border-b border-navy/10 pb-5 mb-5">
            <div className="eyebrow">{ev.category}</div>
            <h1 className="font-display text-2xl md:text-3xl text-navy mt-1 leading-tight">
              {ev.title}
            </h1>
            <div className="mt-2 flex items-baseline gap-2">
              <span className="font-display text-2xl text-navy">
                ₹{ticketPrice.toLocaleString("en-IN")}
              </span>
              <span className="text-sm text-navy/50">/ticket</span>
            </div>
            {ev.freeTicketsPerRegistration > 0 && (
              <p className="mt-1 text-xs text-navy/50">
                {ev.freeTicketsPerRegistration} free ticket(s) per registration
              </p>
            )}
            <p className={`mt-3 text-sm font-semibold ${availableCount <= 5 ? "text-amber-700" : "text-green-700"}`}>
              {availableCount} seats remaining
            </p>
          </div>

          {/* Quantity */}
          <div>
            <label className="label">Quantity <span className="text-navy/40">(max {max})</span></label>
            <div className="flex items-center gap-3 mt-1">
              <button
                type="button"
                onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                className="w-9 h-9 rounded-lg border border-navy/20 text-navy text-lg font-semibold flex items-center justify-center hover:bg-navy/5 transition-colors"
              >
                −
              </button>
              <span className="font-display text-2xl text-navy w-8 text-center">{quantity}</span>
              <button
                type="button"
                onClick={() => setQuantity((q) => Math.min(max, q + 1))}
                className="w-9 h-9 rounded-lg border border-navy/20 text-navy text-lg font-semibold flex items-center justify-center hover:bg-navy/5 transition-colors"
              >
                +
              </button>
            </div>
          </div>

          {/* Payment preference */}
          <div className="mt-5">
            <label className="label">Payment Method</label>
            <div className="grid grid-cols-2 gap-2 mt-1">
              {(["ONLINE", "AT_GATE"] as const).map((p) => (
                <button
                  key={p}
                  type="button"
                  onClick={() => setPay(p)}
                  className={`rounded-lg border px-3 py-2.5 text-sm font-medium transition-colors ${
                    pay === p
                      ? "bg-navy text-white border-navy"
                      : "border-navy/20 text-navy hover:bg-navy/5"
                  }`}
                >
                  {p === "ONLINE" ? "Pay Online" : "Pay at Gate"}
                </button>
              ))}
            </div>
          </div>

          {/* Order summary */}
          <div className="mt-5 rounded-xl bg-cream/60 border border-navy/10 px-4 py-3 flex justify-between items-center">
            <span className="text-sm text-navy/60">Total ({quantity} ticket{quantity > 1 ? "s" : ""})</span>
            <span className="font-display text-xl text-navy">
              {ticketPrice === 0 ? "Free" : `₹${total.toLocaleString("en-IN")}`}
            </span>
          </div>

          {/* CTA */}
          <button
            onClick={proceed}
            disabled={busy || !ev.registrationOpen || availableCount < quantity}
            className="btn-primary w-full mt-6 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {busy ? "Please wait…" : "Proceed to Verification"}
          </button>
          <p className="mt-2 text-[11px] text-navy/50 text-center">
            Members only · OTP will be sent to your registered contact
          </p>
        </div>
      </div>

    </>
  );
}
