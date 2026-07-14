"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { saveSession } from "@/lib/auth";

type Tab = "staff" | "indian" | "overseas";

export default function LoginPage() {
  const [tab, setTab] = useState<Tab>("staff");
  const router = useRouter();

  return (
    <div className="mx-auto max-w-5xl px-6 py-16">
      <div className="text-center mb-10">
        <div className="eyebrow">Access your account</div>
        <h1 className="h1 mt-2">Sign in to EventHora</h1>
        <p className="text-navy/60 mt-2">Choose the login method that applies to you.</p>
      </div>

      <div className="card p-2 mb-6 flex flex-wrap gap-1">
        {([
          ["staff", "Admin / Staff"],
          ["indian", "Member · Indian"],
          ["overseas", "Member · Overseas"],
        ] as [Tab, string][]).map(([k, l]) => (
          <button
            key={k}
            onClick={() => setTab(k)}
            className={`flex-1 min-w-[160px] rounded-lg px-4 py-3 text-sm font-semibold transition-colors ${
              tab === k ? "bg-navy text-white" : "text-navy/70 hover:bg-navy/5"
            }`}
          >
            {l}
          </button>
        ))}
      </div>

      <div className="card p-8">
        {tab === "staff" && <StaffForm onDone={(role) => router.push(role === "ADMIN" ? "/admin/my-events" : "/profile")} />}
        {tab === "indian" && <MemberForm memberType="INDIAN" onDone={() => router.push("/events")} />}
        {tab === "overseas" && <MemberForm memberType="OVERSEAS" onDone={() => router.push("/events")} />}
      </div>
    </div>
  );
}

function StaffForm({ onDone }: { onDone: (role: "ADMIN" | "STAFF") => void }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const submit = async (e: React.FormEvent) => {
    e.preventDefault(); setErr(null); setBusy(true);
    try {
      const res = await api.login(email, password);
      saveSession(res.accessToken, { role: res.role, name: res.name, email: res.email });
      onDone(res.role);
    } catch (e: any) { setErr(e.message || "Login failed"); } finally { setBusy(false); }
  };
  return (
    <form onSubmit={submit} className="space-y-4 max-w-md mx-auto">
      <h2 className="font-display text-2xl text-navy">Admin / Staff Login</h2>
      <div><label className="label">Email</label><input className="input" value={email} onChange={(e) => setEmail(e.target.value)} type="email" required /></div>
      <div><label className="label">Password</label><input className="input" value={password} onChange={(e) => setPassword(e.target.value)} type="password" required /></div>
      {err && <div className="text-sm text-red-600">{err}</div>}
      <button className="btn-dark w-full" disabled={busy}>{busy ? "Signing in…" : "Sign In"}</button>
      <p className="text-xs text-navy/50 text-center">Try admin@eventhora.com / Admin@1234 (dev)</p>
    </form>
  );
}

function MemberForm({ memberType, onDone }: { memberType: "INDIAN" | "OVERSEAS"; onDone: () => void }) {
  const [memberId, setMemberId] = useState("");
  const [identifier, setIdentifier] = useState("");
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault(); setErr(null); setBusy(true);
    try {
      const res: any = await api.verifyMember({ memberId, identifier, memberType });
      if (res?.sessionToken) localStorage.setItem("memberSession", JSON.stringify(res));
      onDone();
    } catch (e: any) { setErr(e.message || "Verification failed"); } finally { setBusy(false); }
  };

  return (
    <form onSubmit={submit} className="space-y-4 max-w-md mx-auto">
      <h2 className="font-display text-2xl text-navy">Member Login · {memberType === "INDIAN" ? "Indian" : "Overseas"}</h2>
      <div><label className="label">Member ID</label><input className="input" value={memberId} onChange={(e) => setMemberId(e.target.value)} required placeholder="RIC-12345" /></div>
      <div>
        <label className="label">{memberType === "INDIAN" ? "Mobile Number" : "Email Address"}</label>
        <input className="input" value={identifier} onChange={(e) => setIdentifier(e.target.value)} type={memberType === "INDIAN" ? "tel" : "email"} required />
      </div>
      {err && <div className="text-sm text-red-600">{err}</div>}
      <button className="btn-primary w-full" disabled={busy}>{busy ? "Verifying…" : "Verify & Continue"}</button>
    </form>
  );
}
