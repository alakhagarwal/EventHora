import jsPDF from "jspdf";

export async function generateTicketPdf(params: {
  ticketReference: string;
  eventTitle: string;
  eventDate: string;
  startTime?: string;
  endTime?: string;
  venue: string;
  additionalVenueInfo?: string | null;
  contactPersonName?: string | null;
  contactPersonPhone?: string | null;
  quantity: number;
  totalAmount: number;
  paymentStatus: string;
  qrImageDataUrl: string;
}): Promise<void> {
  const doc = new jsPDF({ orientation: "portrait", unit: "pt", format: "a4" });
  const pw = 595.28, ph = 841.89, mx = 36;

  const navyR = 15, navyG = 27, navyB = 61;
  const creamR = 250, creamG = 247, creamB = 240;

  const isFree =
    params.paymentStatus === "FREE" ||
    params.paymentStatus === "COMPLIMENTARY";

  const formatEventDate = (d?: string) => {
    if (!d) return "";
    try {
      return new Date(d).toLocaleDateString("en-IN", {
        weekday: "long", year: "numeric", month: "long", day: "numeric",
      });
    } catch { return d; }
  };

  const formatTime = (t?: string) => {
    if (!t) return "";
    try {
      const [h, m] = t.split(":");
      const hr = parseInt(h, 10);
      const ampm = hr >= 12 ? "PM" : "AM";
      const h12 = hr % 12 || 12;
      return `${h12}:${m} ${ampm}`;
    } catch { return t; }
  };

  doc.setFillColor(creamR, creamG, creamB);
  doc.rect(0, 0, pw, ph, "F");

  const headerH = 90;
  doc.setFillColor(navyR, navyG, navyB);
  doc.rect(0, 0, pw, headerH, "F");
  doc.setFillColor(201, 168, 76);
  doc.rect(0, headerH, pw, 4, "F");

  doc.setFont("helvetica", "bold");
  doc.setFontSize(26);
  doc.setTextColor(255, 255, 255);
  doc.text("EVENTHORA", pw / 2, 40, { align: "center" });
  doc.setFontSize(10);
  doc.setFont("helvetica", "normal");
  doc.setTextColor(200, 200, 220);
  doc.text("Event Ticket", pw / 2, 60, { align: "center" });

  const cardX = mx, cardW = pw - mx * 2, cardY = headerH + 24, cardH = 640;
  doc.setFillColor(255, 255, 255);
  doc.setDrawColor(220, 220, 225);
  doc.setLineWidth(0.5);
  doc.roundedRect(cardX, cardY, cardW, cardH, 8, 8, "FD");

  let y = cardY + 28;
  const leftCol = cardX + 24, rightCol = cardX + cardW / 2 + 12;
  const labelColor: [number, number, number] = [140, 145, 160];
  const valueColor: [number, number, number] = [navyR, navyG, navyB];

  const drawField = (lx: number, ly: number, label: string, value: string) => {
    doc.setFontSize(8); doc.setFont("helvetica", "normal");
    doc.setTextColor(...labelColor); doc.text(label.toUpperCase(), lx, ly);
    doc.setFontSize(11); doc.setFont("helvetica", "bold");
    doc.setTextColor(...valueColor); doc.text(value || "—", lx, ly + 14);
  };

  drawField(leftCol, y, "Event", params.eventTitle);
  drawField(rightCol, y, "Date", formatEventDate(params.eventDate));
  y += 42;
  drawField(leftCol, y, "Time", `${formatTime(params.startTime)} – ${formatTime(params.endTime)}`);
  drawField(rightCol, y, "Venue", params.venue);
  y += 42;

  if (params.additionalVenueInfo) {
    doc.setFontSize(8);
    doc.setFont("helvetica", "normal");
    doc.setTextColor(...labelColor);
    doc.text("ADDITIONAL VENUE INFO", leftCol, y);
    doc.setFontSize(10);
    doc.setFont("helvetica", "normal");
    doc.setTextColor(...valueColor);
    doc.text(params.additionalVenueInfo, leftCol, y + 14, { maxWidth: cardW - 48 });
    y += 36;
  }

  y += 8;
  doc.setDrawColor(230, 230, 235);
  doc.setLineWidth(0.5);
  doc.line(cardX + 20, y, cardX + cardW - 20, y);
  y += 20;

  const qrSize = 240, qrX = (pw - qrSize) / 2;
  doc.setFillColor(255, 255, 255);
  doc.setDrawColor(navyR, navyG, navyB);
  doc.setLineWidth(2);
  doc.roundedRect(qrX - 6, y - 6, qrSize + 12, qrSize + 12, 6, 6, "FD");
  doc.addImage(params.qrImageDataUrl, "PNG", qrX, y, qrSize, qrSize);

  y += qrSize + 24;
  doc.setFont("helvetica", "bold");
  doc.setFontSize(16);
  doc.setTextColor(navyR, navyG, navyB);
  doc.text(params.ticketReference, pw / 2, y, { align: "center" });
  y += 28;

  doc.setDrawColor(230, 230, 235);
  doc.setLineWidth(0.5);
  doc.line(cardX + 20, y, cardX + cardW - 20, y);
  y += 24;

  const amountText = isFree
    ? "Free"
    : `Rs. ${Number(params.totalAmount ?? 0).toLocaleString("en-IN")}`;

  const drawDetail = (lx: number, ly: number, label: string, value: string) => {
    doc.setFontSize(8); doc.setFont("helvetica", "normal");
    doc.setTextColor(...labelColor); doc.text(label.toUpperCase(), lx, ly);
    doc.setFontSize(11); doc.setFont("helvetica", "bold");
    doc.setTextColor(...valueColor); doc.text(value || "—", lx, ly + 14);
  };

  drawDetail(leftCol, y, "Ticket Ref", params.ticketReference);
  drawDetail(rightCol, y, "Quantity", String(params.quantity));
  y += 42;
  drawDetail(leftCol, y, "Amount", amountText);
  drawDetail(rightCol, y, "Payment Status", params.paymentStatus);

  if (params.contactPersonName || params.contactPersonPhone) {
    y += 42;
    const contactParts = [params.contactPersonName, params.contactPersonPhone]
      .filter(Boolean)
      .join(" · ");
    drawDetail(leftCol, y, "Event Contact", contactParts);
  }

  const footerH = 52;
  doc.setFillColor(navyR, navyG, navyB);
  doc.rect(0, ph - footerH, pw, footerH, "F");
  doc.setFontSize(9);
  doc.setFont("helvetica", "normal");
  doc.setTextColor(255, 255, 255);
  doc.text("Present this QR code at the entry gate for check-in.", pw / 2, ph - footerH / 2 + 4, { align: "center" });

  doc.save(`EventHora-Ticket-${params.ticketReference}.pdf`);
}
