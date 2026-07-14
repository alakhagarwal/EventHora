export type Role = "ADMIN" | "STAFF";
export interface JwtPayload { sub: string; role: Role; userId: string; name: string; exp: number; }

export function setToken(t: string) { if (typeof window !== "undefined") localStorage.setItem("accessToken", t); }
export function getToken(): string | null { if (typeof window === "undefined") return null; return localStorage.getItem("accessToken"); }
export function clearToken() { if (typeof window !== "undefined") { localStorage.removeItem("accessToken"); localStorage.removeItem("userRole"); localStorage.removeItem("userName"); } }
export function setUser(role: string, name: string) { if (typeof window !== "undefined") { localStorage.setItem("userRole", role); localStorage.setItem("userName", name); } }
export function getRole(): Role | null { if (typeof window === "undefined") return null; return (localStorage.getItem("userRole") as Role) || null; }
export function getName(): string | null { if (typeof window === "undefined") return null; return localStorage.getItem("userName"); }

export function decodeJwt(token: string): JwtPayload | null {
  try {
    const b = token.split(".")[1];
    const json = atob(b.replace(/-/g, "+").replace(/_/g, "/"));
    return JSON.parse(json);
  } catch { return null; }
}

export function setSessionToken(t: string) { if (typeof window !== "undefined") localStorage.setItem("memberSessionToken", t); }
export function getSessionToken(): string | null { if (typeof window === "undefined") return null; return localStorage.getItem("memberSessionToken"); }
