export const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

type Options = RequestInit & { auth?: boolean; json?: any };

function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("accessToken");
}

export async function apiFetch<T = any>(path: string, opts: Options = {}): Promise<T> {
  const headers: Record<string, string> = {
    ...(opts.headers as Record<string, string> | undefined),
  };
  if (opts.json !== undefined) {
    headers["Content-Type"] = "application/json";
    opts.body = JSON.stringify(opts.json);
  }
  if (opts.auth !== false) {
    const t = getToken();
    if (t) headers["Authorization"] = `Bearer ${t}`;
  }
  const res = await fetch(`${API_BASE}${path}`, { ...opts, headers, cache: "no-store" });
  if (res.status === 401 || res.status === 403) {
    if (typeof window !== "undefined" && opts.auth !== false) {
      localStorage.removeItem("accessToken");
      if (!window.location.pathname.startsWith("/login")) window.location.href = "/login";
    }
  }
  const text = await res.text();
  const data = text ? (() => { try { return JSON.parse(text); } catch { return text; } })() : null;
  if (!res.ok) {
    const msg = (data && typeof data === "object" && data.error) || res.statusText || "Request failed";
    throw new Error(msg);
  }
  return data as T;
}

// Domain shortcuts
export const api = {
  login: (email: string, password: string) =>
    apiFetch<{ accessToken: string; role: "ADMIN" | "STAFF"; name: string; email: string }>(
      "/api/auth/login",
      { method: "POST", json: { email, password }, auth: false }
    ),
  me: () => apiFetch("/api/auth/me"),
  users: () => apiFetch<any[]>("/api/auth/users"),
  createUser: (body: { name: string; email: string; password: string; role: "ADMIN" | "STAFF" }) =>
    apiFetch("/api/auth/users", { method: "POST", json: body }),
  deactivateUser: (email: string) =>
    apiFetch(`/api/auth/users/${encodeURIComponent(email)}/deactivate`, { method: "PATCH" }),

  publicEvents: () => apiFetch<any[]>("/api/events", { auth: false }),
  publicEvent: (link: string) => apiFetch<any>(`/api/events/${link}`, { auth: false }),

  adminEvents: () => apiFetch<any[]>("/api/admin/events"),
  adminEvent: (id: string) => apiFetch<any>(`/api/admin/events/${id}`),
  createEvent: (body: any) => apiFetch("/api/events", { method: "POST", json: body }),
  updateEvent: (id: string, body: any) =>
    apiFetch(`/api/events/${id}`, { method: "PATCH", json: body }),
  publishEvent: (id: string) => apiFetch(`/api/events/${id}/publish`, { method: "PATCH" }),
  cancelEvent: (id: string) => apiFetch(`/api/events/${id}`, { method: "DELETE" }),
  uploadBanner: async (id: string, file: File) => {
    const fd = new FormData();
    fd.append("file", file);
    const t = getToken();
    const res = await fetch(`${API_BASE}/api/events/${id}/banner`, {
      method: "POST",
      body: fd,
      headers: t ? { Authorization: `Bearer ${t}` } : undefined,
    });
    if (!res.ok) throw new Error("Banner upload failed");
    return res.json();
  },

  verifyMember: (body: { memberId: string; identifier: string; memberType: "INDIAN" | "OVERSEAS" }) =>
    apiFetch("/api/registration/verify-member", { method: "POST", json: body, auth: false }),
  initiateBooking: (body: {
    sessionToken: string;
    eventId: string;
    quantity: number;
    paymentPreference: "ONLINE" | "AT_GATE";
  }) => apiFetch("/api/registration/initiate", { method: "POST", json: body, auth: false }),
};
