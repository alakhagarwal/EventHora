"use client";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { getSession } from "@/lib/auth";

export default function NewUser() {
  const router = useRouter();
  const [form, setForm] = useState({ name: "", email: "", password: "", role: "STAFF" as "ADMIN" | "STAFF" });
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => { const s = getSession(); if (!s || s.role !== "ADMIN") router.push("/login"); }, [router]);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault(); setErr(null); setBusy(true);
    try { await api.createUser(form); router.push("/admin/users"); }
    catch (e: any) { setErr(e.message); } finally { setBusy(false); }
  };

  return (
    <div className="mx-auto max-w-md px-6 py-16">
      <div className="eyebrow">Admin</div>
      <h1 className="h1 mt-2 mb-8">Create User</h1>
      <form onSubmit={submit} className="card p-8 space-y-4">
        <div><label className="label">Name</label><input className="input" required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></div>
        <div><label className="label">Email</label><input type="email" className="input" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></div>
        <div><label className="label">Password</label><input type="password" className="input" required value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></div>
        <div>
          <label className="label">Role</label>
          <select className="input" value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value as "ADMIN" | "STAFF" })}>
            <option value="STAFF">STAFF</option>
            <option value="ADMIN">ADMIN</option>
          </select>
        </div>
        {err && <div className="text-sm text-red-600">{err}</div>}
        <button className="btn-primary w-full" disabled={busy}>{busy ? "Creating…" : "Create User"}</button>
      </form>
    </div>
  );
}
