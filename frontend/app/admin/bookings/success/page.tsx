"use client";

import { Suspense, useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import QRCode from "qrcode";
import jsPDF from "jspdf";
import { getSession } from "@/lib/auth";

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

function AdminBookingSuccessContent() {
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
  };

  const isFree = booking.paymentStatus === "FREE" || booking.paymentStatus === "COMPLIMENTARY";

  const ticketRef = booking.ticketReference;

  const qrPayload = useMemo(() => {
    if (!ticketRef) return null;
    return `EVTHORA:${ticketRef}`;
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

    const doc = new jsPDF({ orientation: "portrait", unit: "pt", format: "a4" });
    const pw = 595.28, ph = 841.89, mx = 36;

    const navyR = 15, navyG = 27, navyB = 61;
    const goldR = 201, goldG = 168, goldB = 76;
    const creamR = 250, creamG = 247, creamB = 240;

    doc.setFillColor(creamR, creamG, creamB);
    doc.rect(0, 0, pw, ph, "F");

    const headerH = 90;
    doc.setFillColor(navyR, navyG, navyB);
    doc.rect(0, 0, pw, headerH, "F");
    doc.setFillColor(goldR, goldG, goldB);
    doc.rect(0, headerH, pw, 4, "F");

    doc.setFont("helvetica", "bold");
    doc.setFontSize(26);
    doc.setTextColor(255, 255, 255);
    doc.text("EVENTHORA", pw / 2, 40, { align: "center" });
    doc.setFontSize(10);
    doc.setFont("helvetica", "normal");
    doc.setTextColor(200, 200, 220);
    doc.text("Event Ticket", pw / 2, 60, { align: "center" });

    const cardX = mx, cardW = pw - mx * 2, cardY = headerH + 24, cardH = 640;
    doc.setFillColor(255, 255, 255);
    doc.setDrawColor(220, 220, 225);
    doc.setLineWidth(0.5);
    doc.roundedRect(cardX, cardY, cardW, cardH, 8, 8, "FD");

    let y = cardY + 28;
    const leftCol = cardX + 24, rightCol = cardX + cardW / 2 + 12;
    const labelColor: [number, number, number] = [140, 145, 160];
    const valueColor: [number, number, number] = [navyR, navyG, navyB];

    const drawField = (lx: number, ly: number, label: string, value: string) => {
      doc.setFontSize(8); doc.setFont("helvetica", "normal");
      doc.setTextColor(...labelColor); doc.text(label.toUpperCase(), lx, ly);
      doc.setFontSize(11); doc.setFont("helvetica", "bold");
      doc.setTextColor(...valueColor); doc.text(value || "—", lx, ly + 14);
    };

    drawField(leftCol, y, "Event", booking.eventTitle);
    drawField(rightCol, y, "Date", formatEventDate(booking.eventDate));
    y += 42;
    drawField(leftCol, y, "Time", formatTime(booking.eventStartTime));
    drawField(rightCol, y, "Venue", booking.eventVenue);
    y += 42;

    y += 8;
    doc.setDrawColor(230, 230, 235);
    doc.setLineWidth(0.5);
    doc.line(cardX + 20, y, cardX + cardW - 20, y);
    y += 20;

    const qrSize = 240, qrX = (pw - qrSize) / 2;
    doc.setFillColor(255, 255, 255);
    doc.setDrawColor(navyR, navyG, navyB);
    doc.setLineWidth(2);
    doc.roundedRect(qrX - 6, y - 6, qrSize + 12, qrSize + 12, 6, 6, "FD");
    doc.addImage(qrImageDataUrl, "PNG", qrX, y, qrSize, qrSize);

    y += qrSize + 24;
    doc.setFont("helvetica", "bold");
    doc.setFontSize(16);
    doc.setTextColor(navyR, navyG, navyB);
    doc.text(ticketRef, pw / 2, y, { align: "center" });
    y += 28;

    doc.setDrawColor(230, 230, 235);
    doc.setLineWidth(0.5);
    doc.line(cardX + 20, y, cardX + cardW - 20, y);
    y += 24;

    const amountText = isFree ? "Free" : `Rs. ${Number(booking.totalAmount).toLocaleString("en-IN")}`;

    const drawDetail = (lx: number, ly: number, label: string, value: string) => {
      doc.setFontSize(8); doc.setFont("helvetica", "normal");
      doc.setTextColor(...labelColor); doc.text(label.toUpperCase(), lx, ly);
      doc.setFontSize(11); doc.setFont("helvetica", "bold");
      doc.setTextColor(...valueColor); doc.text(value || "—", lx, ly + 14);
    };

    drawDetail(leftCol, y, "Ticket Ref", ticketRef);
    drawDetail(rightCol, y, "Quantity", booking.quantity);
    y += 42;
    drawDetail(leftCol, y, "Amount", amountText);
    drawDetail(rightCol, y, "Payment Status", booking.paymentStatus);

    const footerH = 52;
    doc.setFillColor(navyR, navyG, navyB);
    doc.rect(0, ph - footerH, pw, footerH, "F");
    doc.setFontSize(9);
    doc.setFont("helvetica", "normal");
    doc.setTextColor(255, 255, 255);
    doc.text("Present this QR code at the entry gate for check-in.", pw / 2, ph - footerH / 2 + 4, { align: "center" });

    doc.save(`EventHora-Ticket-${ticketRef}.pdf`);
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
        </div>

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

export default function AdminBookingSuccess() {
  return (
    <Suspense fallback={<div className="p-8 text-center text-navy/70">Loading ticket details...</div>}>
      <AdminBookingSuccessContent />
    </Suspense>
  );
}
