"use client";
import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { clearToken, getRole, getName, getToken } from "@/src/lib/auth";

export default function Navbar() {
  const [role, setRole] = useState<string | null>(null);
  const [name, setName] = useState<string | null>(null);
  const [authed, setAuthed] = useState(false);
  const router = useRouter();

  useEffect(() => {
    setRole(getRole());
    setName(getName());
    setAuthed(!!getToken());
    const handler = () => { setRole(getRole()); setName(getName()); setAuthed(!!getToken()); };
    window.addEventListener("storage", handler);
    return () => window.removeEventListener("storage", handler);
  }, []);

  function logout() { clearToken(); router.push("/login"); router.refresh(); }

  return (
    <nav className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
        <Link href="/" className="text-lg font-bold text-indigo-700">EventHora</Link>
        <div className="flex items-center gap-4 text-sm">
          {!authed && (
            <>
              <Link href="/">Home</Link>
              <Link href="/events">Events</Link>
              <Link href="/login">Login</Link>
            </>
          )}
          {authed && role === "ADMIN" && (
            <>
              <Link href="/admin/my-events">My Events</Link>
              <Link href="/admin/events">All Events</Link>
              <Link href="/admin/events/public">Published</Link>
              <Link href="/admin/users">Users</Link>
              <Link href="/profile">Profile</Link>
            </>
          )}
          {authed && role === "STAFF" && (<Link href="/profile">Profile</Link>)}
          {authed && (
            <>
              <span className="text-slate-500">{name}</span>
              <button onClick={logout} className="btn-outline">Logout</button>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}
