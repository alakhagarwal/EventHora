"use client";

import { useEffect, useState, useRef } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Camera, QrCode, ArrowLeft, RefreshCw, DollarSign, Gift, AlertCircle, Loader2 } from "lucide-react";
import { api } from "@/lib/api";
import { requireStaffAuth } from "@/lib/auth";

interface PendingPaymentInfo {
  ticketReference: string;
  amount: number;
  memberId: string;
  eventTitle: string;
  quantity: number;
  message: string;
}

export default function StaffScanPage() {
  const router = useRouter();
  const [selectedEvent, setSelectedEvent] = useState<{ id: string; title: string } | null>(null);
  const [ticketInput, setTicketInput] = useState("");
  const [processing, setProcessing] = useState(false);
  const [inlineError, setInlineError] = useState<string | null>(null);
  const [pendingPayment, setPendingPayment] = useState<PendingPaymentInfo | null>(null);
  const [scannerKey, setScannerKey] = useState(0);
  const [cameraError, setCameraError] = useState<string | null>(null);

  useEffect(() => {
    const session = requireStaffAuth();
    if (!session) {
      router.push("/login");
      return;
    }

    const raw = sessionStorage.getItem("selectedEvent");
    if (raw) {
      try {
        setSelectedEvent(JSON.parse(raw));
      } catch {
        setSelectedEvent(null);
      }
    }
  }, [router]);

  // Initialize html5-qrcode scanner
  useEffect(() => {
    let html5QrcodeScanner: any = null;

    const timer = setTimeout(async () => {
      try {
        const { Html5QrcodeScanner } = await import("html5-qrcode");
        const element = document.getElementById("qr-reader");
        if (element && !pendingPayment) {
          html5QrcodeScanner = new Html5QrcodeScanner(
            "qr-reader",
            {
              fps: 10,
              qrbox: { width: 250, height: 250 },
              aspectRatio: 1.0,
            },
            /* verbose= */ false
          );

          html5QrcodeScanner.render(
            (decodedText: string) => {
              if (decodedText) {
                html5QrcodeScanner.clear().catch(() => {});
                handleProcessTicket(decodedText.trim());
              }
            },
            (error: any) => {
              // Ignore standard frame scanning failures
            }
          );

          // Style the library's internal buttons/controls after render
          setTimeout(() => {
            const container = document.getElementById("qr-reader");
            if (!container) return;

            // Style scan/dashboard section buttons
            const buttons = container.querySelectorAll("button, a, select");
            buttons.forEach((btn: Element) => {
              const el = btn as HTMLElement;
              el.style.border = "none";
              el.style.borderRadius = "0";
              el.style.padding = "8px 14px";
              el.style.fontSize = "13px";
              el.style.fontWeight = "500";
              el.style.transition = "all 0.15s ease";
              el.style.background = "transparent";
              // Underline key action buttons
              const text = el.textContent?.toLowerCase() || "";
              if (text.includes("scan") || text.includes("start") || text.includes("choose") || text.includes("camera") || text.includes("upload")) {
                el.style.textDecoration = "underline";
                el.style.textUnderlineOffset = "3px";
              }
            });

            // Style dashboard section panels — no borders
            const sections = container.querySelectorAll(
              "#qr-reader__dashboard_section, #qr-reader__dashboard_section_csr, #qr-reader__dashboard_section_fsr"
            );
            sections.forEach((sec: Element) => {
              const el = sec as HTMLElement;
              el.style.border = "none";
              el.style.borderRadius = "0";
              el.style.padding = "12px";
              el.style.marginBottom = "8px";
            });

            // Style the file input area
            const fileInput = container.querySelector("#qr-reader__dashboard_section_fsr input[type='file']");
            if (fileInput) {
              const el = fileInput as HTMLElement;
              el.style.padding = "8px";
              el.style.fontSize = "13px";
              el.style.border = "none";
              el.style.borderRadius = "0";
              el.style.width = "100%";
            }

            // Style select (camera dropdown)
            const selects = container.querySelectorAll("select");
            selects.forEach((sel: Element) => {
              const el = sel as HTMLElement;
              el.style.border = "none";
              el.style.borderRadius = "0";
              el.style.padding = "8px 12px";
              el.style.fontSize = "13px";
              el.style.width = "100%";
              el.style.background = "transparent";
            });
          }, 500);
        }
      } catch (err: any) {
        console.error("Failed to initialize scanner:", err);
        setCameraError("Camera permission denied or camera unavailable. Please use manual input below.");
      }
    }, 200);

    return () => {
      clearTimeout(timer);
      if (html5QrcodeScanner) {
        html5QrcodeScanner.clear().catch(() => {});
      }
    };
  }, [scannerKey, pendingPayment]);

  const handleProcessTicket = async (ticketRef: string) => {
    ticketRef = ticketRef.replace(/^EVTHORA:/i, "");
    if (!ticketRef) return;
    setProcessing(true);
    setInlineError(null);

    try {
      // HTTP 200 OK — Successful check-in or duplicate scan
      const res = await api.checkIn({ ticketReference: ticketRef });
      sessionStorage.setItem("scanResult", JSON.stringify(res));
      router.push("/staff/scan/result");
    } catch (err: any) {
      const status = err?.status;
      const msg = err?.message || "Check-in request failed.";
      const data = err?.data;

      // Branch on HTTP status
      if (status === 409) {
        // Detect if 409 Conflict is due to Pay-at-Gate payment collection required
        const isPaymentRequired =
          msg.toLowerCase().includes("payment collection") ||
          msg.toLowerCase().includes("pay-at-gate") ||
          data?.paymentStatus === "PAY_AT_GATE";

        if (isPaymentRequired) {
          setPendingPayment({
            ticketReference: ticketRef,
            amount: data?.totalAmount ?? 0,
            memberId: data?.memberId ?? "Member",
            eventTitle: data?.eventTitle ?? selectedEvent?.title ?? "Event",
            quantity: data?.quantity ?? 1,
            message: msg,
          });
          setProcessing(false);
          return;
        }
      }

      // For 404 Not Found, 409 PENDING/FAILED, or any other error status
      const errorResult = {
        isError: true,
        status: status || 500,
        message: msg,
        ticketReference: ticketRef,
        eventTitle: selectedEvent?.title || "Event",
      };
      sessionStorage.setItem("scanResult", JSON.stringify(errorResult));
      router.push("/staff/scan/result");
    }
  };


  const handleManualSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!ticketInput.trim()) return;
    handleProcessTicket(ticketInput.trim().toUpperCase());
  };

  const handleRecordPayment = async (action: "PAID" | "COMPLIMENTARY") => {
    if (!pendingPayment) return;
    setProcessing(true);
    setInlineError(null);

    try {
      const res = await api.recordGatePayment({
        ticketReference: pendingPayment.ticketReference,
        action,
      });
      sessionStorage.setItem("scanResult", JSON.stringify(res));
      router.push("/staff/scan/result");
    } catch (err: any) {
      setInlineError(err.message || "Failed to record payment. Please try again.");
      setProcessing(false);
    }
  };

  const restartScanner = () => {
    setPendingPayment(null);
    setInlineError(null);
    setTicketInput("");
    setScannerKey((prev) => prev + 1);
  };

  return (
    <div className="min-h-[calc(100vh-80px)] bg-cream py-6 px-4 md:px-6">
      <div className="mx-auto max-w-xl">
        {/* Top Header */}
        <div className="flex items-center justify-between mb-4">
          <Link
            href="/staff"
            className="flex items-center gap-1.5 text-xs md:text-sm text-navy/70 hover:text-navy font-medium"
          >
            <ArrowLeft className="h-4 w-4" /> Change Event
          </Link>
          {selectedEvent && (
            <span className="chip-gold text-xs font-semibold truncate max-w-[200px]">
              {selectedEvent.title}
            </span>
          )}
        </div>

        <div className="text-center mb-6">
          <h1 className="h2 text-navy">QR Code Scanner</h1>
          <p className="text-xs md:text-sm text-navy/70 mt-1">
            Point camera at member QR code or manually enter ticket reference
          </p>
        </div>

        {/* Inline Error banner */}
        {inlineError && (
          <div className="mb-4 card p-4 bg-red-50 border-red-200 text-red-800 flex items-start gap-3 text-sm">
            <AlertCircle className="h-5 w-5 shrink-0 text-red-600 mt-0.5" />
            <div className="flex-1">
              <p className="font-semibold">Action Failed</p>
              <p className="mt-0.5">{inlineError}</p>
            </div>
          </div>
        )}

        {/* Processing Indicator */}
        {processing && (
          <div className="card p-8 text-center my-6 flex flex-col items-center justify-center space-y-3">
            <Loader2 className="h-8 w-8 text-navy animate-spin" />
            <p className="font-semibold text-navy">Processing Check-In...</p>
          </div>
        )}

        {/* Payment Collection Sub-Flow (PAY_AT_GATE) */}
        {!processing && pendingPayment && (
          <div className="card p-6 border-2 border-gold bg-white space-y-6 shadow-md">
            <div className="text-center">
              <span className="chip-gold text-xs uppercase font-bold tracking-wider">Payment Collection Required</span>
              <h2 className="font-display text-2xl font-bold text-navy mt-2">Pay-at-Gate Ticket</h2>
              <p className="text-xs text-navy/70 mt-1">{pendingPayment.message}</p>
            </div>

            <div className="bg-cream rounded-lg p-4 space-y-2 text-sm text-navy">
              <div className="flex justify-between">
                <span className="text-navy/70">Ticket Reference:</span>
                <span className="font-mono font-bold">{pendingPayment.ticketReference}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-navy/70">Member ID:</span>
                <span className="font-semibold">{pendingPayment.memberId}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-navy/70">Event:</span>
                <span className="font-semibold">{pendingPayment.eventTitle}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-navy/70">Tickets (Qty):</span>
                <span className="font-semibold">{pendingPayment.quantity}</span>
              </div>
              <div className="pt-2 border-t border-navy/10 flex justify-between items-center">
                <span className="font-bold text-navy">Total Amount Due:</span>
                <span className="font-display text-2xl text-navy font-bold">
                  ₹{pendingPayment.amount}
                </span>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
              <button
                onClick={() => handleRecordPayment("PAID")}
                className="btn-primary flex items-center justify-center gap-2 py-3"
              >
                <DollarSign className="h-4 w-4" /> Confirm Cash Payment
              </button>
              <button
                onClick={() => handleRecordPayment("COMPLIMENTARY")}
                className="btn-outline flex items-center justify-center gap-2 py-3 border-navy text-navy hover:bg-navy/5"
              >
                <Gift className="h-4 w-4 text-gold" /> Mark Complimentary
              </button>
            </div>

            <div className="text-center pt-2">
              <button
                onClick={restartScanner}
                className="text-xs text-navy/60 hover:text-navy underline"
              >
                Cancel and Scan Next
              </button>
            </div>
          </div>
        )}

        {/* Main Camera + Manual Input View */}
        {!processing && !pendingPayment && (
          <div className="space-y-6">
            {/* Camera View Card */}
            <div className="card p-4 overflow-hidden bg-white text-center">
              <div className="flex items-center justify-between mb-3 px-2">
                <div className="flex items-center gap-2 text-sm font-semibold text-navy">
                  <Camera className="h-4 w-4 text-gold" /> Camera Scanner
                </div>
                <button
                  onClick={restartScanner}
                  className="text-xs text-navy/60 hover:text-navy flex items-center gap-1"
                  title="Restart Camera"
                >
                  <RefreshCw className="h-3 w-3" /> Reset Camera
                </button>
              </div>

              {cameraError ? (
                <div className="p-6 bg-cream rounded-lg text-xs text-navy/70">
                  <AlertCircle className="h-6 w-6 text-amber-500 mx-auto mb-2" />
                  <p>{cameraError}</p>
                </div>
              ) : (
                <div className="relative min-h-[260px] bg-navy/5 rounded-lg overflow-hidden flex items-center justify-center">
                  <div id="qr-reader" className="w-full" />
                </div>
              )}
            </div>

            {/* Manual Fallback Input */}
            <div className="card p-6 bg-white space-y-4">
              <div className="flex items-center gap-2 text-sm font-semibold text-navy">
                <QrCode className="h-4 w-4 text-gold" /> Manual Reference Entry
              </div>
              <form onSubmit={handleManualSubmit} className="space-y-3">
                <div>
                  <input
                    type="text"
                    value={ticketInput}
                    onChange={(e) => setTicketInput(e.target.value.toUpperCase())}
                    placeholder="Enter ticket reference (e.g. TKT-2026-AB12CD)"
                    className="input font-mono uppercase tracking-wider text-center"
                    maxLength={20}
                  />
                </div>
                <button
                  type="submit"
                  disabled={!ticketInput.trim()}
                  className="btn-primary w-full py-3 disabled:opacity-50"
                >
                  Check In Member
                </button>
              </form>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
