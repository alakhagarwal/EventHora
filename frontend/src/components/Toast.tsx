"use client";

import type { ReactNode } from "react";
import { useEffect, useState } from "react";
import { AlertCircle, CheckCircle2, Info, X } from "lucide-react";
import { useToastActions } from "@/lib/toast";

const AUTO_DISMISS: Record<"success" | "error" | "info", number> = {
  success: 5000,
  error: 8000,
  info: 5000,
};

const TOAST_TONES: Record<"success" | "error" | "info", { icon: ReactNode; border: string; bg: string; iconTone: string }> = {
  success: {
    icon: <CheckCircle2 className="h-5 w-5" />,
    border: "border-green-200",
    bg: "bg-green-50",
    iconTone: "text-green-600",
  },
  error: {
    icon: <AlertCircle className="h-5 w-5" />,
    border: "border-red-200",
    bg: "bg-red-50",
    iconTone: "text-red-600",
  },
  info: {
    icon: <Info className="h-5 w-5" />,
    border: "border-blue-200",
    bg: "bg-blue-50",
    iconTone: "text-blue-600",
  },
};

function ToastCard({ id, type, message }: { id: string; type: "success" | "error" | "info"; message: string }) {
  const { removeToast } = useToastActions();
  const [exiting, setExiting] = useState(false);

  useEffect(() => {
    const timer = window.setTimeout(() => setExiting(true), AUTO_DISMISS[type]);
    return () => window.clearTimeout(timer);
  }, [type]);

  useEffect(() => {
    if (!exiting) return;
    const timer = window.setTimeout(() => removeToast(id), 180);
    return () => window.clearTimeout(timer);
  }, [exiting, id, removeToast]);

  const tone = TOAST_TONES[type];

  return (
    <div
      role="status"
      aria-live="polite"
      className={`pointer-events-auto flex w-full max-w-sm items-start gap-3 rounded-2xl border px-4 py-3 shadow-lg backdrop-blur-md ${tone.bg} ${tone.border} ${exiting ? "opacity-0 translate-x-6" : "opacity-100 translate-x-0"} transition-all duration-200`}
      style={{ animation: exiting ? "toastOut 180ms ease forwards" : "toastIn 220ms ease forwards" }}
    >
      <div className={`mt-0.5 ${tone.iconTone}`}>{tone.icon}</div>
      <div className="min-w-0 flex-1 text-sm font-medium text-navy">{message}</div>
      <button
        type="button"
        onClick={() => setExiting(true)}
        className="rounded-full p-1 text-navy/45 transition-colors hover:bg-navy/5 hover:text-navy"
        aria-label="Dismiss toast"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  );
}

export default function Toast() {
  const { toasts } = useToastActions();

  return (
    <div className="pointer-events-none fixed right-4 top-4 z-[70] flex w-[calc(100vw-2rem)] max-w-sm flex-col gap-3 md:right-6 md:top-6">
      {toasts.map((toast) => (
        <ToastCard key={toast.id} {...toast} />
      ))}
    </div>
  );
}
