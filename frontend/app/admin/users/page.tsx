"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { getSession } from "@/lib/auth";

export default function UsersPage() {
  const [users, setUsers] = useState<any[]>([]);
  const [msg, setMsg] = useState<string | null>(null);
  const router = useRouter();

  const load = () => api.users().then(setUsers).catch((e) => setMsg(e.message));
  useEffect(() => {
    const s = getSession(); if (!s || s.role !== "ADMIN") { router.push("/login"); return; }
    load();
  }, [router]);

  const deactivate = async (email: string) => {
    if (!confirm(`Deactivate ${email}?`)) return;
    try { await api.deactivateUser(email); setMsg("User deactivated."); load(); }
    catch (e: any) { setMsg(e.message); }
  };

  return (
    <div className="mx-auto max-w-6xl px-6 py-12">
      <div className="flex items-end justify-between mb-8">
        <div>
          <div className="eyebrow">Admin</div>
          <h1 className="h1 mt-2">User Management</h1>
        </div>
        <Link href="/admin/users/new" className="btn-primary">+ New User</Link>
      </div>
      {msg && <div className="mb-4 text-sm text-navy/70">{msg}</div>}
      <div className="card overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-navy text-white">
            <tr>
              <th className="text-left px-4 py-3">Name</th>
              <th className="text-left px-4 py-3">Email</th>
              <th className="text-left px-4 py-3">Role</th>
              <th className="text-left px-4 py-3">Created</th>
              <th className="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id || u.email} className="border-t border-navy/10">
                <td className="px-4 py-3 font-medium">{u.name}</td>
                <td className="px-4 py-3">{u.email}</td>
                <td className="px-4 py-3"><span className="chip">{u.role}</span></td>
                <td className="px-4 py-3 text-navy/60">{u.createdAt ? new Date(u.createdAt).toLocaleDateString() : "—"}</td>
                <td className="px-4 py-3 text-right">
                  <button className="text-red-600 text-xs font-semibold hover:underline" onClick={() => deactivate(u.email)}>Deactivate</button>
                </td>
              </tr>
            ))}
            {users.length === 0 && <tr><td colSpan={5} className="px-4 py-10 text-center text-navy/50">No users.</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}
