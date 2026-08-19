"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { getMemberSession, getSession, onAuthChange, type Session } from "@/lib/auth";

/* ── SVG icon components ── */
function HomeIcon({ active }: { active: boolean }) {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
      stroke="currentColor" strokeWidth={active ? 2.2 : 1.8} strokeLinecap="round" strokeLinejoin="round"
      className="h-5 w-5">
      <path d="M3 9.5L12 3l9 6.5V20a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9.5z" />
      <polyline points="9 22 9 12 15 12 15 22" />
    </svg>
  );
}

function CalendarIcon({ active }: { active: boolean }) {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
      stroke="currentColor" strokeWidth={active ? 2.2 : 1.8} strokeLinecap="round" strokeLinejoin="round"
      className="h-5 w-5">
      <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
      <line x1="16" y1="2" x2="16" y2="6" />
      <line x1="8" y1="2" x2="8" y2="6" />
      <line x1="3" y1="10" x2="21" y2="10" />
    </svg>
  );
}

function BookingsIcon({ active }: { active: boolean }) {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
      stroke="currentColor" strokeWidth={active ? 2.2 : 1.8} strokeLinecap="round" strokeLinejoin="round"
      className="h-5 w-5">
      <path d="M5 4h14a2 2 0 0 1 2 2v3a2 2 0 0 0 0 4v3a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-3a2 2 0 0 0 0-4V6a2 2 0 0 1 2-2z" />
      <path d="M8 8h8" />
      <path d="M8 12h8" />
      <path d="M8 16h5" />
    </svg>
  );
}

function UserIcon({ active }: { active: boolean }) {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
      stroke="currentColor" strokeWidth={active ? 2.2 : 1.8} strokeLinecap="round" strokeLinejoin="round"
      className="h-5 w-5">
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
      <circle cx="12" cy="7" r="4" />
    </svg>
  );
}

function LoginIcon({ active }: { active: boolean }) {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
      stroke="currentColor" strokeWidth={active ? 2.2 : 1.8} strokeLinecap="round" strokeLinejoin="round"
      className="h-5 w-5">
      <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4" />
      <polyline points="10 17 15 12 10 7" />
      <line x1="15" y1="12" x2="3" y2="12" />
    </svg>
  );
}

function DashboardIcon({ active }: { active: boolean }) {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
      stroke="currentColor" strokeWidth={active ? 2.2 : 1.8} strokeLinecap="round" strokeLinejoin="round"
      className="h-5 w-5">
      <rect x="3" y="3" width="7" height="9" rx="1" /><rect x="14" y="3" width="7" height="5" rx="1" />
      <rect x="14" y="12" width="7" height="9" rx="1" /><rect x="3" y="16" width="7" height="5" rx="1" />
    </svg>
  );
}

function ScanIcon({ active }: { active: boolean }) {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
      stroke="currentColor" strokeWidth={active ? 2.2 : 1.8} strokeLinecap="round" strokeLinejoin="round"
      className="h-5 w-5">
      <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
      <circle cx="12" cy="13" r="4" />
    </svg>
  );
}

/* ── Nav item type ── */
type NavItem = {
  label: string;
  href: string;
  icon: (props: { active: boolean }) => React.ReactNode;
  matchPaths: string[];
};

/* ── Build nav items based on role ── */
function getNavItems(session: Session, hasMemberSession: boolean): NavItem[] {
  const isStaffOrAdmin = session?.role === "STAFF" || session?.role === "ADMIN";
  const items: NavItem[] = [];

  if (isStaffOrAdmin) {
    items.push({ label: "Dashboard", href: "/admin/dashboard", icon: DashboardIcon, matchPaths: ["/admin/dashboard"] });
    items.push({ label: "Gate Scanner", href: "/staff", icon: ScanIcon, matchPaths: ["/staff", "/staff/scan"] });
  } else {
    items.push({ label: "Home", href: "/", icon: HomeIcon, matchPaths: ["/"] });
  }

  if (session?.role === "ADMIN") {
    items.push({ label: "My Events", href: "/admin/my-events", icon: CalendarIcon, matchPaths: ["/admin/my-events"] });
  } else if (!isStaffOrAdmin) {
    items.push({ label: "Events", href: "/events", icon: CalendarIcon, matchPaths: ["/events"] });
  }

  if (!session && hasMemberSession) {
    items.push({ label: "Bookings", href: "/member/bookings", icon: BookingsIcon, matchPaths: ["/member/bookings"] });
  }

  if (session) {
    items.push({ label: "Profile", href: "/profile", icon: UserIcon, matchPaths: ["/profile"] });
  } else if (hasMemberSession) {
    items.push({ label: "Profile", href: "/member/profile", icon: UserIcon, matchPaths: ["/member/profile"] });
  } else {
    items.push({ label: "Sign In", href: "/login", icon: LoginIcon, matchPaths: ["/login"] });
  }

  return items;
}


export default function MobileBottomNav() {
  const [session, setSession] = useState<Session>(null);
  const [hasMemberSession, setHasMemberSession] = useState(false);
  const pathname = usePathname();

  useEffect(() => {
    setSession(getSession());
    setHasMemberSession(!!getMemberSession());
  }, [pathname]);
  // React to auth changes (login/logout/token deletion, incl. from other tabs)
  useEffect(() => onAuthChange(() => {
    setSession(getSession());
    setHasMemberSession(!!getMemberSession());
  }), []);

  const items = getNavItems(session, hasMemberSession);

  const isActive = (item: NavItem) => {
    if (item.href === "/") return pathname === "/";
    return pathname.startsWith(item.href);
  };

  return (
    <nav className="mobile-bottom-nav md:hidden" aria-label="Mobile navigation">
      {items.map((item) => {
        const active = isActive(item);
        return (
          <Link
            key={item.href}
            href={item.href}
            className={`mobile-bottom-nav-item ${active ? "mobile-bottom-nav-item--active" : ""}`}
          >
            <span className="mobile-bottom-nav-icon">
              {item.icon({ active })}
            </span>
            <span className="mobile-bottom-nav-label">{item.label}</span>
            {active && <span className="mobile-bottom-nav-dot" />}
          </Link>
        );
      })}
    </nav>
  );
}
