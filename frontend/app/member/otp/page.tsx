"use client";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { api, RegistrationResponse } from "@/lib/api";

export default function OtpPage() {
  const router = useRouter();
  const [ctx, setCtx] = useState<any>(null);
  const [remaining, setRemaining] = useState<number>(300);
  const [otp, setOtp] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

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

  const handleConfirm = async () => {
    setError(null);
    setBusy(true);
    try {
      const raw = localStorage.getItem("bookingCtx");
      if (!raw) throw new Error("Booking session not found. Please restart.");
      const bookingCtx = JSON.parse(raw);
      const sessionToken: string = bookingCtx.sessionToken;
      if (!sessionToken) throw new Error("Session token missing. Please restart booking.");

      const result: RegistrationResponse = await api.verifyOtp({ sessionToken, otp });

      if (result.paymentStatus === "FREE" || result.paymentStatus === "COMPLIMENTARY") {
        // Free booking — go straight to thank-you
        sessionStorage.setItem("bookingResult", JSON.stringify(result));
        router.push("/success/thanku");
      } else if (result.paymentStatus === "PAY_AT_GATE") {
        // Pay at venue — go to thank-you with that flag
        sessionStorage.setItem("bookingResult", JSON.stringify(result));
        router.push("/success/thanku");
      } else if (result.paymentStatus === "PENDING") {
        // Open Razorpay checkout
        const razorpay = new (window as any).Razorpay({
          key: process.env.NEXT_PUBLIC_RAZORPAY_KEY_ID,
          amount: Math.round(Number(result.totalAmount) * 100),
          currency: "INR",
          name: "EventHora",
          description: `${result.eventTitle} — ${result.quantity} ticket(s)`,
          order_id: result.razorpayOrderId,
          handler: async (response: any) => {
            try {
              const confirmed = await api.confirmPayment({
                ticketReference: result.ticketReference,
                razorpayOrderId: response.razorpay_order_id,
                razorpayPaymentId: response.razorpay_payment_id,
                razorpaySignature: response.razorpay_signature,
              });
              sessionStorage.setItem("bookingResult", JSON.stringify(confirmed));
              router.push("/success/thanku");
            } catch (err: any) {
              setError(err?.message || "Payment confirmation failed.");
              setBusy(false);
            }
          },
          modal: {
            ondismiss: () => {
              setBusy(false);
            },
          },
        });
        razorpay.open();
        // Don't reset busy here — wait for handler or ondismiss
        return;
      }
    } catch (err: any) {
      setError(err?.message || "Something went wrong. Please try again.");
      setBusy(false);
    }
  };

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
        <div className="text-xs text-navy/50 mt-1">
          {remaining === 0 ? "Expired — please restart booking" : "Time remaining"}
        </div>

        <input
          value={otp}
          onChange={(e) => setOtp(e.target.value.replace(/\D/g, "").slice(0, 6))}
          className="input mt-6 text-center tracking-[0.5em] text-2xl font-mono"
          placeholder="••••••"
        />

        {error && (
          <p className="mt-3 text-sm text-red-600 bg-red-50 rounded-lg px-4 py-2">{error}</p>
        )}

        <button
          className="btn-primary w-full mt-4"
          disabled={otp.length !== 6 || remaining === 0 || busy}
          onClick={handleConfirm}
        >
          {busy ? "Processing…" : "Confirm Booking"}
        </button>

        <Link href="/events" className="btn-ghost mt-4 inline-flex">← Back to events</Link>
      </div>
    </div>
  );
}
