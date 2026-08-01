"use client";
import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { RegistrationResponse } from "@/lib/api";
import QRCode from "qrcode";
import { Share2 } from "lucide-react";
import { generateTicketPdf } from "@/lib/ticketPdf";
import { toast } from "@/lib/toast";

export default function ThankYouPage() {
  const router = useRouter();

  type BookingCtx = {
    eventTitle?: string;
    venue?: string;
    additionalVenueInfo?: string | null;
    eventDate?: string;
    startTime?: string;
    endTime?: string;
    contactPersonName?: string | null;
    contactPersonPhone?: string | null;
    ticketReference?: string;
    quantity?: number;
    totalAmount?: number;
    paymentStatus?: string;
  };

  const [merged, setMerged] = useState<(RegistrationResponse & BookingCtx) | null>(null);
  const [qrImageDataUrl, setQrImageDataUrl] = useState<string | null>(null);

  useEffect(() => {
    const raw = sessionStorage.getItem("bookingResult");
    if (!raw) return;
    const bookingRes: RegistrationResponse = JSON.parse(raw);
    setMerged(bookingRes as RegistrationResponse & BookingCtx);
  }, []);

  const isPayAtGate = merged?.paymentStatus === "PAY_AT_GATE";
  const isFree =
    merged?.paymentStatus === "FREE" || merged?.paymentStatus === "COMPLIMENTARY";

  const ticketRef = merged?.ticketReference ?? "";

  const qrPayload = useMemo(() => {
    if (!ticketRef) return null;
    return ticketRef;
  }, [ticketRef]);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      if (!qrPayload) {
        setQrImageDataUrl(null);
        return;
      }
      try {
        const dataUrl = await QRCode.toDataURL(qrPayload);
        if (!cancelled) setQrImageDataUrl(dataUrl);
      } catch {
        if (!cancelled) setQrImageDataUrl(null);
      }
    };
    run();
    return () => {
      cancelled = true;
    };
  }, [qrPayload]);

  const formatEventDate = (d?: string) => {
    if (!d) return "";
    try {
      return new Date(d).toLocaleDateString("en-IN", {
        weekday: "long",
        year: "numeric",
        month: "long",
        day: "numeric",
      });
    } catch {
      return d;
    }
  };

  const formatTime = (t?: string) => {
    if (!t) return "";
    try {
      const [h, m] = t.split(":");
      const hr = parseInt(h, 10);
      const ampm = hr >= 12 ? "PM" : "AM";
      const h12 = hr % 12 || 12;
      return `${h12}:${m} ${ampm}`;
    } catch {
      return t;
    }
  };

  const buildShareMessage = () => {
    if (!merged) return "";
    return `🎟️ I'm attending ${merged.eventTitle} on ${formatEventDate(merged.eventDate)}! My ticket ref: ${merged.ticketReference}. See you there!`;
  };

  const handleShareBooking = async () => {
    if (!merged?.ticketReference) return;
    const message = buildShareMessage();
    const url = window.location.href;

    try {
      if (navigator.share) {
        await navigator.share({ title: merged.eventTitle || "Booking", text: message, url });
      } else {
        await navigator.clipboard.writeText(`${message}\n${url}`);
        toast.info("Link copied!");
      }
    } catch {
      try {
        await navigator.clipboard.writeText(`${message}\n${url}`);
        toast.info("Link copied!");
      } catch {
        toast.error("Unable to share booking right now.");
      }
    }
  };

  const downloadTicketPdf = async () => {
    if (!merged?.ticketReference || !qrImageDataUrl) return;
    await generateTicketPdf({
      ticketReference: merged.ticketReference,
      eventTitle: merged.eventTitle ?? "",
      eventDate: merged.eventDate ?? "",
      startTime: merged.startTime,
      endTime: merged.endTime,
      venue: merged.venue ?? "",
      additionalVenueInfo: merged.additionalVenueInfo,
      contactPersonName: merged.contactPersonName,
      contactPersonPhone: merged.contactPersonPhone,
      quantity: merged.quantity ?? 1,
      totalAmount: merged.totalAmount ?? 0,
      paymentStatus: merged.paymentStatus ?? "",
      qrImageDataUrl,
    });
  };

  return (
    <div className="mx-auto max-w-lg px-6 py-8 flex flex-col justify-center min-h-[calc(100vh-8.5rem)]">
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

        {merged && (
          <div className="mt-6 text-left rounded-xl border border-navy/10 bg-white/60 divide-y divide-navy/10">
            <div className="flex justify-between px-4 py-3 text-sm">
              <span className="text-navy/50">Event</span>
              <span className="font-semibold text-navy text-right">{merged.eventTitle}</span>
            </div>
            <div className="flex justify-between px-4 py-3 text-sm">
              <span className="text-navy/50">Ticket Ref</span>
              <span className="font-mono font-semibold text-navy">{merged.ticketReference}</span>
            </div>
            <div className="flex justify-between px-4 py-3 text-sm">
              <span className="text-navy/50">Quantity</span>
              <span className="font-semibold text-navy">{merged.quantity}</span>
            </div>
            <div className="flex justify-between px-4 py-3 text-sm">
              <span className="text-navy/50">Amount</span>
              <span className="font-semibold text-navy">
                {isFree
                  ? "Free"
                  : `₹${Number(merged.totalAmount ?? 0).toLocaleString("en-IN")}`}
              </span>
            </div>

            {isPayAtGate && (
              <div className="px-4 py-3 text-sm text-amber-700 bg-amber-50 rounded-b-xl">
                💳 Please pay at the venue on the day of the event.
              </div>
            )}

            {/* QR code section */}
            <div className="px-4 py-5">
              <div className="flex flex-col items-center gap-3">
                {qrImageDataUrl ? (
                  <div className="w-40 h-40 rounded-xl border border-navy/10 bg-white flex items-center justify-center">
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img src={qrImageDataUrl} alt="Ticket QR" className="w-32 h-32" />
                  </div>
                ) : (
                  <div className="w-40 h-40 rounded-xl border border-navy/10 bg-white flex items-center justify-center text-navy/50">
                    Generating QR…
                  </div>
                )}

                <p className="text-[12px] text-navy/60 text-center">
                  Present this QR code at the entry gate for check-in.
                </p>

                <button
                  type="button"
                  onClick={handleShareBooking}
                  disabled={!merged?.ticketReference}
                  className="btn-outline w-full disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <Share2 className="h-4 w-4" /> Share Booking
                </button>

                <button
                  type="button"
                  onClick={downloadTicketPdf}
                  disabled={!qrImageDataUrl || !merged?.ticketReference}
                  className="btn-primary w-full disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Download Ticket
                </button>
              </div>
            </div>
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

