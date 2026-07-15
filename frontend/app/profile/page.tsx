"use client";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";

export default function ProfilePage() {
  const [me, setMe] = useState<any>(null);
  const [err, setErr] = useState<string | null>(null);
  const router = useRouter();
  useEffect(() => {
    if (typeof window !== "undefined" && !localStorage.getItem("accessToken")) { router.push("/login"); return; }
    api.me().then(setMe).catch((e) => setErr(e.message));
  }, [router]);

  return (
    <div className="mx-auto max-w-2xl px-4 md:px-6 py-8 md:py-16">
      <div className="eyebrow">Account</div>
      <h1 className="h1 mt-2">Your Profile</h1>
      {err && <div className="mt-4 text-red-600">{err}</div>}
      {me && (
        <div className="card p-5 md:p-8 mt-6 md:mt-8 space-y-3 md:space-y-4">
          <Row k="Name" v={me.name} />
          <Row k="Email" v={me.email} />
          <Row k="Role" v={me.role} />
          {me.createdAt && <Row k="Joined" v={new Date(me.createdAt).toLocaleString()} />}
        </div>
      )}
    </div>
  );
}
function Row({ k, v }: { k: string; v: string }) {
  return (
    <div className="flex justify-between border-b border-navy/10 pb-3 last:border-0 gap-4">
      <span className="text-navy/60 text-xs md:text-sm">{k}</span>
      <span className="text-navy font-medium text-sm md:text-base text-right">{v}</span>
    </div>
  );
}
