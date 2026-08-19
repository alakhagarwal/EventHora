"use client";
import { useEffect, useRef, useState } from "react";
import { api, type EventMedia } from "@/lib/api";
import { toast } from "@/lib/toast";
import { Upload, Play, X, ChevronUp, ChevronDown } from "lucide-react";

export const EVENT_CATEGORIES = ["MUSIC", "DANCE", "CULTURAL", "EDUCATIONAL", "SOCIAL", "SPORTS", "OTHER"] as const;

export type EventFormValues = {
  title: string; description: string; category: string;
  eventDate: string; startTime: string; endTime: string;
  registrationDeadline: string;
  venue: string; additionalVenueInfo: string;
  totalCapacity: number;
  maxMemberTickets: number; freeMemberTickets: number; memberTicketPrice: number;
  maxGuestTickets: number; freeGuestTickets: number; guestTicketPrice: number;
  platformFeePerTicket: number;
  minimumAge: number | null;
  importantNotes: string[];
  contactPersonName: string; contactPersonPhone: string;
};

export type EventFormMeta = {
  bannerUrl?: string | null;
  media?: EventMedia[];
};

const empty: EventFormValues = {
  title: "", description: "", category: "MUSIC",
  eventDate: "", startTime: "18:00:00", endTime: "20:00:00",
  registrationDeadline: "",
  venue: "", additionalVenueInfo: "",
  totalCapacity: 100,
  maxMemberTickets: 4, freeMemberTickets: 0, memberTicketPrice: 0,
  maxGuestTickets: 0, freeGuestTickets: 0, guestTicketPrice: 0,
  platformFeePerTicket: 0,
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
  const [currentId, setCurrentId] = useState<string | undefined>(eventId);
  const [bannerUrl, setBannerUrl] = useState<string | null>(initial?.bannerUrl ?? null);
  const [pendingBanner, setPendingBanner] = useState<File | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  // Media state
  const [media, setMedia] = useState<EventMedia[]>(initial?.media ?? []);
  const [pendingPhoto, setPendingPhoto] = useState<{ file: File; previewUrl: string } | null>(null);
  const [stagedVideoUrl, setStagedVideoUrl] = useState("");
  const [stagedCaption, setStagedCaption] = useState("");
  const mediaFileRef = useRef<HTMLInputElement>(null);
  const loadedRef = useRef(false);
  const blobUrlRef = useRef<string | null>(null);

  useEffect(() => {
    if (initial && !loadedRef.current) {
      loadedRef.current = true;
      setValues((v) => ({ ...v, ...initial }));
      if (initial.bannerUrl) setBannerUrl(initial.bannerUrl);
      if (initial.media) setMedia(initial.media);
    }
  }, [initial]);

  const set = <K extends keyof EventFormValues>(k: K, v: EventFormValues[K]) => setValues((s) => ({ ...s, [k]: v }));

  const buildPayload = () => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { bannerUrl: _banner, ...rest } = values as any;
    return {
      ...rest,
      minimumAge: values.minimumAge === null || Number.isNaN(values.minimumAge) ? null : Number(values.minimumAge),
    };
  };

  const doAction = async (fn: () => Promise<any>, label: string) => {
    setBusy(label);
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
    setBusy("Publish");
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
      setBusy("Cancel");
      try {
        await api.cancelEvent(currentId);
        toast.success("Event cancelled.");
        onPublished?.(null);
      } catch (e: any) {
        toast.error(e.message || "Cancel failed");
      } finally {
        setBusy(null);
      }
    })();

  const handleFileSelect = (file: File) => {
    if (blobUrlRef.current) URL.revokeObjectURL(blobUrlRef.current);
    const blobUrl = URL.createObjectURL(file);
    blobUrlRef.current = blobUrl;
    setBannerUrl(blobUrl);
    if (currentId) {
      doBannerUpload(currentId, file);
    } else {
      setPendingBanner(file);
    }
    if (fileRef.current) fileRef.current.value = "";
  };

  // ── Media handlers ──

  const handlePhotoSelect = (file: File) => {
    if (pendingPhoto) URL.revokeObjectURL(pendingPhoto.previewUrl);
    const previewUrl = URL.createObjectURL(file);
    setPendingPhoto({ file, previewUrl });
    setStagedCaption("");
    if (mediaFileRef.current) mediaFileRef.current.value = "";
  };

  const handlePhotoUpload = async () => {
    if (!currentId || !pendingPhoto) return;
    setBusy("Photo upload");
    try {
      const r = await api.uploadEventPhoto(currentId, pendingPhoto.file, stagedCaption, media.length);
      setMedia(r.media ?? []);
      setStagedCaption("");
      URL.revokeObjectURL(pendingPhoto.previewUrl);
      setPendingPhoto(null);
      toast.success("Photo uploaded.");
    } catch (e: any) {
      toast.error(e.message || "Photo upload failed");
    } finally {
      setBusy(null);
    }
  };

  const handlePhotoCancel = () => {
    if (pendingPhoto) URL.revokeObjectURL(pendingPhoto.previewUrl);
    setPendingPhoto(null);
    setStagedCaption("");
  };

  const handleVideoPreview = () => {
    const url = stagedVideoUrl.trim();
    if (!url) return;
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
      toast.error("Video URL must start with http:// or https://");
      return;
    }
    setStagedVideoUrl(url);
  };

  const handleVideoAdd = async () => {
    if (!currentId || !stagedVideoUrl.trim()) return;
    const url = stagedVideoUrl.trim();
    setBusy("Add video");
    try {
      const r = await api.addEventVideo(currentId, url, stagedCaption, media.length);
      setMedia(r.media ?? []);
      setStagedCaption("");
      setStagedVideoUrl("");
      toast.success("Video added.");
    } catch (e: any) {
      toast.error(e.message || "Failed to add video");
    } finally {
      setBusy(null);
    }
  };

  const handleVideoCancel = () => {
    setStagedVideoUrl("");
    setStagedCaption("");
  };

  const handleDeleteMedia = async (mediaId: string) => {
    if (!currentId || !confirm("Delete this media item?")) return;
    setBusy("Delete");
    try {
      await api.deleteEventMedia(currentId, mediaId);
      setMedia((prev) => prev.filter((m) => m.id !== mediaId));
      toast.success("Deleted.");
    } catch (e: any) {
      toast.error(e.message || "Delete failed");
    } finally {
      setBusy(null);
    }
  };

  const handleReorderMedia = async (fromIdx: number, toIdx: number) => {
    if (!currentId) return;
    const reordered = [...media];
    const [moved] = reordered.splice(fromIdx, 1);
    reordered.splice(toIdx, 0, moved);
    const orderedIds = reordered.map((m) => m.id);
    try {
      const r = await api.reorderEventMedia(currentId, orderedIds);
      setMedia(r.media ?? []);
    } catch (e: any) {
      toast.error(e.message || "Reorder failed");
    }
  };

  return (
    <div className="space-y-8">
      <div className="card-mobile overflow-hidden">
        <div className="divide-y divide-navy/10 md:divide-y-0 md:space-y-8">
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
            <Grid>
              <Field label="Total Capacity"><input type="number" min="1" className="input" value={values.totalCapacity} onChange={(e) => set("totalCapacity", Number(e.target.value))} /></Field>
              <Field label="Minimum Age"><input type="number" min="0" className="input" value={values.minimumAge ?? ""} onChange={(e) => set("minimumAge", e.target.value === "" ? null : Number(e.target.value))} /></Field>
              <Field label="Platform Fee / Ticket"><input type="number" step="0.01" min="0" className="input" value={values.platformFeePerTicket} onChange={(e) => set("platformFeePerTicket", Number(e.target.value))} /></Field>
            </Grid>

            <div className="mt-4 grid gap-4 md:grid-cols-2">
              <div className="rounded-lg border border-navy/10 p-4 space-y-3">
                <div className="text-xs font-semibold uppercase tracking-wider text-navy/50">Member tickets</div>
                <Grid cols={3}>
                  <Field label="Max"><input type="number" min="1" className="input" value={values.maxMemberTickets} onChange={(e) => set("maxMemberTickets", Number(e.target.value))} /></Field>
                  <Field label="Free"><input type="number" min="0" className="input" value={values.freeMemberTickets} onChange={(e) => set("freeMemberTickets", Number(e.target.value))} /></Field>
                  <Field label="Price"><input type="number" step="0.01" min="0" className="input" value={values.memberTicketPrice} onChange={(e) => set("memberTicketPrice", Number(e.target.value))} /></Field>
                </Grid>
              </div>
              <div className="rounded-lg border border-navy/10 p-4 space-y-3">
                <div className="text-xs font-semibold uppercase tracking-wider text-navy/50">Guest tickets</div>
                <Grid cols={3}>
                  <Field label="Max"><input type="number" min="0" className="input" value={values.maxGuestTickets} onChange={(e) => set("maxGuestTickets", Number(e.target.value))} /></Field>
                  <Field label="Free"><input type="number" min="0" className="input" value={values.freeGuestTickets} onChange={(e) => set("freeGuestTickets", Number(e.target.value))} /></Field>
                  <Field label="Price"><input type="number" step="0.01" min="0" className="input" value={values.guestTicketPrice} onChange={(e) => set("guestTicketPrice", Number(e.target.value))} /></Field>
                </Grid>
                <p className="text-xs text-navy/40">Set Max to 0 to disallow guest tickets.</p>
              </div>
            </div>
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

          <Section title="Media gallery">
            {!currentId ? (
              <p className="text-sm text-navy/50">Save the event before adding media.</p>
            ) : (
              <div className="space-y-5">
                {/* Photo staging */}
                <div>
                  <div className="text-xs font-semibold uppercase tracking-wider text-navy/50 mb-2">Upload photo</div>
                  {pendingPhoto ? (
                    <div className="rounded-xl border border-navy/15 bg-navy/5 p-4 space-y-3">
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img src={pendingPhoto.previewUrl} alt="Preview" className="max-h-48 w-full rounded-lg object-cover" />
                      <Field label="Caption (optional)">
                        <input className="input" placeholder="Add a caption…" value={stagedCaption} onChange={(e) => setStagedCaption(e.target.value)} />
                      </Field>
                      <div className="flex gap-2">
                        <button type="button" className="btn-dark" disabled={busy !== null} onClick={handlePhotoUpload}>
                          {busy === "Photo upload" ? "Uploading…" : "Upload Photo"}
                        </button>
                        <button type="button" className="btn-outline" onClick={handlePhotoCancel}>Cancel</button>
                      </div>
                    </div>
                  ) : (
                    <div
                      className="relative flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-navy/20 bg-navy/5 p-4 transition-colors hover:border-gold/50 hover:bg-gold/5"
                      onClick={() => mediaFileRef.current?.click()}
                    >
                      <Upload className="mb-1 h-6 w-6 text-navy/30" />
                      <p className="text-xs font-medium text-navy/60">Click to upload</p>
                      <p className="text-[11px] text-navy/40">Image only</p>
                      <input
                        ref={mediaFileRef}
                        type="file"
                        accept="image/*"
                        className="hidden"
                        disabled={busy !== null}
                        onChange={(e) => { if (e.target.files?.[0]) handlePhotoSelect(e.target.files[0]); }}
                      />
                    </div>
                  )}
                </div>

                {/* Video staging */}
                <div>
                  <div className="text-xs font-semibold uppercase tracking-wider text-navy/50 mb-2">Add video</div>
                  {stagedVideoUrl ? (
                    <div className="rounded-xl border border-navy/15 bg-navy/5 p-4 space-y-3">
                      <div className="relative aspect-video rounded-lg overflow-hidden bg-navy/5">
                        {/* eslint-disable-next-line @next/next/no-img-element */}
                        <img
                          src={stagedVideoUrl.includes("youtube.com") || stagedVideoUrl.includes("youtu.be")
                            ? `https://img.youtube.com/vi/${(stagedVideoUrl.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/)([^&?#]+)/) || [])[1] || ""}/hqdefault.jpg`
                            : stagedVideoUrl}
                          alt="Video preview"
                          className="w-full h-full object-cover"
                          onError={(e) => { (e.target as HTMLImageElement).style.display = "none"; }}
                        />
                        <div className="absolute inset-0 flex items-center justify-center bg-black/30">
                          <Play className="h-10 w-10 text-white" />
                        </div>
                      </div>
                      <Field label="Caption (optional)">
                        <input className="input" placeholder="Add a caption…" value={stagedCaption} onChange={(e) => setStagedCaption(e.target.value)} />
                      </Field>
                      <div className="flex gap-2">
                        <button type="button" className="btn-dark" disabled={busy !== null || !stagedVideoUrl.trim()} onClick={handleVideoAdd}>
                          {busy === "Add video" ? "Adding…" : "Add Video"}
                        </button>
                        <button type="button" className="btn-outline" onClick={handleVideoCancel}>Cancel</button>
                      </div>
                    </div>
                  ) : (
                    <div className="flex gap-2">
                      <input className="input flex-1" placeholder="https://youtube.com/watch?v=..." value={stagedVideoUrl} onChange={(e) => setStagedVideoUrl(e.target.value)} onKeyDown={(e) => { if (e.key === "Enter") { e.preventDefault(); handleVideoPreview(); } }} />
                      <button type="button" className="btn-outline" disabled={!stagedVideoUrl.trim()} onClick={handleVideoPreview}>Preview</button>
                    </div>
                  )}
                </div>

                {/* Thumbnails */}
                {media.length > 0 && (
                  <div>
                    <div className="text-xs font-semibold uppercase tracking-wider text-navy/50 mb-2">Gallery ({media.length} items)</div>
                    <div className="flex flex-wrap gap-3">
                      {media.map((item, idx) => (
                        <div key={item.id} className="relative group w-28">
                          <div className="relative w-28 h-20 rounded overflow-hidden border border-navy/10">
                            {item.mediaType === "PHOTO" ? (
                              // eslint-disable-next-line @next/next/no-img-element
                              <img src={item.url} alt={item.caption || ""} className="w-full h-full object-cover" />
                            ) : (
                              <>
                                {/* eslint-disable-next-line @next/next/no-img-element */}
                                <img src={item.url} alt={item.caption || "Video"} className="w-full h-full object-cover" onError={(e) => { (e.target as HTMLImageElement).src = "data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 60'><rect fill='%2311193D' width='100' height='60'/><text x='50' y='35' fill='white' font-size='14' text-anchor='middle'>Video</text></svg>"; }} />
                                <div className="absolute inset-0 flex items-center justify-center bg-black/30">
                                  <Play className="h-6 w-6 text-white" />
                                </div>
                              </>
                            )}
                          </div>
                          {item.caption && (
                            <p className="mt-0.5 text-[10px] text-navy/60 truncate">{item.caption}</p>
                          )}
                          <div className="flex items-center justify-between mt-0.5">
                            <div className="flex gap-0.5">
                              <button
                                type="button"
                                disabled={idx === 0}
                                className="p-0.5 text-navy/40 hover:text-navy disabled:opacity-30"
                                onClick={() => handleReorderMedia(idx, idx - 1)}
                              >
                                <ChevronUp className="h-3 w-3" />
                              </button>
                              <button
                                type="button"
                                disabled={idx === media.length - 1}
                                className="p-0.5 text-navy/40 hover:text-navy disabled:opacity-30"
                                onClick={() => handleReorderMedia(idx, idx + 1)}
                              >
                                <ChevronDown className="h-3 w-3" />
                              </button>
                            </div>
                            <button
                              type="button"
                              className="p-0.5 text-red-500 hover:text-red-700"
                              onClick={() => handleDeleteMedia(item.id)}
                            >
                              <X className="h-3 w-3" />
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}
          </Section>
        </div>
      </div>

      <div className="flex flex-wrap gap-2 sticky bottom-0 bg-cream/90 backdrop-blur border-t border-navy/10 p-3 -mx-3">
        {!currentId ? (
          <button className="btn-dark" disabled={busy !== null} onClick={createDraft}>
            {busy === "Create draft" ? "Creating…" : "Create Draft"}
          </button>
        ) : (
          <>
            <button className="btn-dark" disabled={busy !== null} onClick={saveDraft}>
              {busy === "Save"
                ? "Saving…"
                : eventStatus === "DRAFT"
                ? "Save Draft"
                : "Save Changes"}
            </button>

            {(!eventStatus || eventStatus === "DRAFT") && (
              <button className="btn-primary" disabled={busy !== null} onClick={publish}>
                {busy === "Publish" ? "Publishing…" : "Publish"}
              </button>
            )}

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
    <div className="card-md p-5">
      <h3 className="font-display text-lg text-navy mb-3">{title}</h3>
      {children}
    </div>
  );
}
function Grid({ cols = 2, children }: { cols?: number; children: React.ReactNode }) {
  const map: Record<number, string> = { 2: "md:grid-cols-2", 3: "md:grid-cols-3", 4: "md:grid-cols-4" };
  return <div className={`grid gap-3 ${map[cols] || "md:grid-cols-2"}`}>{children}</div>;
}
function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return <div><label className="label">{label}</label>{children}</div>;
}
