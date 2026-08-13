"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getSession, type Session } from "@/lib/auth";

type Role = NonNullable<Session>["role"];

/**
 * Client-side route guard. Returns the session while the current user is
 * authorized, otherwise returns null (and redirects to /login).
 * Renders nothing until the check runs so protected content never flashes.
 *
 * Pass a stable (module-level) roles array to avoid re-running the check on
 * every render.
 */
export function useRequireAuth(roles?: readonly Role[]): NonNullable<Session> | null {
  const router = useRouter();
  const [session, setSession] = useState<NonNullable<Session> | null | undefined>(undefined);

  useEffect(() => {
    const s = getSession();
    if (!s || (roles && !roles.includes(s.role))) {
      router.replace("/login");
      setSession(null);
      return;
    }
    setSession(s);
  }, [router, roles]);

  return session ?? null;
}
