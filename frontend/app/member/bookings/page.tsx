"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { Check, X } from "lucide-react";
import { api } from "@/lib/api";
import { getMemberSession } from "@/lib/auth";

type BookingRow = {
  ticketReference: string;
  quantity: number;
  totalAmount: number;
  paymentStatus: "CONFIRMED" | "FREE" | "PAY_AT_GATE" | "COMPLIMENTARY" | "PENDING" | "FAILED";
  paymentPreference: "ONLINE" | "PAY_AT_GATE";
  isCheckedIn: boolean;
  checkedInAt: string | null;
  eventTitle: string;
  eventDate: string;
  eventStartTime?: string | null;
  eventVenue?: string | null;
  eventUniqueLink: string;
  bookedAt: string;
};

function formatDateTime(dateValue?: string, timeValue?: string | null) {
  if (!dateValue) return "Date TBD";
  const date = new Date(dateValue);
  const datePart = date.toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
  const formattedTime = timeValue
    ? new Date(`1970-01-01T${timeValue}`).toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit" })
    : date.toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit" });
  return `${datePart} · ${formattedTime}`;
}

function formatAmount(amount: number) {
  return `₹${Number(amount || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function StatusBadge({ status }: { status: BookingRow["paymentStatus"] }) {
  const tone =
    status === "CONFIRMED"
      ? "bg-green-100 text-green-800"
      : status === "FREE"
      ? "bg-blue-100 text-blue-800"
      : status === "PAY_AT_GATE"
      ? "bg-orange-100 text-orange-800"
      : status === "COMPLIMENTARY"
      ? "bg-purple-100 text-purple-800"
      : status === "PENDING"
      ? "bg-yellow-100 text-yellow-800"
      : "bg-red-100 text-red-800";

  return <span className={`chip ${tone}`}>{status.replaceAll("_", " ")}</span>;
}

export default function MemberBookingsPage() {
  const router = useRouter();
  const [bookings, setBookings] = useState<BookingRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [authReady, setAuthReady] = useState(false);

  useEffect(() => {
    const memberSession = getMemberSession();
    if (!memberSession?.sessionToken) {
      router.replace("/login");
      setAuthReady(true);
      return;
    }

    setAuthReady(true);
    setLoading(true);
    api.myBookings(memberSession.sessionToken)
      .then((data) => {
        const sorted = [...(data || [])].sort((a, b) => new Date(b.bookedAt).getTime() - new Date(a.bookedAt).getTime());
        setBookings(sorted);
      })
      .catch((err) => setError(err?.message || "Failed to load bookings."))
      .finally(() => setLoading(false));
  }, [router]);

  const bookingCount = useMemo(() => bookings.length, [bookings]);

  if (!authReady) {
    return <div className="mx-auto max-w-5xl px-4 py-12 text-navy/60">Loading...</div>;
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 md:px-6 md:py-12">
      <div className="mb-6">
        <div className="eyebrow">Member</div>
        <h1 className="h1 mt-2">My Bookings</h1>
        {!loading && !error && bookings.length > 0 && (
          <p className="mt-2 text-sm text-navy/70">{bookingCount} booking{bookingCount === 1 ? "" : "s"} found</p>
        )}
      </div>

      {loading ? (
        <div className="space-y-4">
          {Array.from({ length: 3 }).map((_, index) => (
            <div key={index} className="card animate-pulse p-5 space-y-4">
              <div className="h-5 w-2/3 rounded bg-navy/10" />
              <div className="h-4 w-1/2 rounded bg-navy/10" />
              <div className="h-4 w-1/3 rounded bg-navy/10" />
              <div className="grid gap-3 sm:grid-cols-2">
                <div className="h-24 rounded bg-navy/10" />
                <div className="h-24 rounded bg-navy/10" />
              </div>
            </div>
          ))}
        </div>
      ) : error ? (
        <div className="card border-red-200 bg-red-50 p-8 text-red-700">{error}</div>
      ) : bookings.length === 0 ? (
        <div className="card p-12 text-center text-navy/60">
          <h2 className="font-display text-2xl font-semibold text-navy">No bookings yet</h2>
          <p className="mt-2 text-sm">Browse events and make your first booking.</p>
          <Link href="/events" className="btn-primary mt-5 inline-flex">Browse Events</Link>
        </div>
      ) : (
        <div className="space-y-4">
          {bookings.map((booking) => (
            <div key={`${booking.ticketReference}-${booking.bookedAt}`} className="card p-5 md:p-6">
              <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
                <div className="min-w-0 flex-1 space-y-3">
                  <div className="flex flex-wrap items-center gap-2">
                    <Link href={`/events/${booking.eventUniqueLink}`} className="font-display text-2xl font-semibold text-navy hover:text-gold">
                      {booking.eventTitle}
                    </Link>
                    <StatusBadge status={booking.paymentStatus} />
                  </div>
                  <div className="text-sm text-navy/70">
                    {formatDateTime(booking.eventDate, booking.eventStartTime)}
                    {booking.eventVenue ? ` · ${booking.eventVenue}` : ""}
                  </div>
                  <div className="flex flex-wrap gap-3 text-sm text-navy/70">
                    <span>Ticket Ref: <span className="font-medium text-navy">{booking.ticketReference}</span></span>
                    <span>Qty: <span className="font-medium text-navy">{booking.quantity}</span></span>
                    <span>Amount: <span className="font-medium text-navy">{formatAmount(booking.totalAmount)}</span></span>
                  </div>
                </div>

                <div className="flex flex-col gap-2 text-sm text-navy/70 md:items-end">
                  <div className="inline-flex items-center gap-2 font-medium text-navy">
                    {booking.isCheckedIn ? (
                      <><Check className="h-4 w-4 text-green-600" /> Checked in</>
                    ) : (
                      <><X className="h-4 w-4 text-red-600" /> Not checked in</>
                    )}
                  </div>
                  <div>Booked at {formatDateTime(booking.bookedAt)}</div>
                  {booking.checkedInAt && <div>Checked in at {formatDateTime(booking.checkedInAt)}</div>}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}