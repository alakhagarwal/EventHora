"use client";
import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { RegistrationResponse } from "@/lib/api";
import QRCode from "qrcode";
import jsPDF from "jspdf";

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
    totalAmount?: string;
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
    return `EVTHORA:${ticketRef}`;
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

  const downloadTicketPdf = async () => {
    if (!merged?.ticketReference || !qrImageDataUrl) return;

    const doc = new jsPDF({ orientation: "portrait", unit: "pt", format: "a4" });
    const pw = 595.28;
    const ph = 841.89;
    const mx = 36;

    const navyR = 15, navyG = 27, navyB = 61;
    const goldR = 201, goldG = 168, goldB = 76;
    const creamR = 250, creamG = 247, creamB = 240;

    // ── Cream background ──
    doc.setFillColor(creamR, creamG, creamB);
    doc.rect(0, 0, pw, ph, "F");

    // ── Navy header bar ──
    const headerH = 90;
    doc.setFillColor(navyR, navyG, navyB);
    doc.rect(0, 0, pw, headerH, "F");

    // Gold accent line below header
    doc.setFillColor(goldR, goldG, goldB);
    doc.rect(0, headerH, pw, 4, "F");

    // "EVENTHORA" title
    doc.setFont("helvetica", "bold");
    doc.setFontSize(26);
    doc.setTextColor(255, 255, 255);
    doc.text("EVENTHORA", pw / 2, 40, { align: "center" });

    // Subtitle
    doc.setFontSize(10);
    doc.setFont("helvetica", "normal");
    doc.setTextColor(200, 200, 220);
    doc.text("Event Ticket", pw / 2, 60, { align: "center" });

    // ── White content card ──
    const cardX = mx;
    const cardW = pw - mx * 2;
    const cardY = headerH + 24;
    const cardH = 640;
    doc.setFillColor(255, 255, 255);
    doc.setDrawColor(220, 220, 225);
    doc.setLineWidth(0.5);
    doc.roundedRect(cardX, cardY, cardW, cardH, 8, 8, "FD");

    // ── Event info section ──
    let y = cardY + 28;
    const leftCol = cardX + 24;
    const rightCol = cardX + cardW / 2 + 12;

    const labelColor: [number, number, number] = [140, 145, 160];
    const valueColor: [number, number, number] = [navyR, navyG, navyB];

    const drawField = (lx: number, ly: number, label: string, value: string) => {
      doc.setFontSize(8);
      doc.setFont("helvetica", "normal");
      doc.setTextColor(...labelColor);
      doc.text(label.toUpperCase(), lx, ly);
      doc.setFontSize(11);
      doc.setFont("helvetica", "bold");
      doc.setTextColor(...valueColor);
      doc.text(value || "—", lx, ly + 14);
    };

    drawField(leftCol, y, "Event", merged.eventTitle ?? "");
    drawField(rightCol, y, "Date", formatEventDate(merged.eventDate));
    y += 42;
    drawField(leftCol, y, "Time", `${formatTime(merged.startTime)} – ${formatTime(merged.endTime)}`);
    drawField(rightCol, y, "Venue", merged.venue ?? "");
    y += 42;

    if (merged.additionalVenueInfo) {
      doc.setFontSize(8);
      doc.setFont("helvetica", "normal");
      doc.setTextColor(...labelColor);
      doc.text("ADDITIONAL VENUE INFO", leftCol, y);
      doc.setFontSize(10);
      doc.setFont("helvetica", "normal");
      doc.setTextColor(...valueColor);
      doc.text(merged.additionalVenueInfo, leftCol, y + 14, { maxWidth: cardW - 48 });
      y += 36;
    }

    // ── Divider ──
    y += 8;
    doc.setDrawColor(230, 230, 235);
    doc.setLineWidth(0.5);
    doc.line(cardX + 20, y, cardX + cardW - 20, y);
    y += 20;

    // ── QR Code (large, centered) ──
    const qrSize = 240;
    const qrX = (pw - qrSize) / 2;
    doc.setFillColor(255, 255, 255);
    doc.setDrawColor(navyR, navyG, navyB);
    doc.setLineWidth(2);
    doc.roundedRect(qrX - 6, y - 6, qrSize + 12, qrSize + 12, 6, 6, "FD");
    doc.addImage(qrImageDataUrl, "PNG", qrX, y, qrSize, qrSize);

    // Ticket reference below QR
    y += qrSize + 24;
    doc.setFont("helvetica", "bold");
    doc.setFontSize(16);
    doc.setTextColor(navyR, navyG, navyB);
    doc.text(String(merged.ticketReference), pw / 2, y, { align: "center" });

    y += 28;

    // ── Divider ──
    doc.setDrawColor(230, 230, 235);
    doc.setLineWidth(0.5);
    doc.line(cardX + 20, y, cardX + cardW - 20, y);
    y += 24;

    // ── Booking details ──
    const amountText = isFree
      ? "Free"
      : `Rs. ${Number(merged.totalAmount ?? 0).toLocaleString("en-IN")}`;

    const drawDetail = (lx: number, ly: number, label: string, value: string) => {
      doc.setFontSize(8);
      doc.setFont("helvetica", "normal");
      doc.setTextColor(...labelColor);
      doc.text(label.toUpperCase(), lx, ly);
      doc.setFontSize(11);
      doc.setFont("helvetica", "bold");
      doc.setTextColor(...valueColor);
      doc.text(value || "—", lx, ly + 14);
    };

    drawDetail(leftCol, y, "Ticket Ref", String(merged.ticketReference));
    drawDetail(rightCol, y, "Quantity", String(merged.quantity ?? ""));
    y += 42;
    drawDetail(leftCol, y, "Amount", amountText);
    drawDetail(rightCol, y, "Payment Status", merged.paymentStatus ?? "");

    if (merged.contactPersonName || merged.contactPersonPhone) {
      y += 42;
      const contactParts = [merged.contactPersonName, merged.contactPersonPhone]
        .filter(Boolean)
        .join(" · ");
      drawDetail(leftCol, y, "Event Contact", contactParts);
    }

    // ── Navy footer bar ──
    const footerH = 52;
    doc.setFillColor(navyR, navyG, navyB);
    doc.rect(0, ph - footerH, pw, footerH, "F");

    doc.setFontSize(9);
    doc.setFont("helvetica", "normal");
    doc.setTextColor(255, 255, 255);
    doc.text("Present this QR code at the entry gate for check-in.", pw / 2, ph - footerH / 2 + 4, {
      align: "center",
    });

    doc.save(`EventHora-Ticket-${merged.ticketReference}.pdf`);
  };

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

