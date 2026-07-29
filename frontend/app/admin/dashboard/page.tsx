"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { Calendar, MapPin } from "lucide-react";
import { api, displayStatus } from "@/lib/api";
import { getSession } from "@/lib/auth";
import { toast } from "@/lib/toast";
import SearchInput from "@/components/SearchInput";
import { useDebouncedValue } from "@/lib/useDebouncedValue";

type AdminEvent = {
  id: string;
  title?: string;
  eventDate?: string;
  startTime?: string;
  venue?: string;
  status?: string;
  availableCount?: number;
  totalCapacity?: number;
};

type DashboardStats = {
  totalEvents: number;
  publishedEvents: number;
  upcomingEvents: number;
  draftEvents: number;
  completedEvents: number;
  cancelledEvents: number;
  totalRegistrations: number;
  lockedRegistrations: number;
  totalTicketsSold: number;
  registrationsThisMonth: number;
  ticketsSoldThisMonth: number;
  totalRevenue: number;
  pendingGateCollection: number;
  complimentaryWaived: number;
  revenueThisMonth: number;
};

function formatDateTime(eventDate?: string, startTime?: string) {
  if (!eventDate) return "Date TBD";
  const date = new Date(eventDate);
  const formattedTime = startTime
    ? new Date(`1970-01-01T${startTime}`).toLocaleTimeString("en-US", {
        hour: "numeric",
        minute: "2-digit",
      })
    : date.toLocaleTimeString("en-US", {
        hour: "numeric",
        minute: "2-digit",
      });
  const datePart = date.toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
  return `${datePart} · ${formattedTime}`;
}

function StatusBadge({ status }: { status: string }) {
  const tone =
    status === "PUBLISHED"
      ? "bg-green-100 text-green-800"
      : status === "CANCELLED"
      ? "bg-red-100 text-red-800"
      : status === "COMPLETED"
      ? "bg-slate-100 text-slate-700"
      : "bg-amber-100 text-amber-800";

  return <span className={`chip ${tone}`}>{status}</span>;
}

