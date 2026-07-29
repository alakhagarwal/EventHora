"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { Check, Minus, Plus } from "lucide-react";
import { api } from "@/lib/api";
import { getSession } from "@/lib/auth";

type PublishedEvent = {
  id: string;
  title: string;
  eventDate?: string;
  startTime?: string;
  status?: string;
  ticketPrice?: number;
  maxTicketsPerMember?: number;
  venue?: string;
};

type BookingSuccess = {
  ticketReference: string;
  quantity: number;
  totalAmount: number;
  paymentStatus: string;
  memberId: string;
  eventTitle: string;
  eventDate: string;
  eventStartTime?: string;
  eventVenue?: string;
  bookedBy: string;
  bookedAt: string;
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
  return `${datePart} · ${formattedTime}`;
}

function formatAmount(amount: number) {
  return `₹${Number(amount || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function resetFormState(eventId: string) {
  return {
    eventId,
    memberId: "",
    memberType: "INDIAN" as const,
    quantity: 1,
    action: "PAY_AT_GATE" as const,
  };
}

export default function AdminDirectBookingPage() {
  const router = useRouter();
  const [events, setEvents] = useState<PublishedEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<BookingSuccess | null>(null);
  const [authReady, setAuthReady] = useState(false);
  const [form, setForm] = useState<{ eventId: string; memberId: string; memberType: "INDIAN" | "OVERSEAS"; quantity: number; action: "PAY_AT_GATE" | "COMPLIMENTARY"; }>({
    eventId: "",
    memberId: "",
    memberType: "INDIAN",
    quantity: 1,
    action: "PAY_AT_GATE",
  });

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
      .then((data) => {
        const published = (data || []).filter((event: PublishedEvent) => event.status === "PUBLISHED");
        setEvents(published);
        if (published.length > 0) {
          setForm((current) => ({ ...current, eventId: current.eventId || published[0].id }));
        }
      })
      .catch((err) => setError(err?.message || "Failed to load events."))
      .finally(() => setLoading(false));
  }, [router]);

  const selectedEvent = useMemo(
    () => events.find((event) => event.id === form.eventId) || null,
    [events, form.eventId]
  );

  useEffect(() => {
    if (!selectedEvent) return;
    const max = selectedEvent.maxTicketsPerMember || 1;
    setForm((current) => ({ ...current, quantity: Math.min(current.quantity || 1, max), action: current.action || "PAY_AT_GATE" }));
  }, [selectedEvent]);

  const ticketPrice = Number(selectedEvent?.ticketPrice || 0);
  const maxTickets = Number(selectedEvent?.maxTicketsPerMember || 1);
  const isFreeEvent = ticketPrice <= 0;
  const orderTotal = isFreeEvent ? 0 : ticketPrice * form.quantity;

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
      setError("Please select an event.");
      return;
    }
    if (!trimmedMemberId.startsWith("RIC")) {
      setError("Member ID must start with RIC.");
      return;
    }

    setSubmitting(true);
    try {
      const response = await api.adminRegisterMember({
        memberId: trimmedMemberId,
        memberType: form.memberType,
        eventId: selectedEvent.id,
        quantity: form.quantity,
        action: isFreeEvent ? "PAY_AT_GATE" : form.action,
      });
      setSuccess(response as BookingSuccess);
    } catch (err: any) {
      const status = err?.status;
      if (status === 409) {
        setError("This member is already registered for this event");
      } else if (status === 404) {
        setError("Event not found");
      } else if (status === 400) {
        setError(err?.message || "Please check the form details and try again.");
      } else {
        setError(err?.message || "Failed to register member.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  const startAnother = () => {
    setSuccess(null);
    setError(null);
    setForm((current) => resetFormState(current.eventId || events[0]?.id || ""));
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
      ) : success ? (
        <div className="card p-6 md:p-8">
          <div className="flex items-center gap-3 text-green-700">
            <Check className="h-8 w-8" />
            <div>
              <div className="font-display text-2xl font-semibold text-navy">Member registered successfully</div>
              <p className="text-sm text-navy/70">The booking has been created and the ticket is ready.</p>
            </div>
          </div>

          <div className="mt-6 grid gap-4 md:grid-cols-2">
            <div className="card p-4">
              <div className="label">Ticket Reference</div>
              <div className="text-lg font-semibold text-navy">{success.ticketReference}</div>
            </div>
            <div className="card p-4">
              <div className="label">Member ID</div>
              <div className="text-lg font-semibold text-navy">{success.memberId}</div>
            </div>
            <div className="card p-4">
              <div className="label">Event</div>
              <div className="text-lg font-semibold text-navy">{success.eventTitle}</div>
              <div className="mt-1 text-sm text-navy/70">{formatDateTime(success.eventDate, success.eventStartTime)}</div>
              {success.eventVenue && <div className="mt-1 text-sm text-navy/70">{success.eventVenue}</div>}
            </div>
            <div className="card p-4">
              <div className="label">Booking Summary</div>
              <div className="text-sm text-navy/70">Qty: <span className="font-semibold text-navy">{success.quantity}</span></div>
              <div className="text-sm text-navy/70">Amount: <span className="font-semibold text-navy">{formatAmount(success.totalAmount)}</span></div>
              <div className="text-sm text-navy/70">Status: <span className="font-semibold text-navy">{success.paymentStatus}</span></div>
            </div>
            <div className="card p-4 md:col-span-2">
              <div className="label">Booked By</div>
              <div className="text-lg font-semibold text-navy">{success.bookedBy}</div>
              <div className="mt-1 text-sm text-navy/70">Booked at {formatDateTime(success.bookedAt)}</div>
            </div>
          </div>

          <div className="mt-6 flex flex-col gap-3 sm:flex-row">
            <button className="btn-primary" onClick={startAnother}>Register Another Member</button>
            <Link href="/admin/dashboard" className="btn-outline">Back to Dashboard</Link>
          </div>
        </div>
      ) : (
        <form onSubmit={submit} className="card space-y-6 p-5 md:p-6">
          {error && <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <label className="label">Event</label>
              <select
                className="input"
                value={form.eventId}
                onChange={(e) => setForm((current) => ({ ...current, eventId: e.target.value }))}
              >
                {events.map((event) => (
                  <option key={event.id} value={event.id}>
                    {event.title} · {formatDateTime(event.eventDate, event.startTime)}
                  </option>
                ))}
              </select>
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

          {!isFreeEvent && (
            <div>
              <label className="label">Action</label>
              <div className="flex gap-3">
                {(["PAY_AT_GATE", "COMPLIMENTARY"] as const).map((action) => (
                  <button
                    key={action}
                    type="button"
                    onClick={() => setForm((current) => ({ ...current, action }))}
                    className={`btn ${form.action === action ? "btn-dark" : "btn-outline"}`}
                  >
                    {action === "PAY_AT_GATE" ? "Pay at Gate" : "Complimentary"}
                  </button>
                ))}
              </div>
            </div>
          )}

          <div className="card bg-navy/5 p-4">
            <div className="label">Order Summary</div>
            <div className="text-lg font-semibold text-navy">
              {isFreeEvent ? "Free" : `${form.quantity} × ${formatAmount(ticketPrice)} = ${formatAmount(orderTotal)}`}
            </div>
            <div className="mt-1 text-sm text-navy/70">
              {selectedEvent ? `${selectedEvent.title} · ${formatDateTime(selectedEvent.eventDate, selectedEvent.startTime)}` : "Select an event to continue"}
            </div>
          </div>

          <button type="submit" className="btn-primary w-full md:w-auto" disabled={submitting || !selectedEvent}>
            {submitting ? "Registering…" : "Register Member"}
          </button>
        </form>
      )}
    </div>
  );
}