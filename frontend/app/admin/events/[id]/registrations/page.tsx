"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { Check, X } from "lucide-react";
import { api } from "@/lib/api";
import { getSession } from "@/lib/auth";
import SearchInput from "@/components/SearchInput";
import { toast } from "@/lib/toast";
import { useDebouncedValue } from "@/lib/useDebouncedValue";

type RegistrationRow = {
  registrationId: string;
  ticketReference: string;
  memberId: string;
  memberType: "INDIAN" | "OVERSEAS";
  quantity: number;
  totalAmount: number;
  paymentStatus: "CONFIRMED" | "FREE" | "PAY_AT_GATE" | "COMPLIMENTARY" | "PENDING" | "FAILED";
  paymentPreference: "ONLINE" | "PAY_AT_GATE";
  isCheckedIn: boolean;
  checkedInAt: string | null;
  bookedAt: string;
};

function formatDateTime(value?: string | null) {
  if (!value) return "-";
  const date = new Date(value);
  return `${date.toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  })} · ${date.toLocaleTimeString("en-US", {
    hour: "numeric",
    minute: "2-digit",
  })}`;
}

function formatAmount(amount: number) {
  return `₹${(Number(amount) || 0).toFixed(2)}`;
}

function PaymentBadge({ status }: { status: RegistrationRow["paymentStatus"] }) {
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

export default function EventRegistrationsPage() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const eventId = Array.isArray(params.id) ? params.id[0] : params.id;

  const [event, setEvent] = useState<{ title?: string; eventDate?: string } | null>(null);
  const [registrations, setRegistrations] = useState<RegistrationRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [authReady, setAuthReady] = useState(false);
  const [query, setQuery] = useState("");

  useEffect(() => {
    const session = getSession();
    if (!session || (session.role !== "ADMIN" && session.role !== "STAFF")) {
      router.replace("/login");
      setAuthReady(true);
      return;
    }

    setAuthReady(true);
    setLoading(true);
    Promise.all([api.adminEvent(eventId), api.eventRegistrations(eventId)])
      .then(([eventData, registrationData]) => {
        setEvent(eventData);
        setRegistrations(registrationData || []);
      })
      .catch((err) => {
        toast.error(err?.message || "Failed to load registrations.");
        setRegistrations([]);
      })
      .finally(() => setLoading(false));
  }, [eventId, router]);

  const debouncedQuery = useDebouncedValue(query, 300);

  const summary = useMemo(() => {
    const ticketCount = registrations.reduce((total, row) => total + (Number(row.quantity) || 0), 0);
    return { registrationsCount: registrations.length, ticketCount };
  }, [registrations]);

  const filteredRegistrations = useMemo(() => {
    const normalizedQuery = debouncedQuery.trim().toLowerCase();
    return registrations.filter((registration) => {
      if (!normalizedQuery) return true;
      return (
        registration.ticketReference.toLowerCase().includes(normalizedQuery) ||
        registration.memberId.toLowerCase().includes(normalizedQuery)
      );
    });
  }, [debouncedQuery, registrations]);

  if (!authReady) {
    return <div className="mx-auto max-w-7xl px-4 py-12 text-navy/60">Loading...</div>;
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 md:px-6 md:py-12">
      <div className="mb-6 flex flex-col gap-4">
        <Link href="/admin/dashboard" className="btn-ghost w-fit px-0">
          Back to Dashboard
        </Link>
        <div>
          <div className="eyebrow">Admin</div>
          <h1 className="h1 mt-2">Registrations — {event?.title || "Event"}</h1>
          <p className="mt-2 text-sm text-navy/70">
            {event?.eventDate ? new Date(event.eventDate).toLocaleDateString("en-US", {
              month: "short",
              day: "numeric",
              year: "numeric",
            }) : "Date TBD"}
          </p>
        </div>
      </div>

      {loading ? (
        <div className="card p-8 text-navy/60">Loading...</div>
      ) : registrations.length === 0 ? (
        <div className="card p-12 text-center text-navy/60">No registrations found for this event.</div>
      ) : (
        <div className="space-y-4">
          <SearchInput value={query} onChange={setQuery} placeholder="Search by ticket reference or member ID" className="w-full md:max-w-md" />
          <div className="card px-5 py-4 text-sm font-medium text-navy">
            {summary.registrationsCount} registrations · {summary.ticketCount} total tickets
          </div>

          <div className="overflow-x-auto card">
            <table className="min-w-full divide-y divide-navy/10 text-left text-sm">
              <thead className="bg-navy/5 text-xs uppercase tracking-wide text-navy/60">
                <tr>
                  <th className="px-4 py-3">Ticket Ref</th>
                  <th className="px-4 py-3">Member ID</th>
                  <th className="px-4 py-3">Type</th>
                  <th className="px-4 py-3">Qty</th>
                  <th className="px-4 py-3">Amount</th>
                  <th className="px-4 py-3">Payment Status</th>
                  <th className="px-4 py-3">Checked In</th>
                  <th className="px-4 py-3">Booked At</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-navy/10 bg-white">
                {filteredRegistrations.map((registration) => (
                  <tr key={registration.registrationId} className="align-top">
                    <td className="px-4 py-3 font-medium text-navy">{registration.ticketReference}</td>
                    <td className="px-4 py-3 text-navy/80">{registration.memberId}</td>
                    <td className="px-4 py-3 text-navy/80">{registration.memberType}</td>
                    <td className="px-4 py-3 text-navy/80">{registration.quantity}</td>
                    <td className="px-4 py-3 text-navy/80">{formatAmount(registration.totalAmount)}</td>
                    <td className="px-4 py-3"><PaymentBadge status={registration.paymentStatus} /></td>
                    <td className="px-4 py-3">
                      {registration.isCheckedIn ? (
                        <span className="inline-flex items-center gap-1 font-medium text-green-700">
                          <Check className="h-4 w-4" /> Yes
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 font-medium text-red-700">
                          <X className="h-4 w-4" /> No
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-navy/80">{formatDateTime(registration.bookedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {filteredRegistrations.length === 0 && (
            <div className="card p-10 text-center text-navy/60">No registrations match your search.</div>
          )}
        </div>
      )}
    </div>
  );
}