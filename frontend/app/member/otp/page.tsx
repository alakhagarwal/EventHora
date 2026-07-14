"use client";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { memberApi } from "@/src/lib/api";
import { getSessionToken } from "@/src/lib/auth";

export default function OtpPage() {
  const router = useRouter();
  const [msg, setMsg] = useState("");
  const [remaining, setRemaining] = useState(0);
  const [otp, setOtp] = useState("");
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState<any>(null);

  useEffect(() => {
    if (typeof window === "undefined") return;
    setMsg(localStorage.getItem("otpMessage") || "OTP sent to your registered contact");
    const expires = Number(localStorage.getItem("otpExpiresAt") || 0);
    const tick = () => {
      const r = Math.max(0, Math.floor((expires - Date.now()) / 1000));
      setRemaining(r);
    };
    tick();
    const t = setInterval(tick, 1000);
    return () => clearInterval(t);
  }, []);

  async function verify() {
    setErr(null);
    const st = getSessionToken();
    if (!st) { router.push("/login"); return; }
    setBusy(true);
    try {
      const res = await memberApi.verifyOtp(st, otp);
      setResult(res);
    } catch (e: any) {
      setErr(e.message);
    } finally {
      setBusy(false);
    }
  }

  const mm = String(Math.floor(remaining / 60)).padStart(2, "0");
  const ss = String(remaining % 60).padStart(2, "0");

  if (result) {
    return (
      <div className="mx-auto max-w-md card p-6 text-center">
        <div className="mb-4 text-4xl">Booking Confirmed</div>
        <h1 className="text-2xl font-semibold">{result.eventTitle}</h1>
        <p className="mt-2 text-sm text-slate-600">Ticket: <span className="font-mono font-semibold">{result.ticketReference}</span></p>
        <p className="mt-1 text-sm text-slate-600">Quantity: {result.quantity}</p>
        <p className="mt-1 text-sm text-slate-600">Total: ₹{result.totalAmount}</p>
        <p className="mt-1 text-sm text-slate-600">Status: <span className="font-semibold">{result.paymentStatus}</span></p>
        {result.paymentStatus === "PENDING" && (
          <p className="mt-3 text-sm text-amber-600">Online payment — Razorpay checkout will open next.</p>
        )}
        <button className="btn-primary mt-6 w-full" onClick={() => router.push("/events")}>Back to Events</button>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-md card p-6">
      <h1 className="text-2xl font-semibold">Verify OTP</h1>
      <p className="mt-2 text-sm text-slate-600">{msg}</p>
      {remaining > 0 ? (
        <p className="mt-1 text-sm">Expires in: <span className="font-mono font-semibold">{mm}:{ss}</span></p>
      ) : (
        <p className="mt-1 text-sm text-red-600 font-semibold">OTP has expired. Go back and start a new booking.</p>
      )}
      {err && <div className="mt-3 rounded-md bg-red-50 p-3 text-sm text-red-700">{err}</div>}
      <label className="label mt-4">Enter 6-digit OTP</label>
      <input
        className="input tracking-widest text-center text-lg"
        maxLength={6}
        value={otp}
        onChange={e => setOtp(e.target.value.replace(/\D/g, ""))}
        disabled={remaining <= 0}
      />
      <button
        className="btn-primary mt-4 w-full"
        disabled={busy || otp.length !== 6 || remaining <= 0}
        onClick={verify}
      >
        {busy ? "Verifying…" : "Confirm booking"}
      </button>
      <button className="btn-outline mt-3 w-full" onClick={() => router.back()}>Go back</button>
    </div>
  );
}
