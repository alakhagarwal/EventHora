"use client";

import { Suspense, useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import QRCode from "qrcode";
import { getSession } from "@/lib/auth";
import { generateTicketPdf } from "@/lib/ticketPdf";
import { Smartphone } from "lucide-react";

function formatEventDate(d?: string) {
  if (!d) return "";
  try {
    return new Date(d).toLocaleDateString("en-IN", {
      weekday: "long", year: "numeric", month: "long", day: "numeric",
    });
  } catch { return d; }
}

function formatTime(t?: string) {
  if (!t) return "";
  try {
    const [h, m] = t.split(":");
    const hr = parseInt(h, 10);
    const ampm = hr >= 12 ? "PM" : "AM";
    const h12 = hr % 12 || 12;
    return `${h12}:${m} ${ampm}`;
  } catch { return t; }
}

function formatAmount(amount: string) {
  const num = Number(amount || 0);
  return `₹${num.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

export default function AdminBookingSuccessPage() {
  return (
    <Suspense fallback={<div className="mx-auto max-w-3xl px-6 py-16 text-navy/60">Loading...</div>}>
      <AdminBookingSuccess />
    </Suspense>
  );
}

function AdminBookingSuccess() {
  const router = useRouter();
  const params = useSearchParams();

  const booking = {
    ticketReference: params.get("ticketReference") || "",
    quantity: params.get("quantity") || "1",
    totalAmount: params.get("totalAmount") || "0",
    paymentStatus: params.get("paymentStatus") || "",
    memberId: params.get("memberId") || "",
    eventTitle: params.get("eventTitle") || "",
    eventDate: params.get("eventDate") || "",
    eventStartTime: params.get("eventStartTime") || "",
    eventVenue: params.get("eventVenue") || "",
    bookedBy: params.get("bookedBy") || "",
    bookedAt: params.get("bookedAt") || "",
    mobileNumber: params.get("mobileNumber") || "",
  };

  const isFree = booking.paymentStatus === "FREE" || booking.paymentStatus === "COMPLIMENTARY";

  const whatsappMessage = useMemo(() => {
    const msg = `🎟️ *${booking.eventTitle}* Ticket%0A%0A📅 ${formatEventDate(booking.eventDate)} · ${formatTime(booking.eventStartTime)}%0A📍 ${booking.eventVenue}%0A%0A🎫 *Ref:* ${booking.ticketReference}%0A👤 Member: ${booking.memberId}%0A🎟️ Qty: ${booking.quantity}%0A💰 Amount: ${formatAmount(booking.totalAmount)}%0A%0APresent this ticket reference at the entry gate.`;
    return msg;
  }, [booking]);

  const ticketRef = booking.ticketReference;

  const qrPayload = useMemo(() => {
    if (!ticketRef) return null;
    return ticketRef;
  }, [ticketRef]);

  const [qrImageDataUrl, setQrImageDataUrl] = useState<string | null>(null);

  useEffect(() => {
    const session = getSession();
    if (!session || (session.role !== "ADMIN" && session.role !== "STAFF")) {
      router.replace("/login");
      return;
    }
    if (!ticketRef) {
      router.replace("/admin/bookings/new");
    }
  }, [router, ticketRef]);

  useEffect(() => {
    let cancelled = false;
    if (!qrPayload) {
      setQrImageDataUrl(null);
      return;
    }
    QRCode.toDataURL(qrPayload, { width: 300, margin: 2 })
      .then((dataUrl: string) => { if (!cancelled) setQrImageDataUrl(dataUrl); })
      .catch(() => { if (!cancelled) setQrImageDataUrl(null); });
    return () => { cancelled = true; };
  }, [qrPayload]);

  const handleDownloadPdf = async () => {
    if (!ticketRef || !qrImageDataUrl) return;
    await generateTicketPdf({
      ticketReference: ticketRef,
      eventTitle: booking.eventTitle,
      eventDate: booking.eventDate,
      startTime: booking.eventStartTime,
      venue: booking.eventVenue,
      quantity: Number(booking.quantity),
      totalAmount: Number(booking.totalAmount),
      paymentStatus: booking.paymentStatus,
      qrImageDataUrl,
    });
  };

  const handlePrint = () => {
    if (!qrImageDataUrl) return;
    const win = window.open("", "_blank");
    if (!win) return;
    win.document.write(`
      <html><head><title>Ticket - ${ticketRef}</title>
      <style>body{font-family:sans-serif;text-align:center;padding:40px}
      img{max-width:300px} h2{margin-top:20px}
      p{color:#555;margin:4px 0}</style></head><body>
      <h2>${booking.eventTitle}</h2>
      <p>${formatEventDate(booking.eventDate)} · ${formatTime(booking.eventStartTime)}</p>
      <p>${booking.eventVenue}</p>
      <img src="${qrImageDataUrl}" />
      <p style="font-size:18px;font-weight:bold;margin-top:12px">${ticketRef}</p>
      <p>Member: ${booking.memberId}</p>
      <p>Tickets: ${booking.quantity} | ${formatAmount(booking.totalAmount)}</p>
      </body></html>
    `);
    win.document.close();
    win.print();
  };

  if (!ticketRef) {
    return <div className="mx-auto max-w-3xl px-6 py-16 text-navy/60">Redirecting...</div>;
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-8 md:px-6 md:py-12">
      <div className="mb-6">
        <div className="eyebrow text-green-700">Registration Successful</div>
        <h1 className="h1 mt-2">Booking Confirmed</h1>
      </div>

      <div className="card space-y-6 p-5 md:p-6">
        <div className="flex flex-col items-center gap-4 sm:flex-row">
          <div className="flex-shrink-0">
            {qrImageDataUrl ? (
              <img src={qrImageDataUrl} alt="QR Code" className="h-40 w-40 rounded-lg border" />
            ) : (
              <div className="h-40 w-40 animate-pulse rounded-lg bg-navy/10" />
            )}
          </div>
          <div className="min-w-0 flex-1">
            <h2 className="text-xl font-semibold text-navy">{booking.eventTitle}</h2>
            <p className="mt-1 text-sm text-navy/70">{formatEventDate(booking.eventDate)} · {formatTime(booking.eventStartTime)}</p>
            <p className="text-sm text-navy/70">{booking.eventVenue}</p>
            <p className="mt-3 font-mono text-lg font-bold text-gold">{ticketRef}</p>
          </div>
        </div>

        <div className="grid gap-4 border-t border-navy/10 pt-4 sm:grid-cols-2">
          <div><span className="label">Member ID</span><p className="text-navy">{booking.memberId}</p></div>
          <div><span className="label">Tickets</span><p className="text-navy">{booking.quantity}</p></div>
          <div><span className="label">Total</span><p className="text-navy">{formatAmount(booking.totalAmount)}</p></div>
          <div><span className="label">Status</span><p className="text-navy">{booking.paymentStatus.replace("_", " ")}</p></div>
          <div><span className="label">Booked By</span><p className="text-navy">{booking.bookedBy}</p></div>
          <div><span className="label">Booked At</span><p className="text-navy">{booking.bookedAt ? new Date(booking.bookedAt).toLocaleString() : "-"}</p></div>
          {booking.mobileNumber && (
            <div><span className="label">Mobile (Ticket Delivery)</span><p className="text-navy">{booking.mobileNumber}</p></div>
          )}
        </div>

        {booking.mobileNumber && (
          <div className="border-t border-navy/10 pt-4">
            <a
              href={`https://wa.me/${booking.mobileNumber.replace(/^\+/, "").replace(/[^0-9]/g, "")}?text=${whatsappMessage}`}
              target="_blank"
              rel="noopener noreferrer"
              className="btn-outline w-full flex items-center justify-center gap-2 py-3 border-green-600 text-green-700 hover:bg-green-50"
            >
              <Smartphone className="h-4 w-4" /> Send Ticket via WhatsApp
            </a>
          </div>
        )}

        <div className="flex flex-wrap gap-3 border-t border-navy/10 pt-4">
          <button onClick={handleDownloadPdf} className="btn-dark">
            Download Ticket PDF
          </button>
          <button onClick={handlePrint} className="btn-outline">
            Print Ticket
          </button>
          <Link href="/admin/bookings/new" className="btn-outline">
            Register Another Member
          </Link>
          <Link href="/admin/dashboard" className="btn-outline">
            Back to Dashboard
          </Link>
        </div>
      </div>
    </div>
  );
}
