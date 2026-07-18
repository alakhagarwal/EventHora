"use client";

export type Session = { role: "ADMIN" | "STAFF"; name: string; email: string } | null;

export function saveSession(token: string, s: NonNullable<Session>) {
  localStorage.setItem("accessToken", token);
  localStorage.setItem("session", JSON.stringify(s));
}

export function getSession(): Session {
  if (typeof window === "undefined") return null;
  try {
    const raw = localStorage.getItem("session");
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function clearSession() {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("session");
}

export type MemberSession = { sessionToken: string; memberId?: string; memberType?: "INDIAN" | "OVERSEAS"; maskedIdentifier?: string } | null;

export function getMemberSession(): MemberSession {
  if (typeof window === "undefined") return null;
  try {
    const raw = localStorage.getItem("memberSession");
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function clearMemberSession() {
  localStorage.removeItem("memberSession");
  localStorage.removeItem("bookingCtx");
}

export function isLoggedIn(): boolean {
  return !!getSession() || !!getMemberSession();
}
