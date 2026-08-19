"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { getSession, onAuthChange, type Session } from "@/lib/auth";

/* ── Sidebar link icons ── */
const icons = {
  home: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 9.5L12 3l9 6.5V20a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9.5z" />
      <polyline points="9 22 9 12 15 12 15 22" />
    </svg>
  ),
  scan: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
      <circle cx="12" cy="13" r="4" />
    </svg>
  ),
  dashboard: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="3" width="7" height="9" rx="1" /><rect x="14" y="3" width="7" height="5" rx="1" />
      <rect x="14" y="12" width="7" height="9" rx="1" /><rect x="3" y="16" width="7" height="5" rx="1" />
    </svg>
  ),
  myEvents: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="4" width="18" height="18" rx="2" /><line x1="16" y1="2" x2="16" y2="6" /><line x1="8" y1="2" x2="8" y2="6" />
      <line x1="3" y1="10" x2="21" y2="10" /><path d="M9 16l2 2 4-4" />
    </svg>
  ),
  registerMember: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21a8 8 0 0 1 16 0" />
      <path d="M19 8v6" />
      <path d="M16 11h6" />
    </svg>
  ),
  users: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" />
      <path d="M23 21v-2a4 4 0 0 0-3-3.87" /><path d="M16 3.13a4 4 0 0 1 0 7.75" />
    </svg>
  ),
  logout: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
      <polyline points="16 17 21 12 16 7" /><line x1="21" y1="12" x2="9" y2="12" />
    </svg>
  ),
};

const sidebarLinks = [
  { href: "/", icon: icons.home, label: "Home", matchExact: true },
  { href: "/admin/dashboard", icon: icons.dashboard, label: "Dashboard", matchExact: false },
  { href: "/staff", icon: icons.scan, label: "Gate Scanner", matchExact: false },
  { href: "/admin/my-events", icon: icons.myEvents, label: "My Events", matchExact: false },
  { href: "/admin/bookings/new", icon: icons.registerMember, label: "Register Member", matchExact: false },
  { href: "/admin/users", icon: icons.users, label: "Users", matchExact: false },
];

export default function AdminSidebar() {
  const [session, setSession] = useState<Session>(null);
  const [mounted, setMounted] = useState(false);
  const pathname = usePathname();

  useEffect(() => { setSession(getSession()); setMounted(true); }, [pathname]);
  useEffect(() => onAuthChange(() => { setSession(getSession()); }), []);

  const isAdmin = mounted && session?.role === "ADMIN";

  const isActive = (item: typeof sidebarLinks[0]) =>
    item.matchExact ? pathname === item.href : pathname.startsWith(item.href) && item.href !== "/";

  return (
    <aside className={`admin-sidebar ${isAdmin ? "" : "admin-sidebar--hidden"}`}>
      {isAdmin && (
        <nav className="admin-sidebar-nav">
          {sidebarLinks.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className={`admin-sidebar-link ${isActive(item) ? "admin-sidebar-link--active" : ""}`}
            >
              <span className="admin-sidebar-icon">{item.icon}</span>
              <span className="admin-sidebar-label">{item.label}</span>
            </Link>
          ))}
        </nav>
      )}
    </aside>
  );
}
