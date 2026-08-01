"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { CheckCircle2, AlertTriangle, XCircle, ArrowRight, User, Calendar, Ticket, Clock, ShieldAlert } from "lucide-react";
import { requireStaffAuth } from "@/lib/auth";
import type { CheckInResponse } from "@/types/staff";

interface ErrorScanResult {
  isError: true;
  status: number;
  message: string;
  ticketReference: string;
  eventTitle?: string;
}

type ScanResultData = CheckInResponse | ErrorScanResult;

export default function ScanResultPage() {
  const router = useRouter();
  const [result, setResult] = useState<ScanResultData | null>(null);

  useEffect(() => {
    const session = requireStaffAuth();
    if (!session) {
      router.push("/login");
      return;
    }

    const raw = sessionStorage.getItem("scanResult");
    if (raw) {
      try {
        setResult(JSON.parse(raw));
      } catch {
        setResult(null);
      }
    }
  }, [router]);

  const handleScanNext = () => {
    sessionStorage.removeItem("scanResult");
    router.push("/staff/scan");
  };

  if (!result) {
    return (
      <div className="min-h-[calc(100vh-80px)] bg-cream py-12 px-4 flex items-center justify-center">
        <div className="card p-8 text-center max-w-md w-full space-y-4">
          <ShieldAlert className="h-10 w-10 text-navy/40 mx-auto" />
          <h2 className="font-display text-lg font-bold text-navy">No Scan Result Found</h2>
          <p className="text-sm text-navy/70">Please scan a ticket to view entry results.</p>
          <button onClick={handleScanNext} className="btn-primary w-full py-3">
            Go to Scanner
          </button>
        </div>
      </div>
    );
  }

  const isError = "isError" in result && result.isError;
  const isDuplicate = !isError && (result as CheckInResponse).alreadyCheckedIn;

  // Render Green, Yellow, or Red Screen
  return (
    <div className="bg-cream flex items-stretch justify-center h-[calc(100vh-8.5rem)] min-h-0 px-3 py-3 md:h-auto md:min-h-[calc(100vh-80px)] md:items-center md:px-6 md:py-8">
      <div className="mx-auto max-w-lg w-full flex flex-col min-h-0">
        {/* GREEN SCREEN — Check-in Successful */}
        {!isError && !isDuplicate && (
          <div className="card overflow-hidden border-2 border-green-300 bg-green-50/90 text-green-900 shadow-lg p-4 md:p-8 space-y-3 md:space-y-6 flex flex-col flex-1 min-h-0">
            <div className="text-center space-y-1.5 md:space-y-3 shrink-0">
              <div className="inline-grid h-16 w-16 md:h-20 md:w-20 place-items-center rounded-full bg-green-100 text-green-600 shadow-inner mx-auto">
                <CheckCircle2 className="h-9 w-9 md:h-12 md:w-12" />
              </div>
              <h1 className="font-display text-xl md:text-3xl font-bold text-green-900">
                Check-in Successful
              </h1>
              <span className="chip bg-green-200 text-green-800 text-xs font-semibold px-3 py-1">
                Admitted
              </span>
            </div>

            <div className="bg-white/80 backdrop-blur rounded-xl p-3.5 md:p-5 space-y-1.5 md:space-y-3 text-sm text-green-950 border border-green-200/60 shadow-sm flex-1 overflow-y-auto min-h-0">
              <div className="flex items-center justify-between py-1 border-b border-green-100">
                <span className="text-green-800/80 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider">
                  Ticket Reference
                </span>
                <span className="font-mono font-bold text-base">{result.ticketReference}</span>
              </div>
              {"memberId" in result && result.memberId && (
                <div className="flex items-center justify-between py-1 border-b border-green-100">
                  <span className="text-green-800/80 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider">
                    <User className="h-3.5 w-3.5" /> Member ID
                  </span>
                  <span className="font-semibold">{result.memberId}</span>
                </div>
              )}
              {"eventTitle" in result && result.eventTitle && (
                <div className="flex items-center justify-between py-1 border-b border-green-100">
                  <span className="text-green-800/80 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider">
                    <Calendar className="h-3.5 w-3.5" /> Event
                  </span>
                  <span className="font-semibold truncate max-w-[200px] text-right">{result.eventTitle}</span>
                </div>
              )}
              {"quantity" in result && (
                <div className="flex items-center justify-between py-1 border-b border-green-100">
                  <span className="text-green-800/80 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider">
                    <Ticket className="h-3.5 w-3.5" /> Tickets (Qty)
                  </span>
                  <span className="font-bold text-lg text-green-900">{result.quantity}</span>
                </div>
              )}
              {"checkedInAt" in result && result.checkedInAt && (
                <div className="flex items-center justify-between py-1">
                  <span className="text-green-800/80 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider">
                    <Clock className="h-3.5 w-3.5" /> Time
                  </span>
                  <span className="font-medium">
                    {new Date(result.checkedInAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                  </span>
                </div>
              )}
            </div>

            <button
              onClick={handleScanNext}
              className="btn-primary w-full py-3 md:py-4 text-base font-bold flex items-center justify-center gap-2 shadow-md hover:shadow-lg shrink-0"
            >
              Scan Next <ArrowRight className="h-5 w-5" />
            </button>
          </div>
        )}

        {/* YELLOW SCREEN — Already Checked In */}
        {!isError && isDuplicate && (
          <div className="card overflow-hidden border-2 border-amber-300 bg-amber-50/90 text-amber-900 shadow-lg p-4 md:p-8 space-y-3 md:space-y-6 flex flex-col flex-1 min-h-0">
            <div className="text-center space-y-1.5 md:space-y-3 shrink-0">
              <div className="inline-grid h-16 w-16 md:h-20 md:w-20 place-items-center rounded-full bg-amber-100 text-amber-600 shadow-inner mx-auto">
                <AlertTriangle className="h-9 w-9 md:h-12 md:w-12" />
              </div>
              <h1 className="font-display text-xl md:text-3xl font-bold text-amber-950">
                Already Checked In
              </h1>
              <p className="text-xs md:text-sm text-amber-800 font-medium max-w-sm mx-auto">
                {"message" in result ? result.message : "This ticket has already been scanned."}
              </p>
            </div>

            <div className="bg-white/80 backdrop-blur rounded-xl p-3.5 md:p-5 space-y-1.5 md:space-y-3 text-sm text-amber-950 border border-amber-200/60 shadow-sm flex-1 overflow-y-auto min-h-0">
              <div className="flex items-center justify-between py-1 border-b border-amber-100">
                <span className="text-amber-800/80 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider">
                  Ticket Reference
                </span>
                <span className="font-mono font-bold text-base">{result.ticketReference}</span>
              </div>
              {"memberId" in result && result.memberId && (
                <div className="flex items-center justify-between py-1 border-b border-amber-100">
                  <span className="text-amber-800/80 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider">
                    <User className="h-3.5 w-3.5" /> Member ID
                  </span>
                  <span className="font-semibold">{result.memberId}</span>
                </div>
              )}
              {"eventTitle" in result && result.eventTitle && (
                <div className="flex items-center justify-between py-1 border-b border-amber-100">
                  <span className="text-amber-800/80 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider">
                    <Calendar className="h-3.5 w-3.5" /> Event
                  </span>
                  <span className="font-semibold truncate max-w-[200px] text-right">{result.eventTitle}</span>
                </div>
              )}
              {"quantity" in result && (
                <div className="flex items-center justify-between py-1 border-b border-amber-100">
                  <span className="text-amber-800/80 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider">
                    <Ticket className="h-3.5 w-3.5" /> Tickets (Qty)
                  </span>
                  <span className="font-bold text-lg">{result.quantity}</span>
                </div>
              )}
              {"checkedInAt" in result && result.checkedInAt && (
                <div className="flex items-center justify-between py-1">
                  <span className="text-amber-800/80 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider">
                    <Clock className="h-3.5 w-3.5" /> First Scanned At
                  </span>
                  <span className="font-medium text-amber-900">
                    {new Date(result.checkedInAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                  </span>
                </div>
              )}
            </div>

            <div className="p-3 bg-amber-100/60 rounded-lg text-xs text-amber-900 text-center font-medium shrink-0">
              💡 Staff note: Please verify visually if this is the same attendee or shared ticket.
            </div>

            <button
              onClick={handleScanNext}
              className="btn-primary w-full py-3 md:py-4 text-base font-bold flex items-center justify-center gap-2 shadow-md hover:shadow-lg shrink-0"
            >
              Scan Next <ArrowRight className="h-5 w-5" />
            </button>
          </div>
        )}

        {/* RED SCREEN — Entry Denied */}
        {isError && (
          <div className="card overflow-hidden border-2 border-red-300 bg-red-50/90 text-red-950 shadow-lg p-4 md:p-8 space-y-3 md:space-y-6 flex flex-col flex-1 min-h-0">
            <div className="text-center space-y-1.5 md:space-y-3 shrink-0">
              <div className="inline-grid h-16 w-16 md:h-20 md:w-20 place-items-center rounded-full bg-red-100 text-red-600 shadow-inner mx-auto">
                <XCircle className="h-9 w-9 md:h-12 md:w-12" />
              </div>
              <h1 className="font-display text-xl md:text-3xl font-bold text-red-950">
                Entry Denied
              </h1>
            </div>

            <div className="bg-white/90 backdrop-blur rounded-xl p-3.5 md:p-5 space-y-1.5 md:space-y-3 text-sm text-red-950 border border-red-200 shadow-sm flex-1 overflow-y-auto min-h-0">
              <div className="flex items-center justify-between py-1 border-b border-red-100">
                <span className="text-red-800/80 text-xs font-semibold uppercase tracking-wider">
                  Ticket Reference
                </span>
                <span className="font-mono font-bold text-base">{result.ticketReference || "Unknown"}</span>
              </div>
              {result.eventTitle && (
                <div className="flex items-center justify-between py-1 border-b border-red-100">
                  <span className="text-red-800/80 text-xs font-semibold uppercase tracking-wider">
                    Event
                  </span>
                  <span className="font-semibold">{result.eventTitle}</span>
                </div>
              )}
              <div className="pt-2">
                <span className="text-xs font-bold uppercase tracking-wider text-red-700 block mb-1">
                  Reason / Error Message
                </span>
                <p className="font-medium text-sm text-red-900 bg-red-100/50 p-3 rounded-md">
                  {result.message}
                </p>
              </div>
            </div>

            <button
              onClick={handleScanNext}
              className="btn-primary w-full py-3 md:py-4 text-base font-bold flex items-center justify-center gap-2 shadow-md hover:shadow-lg shrink-0"
            >
              Scan Next <ArrowRight className="h-5 w-5" />
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
