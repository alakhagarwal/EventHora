"use client";

export type Session = { role: "ADMIN" | "STAFF"; name: string; email: string } | null;

export type MemberSession = { sessionToken: string; memberId?: string; memberType?: "INDIAN" | "OVERSEAS"; maskedIdentifier?: string } | null;

const AUTH_EVENT = "eventhora:authchange";

function notifyAuthChange() {
  if (typeof window === "undefined") return;
  window.dispatchEvent(new Event(AUTH_EVENT));
}

function isValidSession(value: any): value is NonNullable<Session> {
  return (
    !!value &&
    typeof value === "object" &&
    (value.role === "ADMIN" || value.role === "STAFF") &&
    typeof value.name === "string" &&
    typeof value.email === "string"
  );
}

function isValidMemberSession(value: any): value is NonNullable<MemberSession> {
  return !!value && typeof value === "object" && typeof value.sessionToken === "string" && value.sessionToken.length > 0;
}

export function saveSession(token: string, s: NonNullable<Session>) {
  localStorage.setItem("accessToken", token);
  localStorage.setItem("session", JSON.stringify(s));
  notifyAuthChange();
}

export function getSession(): Session {
  if (typeof window === "undefined") return null;
  try {
    if (!localStorage.getItem("accessToken")) {
      // Token was deleted/expired — never leave an orphaned session behind.
      localStorage.removeItem("session");
      return null;
    }
    const raw = localStorage.getItem("session");
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    if (!isValidSession(parsed)) {
      localStorage.removeItem("session");
      localStorage.removeItem("accessToken");
      return null;
    }
    return parsed;
  } catch {
    localStorage.removeItem("session");
    return null;
  }
}

export function clearSession() {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("session");
  notifyAuthChange();
}

export function getMemberSession(): MemberSession {
  if (typeof window === "undefined") return null;
  try {
    const raw = localStorage.getItem("memberSession");
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    if (!isValidMemberSession(parsed)) {
      localStorage.removeItem("memberSession");
      localStorage.removeItem("bookingCtx");
      return null;
    }
    return parsed;
  } catch {
    localStorage.removeItem("memberSession");
    return null;
  }
}

export function saveMemberSession(s: NonNullable<MemberSession>) {
  localStorage.setItem("memberSession", JSON.stringify(s));
  notifyAuthChange();
}

export function clearMemberSession() {
  localStorage.removeItem("memberSession");
  localStorage.removeItem("bookingCtx");
  notifyAuthChange();
}

/**
 * Subscribe to auth state changes (same-tab save/clear + cross-tab storage events).
 * Returns an unsubscribe function.
 */
export function onAuthChange(cb: () => void): () => void {
  if (typeof window === "undefined") return () => {};
  const onStorage = (e: StorageEvent) => {
    if (["accessToken", "session", "memberSession", "bookingCtx"].includes(e.key || "")) cb();
  };
  window.addEventListener(AUTH_EVENT, cb);
  window.addEventListener("storage", onStorage);
  return () => {
    window.removeEventListener(AUTH_EVENT, cb);
    window.removeEventListener("storage", onStorage);
  };
}

export function isLoggedIn(): boolean {
  return !!getSession() || !!getMemberSession();
}

export function requireStaffAuth(): Session {
  const session = getSession();
  if (!session || (session.role !== "STAFF" && session.role !== "ADMIN")) {
    return null;
  }
  return session;
}
