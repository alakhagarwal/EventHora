"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { getSession } from "@/lib/auth";

type PaymentSummary = {
  totalCapacity: number;
  seatsLocked: number;
  seatsRemaining: number;
  confirmedCount: number;
  payAtGateCount: number;
  freeCount: number;
  complimentaryCount: number;
  pendingCount: number;
  failedCount: number;
  totalRegistrations: number;
  checkedInCount: number;
  notCheckedInCount: number;
  checkedInTickets: number;
  notCheckedInTickets: number;
  totalRevenue: number;
  pendingGateCollection: number;
  complimentaryWaived: number;
};

function formatAmount(amount: number) {
  return `₹${(Number(amount) || 0).toFixed(2)}`;
}

function StatCard({ label, value, accent }: { label: string; value: string | number; accent: string }) {
  return (
    <div className="card p-4">
      <div className={`h-1.5 w-14 rounded-full ${accent}`} />
      <div className="mt-3 text-2xl font-semibold text-navy">{value}</div>
      <div className="mt-1 text-sm text-navy/70">{label}</div>
    </div>
  );
}

function ProgressBar({ percent, color }: { percent: number; color: string }) {
  return (
    <div className="h-3 w-full overflow-hidden rounded-full bg-navy/10">
      <div className={`h-full rounded-full ${color}`} style={{ width: `${Math.min(100, Math.max(0, percent))}%` }} />
    </div>
  );
}

function CheckInStat({ label, value, accent }: { label: string; value: number; accent: string }) {
  return <StatCard label={label} value={value} accent={accent} />;
}

export default function EventPaymentSummaryPage() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const eventId = Array.isArray(params.id) ? params.id[0] : params.id;

  const [event, setEvent] = useState<{ title?: string; eventDate?: string } | null>(null);
  const [summary, setSummary] = useState<PaymentSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [authReady, setAuthReady] = useState(false);

  useEffect(() => {
    const session = getSession();
    if (!session || (session.role !== "ADMIN" && session.role !== "STAFF")) {
      router.replace("/login");
      setAuthReady(true);
      return;
    }

    setAuthReady(true);
    setLoading(true);
    Promise.all([api.adminEvent(eventId), api.eventPaymentSummary(eventId)])
      .then(([eventData, summaryData]) => {
        setEvent(eventData);
        setSummary(summaryData);
      })
      .catch((err) => setError(err?.message || "Failed to load payment summary."))
      .finally(() => setLoading(false));
  }, [eventId, router]);

  const checkInPercent = useMemo(() => {
    if (!summary || !summary.totalRegistrations) return 0;
    return (summary.checkedInCount / summary.totalRegistrations) * 100;
  }, [summary]);

  const capacityColor = useMemo(() => {
    if (!summary || !summary.totalCapacity) return "bg-green-500";
    const remainingRatio = summary.seatsRemaining / summary.totalCapacity;
    if (remainingRatio > 0.5) return "bg-green-500";
    if (remainingRatio >= 0.2) return "bg-amber-500";
    return "bg-red-500";
  }, [summary]);

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
          <h1 className="h1 mt-2">Payment Summary — {event?.title || "Event"}</h1>
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
      ) : error ? (
        <div className="card border-red-200 bg-red-50 p-8 text-red-700">{error}</div>
      ) : summary ? (
        <div className="space-y-6">
          <section className="card p-5 md:p-6">
            <div className="flex items-center justify-between gap-4">
              <div>
                <h2 className="font-display text-2xl font-semibold text-navy">Capacity</h2>
                <p className="mt-1 text-sm text-navy/70">Locked seats versus total capacity.</p>
              </div>
              <div className="text-right text-sm text-navy/70">
                <div>Total Capacity: <span className="font-semibold text-navy">{summary.totalCapacity}</span></div>
                <div>Seats Locked: <span className="font-semibold text-navy">{summary.seatsLocked}</span></div>
                <div>Seats Remaining: <span className="font-semibold text-navy">{summary.seatsRemaining}</span></div>
              </div>
            </div>
            <div className="mt-5 space-y-2">
              <ProgressBar
                percent={summary.totalCapacity ? (summary.seatsLocked / summary.totalCapacity) * 100 : 0}
                color={capacityColor}
              />
              <div className="text-xs text-navy/60">
                {summary.totalCapacity ? `${Math.round((summary.seatsRemaining / summary.totalCapacity) * 100)}% seats remaining` : "No capacity data available"}
              </div>
            </div>
          </section>

          <section className="space-y-4">
            <div>
              <h2 className="font-display text-2xl font-semibold text-navy">Registration Breakdown</h2>
              <p className="mt-1 text-sm text-navy/70">Booking counts by payment state.</p>
            </div>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <StatCard label="Confirmed" value={summary.confirmedCount} accent="bg-green-500" />
              <StatCard label="Pay at Gate" value={summary.payAtGateCount} accent="bg-orange-500" />
              <StatCard label="Free" value={summary.freeCount} accent="bg-blue-500" />
              <StatCard label="Complimentary" value={summary.complimentaryCount} accent="bg-purple-500" />
              <StatCard label="Pending" value={summary.pendingCount} accent="bg-yellow-500" />
              <StatCard label="Failed" value={summary.failedCount} accent="bg-red-500" />
            </div>
            <div className="card px-5 py-4 text-sm font-medium text-navy">
              Total Registrations: {summary.totalRegistrations}
            </div>
          </section>

          <section className="space-y-4">
            <div>
              <h2 className="font-display text-2xl font-semibold text-navy">Check-in Progress</h2>
              <p className="mt-1 text-sm text-navy/70">Bookings and tickets already checked in.</p>
            </div>
            <div className="grid gap-4 md:grid-cols-2">
              <CheckInStat label="Checked In" value={summary.checkedInCount} accent="bg-green-500" />
              <CheckInStat label="Not Checked In" value={summary.notCheckedInCount} accent="bg-red-500" />
            </div>
            <div className="grid gap-4 md:grid-cols-2">
              <CheckInStat label="Checked In Seats" value={summary.checkedInTickets} accent="bg-green-500" />
              <CheckInStat label="Not Checked In Seats" value={summary.notCheckedInTickets} accent="bg-red-500" />
            </div>
            <div className="card p-5">
              <div className="flex items-center justify-between text-sm text-navy/70">
                <span>Checked-in percentage</span>
                <span>{Math.round(checkInPercent)}%</span>
              </div>
              <div className="mt-3">
                <ProgressBar percent={checkInPercent} color="bg-green-500" />
              </div>
            </div>
          </section>

          <section className="space-y-4">
            <div>
              <h2 className="font-display text-2xl font-semibold text-navy">Revenue</h2>
              <p className="mt-1 text-sm text-navy/70">Collected, expected, and waived amounts.</p>
            </div>
            <div className="grid gap-4 md:grid-cols-3">
              <StatCard label="Total Revenue" value={formatAmount(summary.totalRevenue)} accent="bg-green-500" />
              <StatCard label="Pending Gate Collection" value={formatAmount(summary.pendingGateCollection)} accent="bg-orange-500" />
              <StatCard label="Complimentary Waived" value={formatAmount(summary.complimentaryWaived)} accent="bg-purple-500" />
            </div>
          </section>
        </div>
      ) : null}
    </div>
  );
}