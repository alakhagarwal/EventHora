"use client";
import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { getSession } from "@/lib/auth";
import SearchInput from "@/components/SearchInput";
import { toast } from "@/lib/toast";
import { useDebouncedValue } from "@/lib/useDebouncedValue";

export default function UsersPage() {
  const [users, setUsers] = useState<any[]>([]);
  const router = useRouter();
  const [query, setQuery] = useState("");

  const load = () => api.users().then(setUsers).catch((e) => toast.error(e.message));
  useEffect(() => {
    const s = getSession(); if (!s || s.role !== "ADMIN") { router.push("/login"); return; }
    load();
  }, [router]);

  const debouncedQuery = useDebouncedValue(query, 300);
  const filteredUsers = useMemo(() => {
    const normalizedQuery = debouncedQuery.trim().toLowerCase();
    return users.filter((user) => {
      if (!normalizedQuery) return true;
      return (user.name || "").toLowerCase().includes(normalizedQuery) || (user.email || "").toLowerCase().includes(normalizedQuery);
    });
  }, [debouncedQuery, users]);

  const deactivate = async (email: string) => {
    if (!confirm(`Deactivate ${email}?`)) return;
    try { await api.deactivateUser(email); toast.success("User deactivated."); load(); }
    catch (e: any) { toast.error(e.message); }
  };

  return (
    <div className="mx-auto max-w-6xl px-4 md:px-6 py-8 md:py-12">
      <div className="flex flex-col sm:flex-row sm:items-end justify-between mb-6 md:mb-8 gap-4">
        <div>
          <div className="eyebrow">Admin</div>
          <h1 className="h1 mt-2">User Management</h1>
        </div>
        <div className="flex flex-col sm:flex-row gap-3 w-full sm:w-auto">
          <SearchInput value={query} onChange={setQuery} placeholder="Search by name or email" className="w-full sm:w-72" />
          <Link href="/admin/users/new" className="btn-primary self-start sm:self-auto">+ New User</Link>
        </div>
      </div>
      <div className="card overflow-hidden table-scroll-mobile">
        <table className="w-full text-sm">
          <thead className="bg-navy text-white">
            <tr>
              <th className="text-left px-4 py-3">Name</th>
              <th className="text-left px-4 py-3">Email</th>
              <th className="text-left px-4 py-3">Role</th>
              <th className="text-left px-4 py-3 hidden sm:table-cell">Created</th>
              <th className="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody>
            {filteredUsers.map((u) => (
              <tr key={u.id || u.email} className="border-t border-navy/10">
                <td className="px-4 py-3 font-medium">{u.name}</td>
                <td className="px-4 py-3 text-xs md:text-sm">{u.email}</td>
                <td className="px-4 py-3"><span className="chip">{u.role}</span></td>
                <td className="px-4 py-3 text-navy/60 hidden sm:table-cell">{u.createdAt ? new Date(u.createdAt).toLocaleDateString() : "—"}</td>
                <td className="px-4 py-3 text-right">
                  <button className="text-red-600 text-xs font-semibold hover:underline" onClick={() => deactivate(u.email)}>Deactivate</button>
                </td>
              </tr>
            ))}
            {filteredUsers.length === 0 && <tr><td colSpan={5} className="px-4 py-10 text-center text-navy/50">No users.</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}
