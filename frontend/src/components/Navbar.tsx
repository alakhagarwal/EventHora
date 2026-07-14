"use client";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { clearSession, getSession, type Session } from "@/lib/auth";

export default function Navbar() {
  const [session, setSession] = useState<Session>(null);
  const [open, setOpen] = useState(false);
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => { setSession(getSession()); }, [pathname]);

  const logout = () => { clearSession(); setSession(null); router.push("/"); };

  const linkCls = (href: string) =>
    `text-sm font-medium transition-colors ${pathname === href ? "text-gold" : "text-white/80 hover:text-white"}`;

  return (
    <header className="sticky top-0 z-40 bg-navy text-white">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
        <Link href="/" className="flex items-center gap-2">
          <span className="grid h-8 w-8 place-items-center rounded-md bg-gold text-navy font-bold">E</span>
          <span className="font-display text-xl">EventHora</span>
        </Link>

        <nav className="hidden md:flex items-center gap-8">
          <Link href="/" className={linkCls("/")}>Home</Link>
          <Link href="/events" className={linkCls("/events")}>Events</Link>
          {session?.role === "ADMIN" && (
            <>
              <Link href="/admin/my-events" className={linkCls("/admin/my-events")}>My Events</Link>
              <Link href="/admin/events" className={linkCls("/admin/events")}>All Events</Link>
              <Link href="/admin/users" className={linkCls("/admin/users")}>Users</Link>
            </>
          )}
          {session && <Link href="/profile" className={linkCls("/profile")}>Profile</Link>}
        </nav>

        <div className="flex items-center gap-3">
          {!session ? (
            <>
              <Link href="/login" className="hidden md:inline text-sm text-white/80 hover:text-white">Sign In</Link>
              <Link href="/login" className="btn-primary">Get Started</Link>
            </>
          ) : (
            <>
              <span className="hidden md:inline text-xs text-white/60">{session.name} · {session.role}</span>
              <button onClick={logout} className="btn-primary">Logout</button>
            </>
          )}
          <button className="md:hidden text-white" onClick={() => setOpen(!open)} aria-label="menu">☰</button>
        </div>
      </div>
      {open && (
        <div className="md:hidden border-t border-white/10 px-6 py-4 flex flex-col gap-3">
          <Link href="/" onClick={() => setOpen(false)}>Home</Link>
          <Link href="/events" onClick={() => setOpen(false)}>Events</Link>
          {session?.role === "ADMIN" && (
            <>
              <Link href="/admin/my-events" onClick={() => setOpen(false)}>My Events</Link>
              <Link href="/admin/events" onClick={() => setOpen(false)}>All Events</Link>
              <Link href="/admin/users" onClick={() => setOpen(false)}>Users</Link>
            </>
          )}
          {session && <Link href="/profile" onClick={() => setOpen(false)}>Profile</Link>}
        </div>
      )}
    </header>
  );
}
