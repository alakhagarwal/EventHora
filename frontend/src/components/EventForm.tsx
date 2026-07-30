"use client";
import { useEffect, useRef, useState } from "react";
import { api } from "@/lib/api";
import { toast } from "@/lib/toast";
import { Upload } from "lucide-react";

export const EVENT_CATEGORIES = ["MUSIC", "DANCE", "CULTURAL", "EDUCATIONAL", "SOCIAL", "SPORTS", "OTHER"] as const;

export type EventFormValues = {
  title: string; description: string; category: string;
  eventDate: string; startTime: string; endTime: string;
  registrationDeadline: string;
  venue: string; additionalVenueInfo: string;
  totalCapacity: number; maxTicketsPerMember: number; freeTicketsPerRegistration: number;
  ticketPrice: number; platformFeePerTicket: number;
  minimumAge: number | null;
  importantNotes: string[];
  contactPersonName: string; contactPersonPhone: string;
};

export type EventFormMeta = {
  bannerUrl?: string | null;
};

const empty: EventFormValues = {
  title: "", description: "", category: "MUSIC",
  eventDate: "", startTime: "18:00:00", endTime: "20:00:00",
  registrationDeadline: "",
  venue: "", additionalVenueInfo: "",
  totalCapacity: 100, maxTicketsPerMember: 4, freeTicketsPerRegistration: 0,
  ticketPrice: 0, platformFeePerTicket: 0,
  minimumAge: null,
  importantNotes: [],
  contactPersonName: "", contactPersonPhone: "",
};

