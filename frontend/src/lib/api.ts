import { getToken, clearToken } from "./auth";

export const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

async function request<T>(path: string, opts: RequestInit & { auth?: boolean; formData?: boolean } = {}): Promise<T> {
  const headers: Record<string, string> = {};
  if (!opts.formData) headers["Content-Type"] = "application/json";
  if (opts.auth) {
    const t = getToken();
    if (t) headers["Authorization"] = `Bearer ${t}`;
  }
  const res = await fetch(`${API_BASE}${path}`, { ...opts, headers: { ...headers, ...(opts.headers || {}) } });
  if (res.status === 401 || res.status === 403) {
    if (opts.auth && typeof window !== "undefined") {
      clearToken();
      window.location.href = "/login";
    }
  }
  const text = await res.text();
  const data = text ? (() => { try { return JSON.parse(text); } catch { return text; } })() : null;
  if (!res.ok) {
    const msg = (data && typeof data === "object" && data.error) || `Request failed: ${res.status}`;
    throw new Error(msg);
  }
  return data as T;
}

// Auth
export const authApi = {
  login: (email: string, password: string) => request<{ accessToken: string; role: string; name: string; email: string }>("/api/auth/login", { method: "POST", body: JSON.stringify({ email, password }) }),
  me: () => request<any>("/api/auth/me", { auth: true }),
  users: () => request<any[]>("/api/auth/users", { auth: true }),
  createUser: (payload: any) => request<any>("/api/auth/users", { method: "POST", auth: true, body: JSON.stringify(payload) }),
  deactivateUser: (email: string) => request<any>(`/api/auth/users/${encodeURIComponent(email)}/deactivate`, { method: "PATCH", auth: true }),
};

// Events
export const eventsApi = {
  publicList: () => request<any[]>("/api/events"),
  publicDetail: (link: string) => request<any>(`/api/events/${link}`),
  adminList: () => request<any[]>("/api/admin/events", { auth: true }),
  adminDetail: (id: string) => request<any>(`/api/admin/events/${id}`, { auth: true }),
  create: (payload: any) => request<any>("/api/events", { method: "POST", auth: true, body: JSON.stringify(payload) }),
  update: (id: string, payload: any) => request<any>(`/api/events/${id}`, { method: "PATCH", auth: true, body: JSON.stringify(payload) }),
  publish: (id: string) => request<any>(`/api/events/${id}/publish`, { method: "PATCH", auth: true }),
  cancel: (id: string) => request<any>(`/api/events/${id}`, { method: "DELETE", auth: true }),
  uploadBanner: async (id: string, file: File) => {
    const fd = new FormData(); fd.append("file", file);
    const t = getToken();
    const res = await fetch(`${API_BASE}/api/events/${id}/banner`, { method: "POST", body: fd, headers: t ? { Authorization: `Bearer ${t}` } : {} });
    if (!res.ok) throw new Error("Banner upload failed");
    return res.json();
  },
};

// Member
export const memberApi = {
  verify: (memberId: string, identifier: string, memberType: "INDIAN" | "OVERSEAS") =>
    request<{ sessionToken: string; memberId: string; memberType: string; maskedIdentifier: string }>("/api/registration/verify-member", { method: "POST", body: JSON.stringify({ memberId, identifier, memberType }) }),
  initiate: (sessionToken: string, eventId: string, quantity: number, paymentPreference: "ONLINE" | "AT_GATE") =>
    request<{ message: string; expiresInSeconds: number }>("/api/registration/initiate", { method: "POST", body: JSON.stringify({ sessionToken, eventId, quantity, paymentPreference }) }),
  verifyOtp: (sessionToken: string, otp: string) =>
    request<{ ticketReference: string; eventTitle: string; quantity: number; totalAmount: number; paymentStatus: string }>("/api/registration/verify-otp", { method: "POST", body: JSON.stringify({ sessionToken, otp }) }),
};
