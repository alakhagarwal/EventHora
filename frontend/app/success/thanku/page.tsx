"use client";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { RegistrationResponse } from "@/lib/api";

export default function ThankYouPage() {
  const router = useRouter();
  const [result, setResult] = useState<RegistrationResponse | null>(null);

  useEffect(() => {
    const raw = sessionStorage.getItem("bookingResult");
    if (raw) {
      setResult(JSON.parse(raw));
    }
  }, []);

  const isPayAtGate = result?.paymentStatus === "PAY_AT_GATE";
  const isFree =
    result?.paymentStatus === "FREE" || result?.paymentStatus === "COMPLIMENTARY";

  return (
    <div className="mx-auto max-w-lg px-6 py-16">
      <div className="card p-8 text-center">
        {/* Icon */}
        <div className="mx-auto w-16 h-16 rounded-full bg-green-100 flex items-center justify-center mb-6">
          <svg
            className="w-8 h-8 text-green-600"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={2.5}
          >
            <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
          </svg>
        </div>

        <div className="eyebrow">Booking Confirmed</div>
        <h1 className="font-display text-3xl text-navy mt-2">Thank You!</h1>
        <p className="mt-2 text-navy/60 text-sm">
          Your registration is complete. We look forward to seeing you!
        </p>

        {result && (
          <div className="mt-6 text-left rounded-xl border border-navy/10 bg-white/60 divide-y divide-navy/10">
            <div className="flex justify-between px-4 py-3 text-sm">
              <span className="text-navy/50">Event</span>
              <span className="font-semibold text-navy text-right">{result.eventTitle}</span>
            </div>
            <div className="flex justify-between px-4 py-3 text-sm">
              <span className="text-navy/50">Ticket Ref</span>
              <span className="font-mono font-semibold text-navy">{result.ticketReference}</span>
            </div>
            <div className="flex justify-between px-4 py-3 text-sm">
              <span className="text-navy/50">Quantity</span>
              <span className="font-semibold text-navy">{result.quantity}</span>
            </div>
            <div className="flex justify-between px-4 py-3 text-sm">
              <span className="text-navy/50">Amount</span>
              <span className="font-semibold text-navy">
                {isFree
                  ? "Free"
                  : `₹${Number(result.totalAmount).toLocaleString("en-IN")}`}
              </span>
            </div>
            {isPayAtGate && (
              <div className="px-4 py-3 text-sm text-amber-700 bg-amber-50 rounded-b-xl">
                💳 Please pay at the venue on the day of the event.
              </div>
            )}
          </div>
        )}

        <div className="mt-8 flex flex-col gap-3">
          <Link href="/events" className="btn-primary w-full text-center">
            ← Back to Events
          </Link>
        </div>
      </div>
    </div>
  );
}