export default function EventForm({
  eventId,
  initial,
  eventStatus,
  onSaved,
  onPublished,
}: {
  eventId?: string;
  initial?: Partial<EventFormValues> & EventFormMeta;
  eventStatus?: "DRAFT" | "PUBLISHED" | "CANCELLED" | "COMPLETED";
  onSaved?: (ev: any) => void;
  onPublished?: (ev: any) => void;
}) {
  const [values, setValues] = useState<EventFormValues>({ ...empty, ...initial });
  const [noteInput, setNoteInput] = useState("");
  const [busy, setBusy] = useState<string | null>(null);
  const [msg, setMsg] = useState<{ kind: "ok" | "err"; text: string } | null>(null);
  const [currentId, setCurrentId] = useState<string | undefined>(eventId);
  const [bannerUrl, setBannerUrl] = useState<string | null>(initial?.bannerUrl ?? null);
  const [pendingBanner, setPendingBanner] = useState<File | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  useEffect(() => { if (initial) { setValues((v) => ({ ...v, ...initial })); if (initial.bannerUrl) setBannerUrl(initial.bannerUrl); } }, [initial]);

  const set = <K extends keyof EventFormValues>(k: K, v: EventFormValues[K]) => setValues((s) => ({ ...s, [k]: v }));

  const buildPayload = () => {
    // Exclude bannerUrl — it's a pre-signed S3 URL from the API response and must NOT
    // be sent back in the PATCH body. The backend would store the temporary URL in the DB,
    // breaking the banner after 7 days. Banner changes use POST /api/events/{id}/banner.
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { bannerUrl: _banner, ...rest } = values as any;
    return {
      ...rest,
      minimumAge: values.minimumAge === null || Number.isNaN(values.minimumAge) ? null : Number(values.minimumAge),
    };
  };

  const doAction = async (fn: () => Promise<any>, label: string) => {
    setBusy(label); setMsg(null);
    try { const r = await fn(); toast.success(`${label} successful.`); onSaved?.(r); return r; }
    catch (e: any) { toast.error(e.message || "Action failed"); }
    finally { setBusy(null); }
  };

  const doBannerUpload = async (id: string, file: File) => {
    setBusy("Upload");
    try {
      const r = await api.uploadBanner(id, file);
      setBannerUrl(r.bannerUrl);
      onSaved?.(r);
      return r;
    } catch (e: any) {
      toast.error(e.message || "Banner upload failed");
      throw e;
    } finally {
      setBusy(null);
    }
  };

  const createDraft = () => doAction(async () => {
    const r: any = await api.createEvent(buildPayload());
    if (r?.id) {
      setCurrentId(r.id);
      if (r.bannerUrl) setBannerUrl(r.bannerUrl);
      if (pendingBanner) {
        await doBannerUpload(r.id, pendingBanner);
        setPendingBanner(null);
      }
    }
    return r;
  }, "Create draft");

  const saveDraft = () => currentId && doAction(() => api.updateEvent(currentId, buildPayload()), "Save");
  const publish = async () => {
    if (!currentId) return;
    setBusy("Publish"); setMsg(null);
    try {
      const r = await api.publishEvent(currentId);
      toast.success("Publish successful.");
      onPublished?.(r);
    } catch (e: any) {
      toast.error(e.message || "Action failed");
    } finally {
      setBusy(null);
    }
  };
  const cancel = () =>
    currentId &&
    confirm("Cancel this event? This cannot be undone.") &&
    (async () => {
      setBusy("Cancel"); setMsg(null);
      try {
        await api.cancelEvent(currentId);
        toast.success("Event cancelled.");
        onPublished?.(null); // reuse navigation hook — parent routes to /admin/my-events
      } catch (e: any) {
        toast.error(e.message || "Cancel failed");
      } finally {
        setBusy(null);
      }
    })();

  const handleFileSelect = (file: File) => {
    if (currentId) {
      doBannerUpload(currentId, file);
    } else {
      setPendingBanner(file);
      setBannerUrl(URL.createObjectURL(file));
    }
    if (fileRef.current) fileRef.current.value = "";
  };

  return (
    <div className="space-y-8">
      <Section title="Basic details">
        <Grid>
          <Field label="Title"><input className="input" value={values.title} onChange={(e) => set("title", e.target.value)} /></Field>
          <Field label="Category">
            <select className="input" value={values.category} onChange={(e) => set("category", e.target.value)}>
              {EVENT_CATEGORIES.map((c) => <option key={c}>{c}</option>)}
            </select>
          </Field>
        </Grid>
        <Field label="Description"><textarea rows={5} className="input" value={values.description} onChange={(e) => set("description", e.target.value)} /></Field>
      </Section>

      <Section title="Banner">
        <div
          className="relative flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-navy/20 bg-navy/5 p-6 transition-colors hover:border-gold/50 hover:bg-gold/5"
          onClick={() => fileRef.current?.click()}
        >
          {bannerUrl ? (
            <>
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={bannerUrl} alt="Banner preview" className="max-h-48 w-full rounded-lg object-cover" />
              <button
                type="button"
                className="btn-outline mt-3 text-xs"
                onClick={(e) => { e.stopPropagation(); fileRef.current?.click(); }}
              >
                Change banner
              </button>
            </>
          ) : (
            <>
              <Upload className="mb-2 h-8 w-8 text-navy/30" />
              <p className="text-sm font-medium text-navy/60">Click to upload a banner image</p>
              <p className="mt-1 text-xs text-navy/40">
                {pendingBanner ? `${pendingBanner.name} (pending — will upload on save)` : "Recommended: 1200×600px"}
              </p>
            </>
          )}
          <input
            ref={fileRef}
            type="file"
            accept="image/*"
            className="hidden"
            onChange={(e) => e.target.files?.[0] && handleFileSelect(e.target.files[0])}
          />
        </div>
      </Section>

      <Section title="Schedule">
        <Grid cols={4}>
          <Field label="Event Date"><input type="date" className="input" value={values.eventDate} onChange={(e) => set("eventDate", e.target.value)} /></Field>
          <Field label="Start Time"><input type="time" step={1} className="input" value={values.startTime.slice(0,8)} onChange={(e) => set("startTime", (e.target.value.length === 5 ? e.target.value + ":00" : e.target.value))} /></Field>
          <Field label="End Time"><input type="time" step={1} className="input" value={values.endTime.slice(0,8)} onChange={(e) => set("endTime", (e.target.value.length === 5 ? e.target.value + ":00" : e.target.value))} /></Field>
          <Field label="Registration Deadline"><input type="datetime-local" className="input" value={values.registrationDeadline?.slice(0,16)} onChange={(e) => set("registrationDeadline", e.target.value.length === 16 ? e.target.value + ":00" : e.target.value)} /></Field>
        </Grid>
      </Section>

      <Section title="Venue">
        <Grid>
          <Field label="Venue"><input className="input" value={values.venue} onChange={(e) => set("venue", e.target.value)} /></Field>
          <Field label="Additional Venue Info"><input className="input" value={values.additionalVenueInfo} onChange={(e) => set("additionalVenueInfo", e.target.value)} /></Field>
        </Grid>
      </Section>

      <Section title="Capacity & pricing">
        <Grid cols={4}>
          <Field label="Total Capacity"><input type="number" className="input" value={values.totalCapacity} onChange={(e) => set("totalCapacity", Number(e.target.value))} /></Field>
          <Field label="Max Tickets / Member"><input type="number" className="input" value={values.maxTicketsPerMember} onChange={(e) => set("maxTicketsPerMember", Number(e.target.value))} /></Field>
          <Field label="Free Tickets"><input type="number" className="input" value={values.freeTicketsPerRegistration} onChange={(e) => set("freeTicketsPerRegistration", Number(e.target.value))} /></Field>
          <Field label="Minimum Age"><input type="number" className="input" value={values.minimumAge ?? ""} onChange={(e) => set("minimumAge", e.target.value === "" ? null : Number(e.target.value))} /></Field>
          <Field label="Ticket Price"><input type="number" step="0.01" className="input" value={values.ticketPrice} onChange={(e) => set("ticketPrice", Number(e.target.value))} /></Field>
          <Field label="Platform Fee / Ticket"><input type="number" step="0.01" className="input" value={values.platformFeePerTicket} onChange={(e) => set("platformFeePerTicket", Number(e.target.value))} /></Field>
        </Grid>
      </Section>

      <Section title="Important notes">
        <div className="flex gap-2">
          <input className="input" placeholder="Add a note…" value={noteInput} onChange={(e) => setNoteInput(e.target.value)} />
          <button type="button" className="btn-outline" onClick={() => { if (noteInput.trim()) { set("importantNotes", [...values.importantNotes, noteInput.trim()]); setNoteInput(""); } }}>Add</button>
        </div>
        <ul className="mt-3 space-y-1">
          {values.importantNotes.map((n, i) => (
            <li key={i} className="flex items-center justify-between rounded bg-navy/5 px-3 py-1 text-sm">
              <span>• {n}</span>
              <button className="text-xs text-red-600" onClick={() => set("importantNotes", values.importantNotes.filter((_, j) => j !== i))}>Remove</button>
            </li>
          ))}
        </ul>
      </Section>

      <Section title="Contact">
        <Grid>
          <Field label="Contact Person"><input className="input" value={values.contactPersonName} onChange={(e) => set("contactPersonName", e.target.value)} /></Field>
          <Field label="Contact Phone"><input className="input" value={values.contactPersonPhone} onChange={(e) => set("contactPersonPhone", e.target.value)} /></Field>
        </Grid>
      </Section>

      <div className="flex flex-wrap gap-2 sticky bottom-0 bg-cream/90 backdrop-blur border-t border-navy/10 p-3 -mx-4">
        {!currentId ? (
          // New event — not yet saved
          <button className="btn-dark" disabled={busy !== null} onClick={createDraft}>
            {busy === "Create draft" ? "Creating…" : "Create Draft"}
          </button>
        ) : (
          <>
            {/* Save — label changes based on current status */}
            <button className="btn-dark" disabled={busy !== null} onClick={saveDraft}>
              {busy === "Save"
                ? "Saving…"
                : eventStatus === "DRAFT"
                ? "Save Draft"
                : "Save Changes"}
            </button>

            {/* Publish — only shown for DRAFT events */}
            {(!eventStatus || eventStatus === "DRAFT") && (
              <button className="btn-primary" disabled={busy !== null} onClick={publish}>
                {busy === "Publish" ? "Publishing…" : "Publish"}
              </button>
            )}

            {/* Cancel — only shown if not already cancelled or completed */}
            {(!eventStatus || (eventStatus !== "CANCELLED" && eventStatus !== "COMPLETED")) && (
              <button
                className="btn-outline text-red-700 border-red-200 hover:bg-red-50"
                disabled={busy !== null}
                onClick={cancel}
              >
                {busy === "Cancel" ? "Cancelling…" : "Cancel Event"}
              </button>
            )}
          </>
        )}
      </div>
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="card p-6">
      <h3 className="font-display text-lg text-navy mb-4">{title}</h3>
      {children}
    </div>
  );
}
function Grid({ cols = 2, children }: { cols?: number; children: React.ReactNode }) {
  const map: Record<number, string> = { 2: "md:grid-cols-2", 3: "md:grid-cols-3", 4: "md:grid-cols-4" };
  return <div className={`grid gap-4 ${map[cols] || "md:grid-cols-2"}`}>{children}</div>;
}
function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return <div><label className="label">{label}</label>{children}</div>;
}
