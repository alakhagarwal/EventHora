"use client";

import Link from "next/link";
import { useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Minus, Plus, Search, ChevronDown, CheckCircle2 } from "lucide-react";
import { api } from "@/lib/api";
import { getSession } from "@/lib/auth";
import { toast } from "@/lib/toast";

type PublishedEvent = {
  id: string;
  title: string;
  eventDate?: string;
  startTime?: string;
  status?: string;
  ticketPrice?: number;
  maxTicketsPerMember?: number;
  freeTicketsPerRegistration?: number;
  venue?: string;
  availableCount?: number;
  totalCapacity?: number;
  isSoldOut?: boolean;
};

function formatDateTime(value?: string, timeValue?: string) {
  if (!value) return "Date TBD";
  const date = new Date(value);
  const datePart = date.toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
  const formattedTime = timeValue
    ? new Date(`1970-01-01T${timeValue}`).toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit" })
    : date.toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit" });
  return `${datePart} \u00b7 ${formattedTime}`;
}

function formatAmount(amount: number) {
  return `\u20b9${Number(amount || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

export default function AdminDirectBookingPage() {
  const router = useRouter();
  const [events, setEvents] = useState<PublishedEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [authReady, setAuthReady] = useState(false);
  const [mobileNumber, setMobileNumber] = useState("");
  const [form, setForm] = useState<{
    eventId: string;
    memberId: string;
    memberType: "INDIAN" | "OVERSEAS";
    quantity: number;
  }>({
    eventId: "",
    memberId: "",
    memberType: "INDIAN",
    quantity: 1,
  });

  const [searchQuery, setSearchQuery] = useState("");
  const [showDropdown, setShowDropdown] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const [highlightIdx, setHighlightIdx] = useState(-1);

  useEffect(() => {
    const session = getSession();
    if (!session || (session.role !== "ADMIN" && session.role !== "STAFF")) {
      router.replace("/login");
      setAuthReady(true);
      return;
    }
    setAuthReady(true);
    setLoading(true);
    api.adminEvents()
      .then(async (data) => {
        const published = (data || []).filter((event: PublishedEvent) => event.status === "PUBLISHED");
        const detailedEvents = await Promise.all(
          published.map(async (event: PublishedEvent) => {
            try {
              const full = await api.adminEvent(event.id);
              return {
                ...event,
                ...full,
                isSoldOut: Number(full.availableCount ?? event.availableCount ?? 0) <= 0,
              };
            } catch {
              return {
                ...event,
                isSoldOut: Number(event.availableCount ?? 0) <= 0,
              };
            }
          })
        );
        setEvents(detailedEvents);
        if (detailedEvents.length > 0) {
          setForm((current) => ({ ...current, eventId: current.eventId || detailedEvents[0].id }));
        }
      })
      .catch((err) => {
        toast.error(err?.message || "Failed to load events.");
        setEvents([]);
      })
      .finally(() => setLoading(false));
  }, [router]);

  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setShowDropdown(false);
      }
    };
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, []);

  const selectedEvent = useMemo(
    () => events.find((event) => event.id === form.eventId) || null,
    [events, form.eventId]
  );

  useEffect(() => {
    if (!selectedEvent) return;
    const max = selectedEvent.maxTicketsPerMember || 1;
    setForm((current) => ({
      ...current,
      quantity: Math.min(current.quantity || 1, max),
    }));
  }, [selectedEvent]);

  const filteredEvents = useMemo(() => {
    if (!searchQuery.trim()) return events;
    const q = searchQuery.toLowerCase();
    return events.filter((e) => e.title.toLowerCase().includes(q));
  }, [events, searchQuery]);

  useEffect(() => {
    setHighlightIdx(-1);
  }, [searchQuery]);

  const selectEvent = (eventId: string) => {
    setForm((current) => ({ ...current, eventId }));
    const ev = events.find((e) => e.id === eventId);
    setSearchQuery(ev?.title || "");
    setShowDropdown(false);
    setHighlightIdx(-1);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!showDropdown || filteredEvents.length === 0) return;
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setHighlightIdx((prev) => (prev < filteredEvents.length - 1 ? prev + 1 : 0));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setHighlightIdx((prev) => (prev > 0 ? prev - 1 : filteredEvents.length - 1));
    } else if (e.key === "Enter" && highlightIdx >= 0) {
      e.preventDefault();
      selectEvent(filteredEvents[highlightIdx].id);
    } else if (e.key === "Escape") {
      setShowDropdown(false);
    }
  };

  const ticketPrice = Number(selectedEvent?.ticketPrice || 0);
  const maxTickets = Number(selectedEvent?.maxTicketsPerMember || 1);
  const freeTickets = Number(selectedEvent?.freeTicketsPerRegistration || 0);
  const paidTickets = Math.max(0, form.quantity - freeTickets);
  const orderTotal = ticketPrice * paidTickets;
  const isFreeEvent = orderTotal <= 0;

  const updateQuantity = (delta: number) => {
    setForm((current) => {
      const next = Math.min(maxTickets, Math.max(1, current.quantity + delta));
      return { ...current, quantity: next };
    });
  };

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);

    const trimmedMemberId = form.memberId.trim().toUpperCase();
    if (!selectedEvent) {
      toast.error("Please select an event.");
      return;
    }
    if (!trimmedMemberId.startsWith("RIC")) {
      toast.error("Member ID must start with RIC.");
      return;
    }
    if (Number(selectedEvent.availableCount || 0) <= 0 || selectedEvent.isSoldOut) {
      toast.error("This event is sold out. Please select another event.");
      return;
    }

    setSubmitting(true);
    try {
      const response = await api.adminRegisterMember({
        memberId: trimmedMemberId,
        memberType: form.memberType,
        eventId: selectedEvent.id,
        quantity: form.quantity,
        action: "COMPLIMENTARY",
        mobileNumber: mobileNumber || undefined,
      });
      toast.success(`Member registered: ${response.ticketReference}`);
      const params = new URLSearchParams({
        ticketReference: response.ticketReference || "",
        quantity: String(response.quantity || form.quantity),
        totalAmount: String(response.totalAmount ?? 0),
        paymentStatus: response.paymentStatus || "FREE",
        memberId: response.memberId || trimmedMemberId,
        eventTitle: response.eventTitle || selectedEvent.title || "",
        eventDate: response.eventDate || selectedEvent.eventDate || "",
        eventStartTime: response.eventStartTime || selectedEvent.startTime || "",
        eventVenue: response.eventVenue || selectedEvent.venue || "",
        bookedBy: response.bookedBy || "",
        bookedAt: response.bookedAt || "",
        mobileNumber: mobileNumber || "",
      });
      router.push(`/admin/bookings/success?${params.toString()}`);
    } catch (err: any) {
      const status = err?.status;
      if (status === 409) {
        toast.error("This member is already registered for this event");
      } else if (status === 404) {
        toast.error("Event not found");
      } else if (status === 400) {
        toast.error(err?.message || "Please check the form details and try again.");
      } else {
        toast.error(err?.message || "Failed to register member.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (!authReady) {
    return <div className="mx-auto max-w-4xl px-4 py-12 text-navy/60">Loading...</div>;
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 md:px-6 md:py-12">
      <div className="mb-6 flex items-center justify-between gap-4">
        <div>
          <div className="eyebrow">Admin</div>
          <h1 className="h1 mt-2">Register Member</h1>
        </div>
        <Link href="/admin/dashboard" className="btn-outline">Back to Dashboard</Link>
      </div>

      {loading ? (
        <div className="card p-8 text-navy/60">Loading...</div>
      ) : events.length === 0 ? (
        <div className="card p-8 text-navy/60">No published events are available for walk-in registration.</div>
      ) : (
        <form onSubmit={submit} className="card space-y-6 p-5 md:p-6">
          {/* Event + Member ID */}
          <div className="grid gap-4 md:grid-cols-2">
            <div className="relative" ref={dropdownRef}>
              <label className="label">Event</label>
              <div className="relative mt-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-navy/40" />
                <input
                  type="text"
                  className="input pl-9 pr-8"
                  placeholder="Search events..."
                  value={searchQuery}
                  onChange={(e) => {
                    setSearchQuery(e.target.value);
                    setShowDropdown(true);
                    if (!e.target.value) setForm((current) => ({ ...current, eventId: "" }));
                  }}
                  onFocus={() => {
                    setShowDropdown(true);
                    if (!searchQuery && selectedEvent) {
                      setSearchQuery(selectedEvent.title);
                    }
                  }}
                  onKeyDown={handleKeyDown}
                />
                <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-navy/40" />
              </div>
              {showDropdown && filteredEvents.length > 0 && (
                <ul className="absolute z-20 mt-1 w-full max-h-48 overflow-y-auto rounded-lg border border-navy/15 bg-white shadow-lg">
                  {filteredEvents.map((event, idx) => (
                    <li
                      key={event.id}
                      className={`px-3 py-2.5 text-sm cursor-pointer transition-colors ${idx === highlightIdx ? "bg-navy/10 text-navy font-medium" : "text-navy/80 hover:bg-navy/5"} ${event.id === form.eventId ? "font-semibold" : ""}`}
                      onClick={() => selectEvent(event.id)}
                      onMouseEnter={() => setHighlightIdx(idx)}
                    >
                      {event.title}
                    </li>
                  ))}
                </ul>
              )}
              {showDropdown && filteredEvents.length === 0 && searchQuery.trim() && (
                <div className="absolute z-20 mt-1 w-full rounded-lg border border-navy/15 bg-white p-3 text-sm text-navy/50 text-center">No events found</div>
              )}
              {selectedEvent && (
                <p className={`mt-1 text-xs ${selectedEvent.isSoldOut ? "text-red-700" : "text-navy/60"}`}>
                  {selectedEvent.availableCount}/{selectedEvent.totalCapacity} seats remaining
                </p>
              )}
            </div>
            <div>
              <label className="label">Member ID</label>
              <input
                className="input"
                placeholder="RIC-2024-XXXXX"
                value={form.memberId}
                onChange={(e) => setForm((current) => ({ ...current, memberId: e.target.value.toUpperCase() }))}
              />
            </div>
          </div>

          {/* Mobile Number */}
          <div>
            <label className="label">Mobile Number <span className="text-navy/40">(for ticket delivery, optional)</span></label>
            <input
              type="tel"
              className="input"
              placeholder="e.g. 9876543210"
              value={mobileNumber}
              onChange={(e) => setMobileNumber(e.target.value)}
            />
          </div>

          {/* Member Type + Quantity */}
          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <label className="label">Member Type</label>
              <div className="flex gap-3">
                {(["INDIAN", "OVERSEAS"] as const).map((type) => (
                  <button
                    key={type}
                    type="button"
                    onClick={() => setForm((current) => ({ ...current, memberType: type }))}
                    className={`btn ${form.memberType === type ? "btn-dark" : "btn-outline"}`}
                  >
                    {type}
                  </button>
                ))}
              </div>
            </div>
            <div>
              <label className="label">Quantity</label>
              <div className="flex items-center gap-3">
                <button type="button" className="btn-outline" onClick={() => updateQuantity(-1)}><Minus className="h-4 w-4" /></button>
                <input
                  type="number"
                  min={1}
                  max={maxTickets}
                  className="input text-center"
                  value={form.quantity}
                  onChange={(e) => setForm((current) => ({ ...current, quantity: Math.max(1, Math.min(maxTickets, Number(e.target.value) || 1)) }))}
                />
                <button type="button" className="btn-outline" onClick={() => updateQuantity(1)}><Plus className="h-4 w-4" /></button>
              </div>
              <p className="mt-1 text-xs text-navy/60">Max {maxTickets} ticket{maxTickets === 1 ? "" : "s"} per member</p>
            </div>
          </div>

          {/* Action */}
          {!isFreeEvent && (
            <div className="flex items-center gap-2 text-sm text-navy/70">
              <CheckCircle2 className="h-4 w-4 text-green-600" />
              <span>
                This booking will be marked <strong>Payment Verified</strong> — payment already collected.
              </span>
            </div>
          )}

          {/* Order Summary */}
          <div className="card bg-navy/5 p-4">
            <div className="label">Order Summary</div>
            <div className="text-lg font-semibold text-navy">
              {isFreeEvent ? "Free" : `${paidTickets} x ${formatAmount(ticketPrice)} = ${formatAmount(orderTotal)}`}
            </div>
            <div className="mt-1 text-sm text-navy/70">
              {form.quantity} ticket{form.quantity > 1 ? "s" : ""}
              {paidTickets !== form.quantity ? ` (${paidTickets} paid, ${freeTickets} free)` : ""}
            </div>
            <div className="mt-1 text-sm text-navy/70">
              {selectedEvent ? `${selectedEvent.title} \u00b7 ${formatDateTime(selectedEvent.eventDate, selectedEvent.startTime)}` : "Select an event to continue"}
            </div>
          </div>

          {/* Submit */}
          <button type="submit" className="btn-primary w-full md:w-auto" disabled={submitting || !selectedEvent}>
            {submitting ? "Registering..." : "Register Member"}
          </button>
        </form>
      )}
    </div>
  );
}
