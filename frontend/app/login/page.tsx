"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { authApi, memberApi } from "@/src/lib/api";
import { setToken, setUser, decodeJwt, setSessionToken } from "@/src/lib/auth";

type Tab = "admin" | "indian" | "overseas";

export default function LoginPage() {
  const router = useRouter();
  const [tab, setTab] = useState<Tab>("admin");
  const [err, setErr] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const [email, setEmail] = useState(""); const [password, setPassword] = useState("");
  const [memberId, setMemberId] = useState(""); const [identifier, setIdentifier] = useState("");

  async function doAdminLogin(e: React.FormEvent) {
    e.preventDefault(); setErr(null); setLoading(true);
    try {
      const res = await authApi.login(email, password);
      setToken(res.accessToken); setUser(res.role, res.name);
      const jwt = decodeJwt(res.accessToken);
      const role = jwt?.role || res.role;
      router.push(role === "ADMIN" ? "/admin/my-events" : "/profile");
    } catch (e: any) { setErr(e.message); } finally { setLoading(false); }
  }
  async function doMemberLogin(e: React.FormEvent, memberType: "INDIAN" | "OVERSEAS") {
    e.preventDefault(); setErr(null); setLoading(true);
    try {
      const res = await memberApi.verify(memberId, identifier, memberType);
      setSessionToken(res.sessionToken);
      if (typeof window !== "undefined") {
        localStorage.setItem("memberId", res.memberId);
        localStorage.setItem("memberType", res.memberType);
        localStorage.setItem("maskedIdentifier", res.maskedIdentifier);
      }
      router.push("/events");
    } catch (e: any) { setErr(e.message); } finally { setLoading(false); }
  }

  const TabBtn = ({v,l}:{v:Tab;l:string}) => (
    <button onClick={()=>{setTab(v);setErr(null);}} className={`flex-1 rounded-md px-3 py-2 text-sm font-medium transition ${tab===v?"bg-indigo-600 text-white":"bg-slate-100 text-slate-700 hover:bg-slate-200"}`}>{l}</button>
  );

  return (
    <div className="mx-auto max-w-lg">
      <h1 className="mb-6 text-2xl font-semibold">Sign in</h1>
      <div className="mb-4 flex gap-2">
        <TabBtn v="admin" l="Admin / Staff" />
        <TabBtn v="indian" l="Member (Indian)" />
        <TabBtn v="overseas" l="Member (Overseas)" />
      </div>
      <div className="card p-6">
        {err && <div className="mb-4 rounded-md bg-red-50 p-3 text-sm text-red-700">{err}</div>}
        {tab === "admin" && (
          <form onSubmit={doAdminLogin} className="space-y-4">
            <div><label className="label">Email</label><input className="input" type="email" value={email} onChange={e=>setEmail(e.target.value)} required /></div>
            <div><label className="label">Password</label><input className="input" type="password" value={password} onChange={e=>setPassword(e.target.value)} required /></div>
            <button className="btn-primary w-full" disabled={loading}>{loading?"Signing in…":"Sign in"}</button>
          </form>
        )}
        {tab === "indian" && (
          <form onSubmit={e=>doMemberLogin(e,"INDIAN")} className="space-y-4">
            <div><label className="label">Member ID</label><input className="input" value={memberId} onChange={e=>setMemberId(e.target.value)} required /></div>
            <div><label className="label">Mobile Number</label><input className="input" value={identifier} onChange={e=>setIdentifier(e.target.value)} required /></div>
            <button className="btn-primary w-full" disabled={loading}>{loading?"Verifying…":"Verify member"}</button>
          </form>
        )}
        {tab === "overseas" && (
          <form onSubmit={e=>doMemberLogin(e,"OVERSEAS")} className="space-y-4">
            <div><label className="label">Member ID</label><input className="input" value={memberId} onChange={e=>setMemberId(e.target.value)} required /></div>
            <div><label className="label">Email</label><input className="input" type="email" value={identifier} onChange={e=>setIdentifier(e.target.value)} required /></div>
            <button className="btn-primary w-full" disabled={loading}>{loading?"Verifying…":"Verify member"}</button>
          </form>
        )}
      </div>
    </div>
  );
}
