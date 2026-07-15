"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { getSession, type Session } from "@/lib/auth";

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

function UsersIcon({ active }: { active: boolean }) {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
      stroke="currentColor" strokeWidth={active ? 2.2 : 1.8} strokeLinecap="round" strokeLinejoin="round"
      className="h-5 w-5">
      <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
      <path d="M16 3.13a4 4 0 0 1 0 7.75" />
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

/* ── Nav item type ── */
type NavItem = {
  label: string;
  href: string;
  icon: (props: { active: boolean }) => React.ReactNode;
  matchPaths: string[]; // paths that mark this item as active
};

/* ── Build nav items based on role ── */
function getNavItems(session: Session, hasMemberSession: boolean): NavItem[] {
  const items: NavItem[] = [
    { label: "Home", href: "/", icon: HomeIcon, matchPaths: ["/"] },
    { label: "Events", href: "/events", icon: CalendarIcon, matchPaths: ["/events"] },
  ];

  if (session?.role === "ADMIN") {
    items.push({ label: "Users", href: "/admin/users", icon: UsersIcon, matchPaths: ["/admin/users"] });
    items.push({ label: "Profile", href: "/profile", icon: UserIcon, matchPaths: ["/profile"] });
  } else if (session?.role === "STAFF") {
    items.push({ label: "Profile", href: "/profile", icon: UserIcon, matchPaths: ["/profile"] });
  } else if (hasMemberSession) {
    items.push({ label: "Profile", href: "/profile", icon: UserIcon, matchPaths: ["/profile"] });
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
    setHasMemberSession(!!localStorage.getItem("memberSession"));
  }, [pathname]);

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
