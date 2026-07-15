"use client";
import { useEffect, useState } from "react";
import Link from "next/link";

export default function OtpPage() {
  const [ctx, setCtx] = useState<any>(null);
  const [remaining, setRemaining] = useState<number>(300);
  const [otp, setOtp] = useState("");

  useEffect(() => {
    const raw = localStorage.getItem("bookingCtx");
    if (raw) {
      const c = JSON.parse(raw);
      setCtx(c);
      const elapsed = Math.floor((Date.now() - (c.startedAt || Date.now())) / 1000);
      const total = c.expiresInSeconds || 300;
      setRemaining(Math.max(0, total - elapsed));
    }
  }, []);

  useEffect(() => {
    if (remaining <= 0) return;
    const t = setInterval(() => setRemaining((r) => Math.max(0, r - 1)), 1000);
    return () => clearInterval(t);
  }, [remaining]);

  const mm = String(Math.floor(remaining / 60)).padStart(2, "0");
  const ss = String(remaining % 60).padStart(2, "0");

  return (
    <div className="mx-auto max-w-md px-6 py-16">
      <div className="card p-8 text-center">
        <div className="eyebrow">Verification</div>
        <h1 className="font-display text-3xl text-navy mt-2">Enter OTP</h1>
        <p className="mt-3 text-navy/70 text-sm">
          {ctx?.message || "A 6-digit code has been sent to your registered contact."}
        </p>

        <div className={`mt-6 font-display text-4xl ${remaining === 0 ? "text-red-600" : "text-navy"}`}>
          {mm}:{ss}
        </div>
        <div className="text-xs text-navy/50 mt-1">{remaining === 0 ? "Expired — please restart booking" : "Time remaining"}</div>

        <input
          value={otp}
          onChange={(e) => setOtp(e.target.value.replace(/\D/g, "").slice(0, 6))}
          className="input mt-6 text-center tracking-[0.5em] text-2xl font-mono"
          placeholder="••••••"
        />
        <button className="btn-primary w-full mt-4" disabled={otp.length !== 6 || remaining === 0}>
          Confirm Booking
        </button>
        <p className="text-[11px] text-navy/50 mt-3">
          Note: OTP confirmation endpoint is handled server-side per your backend. Wire it here when available.
        </p>
        <Link href="/events" className="btn-ghost mt-4 inline-flex">← Back to events</Link>
      </div>
    </div>
  );
}
