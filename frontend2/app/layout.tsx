import type { Metadata } from "next";
import "./globals.css";
import Navbar from "@/components/Navbar";

export const metadata: Metadata = {
  title: "EventHora — Premier Event Management",
  description: "Discover, register, and manage extraordinary events with EventHora.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="" />
        <link
          href="https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,600;0,700;1,700&family=Inter:wght@400;500;600;700&display=swap"
          rel="stylesheet"
        />
      </head>
      <body className="min-h-screen">
        <Navbar />
        <main>{children}</main>
        <footer className="mt-24 border-t border-navy/10 bg-white/60">
          <div className="mx-auto max-w-7xl px-6 py-8 flex flex-col md:flex-row justify-between gap-4 text-sm text-navy/60">
            <div className="font-display text-navy text-lg">EventHora</div>
            <div>© {new Date().getFullYear()} EventHora. All rights reserved.</div>
          </div>
        </footer>
      </body>
    </html>
  );
}
