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
  const [memberCount, setMemberCount] = useState(1);
  const [guestCount, setGuestCount] = useState(0);
  const [busy, setBusy] = useState(false);
  const memberCountRef = useRef(1);
  const guestCountRef = useRef(0);

  useEffect(() => {
    memberCountRef.current = memberCount;
  }, [memberCount]);

  useEffect(() => {
    guestCountRef.current = guestCount;
  }, [guestCount]);

  useEffect(() => {
    let cancelled = false;
    const load = () =>
      api.publicEvent(link)
        .then((nextEvent) => {
          if (cancelled) return;
          setEv((current: any) => {
            const nextAvailable = Number(nextEvent?.availableCount ?? 0);
            if (current && current.availableCount !== undefined && nextAvailable < memberCountRef.current + guestCountRef.current) {
              toast.error("Seats are running low. Your quantity has been capped.");
              const newMember = Math.min(memberCountRef.current, nextAvailable);
              const newGuest = Math.max(0, Math.min(guestCountRef.current, nextAvailable - newMember));
              setMemberCount(Math.max(1, newMember));
              setGuestCount(newGuest);
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
      const quantity = memberCount + guestCount;
      const availableCount = Number(ev?.availableCount ?? 0);
      if (ev?.isSoldOut || availableCount <= 0) {
        toast.error("This event is sold out.");
        return;
      }
      if (availableCount < quantity) {
        toast.error("Not enough seats remain for the selected quantity.");
        const newMember = Math.min(memberCount, availableCount);
        const newGuest = Math.max(0, Math.min(guestCount, availableCount - newMember));
        setMemberCount(Math.max(1, newMember));
        setGuestCount(newGuest);
        return;
      }
      const sess = JSON.parse(raw);
      const res: any = await api.initiateBooking({
        sessionToken: sess.sessionToken,
        eventId: ev.id,
        memberQuantity: memberCount,
        guestQuantity: guestCount,
        paymentPreference: "ONLINE",
      });
      localStorage.setItem(
        "bookingCtx",
        JSON.stringify({
          ...res,
          sessionToken: sess.sessionToken,
          eventId: ev.id,
          quantity,
          pay: "ONLINE",
          startedAt: Date.now(),
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

  const maxMemberTickets = ev.maxMemberTickets ?? 4;
  const memberPrice = Number(ev.memberTicketPrice || 0);
  const freeMemberTickets = Number(ev.freeMemberTickets || 0);
  const maxGuestTickets = ev.maxGuestTickets || 0;
  const guestPrice = Number(ev.guestTicketPrice || 0);
  const freeGuestTickets = Number(ev.freeGuestTickets || 0);
  const hasGuests = maxGuestTickets > 0;

  const paidMemberTickets = Math.max(0, memberCount - freeMemberTickets);
  const paidGuestTickets = Math.max(0, guestCount - freeGuestTickets);
  const total = paidMemberTickets * memberPrice + paidGuestTickets * guestPrice;
  const quantity = memberCount + guestCount;
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
      <div className="mx-auto max-w-lg px-4 md:px-6 py-8 md:py-12">
        <Link
          href={`/events/${link}`}
          className="inline-flex items-center gap-1.5 text-sm text-navy/60 hover:text-navy mb-6 transition-colors"
        >
          <ArrowLeft size={16} />
          Back to event
        </Link>

        <div className="card p-6 md:p-8">
          <div className="border-b border-navy/10 pb-5 mb-5">
            <div className="eyebrow">{ev.category}</div>
            <h1 className="font-display text-2xl md:text-3xl text-navy mt-1 leading-tight">
              {ev.title}
            </h1>
            <div className="mt-2 flex items-baseline gap-2">
              <span className="font-display text-2xl text-navy">
                ₹{memberPrice.toLocaleString("en-IN")}
              </span>
              <span className="text-sm text-navy/50">/member ticket</span>
            </div>
            {hasGuests && (
              <div className="mt-1 flex items-baseline gap-2">
                <span className="font-display text-lg text-navy">
                  ₹{guestPrice.toLocaleString("en-IN")}
                </span>
                <span className="text-sm text-navy/50">/guest ticket</span>
              </div>
            )}
            {ev.freeMemberTickets > 0 && (
              <p className="mt-1 text-xs text-green-700">
                {ev.freeMemberTickets} free member ticket(s) per registration
              </p>
            )}
          </div>

          {/* Member tickets */}
          <div>
            <label className="label">Member tickets <span className="text-navy/40">(min 1, max {maxMemberTickets})</span></label>
            <div className="flex items-center gap-3 mt-1">
              <button
                type="button"
                onClick={() => setMemberCount((q) => Math.max(1, q - 1))}
                className="w-9 h-9 rounded-lg border border-navy/20 text-navy text-lg font-semibold flex items-center justify-center hover:bg-navy/5 transition-colors"
              >
                −
              </button>
              <span className="font-display text-2xl text-navy w-8 text-center">{memberCount}</span>
              <button
                type="button"
                onClick={() => setMemberCount((q) => Math.min(maxMemberTickets, q + 1))}
                className="w-9 h-9 rounded-lg border border-navy/20 text-navy text-lg font-semibold flex items-center justify-center hover:bg-navy/5 transition-colors"
              >
                +
              </button>
            </div>
            {freeMemberTickets > 0 && (
              <p className="mt-1 text-xs text-navy/50">
                First {freeMemberTickets} free — {Math.max(0, memberCount - freeMemberTickets)} paid × ₹{memberPrice.toLocaleString("en-IN")}
              </p>
            )}
          </div>

          {/* Guest tickets */}
          {hasGuests && (
            <div className="mt-5">
              <label className="label">Guest tickets <span className="text-navy/40">(max {maxGuestTickets})</span></label>
              <div className="flex items-center gap-3 mt-1">
                <button
                  type="button"
                  onClick={() => setGuestCount((q) => Math.max(0, q - 1))}
                  className="w-9 h-9 rounded-lg border border-navy/20 text-navy text-lg font-semibold flex items-center justify-center hover:bg-navy/5 transition-colors"
                >
                  −
                </button>
                <span className="font-display text-2xl text-navy w-8 text-center">{guestCount}</span>
                <button
                  type="button"
                  onClick={() => setGuestCount((q) => Math.min(maxGuestTickets, q + 1))}
                  className="w-9 h-9 rounded-lg border border-navy/20 text-navy text-lg font-semibold flex items-center justify-center hover:bg-navy/5 transition-colors"
                >
                  +
                </button>
              </div>
              {freeGuestTickets > 0 && (
                <p className="mt-1 text-xs text-navy/50">
                  First {freeGuestTickets} free — {Math.max(0, guestCount - freeGuestTickets)} paid × ₹{guestPrice.toLocaleString("en-IN")}
                </p>
              )}
            </div>
          )}

          <div className="mt-5">
            <label className="label">Payment Method</label>
            <div className="mt-1">
              <div className="rounded-lg border border-navy px-3 py-2.5 text-sm font-medium bg-navy text-white text-center">
                Pay Online
              </div>
            </div>
          </div>

          <div className="mt-5 rounded-xl bg-cream/60 border border-navy/10 px-4 py-3 flex justify-between items-center">
            <span className="text-sm text-navy/60">
              {quantity} ticket{quantity !== 1 ? "s" : ""}
              {paidMemberTickets !== memberCount || paidGuestTickets !== guestCount
                ? ` (${paidMemberTickets} member + ${paidGuestTickets} guest paid)`
                : ""}
            </span>
            <span className="font-display text-xl text-navy">
              {total === 0 ? "Free" : `₹${total.toLocaleString("en-IN")}`}
            </span>
          </div>

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
