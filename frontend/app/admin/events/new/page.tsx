"use client";
import EventForm from "@/components/EventForm";
import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { getSession } from "@/lib/auth";

export default function NewEvent() {
  const router = useRouter();
  useEffect(() => { const s = getSession(); if (!s || s.role !== "ADMIN") router.push("/login"); }, [router]);
  return (
    <div className="mx-auto max-w-5xl px-6 py-12">
      <div className="eyebrow">Admin</div>
      <h1 className="h1 mt-2 mb-8">Create Event</h1>
      <EventForm onSaved={(r) => { if (r?.id) router.push(`/admin/events/${r.id}`); }} />
    </div>
  );
}
