"use client";
import EventForm from "@/components/EventForm";
import { useRouter } from "next/navigation";
import { useRequireAuth } from "@/lib/useRequireAuth";

const ADMIN_ONLY = ["ADMIN"] as const;

export default function NewEvent() {
  const session = useRequireAuth(ADMIN_ONLY);
  const router = useRouter();

  if (!session) return null;

  return (
    <div className="mx-auto max-w-5xl px-3 py-12 md:px-6">
      <div className="eyebrow">Admin</div>
      <h1 className="h1 mt-2 mb-8">Create Event</h1>
      <EventForm
        onSaved={(r) => { if (r?.id) router.push(`/admin/events/${r.id}`); }}
        onPublished={() => router.push("/admin/my-events")}
      />
    </div>
  );
}
