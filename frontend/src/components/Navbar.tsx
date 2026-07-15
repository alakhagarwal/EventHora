"use client";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { clearSession, getSession, type Session } from "@/lib/auth";

/* ── Drawer link icons (small inline SVGs) ── */
const icons = {
  home: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 9.5L12 3l9 6.5V20a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9.5z" />
      <polyline points="9 22 9 12 15 12 15 22" />
    </svg>
  ),
  events: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
      <line x1="16" y1="2" x2="16" y2="6" /><line x1="8" y1="2" x2="8" y2="6" />
      <line x1="3" y1="10" x2="21" y2="10" />
    </svg>
  ),
  myEvents: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="4" width="18" height="18" rx="2" /><line x1="16" y1="2" x2="16" y2="6" /><line x1="8" y1="2" x2="8" y2="6" />
      <line x1="3" y1="10" x2="21" y2="10" /><path d="M9 16l2 2 4-4" />
    </svg>
  ),
  allEvents: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="3" width="7" height="7" /><rect x="14" y="3" width="7" height="7" />
      <rect x="3" y="14" width="7" height="7" /><rect x="14" y="14" width="7" height="7" />
    </svg>
  ),
  users: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" />
      <path d="M23 21v-2a4 4 0 0 0-3-3.87" /><path d="M16 3.13a4 4 0 0 1 0 7.75" />
    </svg>
  ),
  profile: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" />
    </svg>
  ),
  logout: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
      <polyline points="16 17 21 12 16 7" /><line x1="21" y1="12" x2="9" y2="12" />
    </svg>
  ),
};

export default function Navbar() {
  const [session, setSession] = useState<Session>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => { setSession(getSession()); }, [pathname]);
  // Close drawer on route change
  useEffect(() => { setDrawerOpen(false); }, [pathname]);
  // Lock body scroll when drawer is open
  useEffect(() => {
    if (drawerOpen) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "";
    }
    return () => { document.body.style.overflow = ""; };
  }, [drawerOpen]);

  const logout = () => { clearSession(); setSession(null); setDrawerOpen(false); router.push("/"); };

  const linkCls = (href: string) =>
    `text-sm font-medium transition-colors ${pathname === href ? "text-gold" : "text-white/80 hover:text-white"}`;

  const drawerLinkCls = (href: string) =>
    `drawer-link ${pathname === href || (href !== "/" && pathname.startsWith(href)) ? "drawer-link--active" : ""}`;

  const initials = session?.name
    ? session.name.split(" ").map((w) => w[0]).join("").toUpperCase().slice(0, 2)
    : "?";

  return (
    <>
      <header className="sticky top-0 z-40 bg-navy text-white">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-3 md:px-6 md:py-4">
          {/* Left: hamburger (mobile) + logo */}
          <div className="flex items-center gap-3">
            <button
              className={`hamburger-btn md:hidden ${drawerOpen ? "hamburger-btn--open" : ""}`}
              onClick={() => setDrawerOpen(!drawerOpen)}
              aria-label="Toggle menu"
            >
              <span className="hamburger-line" />
              <span className="hamburger-line" />
              <span className="hamburger-line" />
            </button>
            <Link href="/" className="flex items-center gap-2">
              <span className="grid h-8 w-8 place-items-center rounded-md bg-gold text-navy font-bold">E</span>
              <span className="font-display text-xl">EventHora</span>
            </Link>
          </div>

          {/* Center: desktop nav links */}
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

          {/* Right: auth buttons */}
          <div className="flex items-center gap-3">
            {!session ? (
              <>
                <Link href="/login" className="hidden md:inline text-sm text-white/80 hover:text-white">Sign In</Link>
                <Link href="/login" className="btn-primary text-xs md:text-sm">Get Started</Link>
              </>
            ) : (
              <>
                <span className="hidden md:inline text-xs text-white/60">{session.name} · {session.role}</span>
                <button onClick={logout} className="btn-primary text-xs md:text-sm">Logout</button>
              </>
            )}
          </div>
        </div>
      </header>

      {/* ── Mobile Slide-Out Drawer ── */}
      {drawerOpen && (
        <>
          {/* Overlay */}
          <div className="drawer-overlay md:hidden" onClick={() => setDrawerOpen(false)} />

          {/* Drawer panel */}
          <div className="drawer-panel md:hidden">
            {/* Drawer header */}
            <div className="drawer-header">
              <div className="drawer-avatar">{session ? initials : "?"}</div>
              <div>
                <div className="drawer-user-name">{session ? session.name : "Guest"}</div>
                <div className="drawer-user-role">{session ? session.role : "Not signed in"}</div>
              </div>
            </div>

            {/* Drawer navigation */}
            <nav className="drawer-nav">
              <Link href="/" className={drawerLinkCls("/")} onClick={() => setDrawerOpen(false)}>
                {icons.home} Home
              </Link>
              <Link href="/events" className={drawerLinkCls("/events")} onClick={() => setDrawerOpen(false)}>
                {icons.events} Events
              </Link>

              {session?.role === "ADMIN" && (
                <>
                  <div className="drawer-divider" />
                  <Link href="/admin/my-events" className={drawerLinkCls("/admin/my-events")} onClick={() => setDrawerOpen(false)}>
                    {icons.myEvents} My Events
                  </Link>
                  <Link href="/admin/events" className={drawerLinkCls("/admin/events")} onClick={() => setDrawerOpen(false)}>
                    {icons.allEvents} All Events
                  </Link>
                  <Link href="/admin/users" className={drawerLinkCls("/admin/users")} onClick={() => setDrawerOpen(false)}>
                    {icons.users} Users
                  </Link>
                </>
              )}

              {session && (
                <>
                  <div className="drawer-divider" />
                  <Link href="/profile" className={drawerLinkCls("/profile")} onClick={() => setDrawerOpen(false)}>
                    {icons.profile} Profile
                  </Link>
                </>
              )}
            </nav>

            {/* Drawer footer */}
            {session && (
              <div className="drawer-footer">
                <button onClick={logout} className="drawer-logout-btn">
                  {icons.logout} Sign Out
                </button>
              </div>
            )}
            {!session && (
              <div className="drawer-footer">
                <Link href="/login" className="drawer-logout-btn" onClick={() => setDrawerOpen(false)}>
                  Sign In / Get Started
                </Link>
              </div>
            )}
          </div>
        </>
      )}
    </>
  );
}