function formatAmount(amount: number) {
  return `₹${Number(amount || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
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

function StatSkeleton() {
  return (
    <div className="card animate-pulse p-4">
      <div className="h-1.5 w-14 rounded-full bg-navy/10" />
      <div className="mt-3 h-8 w-20 rounded bg-navy/10" />
      <div className="mt-2 h-4 w-28 rounded bg-navy/10" />
    </div>
  );
}

export default function AdminDashboardPage() {
  const router = useRouter();
  const [events, setEvents] = useState<AdminEvent[]>([]);
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState("");
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
    Promise.all([api.dashboardStats(), api.adminEvents()])
      .then(([statsData, eventsData]) => {
        setStats(statsData);
        setEvents(eventsData || []);
      })
      .catch((err) => {
        toast.error(err?.message || "Failed to load events.");
        setStats(null);
        setEvents([]);
      })
      .finally(() => setLoading(false));
  }, [router]);

  const debouncedQuery = useDebouncedValue(query, 300);

  const filteredEvents = useMemo(() => {
    const normalizedQuery = debouncedQuery.trim().toLowerCase();
    return [...events]
      .sort((a, b) => new Date(b.eventDate || 0).getTime() - new Date(a.eventDate || 0).getTime())
      .filter((event) => !normalizedQuery || (event.title || "").toLowerCase().includes(normalizedQuery));
  }, [debouncedQuery, events]);

  if (!authReady) {
    return <div className="mx-auto max-w-7xl px-4 py-12 text-navy/60">Loading...</div>;
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 md:px-6 md:py-12">
      <div className="mb-6 flex flex-col gap-4 md:mb-8 md:flex-row md:items-end md:justify-between">
        <div>
          <div className="eyebrow">Admin</div>
          <h1 className="h1 mt-2">Dashboard</h1>
        </div>

        <SearchInput className="w-full md:max-w-md" value={query} onChange={setQuery} placeholder="Search events by title" />
      </div>

      <div className="space-y-6 mb-8">
        <section className="space-y-3">
          <h2 className="text-sm font-semibold uppercase tracking-[0.2em] text-navy/50">Events overview</h2>
          <div className="grid grid-cols-2 gap-4 lg:grid-cols-3">
            {loading || !stats ? (
              Array.from({ length: 6 }).map((_, index) => <StatSkeleton key={index} />)
            ) : (
              <>
                <StatCard label="Total Events" value={stats.totalEvents} accent="bg-gold" />
                <StatCard label="Published" value={stats.publishedEvents} accent="bg-green-500" />
                <StatCard label="Upcoming" value={stats.upcomingEvents} accent="bg-blue-500" />
                <StatCard label="Draft" value={stats.draftEvents} accent="bg-amber-500" />
                <StatCard label="Completed" value={stats.completedEvents} accent="bg-slate-500" />
                <StatCard label="Cancelled" value={stats.cancelledEvents} accent="bg-red-500" />
              </>
            )}
          </div>
        </section>

        <section className="space-y-3">
          <h2 className="text-sm font-semibold uppercase tracking-[0.2em] text-navy/50">Registrations</h2>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
            {loading || !stats ? (
              Array.from({ length: 3 }).map((_, index) => <StatSkeleton key={index} />)
            ) : (
              <>
                <StatCard label="Total Registrations" value={stats.totalRegistrations} accent="bg-gold" />
                <StatCard label="Locked Registrations" value={stats.lockedRegistrations} accent="bg-orange-500" />
                <StatCard label="Tickets Sold" value={stats.totalTicketsSold} accent="bg-green-500" />
              </>
            )}
          </div>
        </section>

        <section className="space-y-3">
          <h2 className="text-sm font-semibold uppercase tracking-[0.2em] text-navy/50">This month</h2>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            {loading || !stats ? (
              Array.from({ length: 2 }).map((_, index) => <StatSkeleton key={index} />)
            ) : (
              <>
                <StatCard label="Registrations This Month" value={stats.registrationsThisMonth} accent="bg-blue-500" />
                <StatCard label="Tickets Sold This Month" value={stats.ticketsSoldThisMonth} accent="bg-green-500" />
              </>
            )}
          </div>
        </section>

        <section className="space-y-3">
          <h2 className="text-sm font-semibold uppercase tracking-[0.2em] text-navy/50">Revenue</h2>
          <div className="grid grid-cols-2 gap-4 xl:grid-cols-4">
            {loading || !stats ? (
              Array.from({ length: 4 }).map((_, index) => <StatSkeleton key={index} />)
            ) : (
              <>
                <StatCard label="Total Revenue" value={formatAmount(stats.totalRevenue)} accent="bg-green-500" />
                <StatCard label="Pending Gate Collection" value={formatAmount(stats.pendingGateCollection)} accent="bg-orange-500" />
                <StatCard label="Complimentary Waived" value={formatAmount(stats.complimentaryWaived)} accent="bg-purple-500" />
                <StatCard label="Revenue This Month" value={formatAmount(stats.revenueThisMonth)} accent="bg-gold" />
              </>
            )}
          </div>
        </section>
      </div>

      {loading ? (
        <div className="card divide-y divide-navy/10 overflow-hidden">
          {[1, 2, 3, 4].map((item) => (
            <div key={item} className="flex flex-col gap-4 p-5 md:flex-row md:items-center md:justify-between animate-pulse">
              <div className="space-y-3">
                <div className="h-5 w-72 rounded bg-navy/10" />
                <div className="h-4 w-52 rounded bg-navy/10" />
                <div className="h-4 w-40 rounded bg-navy/10" />
              </div>
              <div className="flex gap-3">
                <div className="h-10 w-32 rounded-md bg-navy/10" />
                <div className="h-10 w-36 rounded-md bg-navy/10" />
              </div>
            </div>
          ))}
        </div>
      ) : filteredEvents.length === 0 ? (
        <div className="card p-12 text-center text-navy/60">
          <Calendar className="mx-auto mb-3 h-12 w-12 text-navy/30" />
          <div className="font-display text-lg font-semibold text-navy">No events found</div>
          <p className="mt-1 text-sm">There are no events matching your search.</p>
        </div>
      ) : (
        <div className="card divide-y divide-navy/10 overflow-hidden">
          {filteredEvents.map((event) => (
            <div
              key={event.id}
              className="flex flex-col gap-4 p-5 transition-colors hover:bg-navy/5 md:flex-row md:items-center md:justify-between"
            >
              <div className="min-w-0 flex-1 space-y-2">
                <div className="flex flex-wrap items-center gap-2">
                  <h2 className="min-w-0 font-display text-xl font-semibold text-navy">{event.title || "Untitled event"}</h2>
                  <StatusBadge status={displayStatus(event)} />
                </div>
                <div className="flex flex-wrap items-center gap-4 text-sm text-navy/70">
                  <span className="flex items-center gap-2">
                    <Calendar className="h-4 w-4 text-gold" />
                    {formatDateTime(event.eventDate, event.startTime)}
                  </span>
                  <span className="flex items-center gap-2">
                    <MapPin className="h-4 w-4 text-gold" />
                    {event.venue || "Venue TBD"}
                  </span>
                </div>
                <div className="text-sm text-navy/70">
                  Seats: {event.availableCount ?? 0} / {event.totalCapacity ?? 0}
                </div>
              </div>

              <div className="flex flex-col gap-2 sm:flex-row">
                <Link href={`/admin/events/${event.id}/registrations`} className="btn-outline whitespace-nowrap">
                  Registrations
                </Link>
                <Link href={`/admin/events/${event.id}/summary`} className="btn-dark whitespace-nowrap">
                  Payment Summary
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}