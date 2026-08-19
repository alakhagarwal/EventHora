"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { Check, Share2, Download, X } from "lucide-react";
import { api } from "@/lib/api";
import { clearMemberSession, getMemberSession } from "@/lib/auth";
import { toast } from "@/lib/toast";
import SearchInput from "@/components/SearchInput";
import EventSlider from "@/components/EventSlider";
import { useDebouncedValue } from "@/lib/useDebouncedValue";
import QRCode from "qrcode";
import { generateTicketPdf } from "@/lib/ticketPdf";
import type { EventSummary } from "@/components/EventCard";

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
  // banner may come from API; fall back gracefully
  bannerUrl?: string | null;
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

/** Convert a BookingRow into an EventSummary shape for the slider */
function bookingToEvent(b: BookingRow): EventSummary {
  return {
    id: b.ticketReference,
    title: b.eventTitle,
    eventDate: b.eventDate,
    startTime: b.eventStartTime ?? undefined,
    venue: b.eventVenue ?? undefined,
    bannerUrl: b.bannerUrl ?? null,
    uniqueEventLink: b.eventUniqueLink,
    registrationOpen: undefined, // not relevant for past bookings
  };
}

export default function MemberBookingsPage() {
  const router = useRouter();
  const [bookings, setBookings] = useState<BookingRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [authReady, setAuthReady] = useState(false);
  const [query, setQuery] = useState("");

  useEffect(() => {
    const memberSession = getMemberSession();
    if (!memberSession?.sessionToken) {
      router.replace("/login");
      return;
    }

    setAuthReady(true);
    setLoading(true);
    api.myBookings(memberSession.sessionToken)
      .then((data) => {
        const sorted = [...(data || [])].sort((a, b) => new Date(b.bookedAt).getTime() - new Date(a.bookedAt).getTime());
        setBookings(sorted);
      })
      .catch((err: any) => {
        if (err?.status === 401 || err?.status === 403 || /session|expired|unauthorized/i.test(err?.message || "")) {
          clearMemberSession();
          toast.error("Session expired. Please log in again.");
          router.replace("/login");
          return;
        }
        toast.error(err?.message || "Failed to load bookings.");
        setBookings([]);
      })
      .finally(() => setLoading(false));
  }, [router]);

  const bookingCount = useMemo(() => bookings.length, [bookings]);
  const debouncedQuery = useDebouncedValue(query, 300);

  const filteredBookings = useMemo(() => {
    const normalizedQuery = debouncedQuery.trim().toLowerCase();
    return bookings.filter((booking) => {
      if (!normalizedQuery) return true;
      return (
        booking.ticketReference.toLowerCase().includes(normalizedQuery) ||
        booking.eventTitle.toLowerCase().includes(normalizedQuery)
      );
    });
  }, [bookings, debouncedQuery]);

  /* Split into upcoming vs past for the sliders */
  const upcomingBookings = useMemo(
    () => filteredBookings.filter((b) => !b.eventDate || new Date(b.eventDate) >= new Date()),
    [filteredBookings]
  );

  const pastBookings = useMemo(
    () => filteredBookings.filter((b) => b.eventDate && new Date(b.eventDate) < new Date()),
    [filteredBookings]
  );

  const downloadBookingTicket = async (booking: BookingRow) => {
    try {
      const qrPayload = booking.ticketReference;
      const qrImageDataUrl = await QRCode.toDataURL(qrPayload, { width: 300, margin: 2 });
      await generateTicketPdf({
        ticketReference: booking.ticketReference,
        eventTitle: booking.eventTitle,
        eventDate: booking.eventDate,
        startTime: booking.eventStartTime ?? undefined,
        venue: booking.eventVenue ?? "",
        quantity: booking.quantity,
        totalAmount: booking.totalAmount,
        paymentStatus: booking.paymentStatus,
        qrImageDataUrl,
      });
    } catch (err: any) {
      toast.error(err?.message || "Failed to generate ticket PDF.");
    }
  };

  const shareBooking = async (booking: BookingRow) => {
    const message = `🎟️ I'm attending ${booking.eventTitle} on ${formatDateTime(booking.eventDate, booking.eventStartTime)}! My ticket ref: ${booking.ticketReference}. See you there!`;
    const shareUrl = `${window.location.origin}/events/${booking.eventUniqueLink}`;

    try {
      if (navigator.share) {
        await navigator.share({ title: booking.eventTitle, text: message, url: shareUrl });
      } else {
        await navigator.clipboard.writeText(`${message}\n${shareUrl}`);
        toast.info("Link copied!");
      }
    } catch {
      try {
        await navigator.clipboard.writeText(`${message}\n${shareUrl}`);
        toast.info("Link copied!");
      } catch {
        toast.error("Unable to share booking right now.");
      }
    }
  };

  if (!authReady) {
    return <div className="mx-auto max-w-5xl px-4 py-12 text-navy/60">Loading...</div>;
  }

  /* ── Empty state ── */
  if (!loading && bookings.length === 0) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-8 md:px-6 md:py-12">
        <div className="mb-6">
          <div className="flex items-end justify-between gap-4">
            <div>
              <div className="eyebrow">Member</div>
              <h1 className="h1 mt-2">My Bookings</h1>
            </div>
            <Link href="/member/profile" className="btn-outline">Profile</Link>
          </div>
        </div>
        <div className="card p-12 text-center text-navy/60">
          <h2 className="font-display text-2xl font-semibold text-navy">No bookings yet</h2>
          <p className="mt-2 text-sm">Browse events and make your first booking.</p>
          <Link href="/events" className="btn-primary mt-5 inline-flex">Browse Events</Link>
        </div>
      </div>
    );
  }

  return (
    <>
      {/* ── Mobile Layout: BookMyShow-style sliders ── */}
      <div className="md:hidden">
        {/* Header */}
        <div className="px-4 pt-5 pb-3">
          <div className="eyebrow">Member</div>
          <div className="flex items-center justify-between gap-3">
            <h1 className="font-display text-2xl font-bold text-navy leading-tight mb-1">
              My Bookings
            </h1>
            <Link href="/member/profile" className="btn-outline shrink-0">Profile</Link>
          </div>
          {!loading && bookings.length > 0 && (
            <p className="text-xs text-navy/60 mb-3">
              {bookingCount} booking{bookingCount === 1 ? "" : "s"}
            </p>
          )}
          {!loading && bookings.length > 0 && (
            <SearchInput
              className="w-full"
              value={query}
              onChange={setQuery}
              placeholder="Search by title or ref…"
            />
          )}
        </div>

        {/* Upcoming Events Slider */}
        <EventSlider
          title="Upcoming Events"
          eyebrow="Your tickets"
          events={upcomingBookings.map(bookingToEvent)}
          getHref={(e) => `/events/${e.uniqueEventLink}`}
          actionLabel="View Ticket"
          loading={loading}
          emptyMessage="No upcoming bookings."
        />

        {/* Past Events Slider */}
        <EventSlider
          title="Past Events"
          eyebrow="Your history"
          events={pastBookings.map(bookingToEvent)}
          getHref={(e) => `/events/${e.uniqueEventLink}`}
          loading={loading}
          emptyMessage="No past bookings."
        />

        {/* Search result list (appears when searching) */}
        {!loading && debouncedQuery && filteredBookings.length > 0 && (
          <div className="px-4 pb-6">
            <h2 className="font-display text-base font-semibold text-navy mb-3">
              Search Results
            </h2>
            <div className="space-y-3">
              {filteredBookings.map((booking) => (
                <MobileBookingCard
                  key={`${booking.ticketReference}-${booking.bookedAt}`}
                  booking={booking}
                  onDownload={downloadBookingTicket}
                  onShare={shareBooking}
                />
              ))}
            </div>
          </div>
        )}
      </div>

      {/* ── Desktop Layout: Table/card list (unchanged) ── */}
      <div className="hidden md:block mx-auto max-w-5xl px-6 py-12">
        <div className="mb-6">
          <div className="flex items-end justify-between gap-4">
            <div>
              <div className="eyebrow">Member</div>
              <h1 className="h1 mt-2">My Bookings</h1>
            </div>
            <Link href="/member/profile" className="btn-outline">Profile</Link>
          </div>
          {!loading && bookings.length > 0 && (
            <p className="mt-2 text-sm text-navy/70">{bookingCount} booking{bookingCount === 1 ? "" : "s"} found</p>
          )}
        </div>

        {!loading && bookings.length > 0 && (
          <div className="mb-6 flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
            <SearchInput
              className="w-full md:max-w-md"
              value={query}
              onChange={setQuery}
              placeholder="Search by ticket reference or event title"
            />
            <p className="text-sm text-navy/60">Showing {filteredBookings.length} of {bookingCount} bookings</p>
          </div>
        )}

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
        ) : filteredBookings.length === 0 ? (
          <div className="card p-12 text-center text-navy/60">
            <h2 className="font-display text-2xl font-semibold text-navy">No matches found</h2>
            <p className="mt-2 text-sm">Try a different ticket reference or event title.</p>
          </div>
        ) : (
          <div className="space-y-4">
            {filteredBookings.map((booking) => (
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
                    <div className="flex gap-2">
                      {["CONFIRMED", "FREE", "COMPLIMENTARY", "PAY_AT_GATE"].includes(booking.paymentStatus) && (
                        <button type="button" className="btn-outline w-fit" onClick={() => downloadBookingTicket(booking)}>
                          <Download className="h-4 w-4" /> Download
                        </button>
                      )}
                      <button type="button" className="btn-outline w-fit" onClick={() => shareBooking(booking)}>
                        <Share2 className="h-4 w-4" /> Share
                      </button>
                    </div>
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
    </>
  );
}

/* Compact booking card used in search results on mobile */
function MobileBookingCard({
  booking,
  onDownload,
  onShare,
}: {
  booking: BookingRow;
  onDownload: (b: BookingRow) => void;
  onShare: (b: BookingRow) => void;
}) {
  return (
    <div className="card p-4">
      <div className="flex items-start justify-between gap-2 mb-2">
        <Link
          href={`/events/${booking.eventUniqueLink}`}
          className="font-semibold text-navy text-sm leading-tight hover:text-gold"
        >
          {booking.eventTitle}
        </Link>
        <StatusBadge status={booking.paymentStatus} />
      </div>
      <div className="text-xs text-navy/60 mb-1">
        {new Date(booking.eventDate).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" })}
        {booking.eventVenue ? ` · ${booking.eventVenue}` : ""}
      </div>
      <div className="text-xs text-navy/60 mb-3">
        Ref: <span className="font-medium text-navy">{booking.ticketReference}</span>
        {" · "}Qty: <span className="font-medium text-navy">{booking.quantity}</span>
      </div>
      <div className="flex gap-2">
        {["CONFIRMED", "FREE", "COMPLIMENTARY", "PAY_AT_GATE"].includes(booking.paymentStatus) && (
          <button
            type="button"
            className="btn-outline text-xs py-1 px-2 w-fit"
            onClick={() => onDownload(booking)}
          >
            <Download className="h-3 w-3" /> PDF
          </button>
        )}
        <button
          type="button"
          className="btn-outline text-xs py-1 px-2 w-fit"
          onClick={() => onShare(booking)}
        >
          <Share2 className="h-3 w-3" /> Share
        </button>
      </div>
    </div>
  );
}